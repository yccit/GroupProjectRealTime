/*package com.soccergame.client.util;

import javax.sound.sampled.*;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class SoundManager {
    private Map<String, Clip> soundMap = new HashMap<>();

    public SoundManager() {
        // Load sounds when the game starts
        loadSound("goal", "/sounds/goal.wav");
        loadSound("hit", "/sounds/hit.wav");
        loadSound("win", "/sounds/win.wav");
    }

    private void loadSound(String name, String path) {
        try {
            URL url = getClass().getResource(path);
            if (url == null) {
                System.err.println("Sound file not found: " + path);
                return;
            }
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(url);
            Clip clip = AudioSystem.getClip();
            clip.open(audioIn);
            soundMap.put(name, clip);
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    public void play(String name) {
        Clip clip = soundMap.get(name);
        if (clip != null) {
            if (clip.isRunning()) clip.stop(); // Stop if already playing
            clip.setFramePosition(0); // Rewind to start
            clip.start();
        }
    }
}*/