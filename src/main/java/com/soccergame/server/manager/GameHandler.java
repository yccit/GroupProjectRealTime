package com.soccergame.server.manager;

import java.util.Random;
import com.soccergame.server.database.DatabaseManager;

public class GameHandler implements Runnable {
    private ClientManager clientManager;
    private boolean isRunning = true;

    // --- GAME STATE (The "Real Life" Data) ---
    // Field Dimensions (match these to your Client UI size)
    private final int WIDTH = 800;
    private final int HEIGHT = 500;

    // Ball Variables
    private double ballX = WIDTH / 2.0;
    private double ballY = HEIGHT / 2.0;
    private double ballSpeedX = 4.0;
    private double ballSpeedY = 4.0;

    // Player Positions (Y-axis only for simplicity, like Pong Soccer)
    public static int player1Y = 200; // Left Player
    public static int player2Y = 200; // Right Player
    private final int PADDLE_HEIGHT = 80;

    // Score
    private int score1 = 0;
    private int score2 = 0;

    public GameHandler(ClientManager clientManager) {
        this.clientManager = clientManager;
    }

    @Override
    public void run() {
        System.out.println("[GameHandler] Soccer Simulation Started.");
        long lastTime = System.nanoTime();
        double nsPerTick = 1000000000.0 / 60.0; // 60 FPS
        double delta = 0;

        while (isRunning) {
            long now = System.nanoTime();
            delta += (now - lastTime) / nsPerTick;
            lastTime = now;

            if (delta >= 1) {
                tick(); // Calculate Physics
                delta--;
            }
            try { Thread.sleep(2); } catch (InterruptedException e) {}
        }
    }

    // --- THE SOCCER LOGIC ---
    private void tick() {
        // 1. Move the Ball
        ballX += ballSpeedX;
        ballY += ballSpeedY;

        // 2. Bounce off Top and Bottom Walls (Field Boundaries)
        if (ballY <= 0 || ballY >= HEIGHT - 20) {
            ballSpeedY = -ballSpeedY;
        }

        // 3. Check for GOAL (Scoring)
        if (ballX < 0) {
            score2++; // Right player scores
            resetBall();
            System.out.println("GOAL! Score: " + score1 + " - " + score2);
        } else if (ballX > WIDTH) {
            score1++; // Left player scores
            resetBall();
            System.out.println("GOAL! Score: " + score1 + " - " + score2);
        }

        // 4. Check Collision with Players (Simple Logic)
        // Check Left Player (Player 1)
        if (ballX <= 30 && ballY >= player1Y && ballY <= player1Y + PADDLE_HEIGHT) {
            ballSpeedX = Math.abs(ballSpeedX); // Bounce Right
            ballSpeedX += 0.5; // Add slight speed increase for excitement
        }

        // Check Right Player (Player 2)
        if (ballX >= WIDTH - 40 && ballY >= player2Y && ballY <= player2Y + PADDLE_HEIGHT) {
            ballSpeedX = -Math.abs(ballSpeedX); // Bounce Left
            ballSpeedX -= 0.5;
        }

        // 5. Broadcast EVERYTHING to Clients
        // Format: "UPDATE:ballX,ballY,p1Y,p2Y,score1,score2"
        String gameState = String.format("UPDATE:%.1f,%.1f,%d,%d,%d,%d",
                ballX, ballY, player1Y, player2Y, score1, score2);

        clientManager.broadcast(gameState);
    }

    private void resetBall() {
        ballX = WIDTH / 2.0;
        ballY = HEIGHT / 2.0;
        // Reset speed and reverse direction
        ballSpeedX = (ballSpeedX > 0) ? -4.0 : 4.0;
        ballSpeedY = 4.0;
    }
    private void endGame() {
        boolean gameEnded = true;
        System.out.println("GAME OVER! Final Score: " + score1 + " - " + score2);

        // SAVE TO DATABASE
        DatabaseManager.saveMatchRecord(score1, score2);

        // Tell clients the game is over (Optional: Send a special message)
        clientManager.broadcast("GAMEOVER:" + score1 + ":" + score2);
    }
}