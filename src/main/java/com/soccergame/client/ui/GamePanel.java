package com.soccergame.client.ui;

import com.soccergame.client.core.GameState;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * GamePanel handles the visual rendering of the game using JavaFX Canvas.
 * It does not handle game logic; it purely renders the data present in GameState.
 */
public class GamePanel extends Pane {
    private final GameState gameState;
    private final Canvas canvas;
    private final GraphicsContext gc; // The tool used to draw on the canvas

    // Fixed screen dimensions
    private final int WIDTH = 800;
    private final int HEIGHT = 600;

    /**
     * Constructor initializes the Canvas and links the GameState.
     * @param gameState The shared data object containing positions and environment data.
     */
    public GamePanel(GameState gameState) {
        this.gameState = gameState;

        // Initialize Canvas with specific width and height
        this.canvas = new Canvas(WIDTH, HEIGHT);
        this.gc = canvas.getGraphicsContext2D();

        // Add the canvas to this JavaFX Panels it is visible
        this.getChildren().add(canvas);
    }

    /**
     * The main rendering method.
     * This method should be called inside the main AnimationTimer loop.
     * It clears the screen and redraws everything based on the current GameState.
     */
    public void render() {
        // 1. Clear the entire screen before drawing the new frame
        gc.clearRect(0, 0, WIDTH, HEIGHT);

        // 2. Draw Sky (Background)
        // The color is dynamically retrieved from GameState (controlled by EnvironmentManager)
        gc.setFill(gameState.getSkyColor());
        gc.fillRect(0, 0, WIDTH, HEIGHT);

        // 3. Draw the Pitch (Grass)
        gc.setFill(Color.FORESTGREEN);
        gc.fillRect(50, 100, 700, 400);

        // 4. Draw Center Line
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(3);
        gc.strokeLine(400, 100, 400, 500);

        // 5. Draw the Ball
        gc.setFill(Color.WHITE);
        // Draw the ball using coordinates from GameState
        gc.fillOval(gameState.getBallX(), gameState.getBallY(), 20, 20);

        // 6. Draw HUD (Heads-Up Display)
        drawHUD();
    }

    /**
     * Helper method to draw text elements like Score and Status.
     */
    private void drawHUD() {
        gc.setFill(Color.BLACK);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 30));

        // Draw Team A Score
        gc.fillText("Team A: " + gameState.getScoreA(), 50, 50);

        // Draw Team B Score
        gc.fillText("Team B: " + gameState.getScoreB(), 600, 50);

        // Visual indicator for Night time
        if (gameState.getSkyColor().equals(Color.MIDNIGHTBLUE)) {
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("Arial", 15));
            gc.fillText("Time: Night Mode", 360, 50);
        }
    }
}