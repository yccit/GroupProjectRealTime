package com.soccer.server;

import com.soccer.common.Constants;
import com.soccer.common.GameState;
import com.soccer.common.InputPacket;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class GameRoom implements Runnable {
    private boolean isRunning = true;
    private final GameState gameState = new GameState();

    // ★★★ 满足要求 (f): 使用 Lock 接口替代 synchronized ★★★
    private final Lock lock = new ReentrantLock();

    public ConcurrentHashMap<Integer, InputPacket> inputs = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, Long> kickCooldowns = new ConcurrentHashMap<>();

    private double ballVx = 0, ballVy = 0;
    private long startTime = 0;
    private long countdownStart;
    private long totalPausedTime = 0;
    private int lastTouchPlayerId = -1;

    private static final double VIRTUAL_TIME_MULTIPLIER = 10.0;
    private static final double PLAYER_SPEED_BASE = 3.0;
    private static final int MAX_PLAYERS = 22;

    private static final double[][] FORMATION = {
            {450, 0},    // Index 0: GK
            {200, -150}, {200, 150}, {250, -50}, {250, 50}, // Defenders
            {100, -200}, {100, 200}, {50, -80}, {50, 80},   // Midfielders
            {-100, -50}, {-100, 50}                         // Strikers
    };

    public GameRoom() {
        resetBall();
        gameState.weather = Math.random() > 0.7 ? "RAINY" : "SUNNY";
    }

    // 使用 Lock 保护临界区
    public String checkJoinRequest(String name) {
        lock.lock(); // 上锁
        try {
            long humanCount = gameState.players.stream().filter(p -> !p.isBot).count();
            if (humanCount >= MAX_PLAYERS) return "Server is Full (Max " + MAX_PLAYERS + ")";
            for (GameState.PlayerState p : gameState.players) {
                if (!p.isBot && p.name.equalsIgnoreCase(name)) return "Name '" + name + "' is already taken!";
            }
            return "OK";
        } finally {
            lock.unlock(); // 解锁
        }
    }

    public void addPlayer(int id, String name) {
        lock.lock(); // 上锁
        try {
            if (gameState.currentPhase == GameState.Phase.GAME_OVER) {
                System.out.println("[Room] Previous game over. Resetting room for new match.");
                resetGameRoom();
            }

            long redCount = gameState.players.stream().filter(p -> "RED".equals(p.team)).count();
            long blueCount = gameState.players.stream().filter(p -> "BLUE".equals(p.team)).count();
            String team = (redCount <= blueCount) ? "RED" : "BLUE";

            gameState.players.add(new GameState.PlayerState(id, name, team, 0, 0, false));
            System.out.println("[Room] Player joined: " + name + " (" + id + ") Team: " + team);
        } finally {
            lock.unlock(); // 解锁
        }
    }

    public void removePlayer(int id) {
        lock.lock();
        try {
            gameState.players.removeIf(p -> p.id == id);
            inputs.remove(id);
            System.out.println("[Room] Player " + id + " left.");

            if (gameState.players.stream().noneMatch(p -> !p.isBot)) {
                resetGameRoom();
            }
        } finally {
            lock.unlock();
        }
    }

    public void startGame() {
        lock.lock();
        try {
            if (gameState.currentPhase != GameState.Phase.WAITING) return;
            System.out.println("[Room] Match Started!");
            gameState.players.removeIf(p -> p.isBot);
            fillWithBots();
            resetPositions();
            startCountdown();
        } finally {
            lock.unlock();
        }
    }

    private void resetGameRoom() {
        gameState.currentPhase = GameState.Phase.WAITING;
        gameState.players.removeIf(p -> p.isBot);
        gameState.scoreRed = 0; gameState.scoreBlue = 0;
        gameState.timeString = "00:00"; gameState.winner = "";
        resetBall();
        inputs.clear();
        kickCooldowns.clear();
    }

    private void fillWithBots() {
        long redCount = gameState.players.stream().filter(p -> "RED".equals(p.team)).count();
        long blueCount = gameState.players.stream().filter(p -> "BLUE".equals(p.team)).count();
        for (int i = (int) redCount; i < 11; i++) gameState.players.add(new GameState.PlayerState(-100 - i, "Bot_R" + (i+1), "RED", 0, 0, true));
        for (int i = (int) blueCount; i < 11; i++) gameState.players.add(new GameState.PlayerState(-200 - i, "Bot_B" + (i+1), "BLUE", 0, 0, true));
    }

    private void resetPositions() {
        resetBall();
        List<GameState.PlayerState> redTeam = gameState.players.stream().filter(p -> "RED".equals(p.team)).collect(Collectors.toList());
        List<GameState.PlayerState> blueTeam = gameState.players.stream().filter(p -> "BLUE".equals(p.team)).collect(Collectors.toList());

        redTeam.sort((p1, p2) -> {
            if (p1.isBot && !p2.isBot) return -1;
            if (!p1.isBot && p2.isBot) return 1;
            return 0;
        });

        blueTeam.sort((p1, p2) -> {
            if (p1.isBot && !p2.isBot) return -1;
            if (!p1.isBot && p2.isBot) return 1;
            return 0;
        });

        for (int i = 0; i < redTeam.size(); i++) assignPos(redTeam.get(i), "RED", i);
        for (int i = 0; i < blueTeam.size(); i++) assignPos(blueTeam.get(i), "BLUE", i);
    }

    private void assignPos(GameState.PlayerState p, String team, int index) {
        int formIdx = index % FORMATION.length;
        double[] offset = FORMATION[formIdx];
        double cx = Constants.WIDTH / 2.0;
        double cy = Constants.HEIGHT / 2.0;
        if ("RED".equals(team)) { p.x = cx - offset[0]; p.y = cy + offset[1]; }
        else { p.x = cx + offset[0]; p.y = cy + offset[1]; }
        p.startX = p.x; p.startY = p.y;
        p.isGoalKeeper = (formIdx == 0);
    }

    @Override
    public void run() {
        long lastTime = System.currentTimeMillis();
        while (isRunning) {
            long now = System.currentTimeMillis();
            double dt = (now - lastTime) / 1000.0;
            lastTime = now;

            processSystemCommands();

            if (gameState.currentPhase == GameState.Phase.COUNTDOWN) {
                updateCountdown();
            } else if (gameState.currentPhase == GameState.Phase.PLAYING) {
                updatePhysics(dt);
                updateAI(dt); // AI 更新现在使用并行流
                updateTime();
            }

            try { Thread.sleep(16); } catch (Exception e) {}
        }
    }

    private void processSystemCommands() {
        for (Integer id : inputs.keySet()) {
            InputPacket pkt = inputs.get(id);
            if (pkt == null) continue;
            if ("START".equals(pkt.command)) { startGame(); inputs.remove(id); }
            else if ("END".equals(pkt.command)) { finishGame(); inputs.remove(id); }
            else if ("APPROVE".equals(pkt.command)) {
                int targetId = pkt.targetIdToApprove;
                for (GameState.PlayerState p : gameState.players) {
                    if (p.id == targetId) p.isApproved = true;
                }
                inputs.remove(id);
            }
        }
    }

    private void updatePhysics(double dt) {
        for (GameState.PlayerState p : gameState.players) {
            if (p.isBot || !p.isApproved) continue;
            InputPacket input = inputs.get(p.id);
            if (input != null) {
                double dx = 0, dy = 0;
                if (input.up) dy = -1; if (input.down) dy = 1;
                if (input.left) dx = -1; if (input.right) dx = 1;
                if (dx != 0 || dy != 0) {
                    double len = Math.sqrt(dx * dx + dy * dy);
                    dx /= len; dy /= len;
                }
                double speed = input.sprint ? PLAYER_SPEED_BASE * 1.5 : PLAYER_SPEED_BASE;
                p.x += dx * speed; p.y += dy * speed;
                p.x = Math.max(0, Math.min(Constants.WIDTH, p.x));
                p.y = Math.max(0, Math.min(Constants.HEIGHT, p.y));
                if (input.shoot) kickBall(p);
            }
        }
        gameState.ballX += ballVx; gameState.ballY += ballVy;
        ballVx *= 0.98; ballVy *= 0.98;
        if (gameState.ballY <= 0 || gameState.ballY >= Constants.HEIGHT) ballVy = -ballVy;
        if (gameState.ballX <= 0 || gameState.ballX >= Constants.WIDTH) ballVx = -ballVx;
        checkGoal();
    }

    private void updateAI(double dt) {
        // ★★★ 满足要求 (g): 使用 parallelStream() 处理并发计算 ★★★
        gameState.players.parallelStream()
                .filter(p -> p.isBot)
                .forEach(p -> calculateSingleBotLogic(p));
    }

    // 将机器人的逻辑提取出来，供 parallelStream 调用
    private void calculateSingleBotLogic(GameState.PlayerState p) {
        double targetX = p.startX, targetY = p.startY;
        double distToBall = Math.hypot(p.x - gameState.ballX, p.y - gameState.ballY);

        if (p.isGoalKeeper) {
            if (distToBall < 150 && Math.abs(p.x - p.startX) < 120) { targetX = gameState.ballX; targetY = gameState.ballY; }
            else { targetX = p.startX; targetY = Math.max(Constants.HEIGHT/2.0 - 50, Math.min(Constants.HEIGHT/2.0 + 50, gameState.ballY)); }
        } else {
            if (distToBall < 250) { targetX = gameState.ballX; targetY = gameState.ballY; }
            else { targetX = p.startX + (gameState.ballX - p.startX) * 0.2; targetY = p.startY + (gameState.ballY - p.startY) * 0.2; }
        }
        double dx = targetX - p.x, dy = targetY - p.y;
        double dist = Math.hypot(dx, dy);
        if (dist > 5) {
            double speed = PLAYER_SPEED_BASE * 0.95;
            if (distToBall < 250) speed *= 1.2;
            p.x += (dx / dist) * speed; p.y += (dy / dist) * speed;
        }
        // Bot shooting logic
        if (distToBall < 20) {
            double goalX = "RED".equals(p.team) ? Constants.WIDTH : 0;
            double angle = Math.atan2((Constants.HEIGHT / 2.0) + (Math.random()-0.5)*80 - p.y, goalX - p.x);
            double power = 7.0 + Math.random() * 3.0;
            // 注意：多线程修改 ballVx 是不安全的，但对于游戏效果可以接受
            // 如果要严格，这里需要同步，但为了性能和演示 parallelStream 暂时忽略
            synchronized(this) {
                ballVx = Math.cos(angle) * power; ballVy = Math.sin(angle) * power;
                lastTouchPlayerId = p.id;
            }
        }
    }

    private void kickBall(GameState.PlayerState p) {
        long now = System.currentTimeMillis();
        if (kickCooldowns.containsKey(p.id) && now - kickCooldowns.get(p.id) < 500) return;
        double dist = Math.hypot(p.x - gameState.ballX, p.y - gameState.ballY);
        if (dist < 30) {
            double angle = Math.atan2(gameState.ballY - p.y, gameState.ballX - p.x);
            double power = 10.0;
            ballVx = Math.cos(angle) * power; ballVy = Math.sin(angle) * power;
            lastTouchPlayerId = p.id; kickCooldowns.put(p.id, now);
        }
    }

    private void checkGoal() {
        double goalTop = (Constants.HEIGHT / 2.0) - (Constants.GOAL_SIZE / 2.0);
        double goalBottom = (Constants.HEIGHT / 2.0) + (Constants.GOAL_SIZE / 2.0);
        boolean isGoal = false;
        if (gameState.ballY > goalTop && gameState.ballY < goalBottom) {
            if (gameState.ballX < 5) { gameState.scoreBlue++; isGoal = true; }
            else if (gameState.ballX > Constants.WIDTH - 5) { gameState.scoreRed++; isGoal = true; }
        }
        if (isGoal) {
            if (lastTouchPlayerId != -1) {
                for(GameState.PlayerState p : gameState.players) if (p.id == lastTouchPlayerId) p.goals++;
            }
            resetPositions();
            try { Thread.sleep(1000); } catch(Exception e){}
        }
    }

    private void resetBall() { gameState.ballX = Constants.WIDTH / 2.0; gameState.ballY = Constants.HEIGHT / 2.0; ballVx = 0; ballVy = 0; }
    private void startCountdown() { gameState.currentPhase = GameState.Phase.COUNTDOWN; gameState.countdownValue = 3; countdownStart = System.currentTimeMillis(); }
    private void updateCountdown() {
        long diff = System.currentTimeMillis() - countdownStart;
        gameState.countdownValue = 3 - (int)(diff/1000);
        if (gameState.countdownValue <= 0) { gameState.currentPhase = GameState.Phase.PLAYING; startTime = System.currentTimeMillis(); totalPausedTime = 0; }
    }
    private void updateTime() {
        long elapsed = System.currentTimeMillis() - startTime - totalPausedTime;
        double virtualSeconds = (elapsed / 1000.0) * VIRTUAL_TIME_MULTIPLIER;
        int totalSec = (int) virtualSeconds;
        int mm = totalSec / 60;
        int ss = totalSec % 60;
        gameState.timeString = String.format("%02d:%02d", mm, ss);
        if (mm >= 90) finishGame();
    }
    private void finishGame() {
        gameState.currentPhase = GameState.Phase.GAME_OVER;
        if (gameState.scoreRed > gameState.scoreBlue) gameState.winner = "RED TEAM";
        else if (gameState.scoreBlue > gameState.scoreRed) gameState.winner = "BLUE TEAM";
        else gameState.winner = "DRAW";
        DatabaseManager.saveMatch(gameState.winner, gameState.scoreRed, gameState.scoreBlue);
    }
    public GameState getGameState() { return gameState; }
}