package com.soccergame.server.core;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * 网络核心：负责监听端口和接受Socket连接
 */
public class ServerCore {
    private int port;
    private boolean isRunning;
    private ServerSocket serverSocket;

    // 引用管理器，用于后续把连接交给它
    private ClientManager clientManager;

    public ServerCore(int port) {
        this.port = port;
        this.clientManager = new ClientManager();
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            isRunning = true;
            System.out.println("[ServerCore] Server started successfully on port: " + port);

            // 启动一个单独的线程来处理游戏主循环（物理计算、位置同步等）
            new Thread(new GameHandler(clientManager)).start();

            // 主线程死循环：专门用来接待新玩家
            while (isRunning) {
                System.out.println("[ServerCore] Waiting for clients...");
                Socket clientSocket = serverSocket.accept();
                System.out.println("[ServerCore] New client connected: " + clientSocket.getInetAddress());

                // 将新连接交给 ClientManager 进行管理
                clientManager.addClient(clientSocket);
            }

        } catch (IOException e) {
            System.err.println("[ServerCore] Port binding failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            stop();
        }
    }

    public void stop() {
        isRunning = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}