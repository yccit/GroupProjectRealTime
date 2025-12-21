package com.soccergame.common.model;

import com.soccergame.common.util.GameUtils;
import java.io.Serializable;

public class GameState implements Serializable {
    // Screen Dimensions (Shared so client knows how big to draw)
    public static final int WIDTH = 800;
    public static final int HEIGHT = 600;

    // Player Dimensions
    public static final int PLAYER_WIDTH = 20;
    public static final int PLAYER_HEIGHT = 100;
    public static final int BALL_SIZE = 20;

    // Ball Position & Velocity
    public double ballX = WIDTH / 2.0;
    public double ballY = HEIGHT / 2.0;
    public double ballSpeedX = 4.0;
    public double ballSpeedY = 4.0;

    // Player Positions (Y-axis only)
    public int player1Y = (HEIGHT / 2) - (PLAYER_HEIGHT / 2);
    public int player2Y = (HEIGHT / 2) - (PLAYER_HEIGHT / 2);

    // Scores
    public int score1 = 0;
    public int score2 = 0;

    /**
     * Updates a player's position safely within the screen bounds.
     * @param playerID 1 or 2
     * @param deltaY Amount to move (negative = up, positive = down)
     */
    public synchronized void movePlayer(int playerID, int deltaY) {
        if (playerID == 1) {
            player1Y += deltaY;
            // Use Utility function to keep player inside screen
            player1Y = GameUtils.clamp(player1Y, 0, HEIGHT - PLAYER_HEIGHT - 40);
        } else if (playerID == 2) {
            player2Y += deltaY;
            player2Y = GameUtils.clamp(player2Y, 0, HEIGHT - PLAYER_HEIGHT - 40);
        }
    }

    /**
     * Serializes the state to a String for network transmission.
     * Format: "ballX,ballY,p1Y,p2Y,score1,score2"
     */
    @Override
    public synchronized String toString() {
        return String.format("%.2f,%.2f,%d,%d,%d,%d",
                ballX, ballY, player1Y, player2Y, score1, score2);
    }

    /**
     * Parsons a string back into this GameState object (Used by Client).
     */
    public synchronized void updateFromString(String data) {
        try {
            String[] parts = data.split(",");
            if (parts.length >= 6) {
                this.ballX = Double.parseDouble(parts[0]);
                this.ballY = Double.parseDouble(parts[1]);
                this.player1Y = Integer.parseInt(parts[2]);
                this.player2Y = Integer.parseInt(parts[3]);
                this.score1 = Integer.parseInt(parts[4]);
                this.score2 = Integer.parseInt(parts[5]);
            }
        } catch (Exception e) {
            System.err.println("Error parsing game state: " + e.getMessage());
        }
    }
}