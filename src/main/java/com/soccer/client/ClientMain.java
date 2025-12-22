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
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientMain extends Application {
    private Stage primaryStage;

    // --- Network Variables ---
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private volatile boolean isConnected = false;

    // --- Game State ---
    private GameState currentState;
    private InputPacket currentInput = new InputPacket();
    private String playerName;

    // --- UI Components ---
    private ListView<String> lobbyListRed = new ListView<>();
    private ListView<String> lobbyListBlue = new ListView<>();

    // --- Custom Game Panel (The Renderer) ---
    private GamePanel gamePanel;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        showLoginScreen();
    }

    // --- Helper: Create Stadium Background Image ---
    private Background createStadiumBackground() {
        try {
            // Load local resource image
            String bgPath = getClass().getResource("/images/bg.jpg").toExternalForm();
            Image bgImage = new Image(bgPath);

            return new Background(new BackgroundImage(
                    bgImage,
                    BackgroundRepeat.NO_REPEAT,
                    BackgroundRepeat.NO_REPEAT,
                    BackgroundPosition.CENTER,
                    new BackgroundSize(1.0, 1.0, true, true, false, false) // Cover mode
            ));
        } catch (Exception e) {
            System.err.println("Failed to load background image. Please check src/main/resources/images/bg.jpg");
            // Fallback to dark background if image fails
            return new Background(new BackgroundFill(Color.rgb(44, 62, 80), null, null));
        }
    }

    // --- SCENE 1: LOGIN SCREEN ---
    private void showLoginScreen() {
        VBox root = new VBox(25);
        root.setAlignment(Pos.CENTER);
        root.setBackground(createStadiumBackground());

        // Container box with semi-transparent black background
        VBox container = new VBox(20);
        container.setAlignment(Pos.CENTER);
        container.setMaxWidth(400);
        container.setPadding(new javafx.geometry.Insets(40));
        container.setStyle("-fx-background-color: rgba(0, 0, 0, 0.75); -fx-background-radius: 20; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.8), 10, 0, 0, 0);");

        // Title
        Label title = new Label("ULTIMATE SOCCER");
        title.setStyle("-fx-text-fill: white; -fx-font-family: 'Arial Black'; -fx-font-size: 32px;");

        // Styling for inputs
        String inputStyle = "-fx-background-color: rgba(255, 255, 255, 0.9); -fx-background-radius: 30; -fx-padding: 10 20; -fx-font-size: 14px;";

        TextField ipField = new TextField("localhost");
        ipField.setPromptText("Server IP Address");
        ipField.setStyle(inputStyle);
        ipField.setMaxWidth(300);

        TextField nameField = new TextField();
        nameField.setPromptText("Enter Player Name");
        nameField.setStyle(inputStyle);
        nameField.setMaxWidth(300);

        // Styling for button
        Button joinBtn = new Button("JOIN MATCH");
        String btnStyle = "-fx-background-color: linear-gradient(to bottom, #2ecc71, #27ae60); " +
                "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px; " +
                "-fx-background-radius: 30; -fx-padding: 10 40; -fx-cursor: hand;";
        joinBtn.setStyle(btnStyle);

        // Hover effect
        joinBtn.setOnMouseEntered(e -> joinBtn.setStyle(btnStyle + "-fx-background-color: linear-gradient(to bottom, #27ae60, #2ecc71);"));
        joinBtn.setOnMouseExited(e -> joinBtn.setStyle(btnStyle));

        Label statusLabel = new Label("");
        statusLabel.setTextFill(Color.web("#ff6b6b"));

        joinBtn.setOnAction(e -> {
            String ip = ipField.getText().trim();
            String name = nameField.getText().trim();

            if (!name.isEmpty() && !ip.isEmpty()) {
                playerName = name;
                joinBtn.setDisable(true);
                joinBtn.setText("CONNECTING...");
                statusLabel.setText("");
                new Thread(() -> connectToServer(ip, name, statusLabel)).start();
            } else {
                statusLabel.setText("Please enter IP and Name!");
            }
        });

        container.getChildren().addAll(title, new Label(" "), ipField, nameField, new Label(" "), joinBtn, statusLabel);
        root.getChildren().add(container);

        primaryStage.setScene(new Scene(root, 800, 600));
        primaryStage.setTitle("Soccer Game - Login");
        primaryStage.show();
    }

    // --- SCENE 2: LOBBY SCREEN ---
    private void showLobbyScreen() {
        BorderPane root = new BorderPane();
        root.setBackground(createStadiumBackground());

        // Top Header
        Label title = new Label("MATCH LOBBY");
        title.setStyle("-fx-text-fill: white; -fx-font-family: 'Arial Black'; -fx-font-size: 28px;");
        HBox topBox = new HBox(title);
        topBox.setAlignment(Pos.CENTER);
        topBox.setPadding(new javafx.geometry.Insets(20));
        topBox.setStyle("-fx-background-color: rgba(0,0,0,0.6);");
        root.setTop(topBox);

        // Center Lists
        HBox listsBox = new HBox(40);
        listsBox.setAlignment(Pos.CENTER);
        listsBox.setPadding(new javafx.geometry.Insets(20));

        String listStyle = "-fx-background-color: rgba(255,255,255,0.9); -fx-background-radius: 10; -fx-font-size: 16px; -fx-control-inner-background: transparent;";
        lobbyListRed.setStyle(listStyle);
        lobbyListRed.setPrefHeight(300);
        lobbyListBlue.setStyle(listStyle);
        lobbyListBlue.setPrefHeight(300);

        VBox redBox = new VBox(10);
        redBox.setAlignment(Pos.CENTER);
        Label redLabel = new Label("RED TEAM");
        redLabel.setStyle("-fx-text-fill: #ff6b6b; -fx-font-weight: bold; -fx-font-size: 18px; -fx-effect: dropshadow(one-pass-box, black, 5, 0, 0, 0);");
        redBox.getChildren().addAll(redLabel, lobbyListRed);

        VBox blueBox = new VBox(10);
        blueBox.setAlignment(Pos.CENTER);
        Label blueLabel = new Label("BLUE TEAM");
        blueLabel.setStyle("-fx-text-fill: #48dbfb; -fx-font-weight: bold; -fx-font-size: 18px; -fx-effect: dropshadow(one-pass-box, black, 5, 0, 0, 0);");
        blueBox.getChildren().addAll(blueLabel, lobbyListBlue);

        listsBox.getChildren().addAll(redBox, blueBox);
        root.setCenter(listsBox);

        // Bottom Button
        Button startBtn = new Button("START MATCH");
        String startStyle = "-fx-background-color: linear-gradient(to bottom, #e67e22, #d35400); " +
                "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 20px; " +
                "-fx-background-radius: 30; -fx-padding: 15 60; -fx-cursor: hand; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 10, 0, 0, 5);";
        startBtn.setStyle(startStyle);
        startBtn.setOnAction(e -> sendCommand("START"));

        HBox bottomBox = new HBox(startBtn);
        bottomBox.setAlignment(Pos.CENTER);
        bottomBox.setPadding(new javafx.geometry.Insets(30));
        root.setBottom(bottomBox);

        primaryStage.setScene(new Scene(root, 800, 600));
        primaryStage.setTitle("Lobby - " + playerName);
    }

    // --- SCENE 3: GAMEPLAY SCREEN ---
    private void showGameScreen() {
        BorderPane root = new BorderPane();

        // Initialize GamePanel (Images will load automatically)
        gamePanel = new GamePanel();
        root.setCenter(gamePanel);

        Scene gameScene = new Scene(root);

        // Handle Key Input
        gameScene.setOnKeyPressed(e -> KeyHandler.handle(e.getCode(), true, currentInput));
        gameScene.setOnKeyReleased(e -> KeyHandler.handle(e.getCode(), false, currentInput));

        primaryStage.setScene(gameScene);
        primaryStage.setTitle("Soccer Game - Playing");

        // Main Rendering Loop
        new javafx.animation.AnimationTimer() {
            @Override
            public void handle(long now) {
                gamePanel.render(currentState);
            }
        }.start();
    }

    // --- NETWORKING LOGIC ---
    private void connectToServer(String ip, String name, Label statusLabel) {
        try {
            socket = new Socket(ip, Constants.PORT);
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());
            isConnected = true;

            InputPacket joinPacket = new InputPacket();
            joinPacket.command = "JOIN";
            joinPacket.playerName = name;
            sendPacket(joinPacket);

            Platform.runLater(this::showLobbyScreen);

            while (isConnected) {
                Object obj = in.readObject();
                if (obj instanceof GameState) {
                    GameState newState = (GameState) obj;
                    handleServerState(newState);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            Platform.runLater(() -> statusLabel.setText("Connection Failed: " + e.getMessage()));
        }
    }

    private void handleServerState(GameState newState) {
        this.currentState = newState;

        if (newState.currentPhase == GameState.Phase.PLAYING || newState.currentPhase == GameState.Phase.COUNTDOWN) {
            sendPacket(currentInput);
        }

        Platform.runLater(() -> {
            if (newState.currentPhase == GameState.Phase.WAITING) {
                updateLobbyLists(newState);
            }
            else if (newState.currentPhase == GameState.Phase.COUNTDOWN || newState.currentPhase == GameState.Phase.PLAYING) {
                if (primaryStage.getTitle().startsWith("Lobby")) {
                    showGameScreen();
                }
            }
        });
    }

    private void updateLobbyLists(GameState state) {
        lobbyListRed.getItems().clear();
        lobbyListBlue.getItems().clear();
        for (GameState.PlayerState p : state.players) {
            String entry = p.name + (p.isBot ? " (BOT)" : "");
            if ("RED".equals(p.team)) lobbyListRed.getItems().add(entry);
            else lobbyListBlue.getItems().add(entry);
        }
    }

    private synchronized void sendPacket(InputPacket packet) {
        if (out == null) return;
        try {
            out.reset();
            out.writeObject(packet);
            out.flush();
        } catch (IOException e) {
            System.err.println("Send Error: " + e.getMessage());
        }
    }

    private void sendCommand(String cmd) {
        InputPacket pkt = new InputPacket();
        pkt.command = cmd;
        sendPacket(pkt);
    }
}