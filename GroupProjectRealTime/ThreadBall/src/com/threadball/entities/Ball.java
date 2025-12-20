package com.threadball.entities;

public class Ball {
    private double x, y;
    private double velocityX, velocityY;

    public Ball(double x, double y) {
        this.x = x;
        this.y = y;
        this.velocityX = 0;
        this.velocityY = 0;
    }

    public double getX() { return x; }
    public double getY() { return y; }

    public void addForce(double dx, double dy) {
        this.velocityX += dx;
        this.velocityY += dy;
    }

    public void move() {
        this.x += velocityX;
        this.y += velocityY;
        // Friction logic (slows ball down)
        this.velocityX *= 0.98;
        this.velocityY *= 0.98;
    }

    @Override
    public String toString() { return String.format("[%.1f, %.1f]", x, y); }
}