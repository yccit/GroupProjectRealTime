package com.soccer.client.input;

import com.soccer.common.InputPacket;
import javafx.scene.input.KeyCode;

public class KeyHandler {

    public static void handle(KeyCode code, boolean isPressed, InputPacket currentInput) {
        switch (code) {
            case W -> currentInput.up = isPressed;
            case S -> currentInput.down = isPressed;
            case A -> currentInput.left = isPressed;
            case D -> currentInput.right = isPressed;
            case SPACE -> currentInput.shoot = isPressed;

            case UP -> currentInput.up = isPressed;
            case DOWN -> currentInput.down = isPressed;
            case LEFT -> currentInput.left = isPressed;
            case RIGHT -> currentInput.right = isPressed;
            case SHIFT -> currentInput.sprint = isPressed;

            // ADMIN COMMAND: Press 'P' to approve all waiting players
            case P -> currentInput.adminApproveSignal = isPressed;
        }
    }
}