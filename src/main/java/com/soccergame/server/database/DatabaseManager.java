package com.soccergame.server.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

public class DatabaseManager {
    // 1. Connection Details
    private static final String URL = "jdbc:mysql://localhost:3306/threadball";
    private static final String USER = "root";
    private static final String PASSWORD = "1234"; // Keep empty if using XAMPP default

    // 2. Get Connection
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    // 3. Save Match Result
    public static void saveMatchRecord(int scoreTeam1, int scoreTeam2) {
        // SQL query matches your 'game_match' table columns
        String sql = "INSERT INTO game_match (start_time, end_time, score_team1, score_team2) VALUES (?, ?, ?, ?)";

        // Run in a new thread to avoid freezing the game
        new Thread(() -> {
            try (Connection conn = getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                // Get current time
                Timestamp now = new Timestamp(System.currentTimeMillis());

                pstmt.setTimestamp(1, now); // start_time
                pstmt.setTimestamp(2, now); // end_time (simplified: same as start for now)
                pstmt.setInt(3, scoreTeam1);
                pstmt.setInt(4, scoreTeam2);

                int rows = pstmt.executeUpdate();
                if (rows > 0) {
                    System.out.println("[Database] Match saved successfully!");
                }

            } catch (SQLException e) {
                System.err.println("[Database Error] Could not save match: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }
}