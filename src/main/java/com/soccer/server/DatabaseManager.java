package com.soccer.server;

import java.sql.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DatabaseManager {
    // Database connection details
    // Make sure MySQL is running and the 'football_game' DB exists!
    private static final String URL = "jdbc:mysql://localhost:3306/football_game";
    private static final String USER = "root";
    private static final String PASS = "123456";

    // We use a separate thread for database operations.
    // This is important so the main game loop doesn't freeze (lag) while waiting for the database to save.
    private static final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();

    public static void saveMatch(String winner, int scoreRed, int scoreBlue) {
        // Run the save task in the background
        dbExecutor.execute(() -> {
            try (Connection conn = DriverManager.getConnection(URL, USER, PASS)) {

                // Prepare the SQL insert statement
                String sql = "INSERT INTO match_history (winner, score_red, score_blue) VALUES (?, ?, ?)";
                PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

                // Fill in the values
                stmt.setString(1, winner);
                stmt.setInt(2, scoreRed);
                stmt.setInt(3, scoreBlue);

                // Execute the update
                stmt.executeUpdate();
                System.out.println("[DB] Match Saved successfully.");

            } catch (SQLException e) {
                // Print error if connection fails or SQL is wrong
                e.printStackTrace();
            }
        });
    }
}