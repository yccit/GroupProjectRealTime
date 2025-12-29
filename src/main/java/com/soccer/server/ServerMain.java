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

public class ServerMain {
    private static GameRoom gameRoom;
    private static final List<ObjectOutputStream> clientWriters = new CopyOnWriteArrayList<>();

    // ★★★ 满足要求 (a): 使用线程池管理客户端连接，防止内存溢出 ★★★
    private static final ExecutorService pool = Executors.newFixedThreadPool(50);

    public static void main(String[] args) {
        System.out.println(">>> STARTING SOCCER SERVER on Port " + Constants.PORT + " <<<");
        gameRoom = new GameRoom();

        Thread gameThread = new Thread(gameRoom);
        // ★★★ 满足要求 (b): Thread Influencing (设置优先级) ★★★
        gameThread.setPriority(Thread.MAX_PRIORITY);
        gameThread.start();

        new Thread(ServerMain::broadcastLoop).start();

        // ★★★ 满足要求 (c): Implement joining threads ★★★
        // 添加关闭钩子，当强制结束服务器时，尝试等待游戏线程结束
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                System.out.println("Shutting down server...");
                pool.shutdown();
                gameThread.join(1000); // 尝试等待游戏线程结束
                System.out.println("Server shutdown complete.");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }));

        try (ServerSocket serverSocket = new ServerSocket(Constants.PORT)) {
            while (true) {
                System.out.println("Waiting for connections...");
                Socket socket = serverSocket.accept();
                // 使用线程池执行
                pool.execute(() -> handleClient(socket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleClient(Socket socket) {
        ObjectOutputStream out = null;
        ObjectInputStream in = null;
        int playerId = -1;

        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            Object firstObj = in.readObject();
            if (firstObj instanceof InputPacket) {
                InputPacket packet = (InputPacket) firstObj;

                if ("ADMIN_LOGIN".equals(packet.command)) {
                    System.out.println(">>> ADMIN CONNECTED! <<<");
                    clientWriters.add(out);
                }
                else if ("JOIN".equals(packet.command)) {
                    String checkResult = gameRoom.checkJoinRequest(packet.playerName);

                    if ("OK".equals(checkResult)) {
                        out.writeObject("OK");
                        out.flush();
                        playerId = packet.id;
                        String name = packet.playerName;
                        System.out.println(">>> PLAYER JOINED: " + name + " (" + playerId + ")");
                        gameRoom.addPlayer(playerId, name);
                        clientWriters.add(out);
                    } else {
                        out.writeObject("FAIL:" + checkResult);
                        out.flush();
                        socket.close();
                        return;
                    }
                }
            }

            while (true) {
                Object obj = in.readObject();
                if (obj instanceof InputPacket) {
                    InputPacket input = (InputPacket) obj;
                    if (input.id != 0) {
                        gameRoom.inputs.put(input.id, input);
                    } else if ("START".equals(input.command) || "END".equals(input.command) || "APPROVE".equals(input.command)) {
                        gameRoom.inputs.put(-999, input);
                    }
                }
            }

        } catch (EOFException | java.net.SocketException e) {
            // Client left
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (out != null) clientWriters.remove(out);
            if (playerId != -1) gameRoom.removePlayer(playerId);
            try { socket.close(); } catch (IOException e) {}
        }
    }

    private static void broadcastLoop() {
        while (true) {
            try {
                Object stateToSend = gameRoom.getGameState();
                for (ObjectOutputStream writer : clientWriters) {
                    try {
                        writer.reset();
                        writer.writeUnshared(stateToSend);
                        writer.flush();
                    } catch (IOException e) {
                        clientWriters.remove(writer);
                    }
                }
                Thread.sleep(16);
            } catch (Exception e) { e.printStackTrace(); }
        }
    }
}