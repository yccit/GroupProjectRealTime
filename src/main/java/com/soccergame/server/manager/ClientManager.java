package com.soccergame.server.manager;

import java.net.Socket;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 客户端管理器：维护所有在线玩家的列表，处理群发消息
 */
public class ClientManager {
    // 使用线程安全的List，防止多线程并发修改报错
    private CopyOnWriteArrayList<ClientHandler> clients = new CopyOnWriteArrayList<>();

    // 当有新Socket连入时，创建对应的Handler线程
    public void addClient(Socket socket) {
        ClientHandler handler = new ClientHandler(socket, this);
        clients.add(handler);
        new Thread(handler).start(); // 启动该玩家的专属接收线程
    }

    // 玩家断开连接时移除
    public void removeClient(ClientHandler handler) {
        clients.remove(handler);
        System.out.println("[ClientManager] Client removed. Current count: " + clients.size());
        broadcast("PLAYER_LEFT"); // 通知其他人（简单协议示例）
    }

    // 广播消息给所有玩家 (例如：某个玩家移动了，告诉所有人)
    public void broadcast(String message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    // 广播给除了发送者以外的人 (例如：我移动了，只需要告诉别人，不用告诉自己)
    public void broadcastExcept(String message, ClientHandler sender) {
        for (ClientHandler client : clients) {
            if (client != sender) {
                client.sendMessage(message);
            }
        }
    }
}