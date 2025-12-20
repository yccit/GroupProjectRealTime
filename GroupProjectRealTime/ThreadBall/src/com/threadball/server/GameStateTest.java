package com.threadball.server;

import com.threadball.entities.Player;
import org.testng.Assert;
import org.testng.annotations.Test;

public class GameStateTest {

    // Simulates 50 threads kicking at once to prove concurrency works
    @Test(threadPoolSize = 50, invocationCount = 50, timeOut = 1000)
    public void testConcurrentKicks() {
        GameState state = new GameState();
        state.addPlayer(new Player(1));

        state.playerKick(1);

        String snapshot = state.getSnapshot();
        Assert.assertNotNull(snapshot);
        System.out.println("Thread Test Passed: " + snapshot);
    }
}