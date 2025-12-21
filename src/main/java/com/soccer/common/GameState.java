package com.soccer.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class GameState implements Serializable {
    // 移除 HALFTIME，保留 COUNTDOWN 用于进球后的缓冲
    public enum Phase { WAITING, COUNTDOWN, PLAYING, GAME_OVER }

    public Phase currentPhase = Phase.WAITING;
    public int countdownValue = 3;

    public double ballX, ballY;
    public int scoreRed = 0, scoreBlue = 0;
    public String timeString = "00:00";
    public String winner = "";
    public String weather = "SUNNY";

    public List<PlayerState> players = new ArrayList<>();

    public static class PlayerState implements Serializable {
        public int id;
        public String name;
        public String team;
        public double x, y;
        public double stamina;
        public boolean isBot;
        public double startX, startY;
        public boolean isGoalKeeper;
        public int goals = 0; // ★ 新增：个人进球数

        public PlayerState(int id, String name, String team, double x, double y, boolean isBot) {
            this.id = id; this.name = name; this.team = team;
            this.x = x; this.y = y;
            this.startX = x; this.startY = y;
            this.stamina = 100.0;
            this.isBot = isBot;
            this.isGoalKeeper = false;
            this.goals = 0;
        }
    }
}