package com.threadball.entities;

public class Match {
    private int timeRemaining;
    private Team teamA;
    private Team teamB;

    public Match() {
        this.timeRemaining = 300; // 5 minutes
        this.teamA = new Team("Red Team");
        this.teamB = new Team("Blue Team");
    }

    public void tickTimer() {
        if (timeRemaining > 0) timeRemaining--;
    }
}