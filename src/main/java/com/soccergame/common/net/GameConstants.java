package com.soccergame.common.net;

public class GameConstants {
    // Connection Config
    public static final String SERVER_ADDRESS = "localhost"; // Change to IP if playing on different PCs
    public static final int SERVER_PORT = 12345;

    // Commands (Protocol)
    public static final String CMD_UP = "UP";
    public static final String CMD_DOWN = "DOWN";
    public static final String CMD_ID_PREFIX = "ID:";

    // Game Physics Config
    public static final int FPS = 60;
    public static final long OPTIMAL_TIME = 1000000000 / FPS;
}