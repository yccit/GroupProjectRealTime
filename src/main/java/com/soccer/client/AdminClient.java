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
import java.util.ArrayList;
import java.util.List;

public class AdminClient extends Application {

    private ObjectOutputStream out;
    private ObjectInputStream in;
    private boolean isRunning = true;

    // UI Components
    private VBox playerListContainer;
    private Label statusLabel;
    private Button startMatchBtn;

    // ★★★ NEW: Cache to store the previous list for comparison ★★★
    private List<GameState.PlayerState> lastPlayerList = new ArrayList<>();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #2c3e50;");

        // --- Top Title ---
        Label title = new Label("ADMIN CONTROL PANEL");
        title.setFont(new Font("Arial Black", 24));
        title.setTextFill(Color.WHITE);
        HBox topBox = new HBox(title);
        topBox.setAlignment(Pos.CENTER);
        root.setTop(topBox);

        // --- Center: Player List ---
        playerListContainer = new VBox(10);
        playerListContainer.setPadding(new Insets(15));
        ScrollPane scrollPane = new ScrollPane(playerListContainer);
        scrollPane.setFitToWidth(true);
        // Ensure ScrollPane background matches dark theme
        scrollPane.setStyle("-fx-background: #34495e; -fx-border-color: transparent; -fx-control-inner-background: #34495e;");
        root.setCenter(scrollPane);

        // --- Bottom: Control Buttons ---
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

        Scene scene = new Scene(root, 600, 600); // Slightly larger window
        primaryStage.setTitle("Soccer Game - Administrator");
        primaryStage.setScene(scene);
        primaryStage.show();

        // Connect to Server
        new Thread(this::connectToServer).start();
    }

    private void connectToServer() {
        try {
            Socket socket = new Socket("localhost", Constants.PORT);
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            // Identify as ADMIN
            InputPacket loginPkt = new InputPacket();
            loginPkt.command = "ADMIN_LOGIN";
            out.writeObject(loginPkt);
            out.flush();

            Platform.runLater(() -> statusLabel.setText("Connected as ADMIN"));

            while (isRunning) {
                Object obj = in.readObject();
                if (obj instanceof GameState) {
                    GameState state = (GameState) obj;

                    // ★★★ FIX: Only update UI if player list actually changed (ignoring movement) ★★★
                    if (shouldUpdateUI(state.players)) {
                        // Update cache
                        lastPlayerList.clear();
                        lastPlayerList.addAll(state.players);

                        // Refresh UI
                        Platform.runLater(() -> updateDashboard(state));
                    }

                    // Always update time/phase label (this is cheap and doesn't affect buttons)
                    Platform.runLater(() -> statusLabel.setText("Game Phase: " + state.currentPhase + " | Time: " + state.timeString));
                }
            }
        } catch (Exception e) {
            Platform.runLater(() -> statusLabel.setText("Connection Lost"));
        }
    }

    // ★★★ Helper Logic: Check if we need to redraw the list ★★★
    private boolean shouldUpdateUI(List<GameState.PlayerState> newPlayers) {
        // 1. Different number of players? Update.
        if (newPlayers.size() != lastPlayerList.size()) return true;

        // 2. Check individual player status
        for (int i = 0; i < newPlayers.size(); i++) {
            GameState.PlayerState pNew = newPlayers.get(i);
            GameState.PlayerState pOld = lastPlayerList.get(i);

            // If ID, Name, or Approval Status changed, we must update.
            // We do NOT check pNew.x or pNew.y here, so movement doesn't cause flickering.
            if (pNew.id != pOld.id ||
                    pNew.isApproved != pOld.isApproved ||
                    !pNew.name.equals(pOld.name)) {
                return true;
            }
        }
        return false;
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
            // Only show humans
            if (p.isBot) continue;

            HBox row = new HBox(15);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-background-color: #ecf0f1; -fx-background-radius: 5; -fx-padding: 10;");

            // Status Icon
            String statusEmoji = p.isApproved ? "✅" : "❌";
            Label statusIcon = new Label(statusEmoji);
            statusIcon.setStyle("-fx-text-fill: black; -fx-font-size: 16px;");

            // Info Label (Your Requested Black Text)
            Label infoLabel = new Label(p.name + " (ID: " + p.id + ") [" + p.team + "]");
            infoLabel.setStyle("-fx-text-fill: black; -fx-font-weight: bold; -fx-font-size: 14px;");
            infoLabel.setPrefWidth(250);

            // Action Button
            Button actionBtn = new Button(p.isApproved ? "Revoke" : "APPROVE");
            if (!p.isApproved) {
                actionBtn.setStyle("-fx-background-color: #2980b9; -fx-text-fill: white; -fx-cursor: hand;");
                actionBtn.setOnAction(e -> {
                    sendCommand("APPROVE", p.id);
                    actionBtn.setDisable(true); // Temporary disable to prevent double clicks
                });
            } else {
                actionBtn.setStyle("-fx-background-color: #7f8c8d; -fx-text-fill: white;");
                actionBtn.setDisable(true);
            }

            row.getChildren().addAll(statusIcon, infoLabel, actionBtn);
            playerListContainer.getChildren().add(row);
        }
    }

    private void sendCommand(String cmd, int targetId) {
        try {
            InputPacket pkt = new InputPacket();
            pkt.command = cmd;
            pkt.targetIdToApprove = targetId;
            pkt.id = 0; // Admin ID
            out.writeObject(pkt);
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}