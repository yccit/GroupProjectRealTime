package com.soccergame.server;

import com.soccergame.server.core.ServerCore;

/**
 * 服务器启动入口
 */
public class ServerMain {
    public static void main(String[] args) {
        System.out.println("[System] Starting Soccer Game Server...");

        // 设定端口号，建议使用 8888 或 9999 避免冲突
        int port = 8888;

        // 启动核心网络服务
        ServerCore serverCore = new ServerCore(port);
        serverCore.start();
    }
}