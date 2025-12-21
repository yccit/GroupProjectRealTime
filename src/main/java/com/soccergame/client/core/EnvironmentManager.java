package com.soccergame.client.core;

import javafx.scene.paint.Color;

/**
 * EnvironmentManager runs on a separate independent thread.
 * It is responsible for simulating the environment changes (e.g., Day/Night cycle)
 * by updating the GameState periodically.
 */
public class EnvironmentManager implements Runnable {
    private final GameState gameState;
    private boolean isRunning = true;

    // Define colors for Day and Night phases
    private final Color DAY_COLOR = Color.LIGHTBLUE;
    private final Color NIGHT_COLOR = Color.MIDNIGHTBLUE;

    public EnvironmentManager(GameState gameState) {
        this.gameState = gameState;
    }

    @Override
    public void run() {
        System.out.println("[Environment] Thread Started: Day/Night cycle active.");

        while (isRunning) {
            try {
                // Phase 1: Night Time
                // Wait for 5 seconds before changing state
                Thread.sleep(5000);
                gameState.setSkyColor(NIGHT_COLOR);
                System.out.println("[Environment] Time update: It is now NIGHT.");

                // Phase 2: Day Time
                // Wait for 5 seconds before changing state
                Thread.sleep(5000);
                gameState.setSkyColor(DAY_COLOR);
                System.out.println("[Environment] Time update: It is now DAY.");

            } catch (InterruptedException e) {
                System.out.println("[Environment] Thread interrupted, stopping cycle.");
                isRunning = false;
            }
        }
    }

    /**
     * Stops the environment thread safely.
     */
    public void stop() {
        isRunning = false;
    }
}