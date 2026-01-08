package com.soccer.client.input;

import com.soccer.common.InputPacket;
import javafx.scene.input.KeyCode;

// Basic handler to map keyboard presses to our network packet
public class KeyHandler {

    // This method gets called whenever a key is pressed or released
    public static void handle(KeyCode code, boolean isPressed, InputPacket currentInput) {
        switch (code) {
            // Standard WASD controls for movement
            case W -> currentInput.up = isPressed;
            case S -> currentInput.down = isPressed;
            case A -> currentInput.left = isPressed;
            case D -> currentInput.right = isPressed;

            // Action key: Spacebar to kick/shoot the ball
            case SPACE -> currentInput.shoot = isPressed;

            // Added arrow keys just in case someone prefers them over WASD
            case UP -> currentInput.up = isPressed;
            case DOWN -> currentInput.down = isPressed;
            case LEFT -> currentInput.left = isPressed;
            case RIGHT -> currentInput.right = isPressed;

            // Hold Shift to sprint (makes player move faster)
            case SHIFT -> currentInput.sprint = isPressed;

            // ADMIN SHORTCUT: Press 'P' to simulate the admin approval signal
            // Useful for quick testing without clicking the UI
            case P -> currentInput.adminApproveSignal = isPressed;
        }
    }
}