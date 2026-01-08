package com.soccer.common;

public class Constants {
    // Network config
    public static final int PORT = 8888;

    // Window dimensions
    public static final int WIDTH = 1200;
    public static final int HEIGHT = 800;

    // Physics and Gameplay settings
    public static final double BASE_SPEED = 5.0;
    public static final double PLAYER_RADIUS = 15.0;
    public static final double BALL_RADIUS = 10.0;
    public static final double GOAL_SIZE = 200.0;

    // Formation Setup (Standard 4-4-2, offsets relative to the center)
    // We only define the Red team here; the Blue team will just mirror these positions
    // Format: {x, y}
    public static final double[][] FORMATION_OFFSETS = {
            {-550, 0},   // Goalkeeper (Index 0)
            {-400, -200}, {-400, -70}, {-400, 70}, {-400, 200}, // Defenders (1-4)
            {-200, -200}, {-200, -70}, {-200, 70}, {-200, 200}, // Midfielders (5-8)
            {-50, -100}, {-50, 100}  // Strikers (9-10)
    };
}