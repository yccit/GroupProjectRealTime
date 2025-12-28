package com.soccer.common;

import java.io.Serializable;

public class InputPacket implements Serializable {
    private static final long serialVersionUID = 1L;
    public boolean up, down, left, right, shoot;
    public String command; // "JOIN", "START"
    public String playerName;
    public boolean sprint;
    public boolean adminApproveSignal;
    public int id;
    public int targetIdToApprove;

}