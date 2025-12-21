package com.soccer.common;

public class Constants {
    public static final int PORT = 8888;
    public static final int WIDTH = 1200;
    public static final int HEIGHT = 800;

    // 物理参数
    public static final double BASE_SPEED = 5.0;
    public static final double PLAYER_RADIUS = 15.0;
    public static final double BALL_RADIUS = 10.0;

    // 阵型 (4-4-2, 相对中心点的偏移量)
    // 简化：我们只定义红队的相对位置，蓝队镜像翻转
    // 格式: {x, y}
    public static final double[][] FORMATION_OFFSETS = {
            {-550, 0},   // GK (0)
            {-400, -200}, {-400, -70}, {-400, 70}, {-400, 200}, // Defenders (1-4)
            {-200, -200}, {-200, -70}, {-200, 70}, {-200, 200}, // Midfielders (5-8)
            {-50, -100}, {-50, 100}  // Strikers (9-10)
    };
}