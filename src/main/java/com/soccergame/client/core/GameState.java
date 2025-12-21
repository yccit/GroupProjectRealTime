package com.soccergame.client.core;

import javafx.scene.paint.Color;

/**
 * GameState (Mock Version for GUI Development).
 * This class acts as the central data store for the client-side visualization.
 * It holds the positions, scores, and environmental data required for rendering.
 * * Note: 'volatile' is used to ensure visibility across different threads
 * (Network Thread, Logic Thread, and JavaFX Application Thread).
 */
public class GameState {
    // Ball position coordinates
    private volatile double ballX = 400.0;
    private volatile double ballY = 300.0;

    // Team scores
    private volatile int scoreA = 0;
    private volatile int scoreB = 0;

    // Environmental state (Sky color), controlled by EnvironmentManager
    // Default is Light Blue (Day)
    private volatile Color skyColor = Color.LIGHTBLUE;

    // --- Getters ---
    public double getBallX() { return ballX; }
    public double getBallY() { return ballY; }
    public int getScoreA() { return scoreA; }
    public int getScoreB() { return scoreB; }
    public Color getSkyColor() { return skyColor; }

    // --- Setters ---

    /**
     * Updates the ball's position.
     * @param x The new X coordinate.
     * @param y The new Y coordinate.
     */
    public void setBallPosition(double x, double y) {
        this.ballX = x;
        this.ballY = y;
    }

    /**
     * Updates the sky color to simulate day/night cycles.
     * @param color The new sky color.
     */
    public void setSkyColor(Color color) {
        this.skyColor = color;
    }

    /**
     * Updates the scores for both teams.
     * @param scoreA Score for Team A.
     * @param scoreB Score for Team B.
     */
    public void setScore(int scoreA, int scoreB) {
        this.scoreA = scoreA;
        this.scoreB = scoreB;
    }
}