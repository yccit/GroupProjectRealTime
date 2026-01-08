package com.soccer.client;

import com.soccer.client.input.KeyHandler;
import com.soccer.client.ui.GamePanel;
import com.soccer.common.Constants;
import com.soccer.common.GameState;
import com.soccer.common.InputPacket;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientMain extends Application {
    private Stage primaryStage;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private volatile boolean isConnected = false;

    // Generate a random ID so the server knows who sent the packet
    private final int clientId = (int) (Math.random() * 1000000);
    private GameState currentState;
    private InputPacket currentInput = new InputPacket();
    private String playerName;
    private boolean isGameEnding = false;

    // Flag to prevent the game screen from reloading repeatedly
    private boolean isGameScreenActive = false;

    private ListView<String> lobbyListRed = new ListView<>();
    private ListView<String> lobbyListBlue = new ListView<>();
    private GamePanel gamePanel;

    public static void main(String[] args) { launch(args); }

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        this.primaryStage.setTitle("Soccer Client - ID: " + clientId);
        showLoginScreen();
    }

    // Loads the stadium background image
    private Background createStadiumBackground() {
        try {
            // Make sure bg.jpg is in the resources folder
            String bgPath = getClass().getResource("/images/bg.jpg").toExternalForm();
            Image bgImage = new Image(bgPath);
            return new Background(new BackgroundImage(bgImage, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.CENTER, new BackgroundSize(1.0, 1.0, true, true, false, false)));
        } catch (Exception e) {
            // Fallback color if image fails to load
            return new Background(new BackgroundFill(Color.rgb(44, 62, 80), null, null));
        }
    }

    // --- SCENE 1: Login Screen ---
    private void showLoginScreen() {
        VBox root = new VBox(25); root.setAlignment(Pos.CENTER); root.setBackground(createStadiumBackground());
        VBox container = new VBox(20); container.setAlignment(Pos.CENTER); container.setMaxWidth(400); container.setPadding(new javafx.geometry.Insets(40));
        container.setStyle("-fx-background-color: rgba(0, 0, 0, 0.75); -fx-background-radius: 20;");

        Label title = new Label("ULTIMATE SOCCER"); title.setStyle("-fx-text-fill: white; -fx-font-family: 'Arial Black'; -fx-font-size: 32px;");

        // Input fields for Server IP and Player Name
        TextField ipField = new TextField("localhost"); ipField.setPromptText("IP Address");
        TextField nameField = new TextField(); nameField.setPromptText("Name");
        Button joinBtn = new Button("JOIN MATCH"); joinBtn.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold;");
        Label statusLabel = new Label(""); statusLabel.setTextFill(Color.web("#ff6b6b"));

        joinBtn.setOnAction(e -> {
            String ip = ipField.getText().trim(); String name = nameField.getText().trim();
            if (!name.isEmpty() && !ip.isEmpty()) {
                playerName = name; joinBtn.setDisable(true); joinBtn.setText("CONNECTING..."); statusLabel.setText("");
                // Run connection in a separate thread so UI doesn't freeze
                new Thread(() -> connectToServer(ip, name, statusLabel, joinBtn)).start();
            } else statusLabel.setText("Please enter IP and Name!");
        });

        container.getChildren().addAll(title, ipField, nameField, joinBtn, statusLabel);
        root.getChildren().add(container);
        primaryStage.setScene(new Scene(root, 800, 600)); primaryStage.show();
    }

    // Handles the network connection logic
    private void connectToServer(String ip, String name, Label statusLabel, Button joinBtn) {
        try {
            socket = new Socket(ip, Constants.PORT);
            out = new ObjectOutputStream(socket.getOutputStream()); out.flush();
            in = new ObjectInputStream(socket.getInputStream()); isConnected = true;

            // Send initial JOIN request
            InputPacket joinPacket = new InputPacket(); joinPacket.command = "JOIN"; joinPacket.id = clientId; joinPacket.playerName = name;
            sendPacket(joinPacket);

            // Wait for server response (OK or FAIL)
            try {
                Object response = in.readObject();
                if (response instanceof String) {
                    String msg = (String) response;
                    if (msg.startsWith("FAIL:")) {
                        String reason = msg.substring(5);
                        // If rejected, show error and re-enable button
                        Platform.runLater(() -> { statusLabel.setText(reason); joinBtn.setDisable(false); joinBtn.setText("JOIN MATCH"); });
                        socket.close(); return;
                    }
                }
            } catch (Exception e) { return; }

            // If success, go to Lobby
            Platform.runLater(this::showLobbyScreen);

            // Start listening loop
            while (isConnected) {
                Object obj = in.readObject();
                if (obj instanceof GameState) {
                    GameState newState = (GameState) obj;
                    handleServerState(newState);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Platform.runLater(() -> { statusLabel.setText("Connection Failed: " + e.getMessage()); joinBtn.setDisable(false); joinBtn.setText("JOIN MATCH"); });
        }
    }

    // --- SCENE 2: Lobby Screen ---
    private void showLobbyScreen() {
        isGameScreenActive = false; // Reset flag since we are back in lobby

        BorderPane root = new BorderPane(); root.setBackground(createStadiumBackground());
        Label title = new Label("MATCH LOBBY"); title.setStyle("-fx-text-fill: white; -fx-font-family: 'Arial Black'; -fx-font-size: 28px;");
        HBox topBox = new HBox(title); topBox.setAlignment(Pos.CENTER); topBox.setPadding(new javafx.geometry.Insets(20)); topBox.setStyle("-fx-background-color: rgba(0,0,0,0.6);"); root.setTop(topBox);

        HBox listsBox = new HBox(40); listsBox.setAlignment(Pos.CENTER);
        lobbyListRed.setStyle("-fx-background-color: rgba(255,255,255,0.9);"); lobbyListRed.setPrefHeight(300);
        lobbyListBlue.setStyle("-fx-background-color: rgba(255,255,255,0.9);"); lobbyListBlue.setPrefHeight(300);

        VBox redBox = new VBox(10, new Label("RED TEAM"), lobbyListRed); redBox.setAlignment(Pos.CENTER);
        VBox blueBox = new VBox(10, new Label("BLUE TEAM"), lobbyListBlue); blueBox.setAlignment(Pos.CENTER);
        listsBox.getChildren().addAll(redBox, blueBox); root.setCenter(listsBox);

        Button startBtn = new Button("START MATCH"); startBtn.setStyle("-fx-background-color: #e67e22; -fx-text-fill: white; -fx-font-weight: bold;");
        startBtn.setOnAction(e -> sendCommand("START"));
        HBox bottomBox = new HBox(startBtn); bottomBox.setAlignment(Pos.CENTER); bottomBox.setPadding(new javafx.geometry.Insets(30)); root.setBottom(bottomBox);

        primaryStage.setScene(new Scene(root, 800, 600));

        // Immediately populate list if data exists
        if (currentState != null) {
            updateLobbyLists(currentState);
        }
    }

    // --- SCENE 3: Game Screen ---
    private void showGameScreen() {
        isGameScreenActive = true; // Mark game screen as active

        BorderPane root = new BorderPane();
        gamePanel = new GamePanel(clientId);

        // Setup the "End Game" button click handler
        gamePanel.onEndGameClicked = () -> { isGameEnding = true; sendCommand("END"); };

        root.setCenter(gamePanel);
        Scene gameScene = new Scene(root);

        // Hook up keyboard controls
        gameScene.setOnKeyPressed(e -> KeyHandler.handle(e.getCode(), true, currentInput));
        gameScene.setOnKeyReleased(e -> KeyHandler.handle(e.getCode(), false, currentInput));
        primaryStage.setScene(gameScene);

        // Start the game loop (rendering)
        new javafx.animation.AnimationTimer() {
            @Override public void handle(long now) {
                if (isGameScreenActive && currentState != null) {
                    gamePanel.render(currentState);
                }
            }
        }.start();
    }

    // Main logic to route server updates to correct screen
    private void handleServerState(GameState newState) {
        this.currentState = newState;
        currentInput.id = clientId;

        // Only send inputs if game is actually running
        if (!isGameEnding && (newState.currentPhase == GameState.Phase.PLAYING || newState.currentPhase == GameState.Phase.COUNTDOWN)) {
            sendPacket(currentInput);
            // Reset one-time triggers
            currentInput.adminApproveSignal = false;
            currentInput.shoot = false;
        }

        Platform.runLater(() -> {
            // Case 1: Back to Lobby (Waiting Phase)
            if (newState.currentPhase == GameState.Phase.WAITING) {
                if (isGameScreenActive) {
                    showLobbyScreen(); // Switch scene back to lobby
                }
                updateLobbyLists(newState);
            }
            // Case 2: Game Started
            else if ((newState.currentPhase == GameState.Phase.COUNTDOWN || newState.currentPhase == GameState.Phase.PLAYING)) {
                // Only switch scene if we aren't already there
                if (!isGameScreenActive) {
                    showGameScreen();
                }
            }
            // Case 3: Game Over
            else if (newState.currentPhase == GameState.Phase.GAME_OVER) {
                isGameEnding = false;
            }
        });
    }

    private void updateLobbyLists(GameState state) {
        lobbyListRed.getItems().clear(); lobbyListBlue.getItems().clear();
        for (GameState.PlayerState p : state.players) {
            String entry = p.name + (p.isBot ? " (BOT)" : "");
            if ("RED".equals(p.team)) lobbyListRed.getItems().add(entry);
            else lobbyListBlue.getItems().add(entry);
        }
    }

    private synchronized void sendPacket(InputPacket packet) {
        if (out == null) return;
        try { packet.id = clientId; out.reset(); out.writeObject(packet); out.flush(); } catch (IOException e) {}
    }

    private void sendCommand(String cmd) { InputPacket pkt = new InputPacket(); pkt.command = cmd; pkt.id = clientId; sendPacket(pkt); }

    @Override
    public void stop() throws Exception {
        super.stop();
        isConnected = false;
        if (socket != null && !socket.isClosed()) {
            try { socket.close(); } catch (Exception e) {}
        }
        System.out.println("Client Stopped. Exiting system.");
        System.exit(0);
    }
}