package com.threadball.controllers;

import com.threadball.server.GameState;

public class PlayerController {

    public static void handleInput(int playerId, String command, GameState gameState) {
        if (command == null) return;

        // Simple command parsing
        if (command.toUpperCase().startsWith("KICK")) {
            gameState.playerKick(playerId);
        }
    }
}