package com.soccer.common;

import java.io.Serializable;

public class InputPacket implements Serializable {
    public boolean up, down, left, right, shoot;
    public String command; // "JOIN", "START"
    public String playerName;
}