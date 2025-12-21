package com.soccergame.server.manager;

/**
 * 游戏逻辑循环：处理物理计算、定时同步
 */
public class GameHandler implements Runnable {
    private ClientManager clientManager;
    private boolean isRunning = true;

    public GameHandler(ClientManager clientManager) {
        this.clientManager = clientManager;
    }

    @Override
    public void run() {
        System.out.println("[GameHandler] Game logic loop started.");

        // 简单的 60 FPS 循环
        long lastTime = System.nanoTime();
        double nsPerTick = 1000000000.0 / 60.0;
        double delta = 0;

        while (isRunning) {
            long now = System.nanoTime();
            delta += (now - lastTime) / nsPerTick;
            lastTime = now;

            if (delta >= 1) {
                tick(); // 执行一次逻辑更新
                delta--;
            }

            // 防止CPU占用过高
            try { Thread.sleep(2); } catch (InterruptedException e) {}
        }
    }

    // 每一帧的逻辑更新
    private void tick() {
        // 这里处理足球的物理移动
        // 如果球移动了，广播球的新位置
        // String ballPos = ...;
        // clientManager.broadcast("BALL:" + ballPos);
    }
}