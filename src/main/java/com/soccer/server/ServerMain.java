package com.soccer.server;

import com.soccer.common.Constants;
import com.soccer.common.InputPacket;
import com.soccer.common.GameState;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;

public class ServerMain {
    private static GameRoom gameRoom = new GameRoom();
    private static AtomicInteger playerIdGen = new AtomicInteger(1);

    public static void main(String[] args) {
        new Thread(gameRoom).start(); // 启动游戏循环线程
        System.out.println("Server started on port " + Constants.PORT);

        try (ServerSocket serverSocket = new ServerSocket(Constants.PORT)) {
            while (true) {
                Socket client = serverSocket.accept();
                new Thread(new ClientHandler(client)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class ClientHandler implements Runnable {
        private Socket socket;
        private ObjectOutputStream out;
        private ObjectInputStream in;
        private int id;

        public ClientHandler(Socket socket) {
            this.socket = socket;
            this.id = playerIdGen.getAndIncrement();
        }

        @Override
        public void run() {
            try {
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());

                while (true) {
                    Object obj = in.readObject();
                    if (obj instanceof InputPacket) {
                        InputPacket input = (InputPacket) obj;

                        // 处理特殊指令
                        if ("JOIN".equals(input.command)) {
                            gameRoom.addPlayer(id, input.playerName);
                        } else if ("START".equals(input.command)) {
                            gameRoom.startGame();
                        } else {
                            // 处理游戏操作
                            gameRoom.inputs.put(id, input);
                        }
                    }

                    // 每收到一次包，回发一次最新状态
                    // 注意：reset以避免对象缓存引用问题
                    out.reset();
                    out.writeObject(gameRoom.getGameState());
                    out.flush();
                }
            } catch (Exception e) {
                System.out.println("Client " + id + " disconnected.");
            }
        }
    }
}