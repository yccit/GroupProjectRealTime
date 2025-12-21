package com.soccergame.server.manager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private Socket socket;
    private ClientManager clientManager;
    private PrintWriter out;
    private BufferedReader in;
    private int playerID; // 1 = Left, 2 = Right

    public ClientHandler(Socket socket, ClientManager clientManager) {
        this.socket = socket;
        this.clientManager = clientManager;
    }

    @Override
    public void run() {
        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Assign Player ID based on who joined first
            // (You might need logic in ClientManager to assign IDs 1 and 2 accurately)
            // For now, let's assume the client sends their ID or you assign it manually.

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                handleMessage(inputLine);
            }
        } catch (IOException e) {
            System.err.println("Player disconnected.");
        } finally {
            close();
            clientManager.removeClient(this);
        }
    }

    private void handleMessage(String msg) {
        // Handle Movement Requests
        if (msg.startsWith("MOVE:")) {
            // Expected Format: "MOVE:PLAYER_ID:DIRECTION" (e.g., "MOVE:1:UP")
            String[] parts = msg.split(":");
            int pID = Integer.parseInt(parts[1]);
            String dir = parts[2];

            // Update the SERVER STATE (GameHandler static variables)
            int speed = 10;
            if (pID == 1) {
                if (dir.equals("UP")) GameHandler.player1Y -= speed;
                if (dir.equals("DOWN")) GameHandler.player1Y += speed;
            } else if (pID == 2) {
                if (dir.equals("UP")) GameHandler.player2Y -= speed;
                if (dir.equals("DOWN")) GameHandler.player2Y += speed;
            }
        }
    }

    public void sendMessage(String msg) {
        if (out != null) out.println(msg);
    }

    private void close() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException e) { e.printStackTrace(); }
    }
}