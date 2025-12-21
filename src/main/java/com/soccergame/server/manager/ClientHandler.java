package com.soccergame.server.manager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * 玩家连接处理者：负责处理单个玩家的输入输出
 */
public class ClientHandler implements Runnable {
    private Socket socket;
    private ClientManager clientManager;
    private PrintWriter out;
    private BufferedReader in;

    // 简单的玩家标识
    private String playerName = "Unknown";

    public ClientHandler(Socket socket, ClientManager clientManager) {
        this.socket = socket;
        this.clientManager = clientManager;
    }

    @Override
    public void run() {
        try {
            // 初始化输入输出流
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // 握手阶段 (示例)
            out.println("WELCOME TO SOCCER SERVER");

            String inputLine;
            // 循环读取客户端发来的消息
            while ((inputLine = in.readLine()) != null) {
                System.out.println("[ClientHandler] Received: " + inputLine);

                // 解析并处理消息
                handleMessage(inputLine);
            }

        } catch (IOException e) {
            System.err.println("[ClientHandler] Connection lost: " + e.getMessage());
        } finally {
            // 资源清理
            close();
            clientManager.removeClient(this);
        }
    }

    // 简单的协议解析逻辑
    private void handleMessage(String msg) {
        // 假设协议格式: "CMD:DATA"
        // 例如: "MOVE:100,200"

        if (msg.startsWith("LOGIN:")) {
            this.playerName = msg.split(":")[1];
            System.out.println("Player logged in: " + playerName);
            clientManager.broadcast("NEW_PLAYER:" + playerName);
        }
        else if (msg.startsWith("MOVE:")) {
            // 转发移动数据给其他人
            clientManager.broadcastExcept("PLAYER_MOVED:" + playerName + ":" + msg.split(":")[1], this);
        }
        else {
            // 未知指令，暂时忽略
        }
    }

    // 发送消息给该客户端
    public void sendMessage(String msg) {
        if (out != null) {
            out.println(msg);
        }
    }

    private void close() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}