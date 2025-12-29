package com.soccer.server;

import java.sql.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DatabaseManager {
    private static final String URL = "jdbc:mysql://localhost:3306/football_game";
    private static final String USER = "root";
    private static final String PASS = "123456";

    // 使用线程池排队处理数据库写入
    private static final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();

    public static void saveMatch(String winner, int scoreRed, int scoreBlue) {
        dbExecutor.execute(() -> {
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
        });
    }
}