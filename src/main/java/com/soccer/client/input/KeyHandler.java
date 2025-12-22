package com.soccer.client.input;

import com.soccer.common.InputPacket;
import javafx.scene.input.KeyCode;

public class KeyHandler {

    // 这个方法直接修改传入的 inputPacket
    public static void handle(KeyCode code, boolean isPressed, InputPacket currentInput) {
        switch (code) {
            case W -> currentInput.up = isPressed;
            case S -> currentInput.down = isPressed;
            case A -> currentInput.left = isPressed;
            case D -> currentInput.right = isPressed;
            case SPACE -> currentInput.shoot = isPressed;
            // 也可以加上 UP, DOWN, LEFT, RIGHT
            case UP -> currentInput.up = isPressed;
            case DOWN -> currentInput.down = isPressed;
            case LEFT -> currentInput.left = isPressed;
            case RIGHT -> currentInput.right = isPressed;
        }
    }
}