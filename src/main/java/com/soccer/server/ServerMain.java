package com.soccer.server;

import com.soccer.common.Constants;
import com.soccer.common.InputPacket;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// This is the main entry point for the Server side.
// It handles networking, client connections, and broadcasting data.
public class ServerMain {
    private static GameRoom gameRoom;

    // We use CopyOnWriteArrayList here because it's thread-safe for reading while we iterate through it
    private static final List<ObjectOutputStream> clientWriters = new CopyOnWriteArrayList<>();

    // Use a Thread Pool to manage connections
    // We use a FixedThreadPool (limit 50). This prevents the server from crashing (Memory Overflow)
    // if too many people try to connect at the exact same time.
    private static final ExecutorService pool = Executors.newFixedThreadPool(50);

    public static void main(String[] args) {
        System.out.println(">>> STARTING SOCCER SERVER on Port " + Constants.PORT + " <<<");

        // Initialize the game logic
        gameRoom = new GameRoom();

        // Start the Game Loop in its own thread
        Thread gameThread = new Thread(gameRoom);

        // Thread Influencing (Setting Priority)
        // We give the Game Thread MAXIMUM priority. This tells the OS/CPU that calculating
        // physics is the most important task, ensuring smooth gameplay.
        gameThread.setPriority(Thread.MAX_PRIORITY);
        gameThread.start();

        // Start the broadcasting thread (sends data to clients)
        new Thread(ServerMain::broadcastLoop).start();

        // Implement Joining Threads
        // This "Shutdown Hook" runs when you force-stop the server (e.g., Ctrl+C).
        // It tries to gracefully wait for the game thread to finish its current task before killing the process.
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                System.out.println("Shutting down server...");
                pool.shutdown(); // Stop accepting new clients
                gameThread.join(1000); // Wait up to 1 second for the game thread to finish
                System.out.println("Server shutdown complete.");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }));

        // Main Server Loop: Accept connections
        try (ServerSocket serverSocket = new ServerSocket(Constants.PORT)) {
            while (true) {
                System.out.println("Waiting for connections...");
                Socket socket = serverSocket.accept();

                // Instead of "new Thread()", we pass the task to our Thread Pool
                pool.execute(() -> handleClient(socket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Handles the individual conversation with one client
    private static void handleClient(Socket socket) {
        ObjectOutputStream out = null;
        ObjectInputStream in = null;
        int playerId = -1;

        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            // The first thing a client sends is who they are (Handshake)
            Object firstObj = in.readObject();
            if (firstObj instanceof InputPacket) {
                InputPacket packet = (InputPacket) firstObj;

                // Case 1: It's the Admin
                if ("ADMIN_LOGIN".equals(packet.command)) {
                    System.out.println(">>> ADMIN CONNECTED! <<<");
                    clientWriters.add(out);
                }
                // Case 2: It's a Player asking to join
                else if ("JOIN".equals(packet.command)) {
                    String checkResult = gameRoom.checkJoinRequest(packet.playerName);

                    if ("OK".equals(checkResult)) {
                        out.writeObject("OK");
                        out.flush();

                        playerId = packet.id;
                        String name = packet.playerName;
                        System.out.println(">>> PLAYER JOINED: " + name + " (" + playerId + ")");

                        // Add to game logic and broadcast list
                        gameRoom.addPlayer(playerId, name);
                        clientWriters.add(out);
                    } else {
                        // Reject connection (Full or Duplicate Name)
                        out.writeObject("FAIL:" + checkResult);
                        out.flush();
                        socket.close();
                        return;
                    }
                }
            }

            // Continuous loop: Listen for inputs (Keys pressed) from this client
            while (true) {
                Object obj = in.readObject();
                if (obj instanceof InputPacket) {
                    InputPacket input = (InputPacket) obj;

                    // Normal player input
                    if (input.id != 0) {
                        gameRoom.inputs.put(input.id, input);
                    }
                    // Admin commands (Start/End/Approve) usually come with ID 0 or specific flags
                    else if ("START".equals(input.command) || "END".equals(input.command) || "APPROVE".equals(input.command)) {
                        // We use a dummy ID (-999) or handle directly in GameRoom
                        gameRoom.inputs.put(-999, input);
                    }
                }
            }

        } catch (EOFException | java.net.SocketException e) {
            // This happens normally when a client closes the window
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Cleanup: Remove from lists so we don't send data to dead sockets
            if (out != null) clientWriters.remove(out);
            if (playerId != -1) gameRoom.removePlayer(playerId);
            try { socket.close(); } catch (IOException e) {}
        }
    }

    // This loop runs constantly to send the GameState to all connected clients
    private static void broadcastLoop() {
        while (true) {
            try {
                Object stateToSend = gameRoom.getGameState();

                // Iterate through all connected clients and send the object
                for (ObjectOutputStream writer : clientWriters) {
                    try {
                        writer.reset(); // Important: Resets object cache so updated values are sent
                        writer.writeUnshared(stateToSend);
                        writer.flush();
                    } catch (IOException e) {
                        // If sending fails, assume client disconnected
                        clientWriters.remove(writer);
                    }
                }
                // roughly 60 updates per second
                Thread.sleep(16);
            } catch (Exception e) { e.printStackTrace(); }
        }
    }
}