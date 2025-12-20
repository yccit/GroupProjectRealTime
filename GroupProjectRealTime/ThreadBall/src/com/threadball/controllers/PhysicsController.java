package com.threadball.controllers;

import com.threadball.server.GameState;

public class PhysicsController implements Runnable {
    private GameState gameState;

    public PhysicsController(GameState gameState) {
        this.gameState = gameState;
    }

    @Override
    public void run() {
        System.out.println("[Physics] Engine Started...");
        while (true) {
            try {
                gameState.updatePhysics();
                Thread.sleep(16); // ~60 FPS Loop
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}