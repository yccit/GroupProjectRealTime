package com.soccer.client.ui;

import com.soccer.common.Constants;
import com.soccer.common.GameState;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class GamePanel extends Canvas {
    private GraphicsContext gc;

    // Image assets
    private Image imgBall;
    private Image imgPlayerRed;
    private Image imgPlayerBlue;

    public GamePanel() {
        super(Constants.WIDTH, Constants.HEIGHT);
        this.gc = this.getGraphicsContext2D();
        loadImages(); // Load images on startup
    }

    private void loadImages() {
        try {
            // Load images from resources/images
            imgBall = new Image(getClass().getResource("/images/ball.png").toExternalForm());
            imgPlayerRed = new Image(getClass().getResource("/images/player_red.png").toExternalForm());
            imgPlayerBlue = new Image(getClass().getResource("/images/player_blue.png").toExternalForm());

            // Console Message (Now in English)
            System.out.println("All image assets loaded successfully!");
        } catch (Exception e) {
            System.err.println("!!! Failed to load images !!!");
            System.err.println("Please ensure filenames are: ball.png, player_red.png, player_blue.png");
            e.printStackTrace();
        }
    }

    public void render(GameState currentState) {
        if (currentState == null) return;

        // 1. Draw Pitch (Using code to draw stripes, clearer than a dark background image)
        if ("RAINY".equals(currentState.weather)) {
            drawPitch(Color.web("#1e5128"), Color.web("#143d1d")); // Darker for rain
        } else {
            drawPitch(Color.web("#2ecc71"), Color.web("#27ae60")); // Bright for sunny
        }

        // 2. Draw Players (Using images)
        for (GameState.PlayerState p : currentState.players) {
            drawPlayerSprite(p);
        }

        // 3. Draw Ball (Using image)
        drawBallSprite(currentState.ballX, currentState.ballY);

        // 4. Weather Effects
        if ("RAINY".equals(currentState.weather)) drawRain();

        // 5. HUD Information
        drawHUD(currentState);

        // 6. Game Over Screen
        if (currentState.currentPhase == GameState.Phase.GAME_OVER) {
            drawGameOverScreen(currentState);
        }
    }

    // --- Helper Methods ---

    private void drawPlayerSprite(GameState.PlayerState p) {
        double r = Constants.PLAYER_RADIUS;
        double size = r * 2.8; // Scale up slightly to fit the collision circle

        // Select image based on team
        Image sprite = p.team.equals("RED") ? imgPlayerRed : imgPlayerBlue;

        // Draw image (Centered)
        if (sprite != null) {
            gc.drawImage(sprite, p.x - size/2, p.y - size/2, size, size);
        } else {
            // Fallback: Draw circle if image fails
            gc.setFill(p.team.equals("RED") ? Color.RED : Color.BLUE);
            gc.fillOval(p.x, p.y, r*2, r*2);
        }

        // Name Tag
        gc.setFill(Color.rgb(0, 0, 0, 0.5));
        gc.fillRoundRect(p.x - 25, p.y - 35, 50, 18, 10, 10);
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 10));
        gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
        gc.fillText(p.name, p.x, p.y - 22);

        // Stamina Bar
        gc.setFill(Color.BLACK);
        gc.fillRect(p.x - 15, p.y + 20, 30, 4);
        gc.setFill(p.stamina < 30 ? Color.RED : Color.LIGHTGREEN);
        gc.fillRect(p.x - 14, p.y + 21, 28 * (p.stamina / 100.0), 2);
    }

    private void drawBallSprite(double x, double y) {
        double r = Constants.BALL_RADIUS;
        double size = r * 2.2;

        if (imgBall != null) {
            gc.drawImage(imgBall, x - size/2, y - size/2, size, size);
        } else {
            gc.setFill(Color.WHITE);
            gc.fillOval(x - r, y - r, r*2, r*2);
        }
    }

    private void drawPitch(Color lightGrass, Color darkGrass) {
        gc.setFill(lightGrass);
        gc.fillRect(0, 0, Constants.WIDTH, Constants.HEIGHT);
        gc.setFill(darkGrass);
        for (int i = 0; i < Constants.WIDTH; i += 100) {
            if ((i / 100) % 2 == 0) {
                gc.fillRect(i, 0, 100, Constants.HEIGHT);
            }
        }
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(3);
        gc.strokeRect(50, 50, Constants.WIDTH - 100, Constants.HEIGHT - 100);
        gc.strokeLine(Constants.WIDTH / 2.0, 50, Constants.WIDTH / 2.0, Constants.HEIGHT - 50);
        gc.strokeOval(Constants.WIDTH / 2.0 - 70, Constants.HEIGHT / 2.0 - 70, 140, 140);
        gc.strokeRect(50, Constants.HEIGHT / 2.0 - 150, 150, 300);
        gc.strokeRect(Constants.WIDTH - 200, Constants.HEIGHT / 2.0 - 150, 150, 300);
    }

    private void drawHUD(GameState state) {
        gc.setFill(Color.rgb(0, 0, 0, 0.5));
        gc.fillRoundRect(Constants.WIDTH/2.0 - 80, 10, 160, 80, 20, 20);

        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 30));
        gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
        gc.fillText(state.scoreRed + " - " + state.scoreBlue, Constants.WIDTH/2.0, 50);

        gc.setFont(Font.font("Monospaced", FontWeight.BOLD, 20));
        gc.setFill(Color.YELLOW);
        gc.fillText(state.timeString, Constants.WIDTH/2.0, 80);

        gc.setFont(Font.font("Arial", 16));
        gc.setFill(Color.WHITE);
        gc.setTextAlign(javafx.scene.text.TextAlignment.LEFT);
        gc.fillText("Weather: " + state.weather, 20, 40);

        if (state.currentPhase == GameState.Phase.COUNTDOWN) {
            gc.setFill(Color.YELLOW);
            gc.setStroke(Color.BLACK);
            gc.setLineWidth(3);
            gc.setFont(Font.font("Arial", FontWeight.BOLD, 80));
            gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);

            String text = (state.countdownValue <= 0) ? "GO!" : String.valueOf(state.countdownValue);
            gc.fillText(text, Constants.WIDTH/2.0, Constants.HEIGHT/2.0);
            gc.strokeText(text, Constants.WIDTH/2.0, Constants.HEIGHT/2.0);
        }
    }

    private void drawGameOverScreen(GameState state) {
        gc.setFill(Color.rgb(0, 0, 0, 0.85));
        gc.fillRect(0, 0, Constants.WIDTH, Constants.HEIGHT);

        gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
        gc.setFill(Color.GOLD);
        gc.setFont(Font.font("Arial Black", FontWeight.BOLD, 60));
        gc.fillText("GAME OVER", Constants.WIDTH/2.0, 200);

        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 40));
        gc.fillText("WINNER: " + state.winner, Constants.WIDTH/2.0, 280);

        gc.setFont(Font.font("Arial", 20));
        int yOffset = 400;
        gc.fillText("--- Top Scorers ---", Constants.WIDTH/2.0, 360);

        for (GameState.PlayerState p : state.players) {
            if (!p.isBot && p.goals > 0) {
                gc.fillText(p.name + ": " + p.goals + " Goals", Constants.WIDTH/2.0, yOffset);
                yOffset += 30;
            }
        }
    }

    private void drawRain() {
        gc.setStroke(Color.rgb(200, 200, 255, 0.4));
        gc.setLineWidth(2);
        for (int i = 0; i < 150; i++) {
            double rx = Math.random() * Constants.WIDTH;
            double ry = Math.random() * Constants.HEIGHT;
            gc.strokeLine(rx, ry, rx - 10, ry + 20);
        }
    }
}