package com.soccer.client.ui;

import com.soccer.common.Constants;
import com.soccer.common.GameState;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.util.ArrayList; // Needed for sorting list
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// This class handles all the drawing. It's basically the screen renderer.
public class GamePanel extends Canvas {
    private GraphicsContext gc;

    // Image Assets
    // Loaded these so we don't just have circles
    private Image imgBall;
    private Image imgPlayerRed;
    private Image imgPlayerBlue;
    private Image imgHandRed;
    private Image imgHandBlue;
    private Image imgFootRed;
    private Image imgFootBlue;

    // Animation State
    // Keep track of where players were last frame to calculate movement for animation
    private Map<Integer, Double> lastX = new HashMap<>();
    private Map<Integer, Double> lastY = new HashMap<>();
    private long frameCounter = 0;

    // Button Coordinates (End Game Button)
    // Hardcoded position for the top-right button
    private final double BTN_X = Constants.WIDTH - 140;
    private final double BTN_Y = 20;
    private final double BTN_W = 120;
    private final double BTN_H = 40;

    // Callback interface -> notifies ClientMain when button is clicked
    public Runnable onEndGameClicked;

    // Store my own ID so I know if I am approved or not
    private int myClientId;

    // Constructor: Now takes ID to identify 'myself'
    public GamePanel(int clientId) {
        super(Constants.WIDTH, Constants.HEIGHT);
        this.myClientId = clientId; // Save ID
        this.gc = this.getGraphicsContext2D();
        loadImages();

        // Mouse Click Detection
        this.setOnMouseClicked(event -> {
            double mx = event.getX();
            double my = event.getY();

            // Check if user clicked inside the "End Game" button box
            if (mx >= BTN_X && mx <= BTN_X + BTN_W && my >= BTN_Y && my <= BTN_Y + BTN_H) {
                // Only allow clicking if the game is actually running (not waiting for approval)
                System.out.println("End Game Button Clicked!");
                if (onEndGameClicked != null) {
                    onEndGameClicked.run(); // Trigger the callback
                }
            }
        });
    }

    // Load all the png files from resources
    private void loadImages() {
        try {
            imgBall = new Image(getClass().getResource("/images/ball.png").toExternalForm());
            imgPlayerRed = new Image(getClass().getResource("/images/player_red.png").toExternalForm());
            imgPlayerBlue = new Image(getClass().getResource("/images/player_blue.png").toExternalForm());
            imgHandRed = new Image(getClass().getResource("/images/hand_red.png").toExternalForm());
            imgHandBlue = new Image(getClass().getResource("/images/hand_blue.png").toExternalForm());
            imgFootRed = new Image(getClass().getResource("/images/foot_red.png").toExternalForm());
            imgFootBlue = new Image(getClass().getResource("/images/foot_blue.png").toExternalForm());
        } catch (Exception e) {
            // If images fail, we just print error but game might still run with shapes
            System.err.println("!!! Failed to load images: " + e.getMessage());
        }
    }

    // Main drawing loop, called 60 times a second
    public void render(GameState currentState) {
        if (currentState == null) return; // Safety check

        // CHECK ADMIN APPROVAL
        // Need to find my own player object in the list
        GameState.PlayerState myPlayer = null;
        for (GameState.PlayerState p : currentState.players) {
            if (p.id == myClientId) {
                myPlayer = p;
                break;
            }
        }

        // If I exist but Admin hasn't approved me, show the black screen
        if (myPlayer != null && !myPlayer.isApproved) {
            drawWaitingScreen();
            return; // Don't draw the field, just stop here
        }

        // IF APPROVED, DRAW NORMAL GAME
        frameCounter++;

        // 1. Draw Pitch (Background)
        // darker green if it's raining
        if ("RAINY".equals(currentState.weather)) {
            drawPitch(Color.web("#1e5128"), Color.web("#143d1d"));
        } else {
            drawPitch(Color.web("#2ecc71"), Color.web("#27ae60"));
        }

        // 2. Draw Players
        for (GameState.PlayerState p : currentState.players) {
            drawModularPlayer(p); // complex drawing with hands/feet
        }

        // 3. Draw Ball
        drawBallSprite(currentState.ballX, currentState.ballY);

        // 4. Weather effects
        if ("RAINY".equals(currentState.weather)) drawRain();

        // 5. HUD (Score and Time)
        drawHUD(currentState);

        // 6. Draw "End Game" Button (Only show this while playing)
        if (currentState.currentPhase == GameState.Phase.PLAYING) {
            drawEndGameButton();
        }

        // 7. Game Over Screen (Result Page)
        if (currentState.currentPhase == GameState.Phase.GAME_OVER) {
            drawGameOverScreen(currentState);
        }
    }

    // Just a black background with white text
    private void drawWaitingScreen() {
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, Constants.WIDTH, Constants.HEIGHT);

        gc.setFill(Color.WHITE);
        gc.setTextAlign(TextAlignment.CENTER);

        // Main Message
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 40));
        gc.fillText("WAITING FOR ADMIN APPROVAL...", Constants.WIDTH / 2.0, Constants.HEIGHT / 2.0 - 50);

        // Instructions for the user
        gc.setFont(Font.font("Arial", 20));
        gc.setFill(Color.LIGHTGRAY);
        gc.fillText("(Admin: Press 'P' to approve players)", Constants.WIDTH / 2.0, Constants.HEIGHT / 2.0 + 20);
    }

    // Draw the red button top right
    private void drawEndGameButton() {
        // Semi-transparent red background
        gc.setFill(Color.rgb(255, 50, 50, 0.8));
        gc.fillRoundRect(BTN_X, BTN_Y, BTN_W, BTN_H, 10, 10);

        // White border
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(2);
        gc.strokeRoundRect(BTN_X, BTN_Y, BTN_W, BTN_H, 10, 10);

        // Text
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText("END GAME", BTN_X + BTN_W / 2, BTN_Y + 26);
    }

    // The fancy result screen
    private void drawGameOverScreen(GameState state) {
        double w = Constants.WIDTH;
        double h = Constants.HEIGHT;

        // Dim the background
        gc.setFill(Color.rgb(0, 0, 0, 0.85));
        gc.fillRect(0, 0, w, h);

        // Setup panel dimensions
        double panelW = 500;
        double panelH = 420;
        double panelX = (w - panelW) / 2;
        double panelY = (h - panelH) / 2;

        // Shadow
        gc.setFill(Color.rgb(0, 0, 0, 0.5));
        gc.fillRoundRect(panelX + 15, panelY + 15, panelW, panelH, 30, 30);

        // Main Card
        gc.setFill(Color.web("#2c3e50"));
        gc.fillRoundRect(panelX, panelY, panelW, panelH, 30, 30);

        // Gold Border because it looks cool
        gc.setStroke(Color.GOLD);
        gc.setLineWidth(4);
        gc.strokeRoundRect(panelX, panelY, panelW, panelH, 30, 30);

        // Title
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFill(Color.GOLD);
        gc.setFont(Font.font("Arial Black", FontWeight.BOLD, 48));
        gc.fillText("MATCH RESULT", w / 2, panelY + 70);

        // Logic to decide who won
        String winnerText = "DRAW";
        Color winnerColor = Color.WHITE;

        if (state.winner != null && state.winner.toUpperCase().contains("RED")) {
            winnerText = "RED TEAM WINS!";
            winnerColor = Color.web("#ff6b6b");
        } else if (state.winner != null && state.winner.toUpperCase().contains("BLUE")) {
            winnerText = "BLUE TEAM WINS!";
            winnerColor = Color.web("#48dbfb");
        } else {
            winnerText = "MATCH DRAW";
            winnerColor = Color.LIGHTGRAY;
        }

        // Draw winner text
        gc.setFill(winnerColor);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 36));
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1);
        gc.strokeText(winnerText, w / 2, panelY + 130);
        gc.fillText(winnerText, w / 2, panelY + 130);

        // Score display box
        double scoreBoxW = 220;
        double scoreBoxH = 60;
        double scoreBoxX = (w - scoreBoxW) / 2;
        double scoreBoxY = panelY + 150;

        gc.setFill(Color.rgb(0, 0, 0, 0.4));
        gc.fillRoundRect(scoreBoxX, scoreBoxY, scoreBoxW, scoreBoxH, 20, 20);

        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Monospaced", FontWeight.BOLD, 42));
        gc.fillText(state.scoreRed + " - " + state.scoreBlue, w / 2, scoreBoxY + 45);

        // Top Scorers Section
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        gc.setFill(Color.YELLOW);
        gc.fillText("★ TOP SCORERS ★", w / 2, panelY + 250);

        gc.setStroke(Color.GRAY);
        gc.setLineWidth(1);
        gc.strokeLine(panelX + 60, panelY + 260, panelX + panelW - 60, panelY + 260);

        int yOffset = (int) (panelY + 290);
        boolean hasScorer = false;
        int count = 0;

        List<GameState.PlayerState> sortedPlayers = new ArrayList<>(state.players);
        sortedPlayers.sort((p1, p2) -> Integer.compare(p2.goals, p1.goals)); // High to Low

        for (GameState.PlayerState p : sortedPlayers) {
            if (p.goals > 0) {
                hasScorer = true;
                if (count >= 3) break; // Only show top 3

                gc.setFill(p.team.equals("RED") ? Color.PINK : Color.LIGHTBLUE);
                gc.setFont(Font.font("Arial", 16));
                gc.fillText(p.name + "   :   " + p.goals + " Goals", w / 2, yOffset);
                yOffset += 25;
                count++;
            }
        }

        if (!hasScorer) {
            gc.setFill(Color.GRAY);
            gc.setFont(Font.font("Arial", 14));
            gc.fillText("(No goals scored)", w / 2, yOffset);
        }

        // Footer hint
        gc.setFill(Color.rgb(255, 255, 255, 0.4));
        gc.setFont(Font.font("Arial", 10));
        gc.fillText("Re-run application to start a new match", w / 2, panelY + panelH - 15);
    }

    // DRAW PLAYER LOGIC
    // Draws hands, feet, and body separately for animation
    private void drawModularPlayer(GameState.PlayerState p) {
        double r = Constants.PLAYER_RADIUS;
        double bodySize = r * 2.8;
        double handSize = bodySize * 0.35;
        double footSize = bodySize * 0.40;

        // Check if player moved since last frame
        boolean isMoving = false;
        if (lastX.containsKey(p.id)) {
            if (Math.abs(p.x - lastX.get(p.id)) > 0.1 || Math.abs(p.y - lastY.get(p.id)) > 0.1) {
                isMoving = true;
            }
        }
        // Update history
        lastX.put(p.id, p.x);
        lastY.put(p.id, p.y);

        // Simple sine wave for leg swinging effect
        double swingOffset = isMoving ? Math.sin(frameCounter * 0.2) * 6 : 0;

        boolean isRed = p.team.equals("RED");
        Image bodyImg = isRed ? imgPlayerRed : imgPlayerBlue;
        Image handImg = isRed ? imgHandRed : imgHandBlue;
        Image footImg = isRed ? imgFootRed : imgFootBlue;

        // Shadow
        gc.setFill(Color.rgb(0, 0, 0, 0.3));
        gc.fillOval(p.x + 2, p.y + 5, r * 2.2, r * 2.2);

        // Feet (with animation offset)
        if (footImg != null) {
            drawPart(footImg, p.x - bodySize * 0.25, p.y + bodySize * 0.25 + swingOffset, footSize);
            drawPart(footImg, p.x + bodySize * 0.25, p.y + bodySize * 0.25 - swingOffset, footSize);
        }
        // Hands
        if (handImg != null) {
            drawPart(handImg, p.x - bodySize * 0.4, p.y, handSize);
            drawPart(handImg, p.x + bodySize * 0.4, p.y, handSize);
        }
        // Body
        if (bodyImg != null) {
            drawPart(bodyImg, p.x, p.y, bodySize);
        } else {
            // Fallback to simple circle if image fails
            gc.setFill(isRed ? Color.RED : Color.BLUE);
            gc.fillOval(p.x, p.y, r*2, r*2);
        }
        drawPlayerOverlays(p);
    }

    private void drawPart(Image img, double cx, double cy, double size) {
        gc.drawImage(img, cx - size / 2, cy - size / 2, size, size);
    }

    // Draws name tag and stamina bar
    private void drawPlayerOverlays(GameState.PlayerState p) {
        // Name background box
        gc.setFill(Color.rgb(0, 0, 0, 0.5));
        gc.fillRoundRect(p.x - 25, p.y - 35, 50, 18, 10, 10);

        // Name text
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 10));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText(p.name, p.x, p.y - 22);

        // Stamina bar
        gc.setFill(Color.BLACK);
        gc.fillRect(p.x - 15, p.y + 20, 30, 4);
        gc.setFill(p.stamina < 30 ? Color.RED : Color.LIGHTGREEN);
        gc.fillRect(p.x - 14, p.y + 21, 28 * (p.stamina / 100.0), 2);
    }

    // Draws the ball
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

    // Draw the green field and lines
    private void drawPitch(Color lightGrass, Color darkGrass) {
        gc.setFill(lightGrass);
        gc.fillRect(0, 0, Constants.WIDTH, Constants.HEIGHT);
        gc.setFill(darkGrass);

        // Striped pattern
        for (int i = 0; i < Constants.WIDTH; i += 100) {
            if ((i / 100) % 2 == 0) gc.fillRect(i, 0, 100, Constants.HEIGHT);
        }

        // White lines
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(3);
        gc.strokeRect(50, 50, Constants.WIDTH - 100, Constants.HEIGHT - 100);
        gc.strokeLine(Constants.WIDTH / 2.0, 50, Constants.WIDTH / 2.0, Constants.HEIGHT - 50);
        gc.strokeOval(Constants.WIDTH / 2.0 - 70, Constants.HEIGHT / 2.0 - 70, 140, 140);

        // Goal areas
        gc.strokeRect(50, Constants.HEIGHT / 2.0 - 150, 150, 300);
        gc.strokeRect(Constants.WIDTH - 200, Constants.HEIGHT / 2.0 - 150, 150, 300);
    }

    // Draws Score, Time, and Weather info
    private void drawHUD(GameState state) {
        // Background for score
        gc.setFill(Color.rgb(0, 0, 0, 0.5));
        gc.fillRoundRect(Constants.WIDTH/2.0 - 80, 10, 160, 80, 20, 20);

        // Score
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 30));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText(state.scoreRed + " - " + state.scoreBlue, Constants.WIDTH/2.0, 50);

        // Timer
        gc.setFont(Font.font("Monospaced", FontWeight.BOLD, 20));
        gc.setFill(Color.YELLOW);
        gc.fillText(state.timeString, Constants.WIDTH/2.0, 80);

        // Weather text top left
        gc.setFont(Font.font("Arial", 16));
        gc.setFill(Color.WHITE);
        gc.setTextAlign(TextAlignment.LEFT);
        gc.fillText("Weather: " + state.weather, 20, 40);

        // Big countdown text if game starting
        if (state.currentPhase == GameState.Phase.COUNTDOWN) {
            gc.setFill(Color.YELLOW);
            gc.setStroke(Color.BLACK);
            gc.setLineWidth(3);
            gc.setFont(Font.font("Arial", FontWeight.BOLD, 80));
            gc.setTextAlign(TextAlignment.CENTER);
            String text = (state.countdownValue <= 0) ? "GO!" : String.valueOf(state.countdownValue);
            gc.fillText(text, Constants.WIDTH/2.0, Constants.HEIGHT/2.0);
            gc.strokeText(text, Constants.WIDTH/2.0, Constants.HEIGHT/2.0);
        }
    }

    // Simple rain effect
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