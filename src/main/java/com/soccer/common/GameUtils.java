package com.soccer.common;

public class GameUtils {

    /**
     * Clamps a value between a minimum and maximum.
     * Useful for keeping players inside the screen boundaries.
     * * @param value The value to check
     * @param min The minimum allowed value
     * @param max The maximum allowed value
     * @return The clamped value
     */
    public static int clamp(int value, int min, int max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }

    /**
     * Checks if two rectangles intersect (Collision Detection).
     * Simple AABB collision logic.
     */
    public static boolean checkCollision(double x1, double y1, int w1, int h1,
                                         double x2, double y2, int w2, int h2) {
        return x1 < x2 + w2 &&
                x1 + w1 > x2 &&
                y1 < y2 + h2 &&
                y1 + h1 > y2;
    }
}