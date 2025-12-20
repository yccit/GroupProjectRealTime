package com.threadball.server;

import com.threadball.entities.Ball;
import com.threadball.entities.Player;
import com.threadball.entities.Match;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class GameState {
    private Ball ball;
    private List<Player> players;
    private Match match;

    // REQUIREMENT: Lock Interface for Thread Safety
    private final Lock lock = new ReentrantLock();

    public GameState() {
        this.ball = new Ball(400, 300);
        this.players = new ArrayList<>();
        this.match = new Match();
    }

    public void addPlayer(Player p) {
        lock.lock();
        try {
            players.add(p);
        } finally {
            lock.unlock();
        }
    }

    // REQUIREMENT: Synchronizing critical sections
    public void updatePhysics() {
        lock.lock();
        try {
            ball.move();
            // Boundary checks
            if (ball.getX() < 0 || ball.getX() > 800) ball.addForce(2, 0);
            if (ball.getY() < 0 || ball.getY() > 600) ball.addForce(0, 2);
        } finally {
            lock.unlock();
        }
    }

    public void playerKick(int playerId) {
        lock.lock();
        try {
            // Give the ball a random kick force
            ball.addForce(Math.random() * 10 - 5, Math.random() * 10 - 5);
        } finally {
            lock.unlock();
        }
    }

    public String getSnapshot() {
        lock.lock();
        try {
            // REQUIREMENT: Parallel Streams
            long count = players.parallelStream().count();
            return "Ball:" + ball.toString() + " | Players:" + count;
        } finally {
            lock.unlock();
        }
    }
}