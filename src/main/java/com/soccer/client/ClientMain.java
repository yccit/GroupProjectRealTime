package com.soccer.client;

import com.soccer.common.Constants;
import com.soccer.common.GameState;
import com.soccer.common.InputPacket;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
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

    // 网络相关
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private volatile boolean isConnected = false;

    // 游戏状态
    private GameState currentState;
    private InputPacket currentInput = new InputPacket();
    private String playerName;

    // UI 组件
    private ListView<String> lobbyListRed = new ListView<>();
    private ListView<String> lobbyListBlue = new ListView<>();
    private Canvas gameCanvas;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        showLoginScreen();
    }

    // --- 场景 1: 登录 ---
    private void showLoginScreen() {
        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #2c3e50;");

        Label title = new Label("REAL-TIME SOCCER");
        title.setTextFill(Color.WHITE);
        title.setFont(Font.font("Arial", FontWeight.BOLD, 30));

        TextField ipField = new TextField("localhost");
        ipField.setPromptText("Server IP Address");
        ipField.setMaxWidth(200);

        TextField nameField = new TextField();
        nameField.setPromptText("Enter your name");
        nameField.setMaxWidth(200);

        Button joinBtn = new Button("JOIN GAME");
        joinBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-size: 16px;");

        Label statusLabel = new Label("");
        statusLabel.setTextFill(Color.RED);

        joinBtn.setOnAction(e -> {
            String ip = ipField.getText().trim();
            String name = nameField.getText().trim();

            if (!name.isEmpty() && !ip.isEmpty()) {
                playerName = name;
                joinBtn.setDisable(true);
                statusLabel.setText("Connecting...");
                new Thread(() -> connectToServer(ip, name, statusLabel)).start();
            }
        });

        root.getChildren().addAll(title, new Label("Server IP:"), ipField, new Label("Name:"), nameField, joinBtn, statusLabel);
        ((Label)root.getChildren().get(1)).setTextFill(Color.WHITE);
        ((Label)root.getChildren().get(3)).setTextFill(Color.WHITE);

        primaryStage.setScene(new Scene(root, 600, 400));
        primaryStage.setTitle("Soccer Game - Login");
        primaryStage.show();
    }

    // --- 场景 2: 大厅 ---
    private void showLobbyScreen() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #34495e;");

        Label title = new Label("GAME LOBBY - Waiting for players...");
        title.setTextFill(Color.WHITE);
        title.setFont(new Font(24));
        HBox topBox = new HBox(title);
        topBox.setAlignment(Pos.CENTER);
        topBox.setPadding(new javafx.geometry.Insets(20));
        root.setTop(topBox);

        HBox listsBox = new HBox(20);
        listsBox.setAlignment(Pos.CENTER);

        VBox redBox = new VBox(10, new Label("RED TEAM"), lobbyListRed);
        ((Label)redBox.getChildren().get(0)).setTextFill(Color.RED);

        VBox blueBox = new VBox(10, new Label("BLUE TEAM"), lobbyListBlue);
        ((Label)blueBox.getChildren().get(0)).setTextFill(Color.CYAN);

        listsBox.getChildren().addAll(redBox, blueBox);
        root.setCenter(listsBox);

        Button startBtn = new Button("START MATCH");
        startBtn.setStyle("-fx-background-color: #e67e22; -fx-text-fill: white; -fx-font-size: 18px;");
        startBtn.setOnAction(e -> sendCommand("START"));

        HBox bottomBox = new HBox(startBtn);
        bottomBox.setAlignment(Pos.CENTER);
        bottomBox.setPadding(new javafx.geometry.Insets(20));
        root.setBottom(bottomBox);

        primaryStage.setScene(new Scene(root, 800, 600));
        primaryStage.setTitle("Lobby - " + playerName);
    }

    // --- 场景 3: 游戏画面 ---
    private void showGameScreen() {
        BorderPane root = new BorderPane();
        gameCanvas = new Canvas(Constants.WIDTH, Constants.HEIGHT);
        root.setCenter(gameCanvas);

        Scene gameScene = new Scene(root);

        gameScene.setOnKeyPressed(e -> handleKey(e.getCode(), true));
        gameScene.setOnKeyReleased(e -> handleKey(e.getCode(), false));

        primaryStage.setScene(gameScene);
        primaryStage.setTitle("Soccer Game - Playing");

        new javafx.animation.AnimationTimer() {
            @Override
            public void handle(long now) {
                render(gameCanvas.getGraphicsContext2D());
            }
        }.start();
    }

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

    private void handleKey(KeyCode code, boolean isPressed) {
        switch (code) {
            case W -> currentInput.up = isPressed;
            case S -> currentInput.down = isPressed;
            case A -> currentInput.left = isPressed;
            case D -> currentInput.right = isPressed;
            case SPACE -> currentInput.shoot = isPressed;
        }
    }

    // ★★★ 修复后的 Render 方法 (去掉了 HALFTIME，增加了个人进球统计) ★★★
    private void render(GraphicsContext gc) {
        if (currentState == null) return;

        // 1. 背景
        if ("RAINY".equals(currentState.weather)) {
            gc.setFill(Color.rgb(30, 60, 30));
        } else {
            gc.setFill(Color.rgb(50, 150, 50));
        }
        gc.fillRect(0, 0, Constants.WIDTH, Constants.HEIGHT);

        // 2. 场地线
        gc.setStroke(Color.WHITE); gc.setLineWidth(5);
        gc.strokeLine(Constants.WIDTH/2, 0, Constants.WIDTH/2, Constants.HEIGHT);
        gc.strokeRect(0, Constants.HEIGHT/2 - 100, 50, 200);
        gc.strokeRect(Constants.WIDTH - 50, Constants.HEIGHT/2 - 100, 50, 200);

        // 3. 玩家
        for (GameState.PlayerState p : currentState.players) {
            if (p.isGoalKeeper) gc.setFill(Color.YELLOW);
            else gc.setFill(p.team.equals("RED") ? Color.RED : Color.BLUE);
            gc.fillOval(p.x, p.y, Constants.PLAYER_RADIUS*2, Constants.PLAYER_RADIUS*2);

            gc.setFill(Color.BLACK);
            gc.setFont(Font.font("Arial", FontWeight.BOLD, 14));
            gc.fillText(p.name, p.x - 10, p.y - 15);

            // 体力条
            gc.setFill(Color.BLACK);
            gc.fillRect(p.x, p.y - 8, 30, 6);
            gc.setFill(p.stamina < 30 ? Color.RED : Color.LIGHTGREEN);
            gc.fillRect(p.x + 1, p.y - 7, 28 * (p.stamina / 100.0), 4);
        }

        // 4. 球
        gc.setFill(Color.WHITE);
        gc.fillOval(currentState.ballX - Constants.BALL_RADIUS,
                currentState.ballY - Constants.BALL_RADIUS,
                Constants.BALL_RADIUS*2, Constants.BALL_RADIUS*2);

        // 雨滴
        if ("RAINY".equals(currentState.weather)) drawRain(gc);

        // 5. UI 信息 (比分、时间)
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 30));
        gc.fillText(currentState.scoreRed + " - " + currentState.scoreBlue, Constants.WIDTH/2 - 50, 50);
        gc.fillText(currentState.timeString, Constants.WIDTH/2 - 40, 90);
        gc.setFont(Font.font("Arial", 20));
        gc.fillText("Weather: " + currentState.weather, 20, 40);

        // 6. 倒计时显示
        if (currentState.currentPhase == GameState.Phase.COUNTDOWN) {
            gc.setFill(Color.YELLOW);
            gc.setStroke(Color.BLACK);
            gc.setLineWidth(3);
            gc.setFont(Font.font("Arial", FontWeight.BOLD, 80));

            String text = String.valueOf(currentState.countdownValue);
            if (currentState.countdownValue <= 0) text = "GO!";

            gc.fillText(text, Constants.WIDTH/2 - 50, Constants.HEIGHT/2);
            gc.strokeText(text, Constants.WIDTH/2 - 50, Constants.HEIGHT/2);
        }

        // 7. ★ 游戏结束结算面板 (详细信息)
        if (currentState.currentPhase == GameState.Phase.GAME_OVER) {
            // 半透明遮罩
            gc.setFill(Color.rgb(0, 0, 0, 0.8));
            gc.fillRect(0, 0, Constants.WIDTH, Constants.HEIGHT);

            // 标题
            gc.setFill(Color.GOLD);
            gc.setFont(Font.font("Arial", FontWeight.BOLD, 60));
            String resultText = "GAME OVER";
            gc.fillText(resultText, Constants.WIDTH/2 - 180, 200);

            // 获胜者
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("Arial", FontWeight.BOLD, 40));
            gc.fillText("WINNER: " + currentState.winner, Constants.WIDTH/2 - 150, 280);

            // 总比分
            gc.setFill(Color.CYAN);
            gc.fillText("RED " + currentState.scoreRed + " - " + currentState.scoreBlue + " BLUE", Constants.WIDTH/2 - 140, 350);

            // ★ 真人玩家进球统计
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("Arial", 20));
            gc.fillText("--- Player Stats ---", Constants.WIDTH/2 - 80, 420);

            int yOffset = 460;
            for (GameState.PlayerState p : currentState.players) {
                if (!p.isBot) { // 只显示真人
                    String stats = p.name + " (" + p.team + "): " + p.goals + " Goals";
                    gc.fillText(stats, Constants.WIDTH/2 - 100, yOffset);
                    yOffset += 30;
                }
            }
        }
    }

    private void drawRain(GraphicsContext gc) {
        gc.setStroke(Color.rgb(200, 200, 255, 0.5));
        gc.setLineWidth(2);
        for (int i = 0; i < 100; i++) {
            double rx = Math.random() * Constants.WIDTH;
            double ry = Math.random() * Constants.HEIGHT;
            gc.strokeLine(rx, ry, rx - 5, ry + 15);
        }
    }
}