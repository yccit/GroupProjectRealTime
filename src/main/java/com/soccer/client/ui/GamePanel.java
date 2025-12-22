package com.soccer.client.ui;

import com.soccer.common.Constants;
import com.soccer.common.GameState;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.HashMap;
import java.util.Map;

public class GamePanel extends Canvas {
    private GraphicsContext gc;

    // --- Image Assets ---
    private Image imgBall;

    // Body Images (Uses your original player files)
    private Image imgPlayerRed;
    private Image imgPlayerBlue;

    // New Modular Parts (Hands & Feet)
    private Image imgHandRed;
    private Image imgHandBlue;
    private Image imgFootRed;
    private Image imgFootBlue;

    // --- Animation State ---
    // Used to calculate movement for animation
    private Map<Integer, Double> lastX = new HashMap<>();
    private Map<Integer, Double> lastY = new HashMap<>();
    private long frameCounter = 0; // Timer for animation

    public GamePanel() {
        super(Constants.WIDTH, Constants.HEIGHT);
        this.gc = this.getGraphicsContext2D();
        loadImages(); // Load images on startup
    }

    private void loadImages() {
        try {
            // 1. Load Original Assets (Body/Head & Ball)
            imgBall = new Image(getClass().getResource("/images/ball.png").toExternalForm());
            imgPlayerRed = new Image(getClass().getResource("/images/player_red.png").toExternalForm());
            imgPlayerBlue = new Image(getClass().getResource("/images/player_blue.png").toExternalForm());

            // 2. Load New Modular Assets (Hands & Feet)
            // Make sure these files exist in src/main/resources/images/
            imgHandRed = new Image(getClass().getResource("/images/hand_red.png").toExternalForm());
            imgHandBlue = new Image(getClass().getResource("/images/hand_blue.png").toExternalForm());
            imgFootRed = new Image(getClass().getResource("/images/foot_red.png").toExternalForm());
            imgFootBlue = new Image(getClass().getResource("/images/foot_blue.png").toExternalForm());

            System.out.println("All image assets (including modular parts) loaded successfully!");
        } catch (Exception e) {
            System.err.println("!!! Failed to load images !!!");
            System.err.println("Please check if hand_red.png, foot_red.png, etc. exist.");
            // We don't print stack trace here to keep console clean, but you can add it back if needed.
        }
    }

    public void render(GameState currentState) {
        if (currentState == null) return;

        frameCounter++; // Increment animation timer

        // 1. Draw Pitch
        if ("RAINY".equals(currentState.weather)) {
            drawPitch(Color.web("#1e5128"), Color.web("#143d1d")); // Darker for rain
        } else {
            drawPitch(Color.web("#2ecc71"), Color.web("#27ae60")); // Bright for sunny
        }

        // 2. Draw Players (Now using the new Modular Drawer)
        for (GameState.PlayerState p : currentState.players) {
            drawModularPlayer(p);
        }

        // 3. Draw Ball
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

    // --- New Modular Player Rendering Method ---
    private void drawModularPlayer(GameState.PlayerState p) {
        double r = Constants.PLAYER_RADIUS;

        // Define sizes for parts relative to the player radius
        double bodySize = r * 2.8;  // Main body size
        double handSize = bodySize * 0.35; // Hands are smaller
        double footSize = bodySize * 0.40; // Feet are smaller

        // 1. Detect Movement & Animation
        boolean isMoving = false;
        if (lastX.containsKey(p.id)) {
            // Check if position changed since last frame
            if (Math.abs(p.x - lastX.get(p.id)) > 0.1 || Math.abs(p.y - lastY.get(p.id)) > 0.1) {
                isMoving = true;
            }
        }
        // Update history
        lastX.put(p.id, p.x);
        lastY.put(p.id, p.y);

        // Calculate swing offset (Simple sine wave animation)
        // If moving, feet swing back and forth. If not, offset is 0.
        double swingOffset = isMoving ? Math.sin(frameCounter * 0.2) * 6 : 0;

        // 2. Select Team Assets
        boolean isRed = p.team.equals("RED");
        Image bodyImg = isRed ? imgPlayerRed : imgPlayerBlue; // Use original player img as body
        Image handImg = isRed ? imgHandRed : imgHandBlue;
        Image footImg = isRed ? imgFootRed : imgFootBlue;

        // 3. Draw Shadow (Base)
        gc.setFill(Color.rgb(0, 0, 0, 0.3));
        gc.fillOval(p.x + 2, p.y + 5, r * 2.2, r * 2.2);

        // --- DRAW PARTS (Layer order: Feet -> Hands -> Body) ---

        // A. Draw FEET (Bottom layer, animating)
        if (footImg != null) {
            // Left Foot (Swings forward)
            double lfX = p.x - bodySize * 0.25;
            double lfY = p.y + bodySize * 0.25 + swingOffset;
            drawPart(footImg, lfX, lfY, footSize);

            // Right Foot (Swings backward)
            double rfX = p.x + bodySize * 0.25;
            double rfY = p.y + bodySize * 0.25 - swingOffset;
            drawPart(footImg, rfX, rfY, footSize);
        }

        // B. Draw HANDS (Middle layer, mostly static)
        if (handImg != null) {
            // Left Hand
            drawPart(handImg, p.x - bodySize * 0.4, p.y, handSize);
            // Right Hand
            drawPart(handImg, p.x + bodySize * 0.4, p.y, handSize);
        }

        // C. Draw BODY (Top layer)
        if (bodyImg != null) {
            drawPart(bodyImg, p.x, p.y, bodySize);
        } else {
            // Fallback: Draw circle if body image missing
            gc.setFill(isRed ? Color.RED : Color.BLUE);
            gc.fillOval(p.x, p.y, r*2, r*2);
        }

        // 4. Overlays (Name & Stamina)
        drawPlayerOverlays(p);
    }

    // Helper to draw an image centered at (cx, cy)
    private void drawPart(Image img, double cx, double cy, double size) {
        gc.drawImage(img, cx - size / 2, cy - size / 2, size, size);
    }

    private void drawPlayerOverlays(GameState.PlayerState p) {
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

    // --- Original Helpers (Unchanged) ---

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