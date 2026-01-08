package com.soccer.server;

import com.soccer.common.Constants;
import com.soccer.common.GameState;
import com.soccer.common.InputPacket;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

// This class is the engine of the game. It runs on the server and handles physics, AI, and game rules.
public class GameRoom implements Runnable {
    private boolean isRunning = true;
    private final GameState gameState = new GameState();

    // Requirement (f): We are using a specific Lock interface (ReentrantLock)
    // instead of the 'synchronized' keyword to manage thread safety manually.
    private final Lock lock = new ReentrantLock();

    // Thread-safe map to store player inputs coming from different clients
    public ConcurrentHashMap<Integer, InputPacket> inputs = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, Long> kickCooldowns = new ConcurrentHashMap<>();

    // Ball physics variables
    private double ballVx = 0, ballVy = 0;

    // Timer variables
    private long startTime = 0;
    private long countdownStart;
    private long totalPausedTime = 0;
    private int lastTouchPlayerId = -1;

    // Game settings
    private static final double VIRTUAL_TIME_MULTIPLIER = 10.0; // Game runs 10x faster than real life
    private static final double PLAYER_SPEED_BASE = 3.0;
    private static final int MAX_PLAYERS = 22; // 11 vs 11

    // Formation positions relative to center field
    // Index 0 is the Goalkeeper
    private static final double[][] FORMATION = {
            {450, 0},    // Index 0: GK
            {200, -150}, {200, 150}, {250, -50}, {250, 50}, // Defenders
            {100, -200}, {100, 200}, {50, -80}, {50, 80},   // Midfielders
            {-100, -50}, {-100, 50}                         // Strikers
    };

    public GameRoom() {
        resetBall();
        // Fix: We forced the weather to be SUNNY because the rain effect was annoying.
        gameState.weather = "SUNNY";
    }

    // Checks if a client is allowed to join (valid name, server not full)
    public String checkJoinRequest(String name) {
        lock.lock(); // Lock the room so we don't count players wrong
        try {
            long humanCount = gameState.players.stream().filter(p -> !p.isBot).count();
            if (humanCount >= MAX_PLAYERS) return "Server is Full (Max " + MAX_PLAYERS + ")";

            // Check for duplicate names
            for (GameState.PlayerState p : gameState.players) {
                if (!p.isBot && p.name.equalsIgnoreCase(name)) return "Name '" + name + "' is already taken!";
            }
            return "OK";
        } finally {
            lock.unlock(); // Always unlock in finally block
        }
    }

    public void addPlayer(int id, String name) {
        lock.lock();
        try {
            // Figure out which team needs a player (Red or Blue)
            long redCount = gameState.players.stream().filter(p -> "RED".equals(p.team)).count();
            long blueCount = gameState.players.stream().filter(p -> "BLUE".equals(p.team)).count();
            String team = (redCount <= blueCount) ? "RED" : "BLUE";

            // Fix: Logic to kick out a Bot if a real Human joins
            // We find a bot on the target team and remove it to make space
            Optional<GameState.PlayerState> botToRemove = gameState.players.stream()
                    .filter(p -> p.isBot && p.team.equals(team))
                    .findFirst();

            if (botToRemove.isPresent()) {
                System.out.println("[Room] Replacing bot " + botToRemove.get().name + " with real player " + name);
                gameState.players.remove(botToRemove.get());
            }

            // Add the real player
            gameState.players.add(new GameState.PlayerState(id, name, team, 0, 0, false));
            System.out.println("[Room] Player joined: " + name + " (" + id + ") Team: " + team);

            // Fix: Immediately update positions.
            // Without this, new players get stuck at (0,0) until the match starts.
            resetPositions();

        } finally {
            lock.unlock();
        }
    }

    public void removePlayer(int id) {
        lock.lock();
        try {
            // Remove player from list and clear their inputs
            gameState.players.removeIf(p -> p.id == id);
            inputs.remove(id);
            System.out.println("[Room] Player " + id + " left.");

            // If everyone leaves (only bots left), reset the whole room
            if (gameState.players.stream().noneMatch(p -> !p.isBot)) {
                resetGameRoom();
            }
        } finally {
            lock.unlock();
        }
    }

    // Called when Admin clicks "Start Match"
    public void startGame() {
        lock.lock();
        try {
            // Fix: Allow Admin to restart the game if it is currently in GAME_OVER state
            if (gameState.currentPhase == GameState.Phase.GAME_OVER) {
                System.out.println("[Room] Admin requested restart. Resetting...");
                resetGameRoom();
            }

            // Only start if we are currently waiting
            if (gameState.currentPhase != GameState.Phase.WAITING) return;

            System.out.println("[Room] Match Started!");

            // 1. Remove old bots
            gameState.players.removeIf(p -> p.isBot);
            // 2. Add new bots to fill teams to 11
            fillWithBots();
            // 3. Move everyone to formation
            resetPositions();
            // 4. Start 3-second countdown
            startCountdown();
        } finally {
            lock.unlock();
        }
    }

    // Resets the match state (score, time, ball)
    private void resetGameRoom() {
        gameState.currentPhase = GameState.Phase.WAITING;
        gameState.players.removeIf(p -> p.isBot); // Kick all bots
        gameState.scoreRed = 0; gameState.scoreBlue = 0;
        gameState.timeString = "00:00"; gameState.winner = "";

        // Ensure weather stays Sunny
        gameState.weather = "SUNNY";

        resetBall();
        inputs.clear();
        kickCooldowns.clear();
    }

    // Fills empty slots with AI bots until we have 11 players per team
    private void fillWithBots() {
        long redCount = gameState.players.stream().filter(p -> "RED".equals(p.team)).count();
        long blueCount = gameState.players.stream().filter(p -> "BLUE".equals(p.team)).count();

        // Add Red bots
        for (int i = (int) redCount; i < 11; i++)
            gameState.players.add(new GameState.PlayerState(-100 - i, "Bot_R" + (i+1), "RED", 0, 0, true));

        // Add Blue bots
        for (int i = (int) blueCount; i < 11; i++)
            gameState.players.add(new GameState.PlayerState(-200 - i, "Bot_B" + (i+1), "BLUE", 0, 0, true));
    }

    private void resetPositions() {
        // Reset ball to center
        resetBall();

        List<GameState.PlayerState> redTeam = gameState.players.stream().filter(p -> "RED".equals(p.team)).collect(Collectors.toList());
        List<GameState.PlayerState> blueTeam = gameState.players.stream().filter(p -> "BLUE".equals(p.team)).collect(Collectors.toList());

        // Sorting Logic: We put Bots first in the list so they get assigned the Goalkeeper spot (Index 0).
        // Humans get assigned field positions.
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

        // Assign coordinates based on formation array
        for (int i = 0; i < redTeam.size(); i++) assignPos(redTeam.get(i), "RED", i);
        for (int i = 0; i < blueTeam.size(); i++) assignPos(blueTeam.get(i), "BLUE", i);
    }

    // Helper to calculate X/Y based on formation index
    private void assignPos(GameState.PlayerState p, String team, int index) {
        int formIdx = index % FORMATION.length;
        double[] offset = FORMATION[formIdx];
        double cx = Constants.WIDTH / 2.0;
        double cy = Constants.HEIGHT / 2.0;

        // Red team on left, Blue team on right (mirrored)
        if ("RED".equals(team)) { p.x = cx - offset[0]; p.y = cy + offset[1]; }
        else { p.x = cx + offset[0]; p.y = cy + offset[1]; }

        p.startX = p.x; p.startY = p.y;
        p.isGoalKeeper = (formIdx == 0); // First spot is GK
    }

    // Main Game Loop
    @Override
    public void run() {
        long lastTime = System.currentTimeMillis();
        while (isRunning) {
            long now = System.currentTimeMillis();
            double dt = (now - lastTime) / 1000.0; // Delta time in seconds
            lastTime = now;

            // Handle commands like START/END/APPROVE
            processSystemCommands();

            if (gameState.currentPhase == GameState.Phase.COUNTDOWN) {
                updateCountdown();
            } else if (gameState.currentPhase == GameState.Phase.PLAYING) {
                updatePhysics(dt); // Move players
                updateAI(dt);      // Move bots
                updateTime();      // Update clock
            }

            // Cap at roughly 60 FPS
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

    // Handles movement for human players
    private void updatePhysics(double dt) {
        for (GameState.PlayerState p : gameState.players) {
            if (p.isBot || !p.isApproved) continue;
            InputPacket input = inputs.get(p.id);
            if (input != null) {
                double dx = 0, dy = 0;
                if (input.up) dy = -1; if (input.down) dy = 1;
                if (input.left) dx = -1; if (input.right) dx = 1;

                // Normalize diagonal movement
                if (dx != 0 || dy != 0) {
                    double len = Math.sqrt(dx * dx + dy * dy);
                    dx /= len; dy /= len;
                }

                // Sprint check
                double speed = input.sprint ? PLAYER_SPEED_BASE * 1.5 : PLAYER_SPEED_BASE;
                p.x += dx * speed; p.y += dy * speed;

                // Keep player inside screen
                p.x = Math.max(0, Math.min(Constants.WIDTH, p.x));
                p.y = Math.max(0, Math.min(Constants.HEIGHT, p.y));

                if (input.shoot) kickBall(p);
            }
        }

        // Ball friction and bounce logic
        gameState.ballX += ballVx; gameState.ballY += ballVy;
        ballVx *= 0.98; ballVy *= 0.98;
        if (gameState.ballY <= 0 || gameState.ballY >= Constants.HEIGHT) ballVy = -ballVy;
        if (gameState.ballX <= 0 || gameState.ballX >= Constants.WIDTH) ballVx = -ballVx;

        checkGoal();
    }

    private void updateAI(double dt) {
        // Requirement (g): Using parallelStream here allows us to calculate AI logic
        // for multiple bots simultaneously using different CPU cores.
        gameState.players.parallelStream()
                .filter(p -> p.isBot)
                .forEach(p -> calculateSingleBotLogic(p));
    }

    // The brain of the Bot
    private void calculateSingleBotLogic(GameState.PlayerState p) {
        double targetX = p.startX, targetY = p.startY;
        double distToBall = Math.hypot(p.x - gameState.ballX, p.y - gameState.ballY);

        // Goalkeeper logic: stay near goal unless ball is close
        if (p.isGoalKeeper) {
            if (distToBall < 150 && Math.abs(p.x - p.startX) < 120) {
                targetX = gameState.ballX; targetY = gameState.ballY;
            } else {
                targetX = p.startX;
                targetY = Math.max(Constants.HEIGHT/2.0 - 50, Math.min(Constants.HEIGHT/2.0 + 50, gameState.ballY));
            }
        } else {
            // Field player logic: Chase ball if close, otherwise stay in formation
            if (distToBall < 250) {
                targetX = gameState.ballX; targetY = gameState.ballY;
            } else {
                // Move slightly towards ball even if far away
                targetX = p.startX + (gameState.ballX - p.startX) * 0.2;
                targetY = p.startY + (gameState.ballY - p.startY) * 0.2;
            }
        }

        // Move the bot
        double dx = targetX - p.x, dy = targetY - p.y;
        double dist = Math.hypot(dx, dy);
        if (dist > 5) {
            double speed = PLAYER_SPEED_BASE * 0.95;
            if (distToBall < 250) speed *= 1.2; // Sprint if chasing ball
            p.x += (dx / dist) * speed; p.y += (dy / dist) * speed;
        }

        // Bot shooting logic
        if (distToBall < 20) {
            double goalX = "RED".equals(p.team) ? Constants.WIDTH : 0;
            double angle = Math.atan2((Constants.HEIGHT / 2.0) + (Math.random()-0.5)*80 - p.y, goalX - p.x);
            double power = 7.0 + Math.random() * 3.0;

            // Sync needed here because we are writing to shared ball variables
            synchronized(this) {
                ballVx = Math.cos(angle) * power; ballVy = Math.sin(angle) * power;
                lastTouchPlayerId = p.id;
            }
        }
    }

    // Handles kicking the ball
    private void kickBall(GameState.PlayerState p) {
        long now = System.currentTimeMillis();
        // Simple cooldown so you can't kick 60 times a second
        if (kickCooldowns.containsKey(p.id) && now - kickCooldowns.get(p.id) < 500) return;

        double dist = Math.hypot(p.x - gameState.ballX, p.y - gameState.ballY);
        if (dist < 30) {
            double angle = Math.atan2(gameState.ballY - p.y, gameState.ballX - p.x);
            double power = 10.0;
            ballVx = Math.cos(angle) * power; ballVy = Math.sin(angle) * power;
            lastTouchPlayerId = p.id;
            kickCooldowns.put(p.id, now);
        }
    }

    // Checks if the ball went into the goal
    private void checkGoal() {
        double goalTop = (Constants.HEIGHT / 2.0) - (Constants.GOAL_SIZE / 2.0);
        double goalBottom = (Constants.HEIGHT / 2.0) + (Constants.GOAL_SIZE / 2.0);
        boolean isGoal = false;

        if (gameState.ballY > goalTop && gameState.ballY < goalBottom) {
            if (gameState.ballX < 5) {
                gameState.scoreBlue++; isGoal = true;
            } else if (gameState.ballX > Constants.WIDTH - 5) {
                gameState.scoreRed++; isGoal = true;
            }
        }

        if (isGoal) {
            // Award goal to player
            if (lastTouchPlayerId != -1) {
                for(GameState.PlayerState p : gameState.players) if (p.id == lastTouchPlayerId) p.goals++;
            }
            // Reset for kick-off
            resetPositions();
            try { Thread.sleep(1000); } catch(Exception e){}
        }
    }

    private void resetBall() { gameState.ballX = Constants.WIDTH / 2.0; gameState.ballY = Constants.HEIGHT / 2.0; ballVx = 0; ballVy = 0; }

    private void startCountdown() {
        gameState.currentPhase = GameState.Phase.COUNTDOWN;
        gameState.countdownValue = 3;
        countdownStart = System.currentTimeMillis();
    }

    private void updateCountdown() {
        long diff = System.currentTimeMillis() - countdownStart;
        gameState.countdownValue = 3 - (int)(diff/1000);
        if (gameState.countdownValue <= 0) {
            gameState.currentPhase = GameState.Phase.PLAYING;
            startTime = System.currentTimeMillis();
            totalPausedTime = 0;
        }
    }

    // Fix: We now stop the game exactly at 90 minutes. No auto-restart.
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

        // Save to DB (Optional)
        // DatabaseManager.saveMatch(gameState.winner, gameState.scoreRed, gameState.scoreBlue);
    }

    public GameState getGameState() { return gameState; }
}