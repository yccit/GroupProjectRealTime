package com.soccergame.server.database;

public class DatabaseTest {
    public static void main(String[] args) {
        System.out.println("--- Starting Database Test ---");

        // Force save a fake match result (Score: 5 - 3)
        DatabaseManager.saveMatchRecord(5, 3);

        System.out.println("Command sent. Waiting 2 seconds for thread to finish...");

        // Wait briefly because DatabaseManager runs in a separate thread
        try { Thread.sleep(2000); } catch (InterruptedException e) {}

        System.out.println("--- Test Finished. Check your Database now! ---");
    }
}