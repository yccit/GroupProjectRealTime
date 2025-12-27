package com.soccer.server;

import com.soccer.common.Constants;
import com.soccer.common.GameState;
import com.soccer.common.InputPacket;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class GameRoom implements Runnable {
    private boolean isRunning = true;
    private GameState gameState = new GameState();
    public ConcurrentHashMap<Integer, InputPacket> inputs = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Integer, Long> kickCooldowns = new ConcurrentHashMap<>();

    private double ballVx = 0, ballVy = 0;
    private long startTime;
    private long countdownStart;
    private long totalPausedTime = 0; // 仅用于倒计时暂停，不再有中场休息

    // ★ 新增：记录最后触球的玩家ID，用于判定进球算谁的
    private int lastTouchPlayerId = -1;

    // 8分钟 = 480秒
    private static final double GAME_DURATION_REAL_SEC = 480;
    // 虚拟时间倍率：480秒 * 11.25 = 5400秒 = 90分钟
    private static final double VIRTUAL_TIME_MULTIPLIER = 11.25;

    private static final double PLAYER_SPEED_BASE = 2.8;

    public GameRoom() {
        gameState.ballX = Constants.WIDTH / 2.0;
        gameState.ballY = Constants.HEIGHT / 2.0;
        gameState.weather = Math.random() > 0.7 ? "RAINY" : "SUNNY";
    }

    public synchronized void addPlayer(int id, String name) {
        if (gameState.currentPhase != GameState.Phase.WAITING) return;
        long redCount = gameState.players.stream().filter(p -> "RED".equals(p.team)).count();
        long blueCount = gameState.players.stream().filter(p -> "BLUE".equals(p.team)).count();
        String team = (redCount <= blueCount) ? "RED" : "BLUE";
        gameState.players.add(new GameState.PlayerState(id, name, team, 0, 0, false));
    }

    public synchronized void startGame() {
        if (gameState.currentPhase != GameState.Phase.WAITING) return;
        if (gameState.players.size() < 1) return;

        System.out.println("[Room] Starting...");
        fillWithBots();
        resetPositions();
        startCountdown();
    }

    private void startCountdown() {
        gameState.currentPhase = GameState.Phase.COUNTDOWN;
        gameState.countdownValue = 3;
        countdownStart = System.currentTimeMillis();
    }

    private void fillWithBots() {
        int redCount = (int) gameState.players.stream().filter(p -> p.team.equals("RED")).count();
        int blueCount = (int) gameState.players.stream().filter(p -> p.team.equals("BLUE")).count();
        for (int i = redCount; i < 11; i++) gameState.players.add(new GameState.PlayerState(-100 - i, "Bot_R" + i, "RED", 0, 0, true));
        for (int i = blueCount; i < 11; i++) gameState.players.add(new GameState.PlayerState(-200 - i, "Bot_B" + i, "BLUE", 0, 0, true));
    }

    private void resetPositions() {
        assignTeamPositions("RED");
        assignTeamPositions("BLUE");
        gameState.ballX = Constants.WIDTH / 2.0;
        gameState.ballY = Constants.HEIGHT / 2.0;
        ballVx = 0; ballVy = 0;
        kickCooldowns.clear();
        lastTouchPlayerId = -1; // 重置触球人
    }

    private void assignTeamPositions(String team) {
        List<GameState.PlayerState> teamPlayers = gameState.players.stream()
                .filter(p -> p.team.equals(team)).collect(Collectors.toList());
        List<GameState.PlayerState> humans = new ArrayList<>();
        List<GameState.PlayerState> bots = new ArrayList<>();
        for (GameState.PlayerState p : teamPlayers) {
            if (p.isBot) bots.add(p); else humans.add(p);
        }

        GameState.PlayerState gk;
        if (!bots.isEmpty()) gk = bots.remove(0);
        else gk = humans.remove(0);
        setupPlayerPos(gk, team, 0);

        int[] priorityPositions = {9, 10, 5, 6, 7, 8, 1, 2, 3, 4};
        int priorityIdx = 0;
        for (GameState.PlayerState h : humans) setupPlayerPos(h, team, priorityPositions[priorityIdx++]);
        for (GameState.PlayerState b : bots) setupPlayerPos(b, team, priorityPositions[priorityIdx++]);
    }

    private void setupPlayerPos(GameState.PlayerState p, String team, int index) {
        double[] offset = Constants.FORMATION_OFFSETS[index];
        double cx = Constants.WIDTH / 2.0; double cy = Constants.HEIGHT / 2.0;
        p.x = team.equals("RED") ? cx + offset[0] : cx - offset[0];
        p.y = cy + offset[1];
        p.isGoalKeeper = (index == 0);
        p.startX = p.x; p.startY = p.y;
        p.stamina = 100.0;
    }

    @Override
    public void run() {
        long lastTime = System.currentTimeMillis();
        while (isRunning) {
            long now = System.currentTimeMillis();
            double dt = (now - lastTime) / 1000.0;
            lastTime = now;

            if (gameState.currentPhase == GameState.Phase.COUNTDOWN) {
                updateCountdown();
            } else if (gameState.currentPhase == GameState.Phase.PLAYING) {
                // ★ NEW: 每一帧都检查有没有人按了 End Game
                checkEndGameCommand();

                // 如果检查完发现游戏结束了，就跳过下面的物理更新
                if (gameState.currentPhase == GameState.Phase.GAME_OVER) {
                    try { Thread.sleep(16); } catch (Exception e) {}
                    continue;
                }

                updatePhysics(dt);
                updateAI(dt);
                updateTime();
            }

            try { Thread.sleep(16); } catch (Exception e) {}
        }
    }

    // ★ NEW: 专门检查 "END" 指令的方法
    private void checkEndGameCommand() {
        for (InputPacket packet : inputs.values()) {
            if (packet != null && "END".equals(packet.command)) {
                System.out.println("[Room] Received END GAME command!");
                finishGame(); // 立即结束游戏
                break;
            }
        }
    }

    // ★ NEW: 统一的结束游戏逻辑 (无论是时间到还是按钮点击，都走这里)
    private void finishGame() {
        if (gameState.currentPhase == GameState.Phase.GAME_OVER) return; // 防止重复执行

        gameState.currentPhase = GameState.Phase.GAME_OVER;

        // 判定赢家
        if (gameState.scoreRed > gameState.scoreBlue) gameState.winner = "RED TEAM";
        else if (gameState.scoreBlue > gameState.scoreRed) gameState.winner = "BLUE TEAM";
        else gameState.winner = "DRAW";

        // 保存到数据库
        DatabaseManager.saveMatch(gameState.winner, gameState.scoreRed, gameState.scoreBlue);
        System.out.println("[Room] Game Finished. Winner: " + gameState.winner);
    }

    private void updateCountdown() {
        long diff = System.currentTimeMillis() - countdownStart;
        int secondsPassed = (int) (diff / 1000);
        gameState.countdownValue = 3 - secondsPassed;

        if (gameState.countdownValue <= 0) {
            gameState.currentPhase = GameState.Phase.PLAYING;
            if (startTime == 0) startTime = System.currentTimeMillis();
            else totalPausedTime += diff; // 进球后的倒计时算作暂停
        }
    }

    private void updatePhysics(double dt) {
        gameState.ballX += ballVx; gameState.ballY += ballVy;
        ballVx *= 0.98; ballVy *= 0.98;

        if (gameState.ballY < 0 || gameState.ballY > Constants.HEIGHT) ballVy *= -1;

        // ★ 进球判定与个人进球统计
        if (gameState.ballX < 0) {
            // 蓝队得分
            gameState.scoreBlue++;
            recordGoal("BLUE"); // 记录是谁进的
            resetPositions();
            startCountdown();
        } else if (gameState.ballX > Constants.WIDTH) {
            // 红队得分
            gameState.scoreRed++;
            recordGoal("RED"); // 记录是谁进的
            resetPositions();
            startCountdown();
        }

        resolvePlayerCollisions();

        for (GameState.PlayerState p : gameState.players) {
            if (p.isBot) continue;
            InputPacket input = inputs.get(p.id);
            double speed = getSpeedBasedOnStamina(p.stamina);
            double dx = 0, dy = 0;
            boolean moving = false, action = false;

            if (input != null) {
                if (input.up) dy = -speed; if (input.down) dy = speed;
                if (input.left) dx = -speed; if (input.right) dx = speed;
                if (dx != 0 || dy != 0) moving = true;

                if (input.shoot && canKick(p)) {
                    kickBall(p, input.left ? -1 : (input.right ? 1 : (p.team.equals("RED")?1:-1)),
                            input.up ? -1 : (input.down ? 1 : 0), 14.0);
                    action = true;
                }
            }
            movePlayer(p, dx, dy);
            consumeStamina(p, moving, false, action);
        }
    }

    // ★ 记录进球方法
    private void recordGoal(String scoringTeam) {
        // 找到最后触球的玩家
        GameState.PlayerState scorer = gameState.players.stream()
                .filter(p -> p.id == lastTouchPlayerId)
                .findFirst().orElse(null);

        // 如果找到了，且他是进球方的人 (避免乌龙球算分)，给他加一分
        if (scorer != null && scorer.team.equals(scoringTeam)) {
            scorer.goals++;
            System.out.println("Goal scored by " + scorer.name);
        }
    }

    private void resolvePlayerCollisions() {
        double radius = Constants.PLAYER_RADIUS;
        double minDist = radius * 2.2;
        for (int i = 0; i < gameState.players.size(); i++) {
            GameState.PlayerState p1 = gameState.players.get(i);
            for (int j = i + 1; j < gameState.players.size(); j++) {
                GameState.PlayerState p2 = gameState.players.get(j);
                double dx = p1.x - p2.x; double dy = p1.y - p2.y;
                double distSq = dx*dx + dy*dy;
                if (distSq < minDist * minDist && distSq > 0) {
                    double dist = Math.sqrt(distSq);
                    double overlap = minDist - dist;
                    double pushX = (dx / dist) * (overlap / 2.0);
                    double pushY = (dy / dist) * (overlap / 2.0);
                    p1.x += pushX; p1.y += pushY; p2.x -= pushX; p2.y -= pushY;
                }
            }
        }
    }

    private boolean canKick(GameState.PlayerState p) {
        if (dist(p.x, p.y, gameState.ballX, gameState.ballY) > 35) return false;
        long now = System.currentTimeMillis();
        long lastKick = kickCooldowns.getOrDefault(p.id, 0L);
        if (now - lastKick > 400) {
            kickCooldowns.put(p.id, now);
            return true;
        }
        return false;
    }

    private void updateAI(double dt) {
        GameState.PlayerState nearestRed = findNearest(gameState.ballX, gameState.ballY, "RED");
        GameState.PlayerState nearestBlue = findNearest(gameState.ballX, gameState.ballY, "BLUE");

        for (GameState.PlayerState p : gameState.players) {
            if (!p.isBot) continue;

            double speed = getSpeedBasedOnStamina(p.stamina);
            double dx = 0, dy = 0;
            boolean isMoving = false;
            boolean isAction = false;

            if (p.isGoalKeeper) {
                double targetY = gameState.ballY;
                if (targetY < Constants.HEIGHT/2 - 150) targetY = Constants.HEIGHT/2 - 150;
                if (targetY > Constants.HEIGHT/2 + 150) targetY = Constants.HEIGHT/2 + 150;
                if (p.y < targetY - 10) dy = speed * 0.7;
                else if (p.y > targetY + 10) dy = -speed * 0.7;

                if (canKick(p) && dist(p.x, p.y, gameState.ballX, gameState.ballY) < 50) {
                    kickBall(p, p.team.equals("RED") ? 1 : -1, (Math.random()-0.5), 15.0);
                    isAction = true;
                }
            } else {
                boolean isMeNearest = (p == nearestRed || p == nearestBlue);
                double distToBall = dist(p.x, p.y, gameState.ballX, gameState.ballY);
                double enemyGoalX = p.team.equals("RED") ? Constants.WIDTH : 0;

                if (isMeNearest || distToBall < 100) {
                    if (distToBall < 25) {
                        if (canKick(p)) {
                            double angle = Math.atan2(Constants.HEIGHT/2.0 - p.y, enemyGoalX - p.x);
                            kickBall(p, Math.cos(angle), Math.sin(angle), 13.0);
                            isAction = true;
                        }
                    } else {
                        double angle = Math.atan2(gameState.ballY - p.y, gameState.ballX - p.x);
                        dx = Math.cos(angle) * speed;
                        dy = Math.sin(angle) * speed;
                    }
                } else {
                    double shiftX = (gameState.ballX - Constants.WIDTH/2) * 0.4;
                    double targetX = p.startX + shiftX;
                    if (dist(p.x, p.y, targetX, p.startY) > 30) {
                        double angle = Math.atan2(p.startY - p.y, targetX - p.x);
                        dx = Math.cos(angle) * speed * 0.7;
                        dy = Math.sin(angle) * speed * 0.7;
                    }
                }
            }
            if (Math.abs(dx) > 0.1 || Math.abs(dy) > 0.1) isMoving = true;
            movePlayer(p, dx, dy);
            consumeStamina(p, isMoving, false, isAction);
        }
    }

    private GameState.PlayerState findNearest(double bx, double by, String team) {
        return gameState.players.stream().filter(p -> p.team.equals(team) && !p.isGoalKeeper)
                .min(Comparator.comparingDouble(p -> dist(p.x, p.y, bx, by))).orElse(null);
    }

    private void movePlayer(GameState.PlayerState p, double dx, double dy) {
        p.x += dx; p.y += dy;
        if (p.x < 0) p.x = 0; if (p.x > Constants.WIDTH) p.x = Constants.WIDTH;
        if (p.y < 0) p.y = 0; if (p.y > Constants.HEIGHT) p.y = Constants.HEIGHT;
    }

    private void updateTime() {
        // 直接计算时间，不处理 halftime
        long elapsed = (System.currentTimeMillis() - startTime - totalPausedTime) / 1000;

        if (elapsed >= GAME_DURATION_REAL_SEC) {
            // ★ 时间到了，也调用同一个结束逻辑
            finishGame();
        }

        long virtualSeconds = (long) (elapsed * VIRTUAL_TIME_MULTIPLIER);
        // 限制最大显示为 90:00
        if (virtualSeconds > 5400) virtualSeconds = 5400;
        gameState.timeString = String.format("%02d:%02d", virtualSeconds / 60, virtualSeconds % 60);
    }

    private double getSpeedBasedOnStamina(double stamina) {
        double base = PLAYER_SPEED_BASE;
        if (stamina > 80) return base;
        if (stamina > 60) return base * 0.85;
        if (stamina > 40) return base * 0.70;
        if (stamina > 20) return base * 0.55;
        return base * 0.40;
    }

    private void consumeStamina(GameState.PlayerState p, boolean isMoving, boolean isDribbling, boolean isAction) {
        double drain = (gameState.weather.equals("RAINY") ? 0.004 : 0.002);
        if (isAction) drain += 2.0;
        else if (isMoving) drain += 0.008;
        p.stamina = Math.max(0, p.stamina - drain);
        if (!isMoving && !isAction) p.stamina = Math.min(100, p.stamina + 0.03);
    }

    // ★ 踢球方法更新：记录触球人
    private void kickBall(GameState.PlayerState p, double dirX, double dirY, double power) {
        ballVx = dirX * power; ballVy = dirY * power;
        lastTouchPlayerId = p.id; // 记录最后触球ID
    }

    private double dist(double x1, double y1, double x2, double y2) {
        return Math.sqrt(Math.pow(x2-x1, 2) + Math.pow(y2-y1, 2));
    }
    public GameState getGameState() { return gameState; }
}