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

public class ServerMain {
    private static GameRoom gameRoom;
    private static final List<ObjectOutputStream> clientWriters = new CopyOnWriteArrayList<>();

    public static void main(String[] args) {
        System.out.println(">>> STARTING SOCCER SERVER on Port " + Constants.PORT + " <<<");
        gameRoom = new GameRoom();
        new Thread(gameRoom).start();
        new Thread(ServerMain::broadcastLoop).start();

        try (ServerSocket serverSocket = new ServerSocket(Constants.PORT)) {
            while (true) {
                System.out.println("Waiting for connections...");
                Socket socket = serverSocket.accept();
                new Thread(() -> handleClient(socket)).start();
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

            // 1. 读取第一个包 (握手)
            Object firstObj = in.readObject();
            if (firstObj instanceof InputPacket) {
                InputPacket packet = (InputPacket) firstObj;

                if ("ADMIN_LOGIN".equals(packet.command)) {
                    System.out.println(">>> ADMIN CONNECTED! <<<");
                    clientWriters.add(out); // Admin 直接通过
                }
                else if ("JOIN".equals(packet.command)) {
                    // ★★★ NEW: 检查名字和人数 ★★★
                    String checkResult = gameRoom.checkJoinRequest(packet.playerName);

                    if ("OK".equals(checkResult)) {
                        // 通过！发送 OK 给客户端
                        out.writeObject("OK");
                        out.flush();

                        // 加入游戏
                        playerId = packet.id;
                        String name = packet.playerName;
                        System.out.println(">>> PLAYER JOINED: " + name + " (" + playerId + ")");
                        gameRoom.addPlayer(playerId, name);
                        clientWriters.add(out);
                    } else {
                        // 失败！发送错误信息 (FAIL:原因)
                        out.writeObject("FAIL:" + checkResult);
                        out.flush();
                        System.out.println("Rejected join request: " + checkResult);
                        socket.close(); // 断开连接
                        return; // 结束线程
                    }
                }
            }

            // 2. 正常游戏循环
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
            // Client 正常断开
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