package com.soccer.common;

public class GameUtils {

    // Helper method to keep a value within a specific range.
    // We mainly use this to stop players from running off the screen map.
    public static int clamp(int value, int min, int max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }

    // Basic collision detection.
    // Checks if two boxes (rectangles) are overlapping each other.
    public static boolean checkCollision(double x1, double y1, int w1, int h1,
                                         double x2, double y2, int w2, int h2) {
        return x1 < x2 + w2 &&
                x1 + w1 > x2 &&
                y1 < y2 + h2 &&
                y1 + h1 > y2;
    }
}