package com.threadball.entities;

public class Goal {
    private double x, y;
    private double width, height;
    private String teamSide; // "RED" or "BLUE"

    public Goal(double x, double y, String teamSide) {
        this.x = x;
        this.y = y;
        this.width = 50;
        this.height = 200;
        this.teamSide = teamSide;
    }
}