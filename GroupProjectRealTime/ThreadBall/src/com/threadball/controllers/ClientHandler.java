package com.threadball.controllers;

import com.threadball.server.GameState;
import com.threadball.entities.Player;
import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private Socket socket;
    private GameState gameState;
    private Player player;

    public ClientHandler(Socket socket, GameState gameState, int id) {
        this.socket = socket;
        this.gameState = gameState;
        this.player = new Player(id);
    }

    @Override
    public void run() {
        gameState.addPlayer(player);

        try (PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println("Connected! You are Player ID: " + player.getId());

            String input;
            while ((input = in.readLine()) != null) {
                // Use PlayerController for logic
                PlayerController.handleInput(player.getId(), input, gameState);

                // Send update back to client
                out.println(gameState.getSnapshot());
            }
        } catch (IOException e) {
            System.out.println("Player " + player.getId() + " disconnected.");
        }
    }
}