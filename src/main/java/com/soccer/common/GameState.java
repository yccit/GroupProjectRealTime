package com.soccer.common;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class GameState implements Serializable {
    // 1. Serialization Version ID to prevent network errors
    private static final long serialVersionUID = 1L;

    public enum Phase { WAITING, COUNTDOWN, PLAYING, GAME_OVER }

    public Phase currentPhase = Phase.WAITING;
    public int countdownValue = 3;

    public double ballX, ballY;
    public int scoreRed = 0, scoreBlue = 0;
    public String timeString = "00:00";
    public String winner = "";
    public String weather = "SUNNY";

    // 2. CONCURRENCY: Use CopyOnWriteArrayList to handle redundant users/concurrent access safely
    public List<PlayerState> players = new CopyOnWriteArrayList<>();

    public static class PlayerState implements Serializable {
        private static final long serialVersionUID = 1L;

        public int id;
        public String name;
        public String team;
        public double x, y;
        public double stamina;
        public boolean isBot;
        public double startX, startY;
        public boolean isGoalKeeper;
        public int goals = 0;

        // 3. ADMIN APPROVAL: Default false for humans
        public boolean isApproved = false;

        public PlayerState(int id, String name, String team, double x, double y, boolean isBot) {
            this.id = id;
            this.name = name;
            this.team = team;
            this.x = x;
            this.y = y;
            this.startX = x;
            this.startY = y;
            this.stamina = 100.0;
            this.isBot = isBot;
            this.isGoalKeeper = false;
            this.goals = 0;

            // Bots are automatically approved to play; Humans must wait for Admin
            this.isApproved = isBot;
        }
    }
}