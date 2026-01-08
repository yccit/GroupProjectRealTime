package com.soccer.client;

public class AppLauncher {
    public static void main(String[] args) {
        ClientMain.main(args);
        int clientId = (int) (Math.random() * 10000);
    }
}