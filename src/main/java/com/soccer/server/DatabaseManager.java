package com.soccer.server;

import java.sql.*;

public class DatabaseManager {
    private static final String URL = "jdbc:mysql://localhost:3306/football_game";
    private static final String USER = "root"; // 修改为你的数据库账号
    private static final String PASS = "123456"; // 修改为你的数据库密码

    public static void saveMatch(String winner, int scoreRed, int scoreBlue) {
        new Thread(() -> { // 异步保存，不卡游戏线程
            try (Connection conn = DriverManager.getConnection(URL, USER, PASS)) {
                String sql = "INSERT INTO match_history (winner, score_red, score_blue) VALUES (?, ?, ?)";
                PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                stmt.setString(1, winner);
                stmt.setInt(2, scoreRed);
                stmt.setInt(3, scoreBlue);
                stmt.executeUpdate();
                System.out.println("[DB] Match Saved.");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }).start();
    }
}