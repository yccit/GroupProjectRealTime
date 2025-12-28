package com.soccer.client;

import com.soccer.common.Constants;
import com.soccer.common.GameState;
import com.soccer.common.InputPacket;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class AdminClient extends Application {

    private ObjectOutputStream out;
    private ObjectInputStream in;
    private boolean isRunning = true;

    // UI 组件
    private VBox playerListContainer;
    private Label statusLabel;
    private Button startMatchBtn;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #2c3e50;");

        // --- 顶部标题 ---
        Label title = new Label("ADMIN CONTROL PANEL");
        title.setFont(new Font("Arial Black", 24));
        title.setTextFill(Color.WHITE);
        HBox topBox = new HBox(title);
        topBox.setAlignment(Pos.CENTER);
        root.setTop(topBox);

        // --- 中间：玩家列表 ---
        playerListContainer = new VBox(10);
        playerListContainer.setPadding(new Insets(15));
        ScrollPane scrollPane = new ScrollPane(playerListContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: #34495e; -fx-border-color: transparent;");
        root.setCenter(scrollPane);

        // --- 底部：控制按钮 ---
        startMatchBtn = new Button("START MATCH NOW");
        startMatchBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px;");
        startMatchBtn.setPrefWidth(200);
        startMatchBtn.setOnAction(e -> sendCommand("START", 0));

        Button endGameBtn = new Button("FORCE END GAME");
        endGameBtn.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px;");
        endGameBtn.setPrefWidth(200);
        endGameBtn.setOnAction(e -> sendCommand("END", 0));

        statusLabel = new Label("Connecting to server...");
        statusLabel.setTextFill(Color.YELLOW);

        VBox bottomBox = new VBox(15, startMatchBtn, endGameBtn, statusLabel);
        bottomBox.setAlignment(Pos.CENTER);
        bottomBox.setPadding(new Insets(20, 0, 0, 0));
        root.setBottom(bottomBox);

        Scene scene = new Scene(root, 500, 600);
        primaryStage.setTitle("Soccer Game - Administrator");
        primaryStage.setScene(scene);
        primaryStage.show();

        // 连接服务器
        new Thread(this::connectToServer).start();
    }

    private void connectToServer() {
        try {
            Socket socket = new Socket("localhost", Constants.PORT);
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            // 告诉服务器我是 ADMIN
            InputPacket loginPkt = new InputPacket();
            loginPkt.command = "ADMIN_LOGIN";
            out.writeObject(loginPkt);
            out.flush();

            Platform.runLater(() -> statusLabel.setText("Connected as ADMIN"));

            while (isRunning) {
                Object obj = in.readObject();
                if (obj instanceof GameState) {
                    GameState state = (GameState) obj;
                    Platform.runLater(() -> updateDashboard(state));
                }
            }
        } catch (Exception e) {
            Platform.runLater(() -> statusLabel.setText("Connection Lost"));
        }
    }

    private void updateDashboard(GameState state) {
        playerListContainer.getChildren().clear();

        if (state.players.isEmpty()) {
            Label empty = new Label("No players connected.");
            empty.setTextFill(Color.LIGHTGRAY);
            playerListContainer.getChildren().add(empty);
            return;
        }

        for (GameState.PlayerState p : state.players) {
            // 只显示人类玩家，不显示机器人
            if (p.isBot) continue;

            HBox row = new HBox(15);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-background-color: #ecf0f1; -fx-background-radius: 5; -fx-padding: 10;");

            // 状态指示灯
            String statusEmoji = p.isApproved ? "✅" : "❌";
            Label statusIcon = new Label(statusEmoji);

            Label infoLabel = new Label(p.name + " (ID: " + p.id + ") [" + p.team + "]");
            infoLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
            infoLabel.setPrefWidth(250);

            Button actionBtn = new Button(p.isApproved ? "Revoke" : "APPROVE");
            if (!p.isApproved) {
                actionBtn.setStyle("-fx-background-color: #2980b9; -fx-text-fill: white;");
                actionBtn.setOnAction(e -> sendCommand("APPROVE", p.id));
            } else {
                actionBtn.setStyle("-fx-background-color: #7f8c8d; -fx-text-fill: white;");
                actionBtn.setDisable(true); // 批准了就不让撤销了（为了简单）
            }

            row.getChildren().addAll(statusIcon, infoLabel, actionBtn);
            playerListContainer.getChildren().add(row);
        }

        // 更新游戏状态显示
        statusLabel.setText("Game Phase: " + state.currentPhase + " | Time: " + state.timeString);
    }

    private void sendCommand(String cmd, int targetId) {
        try {
            InputPacket pkt = new InputPacket();
            pkt.command = cmd;
            pkt.targetIdToApprove = targetId; // 告诉服务器批准谁
            out.writeObject(pkt);
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}