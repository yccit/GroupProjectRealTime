package com.threadball.entities;

import java.util.ArrayList;
import java.util.List;

public class Team {
    private String name;
    private int score;
    private List<Player> players;

    public Team(String name) {
        this.name = name;
        this.score = 0;
        this.players = new ArrayList<>();
    }

    public void addPlayer(Player p) { players.add(p); }
    public void addScore() { this.score++; }
    public int getScore() { return score; }
}