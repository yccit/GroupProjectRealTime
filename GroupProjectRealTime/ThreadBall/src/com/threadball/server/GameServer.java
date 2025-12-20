package com.threadball.server;

import com.threadball.controllers.ClientHandler;
import com.threadball.controllers.PhysicsController;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GameServer {
    private static final int PORT = 12345;

    public static void main(String[] args) {
        GameState gameState = new GameState();

        // Start Physics Thread
        new Thread(new PhysicsController(gameState)).start();

        // Start Network Pool (Scalability Requirement)
        ExecutorService pool = Executors.newFixedThreadPool(100);

        System.out.println("ThreadBall Server Running on Port " + PORT);

        int idCounter = 0;
        try (ServerSocket server = new ServerSocket(PORT)) {
            while (true) {
                Socket client = server.accept();
                pool.execute(new ClientHandler(client, gameState, ++idCounter));
                System.out.println("New Connection: Player " + idCounter);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}