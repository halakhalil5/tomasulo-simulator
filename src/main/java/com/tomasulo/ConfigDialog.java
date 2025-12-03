package com.tomasulo;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class ConfigDialog extends Stage {

    private Config config;
    private String dialogType;

    public ConfigDialog(Config config, String dialogType) {
        this.config = config;
        this.dialogType = dialogType;

        initModality(Modality.APPLICATION_MODAL);

        switch (dialogType) {
            case "latency":
                createLatencyDialog();
                break;
            case "cache":
                createCacheDialog();
                break;
            case "stations":
                createStationsDialog();
                break;
        }
    }

    private void createLatencyDialog() {
        setTitle("Set Instruction Latencies");

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(20));
        grid.setHgap(10);
        grid.setVgap(10);

        // FP Operations
        Label fpLabel = new Label("Floating Point Operations:");
        fpLabel.setStyle("-fx-font-weight: bold;");
        grid.add(fpLabel, 0, 0, 2, 1);

        grid.add(new Label("ADD.D latency:"), 0, 1);
        TextField addField = new TextField(String.valueOf(config.addLatency));
        grid.add(addField, 1, 1);

        grid.add(new Label("SUB.D latency:"), 0, 2);
        TextField subField = new TextField(String.valueOf(config.subLatency));
        grid.add(subField, 1, 2);

        grid.add(new Label("MUL.D latency:"), 0, 3);
        TextField mulField = new TextField(String.valueOf(config.mulLatency));
        grid.add(mulField, 1, 3);

        grid.add(new Label("DIV.D latency:"), 0, 4);
        TextField divField = new TextField(String.valueOf(config.divLatency));
        grid.add(divField, 1, 4);

        // Integer Operations
        Label intLabel = new Label("Integer Operations:");
        intLabel.setStyle("-fx-font-weight: bold;");
        grid.add(intLabel, 0, 5, 2, 1);

        grid.add(new Label("ADDI latency:"), 0, 6);
        TextField intAddField = new TextField(String.valueOf(config.intAddLatency));
        grid.add(intAddField, 1, 6);

        grid.add(new Label("SUBI latency:"), 0, 7);
        TextField intSubField = new TextField(String.valueOf(config.intSubLatency));
        grid.add(intSubField, 1, 7);

        // Memory Operations
        Label memLabel = new Label("Memory Operations:");
        memLabel.setStyle("-fx-font-weight: bold;");
        grid.add(memLabel, 0, 8, 2, 1);

        grid.add(new Label("Load base latency:"), 0, 9);
        TextField loadField = new TextField(String.valueOf(config.loadLatency));
        grid.add(loadField, 1, 9);

        grid.add(new Label("Store base latency:"), 0, 10);
        TextField storeField = new TextField(String.valueOf(config.storeLatency));
        grid.add(storeField, 1, 10);

        // Branch
        Label branchLabel = new Label("Branch Operations:");
        branchLabel.setStyle("-fx-font-weight: bold;");
        grid.add(branchLabel, 0, 11, 2, 1);

        grid.add(new Label("Branch latency:"), 0, 12);
        TextField branchField = new TextField(String.valueOf(config.branchLatency));
        grid.add(branchField, 1, 12);

        // Buttons
        HBox buttonBox = new HBox(10);
        Button okButton = new Button("OK");
        okButton.setOnAction(e -> {
            try {
                config.addLatency = Integer.parseInt(addField.getText());
                config.subLatency = Integer.parseInt(subField.getText());
                config.mulLatency = Integer.parseInt(mulField.getText());
                config.divLatency = Integer.parseInt(divField.getText());
                config.intAddLatency = Integer.parseInt(intAddField.getText());
                config.intSubLatency = Integer.parseInt(intSubField.getText());
                config.loadLatency = Integer.parseInt(loadField.getText());
                config.storeLatency = Integer.parseInt(storeField.getText());
                config.branchLatency = Integer.parseInt(branchField.getText());
                close();
            } catch (NumberFormatException ex) {
                showError("Invalid input. Please enter valid integers.");
            }
        });

        Button cancelButton = new Button("Cancel");
        cancelButton.setOnAction(e -> close());

        buttonBox.getChildren().addAll(okButton, cancelButton);
        grid.add(buttonBox, 0, 13, 2, 1);

        Scene scene = new Scene(grid, 400, 500);
        setScene(scene);
    }

    private void createCacheDialog() {
        setTitle("Configure Cache");

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(20));
        grid.setHgap(10);
        grid.setVgap(10);

        grid.add(new Label("Cache size (bytes):"), 0, 0);
        TextField sizeField = new TextField(String.valueOf(config.cacheSize));
        grid.add(sizeField, 1, 0);

        grid.add(new Label("Block size (bytes):"), 0, 1);
        TextField blockField = new TextField(String.valueOf(config.blockSize));
        grid.add(blockField, 1, 1);

        grid.add(new Label("Hit latency (cycles):"), 0, 2);
        TextField hitField = new TextField(String.valueOf(config.cacheHitLatency));
        grid.add(hitField, 1, 2);

        grid.add(new Label("Miss penalty (cycles):"), 0, 3);
        TextField missField = new TextField(String.valueOf(config.cacheMissPenalty));
        grid.add(missField, 1, 3);

        Label infoLabel = new Label("Note: Direct-mapped cache is used.\nNumber of blocks = Cache size / Block size");
        infoLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: gray;");
        grid.add(infoLabel, 0, 4, 2, 1);

        // Buttons
        HBox buttonBox = new HBox(10);
        Button okButton = new Button("OK");
        okButton.setOnAction(e -> {
            try {
                int cacheSize = Integer.parseInt(sizeField.getText());
                int blockSize = Integer.parseInt(blockField.getText());

                if (cacheSize % blockSize != 0) {
                    showError("Cache size must be a multiple of block size.");
                    return;
                }

                config.cacheSize = cacheSize;
                config.blockSize = blockSize;
                config.cacheHitLatency = Integer.parseInt(hitField.getText());
                config.cacheMissPenalty = Integer.parseInt(missField.getText());
                close();
            } catch (NumberFormatException ex) {
                showError("Invalid input. Please enter valid integers.");
            }
        });

        Button cancelButton = new Button("Cancel");
        cancelButton.setOnAction(e -> close());

        buttonBox.getChildren().addAll(okButton, cancelButton);
        grid.add(buttonBox, 0, 5, 2, 1);

        Scene scene = new Scene(grid, 450, 250);
        setScene(scene);
    }

    private void createStationsDialog() {
        setTitle("Set Reservation Station and Buffer Sizes");

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(20));
        grid.setHgap(10);
        grid.setVgap(10);

        Label rsLabel = new Label("Reservation Stations:");
        rsLabel.setStyle("-fx-font-weight: bold;");
        grid.add(rsLabel, 0, 0, 2, 1);

        grid.add(new Label("Add/Sub stations:"), 0, 1);
        TextField addSubField = new TextField(String.valueOf(config.addSubStations));
        grid.add(addSubField, 1, 1);

        grid.add(new Label("Mul/Div stations:"), 0, 2);
        TextField mulDivField = new TextField(String.valueOf(config.mulDivStations));
        grid.add(mulDivField, 1, 2);

        grid.add(new Label("Integer stations:"), 0, 3);
        TextField intField = new TextField(String.valueOf(config.integerStations));
        grid.add(intField, 1, 3);

        Label bufLabel = new Label("Load/Store Buffers:");
        bufLabel.setStyle("-fx-font-weight: bold;");
        grid.add(bufLabel, 0, 4, 2, 1);

        grid.add(new Label("Load buffers:"), 0, 5);
        TextField loadField = new TextField(String.valueOf(config.loadBuffers));
        grid.add(loadField, 1, 5);

        grid.add(new Label("Store buffers:"), 0, 6);
        TextField storeField = new TextField(String.valueOf(config.storeBuffers));
        grid.add(storeField, 1, 6);

        Label queueLabel = new Label("Instruction Queue:");
        queueLabel.setStyle("-fx-font-weight: bold;");
        grid.add(queueLabel, 0, 7, 2, 1);

        grid.add(new Label("Queue size:"), 0, 8);
        TextField queueField = new TextField(String.valueOf(config.instructionQueueSize));
        grid.add(queueField, 1, 8);

        Label warnLabel = new Label("Note: Changing these values requires reloading the program.");
        warnLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: red;");
        grid.add(warnLabel, 0, 9, 2, 1);

        // Buttons
        HBox buttonBox = new HBox(10);
        Button okButton = new Button("OK");
        okButton.setOnAction(e -> {
            try {
                config.addSubStations = Integer.parseInt(addSubField.getText());
                config.mulDivStations = Integer.parseInt(mulDivField.getText());
                config.integerStations = Integer.parseInt(intField.getText());
                config.loadBuffers = Integer.parseInt(loadField.getText());
                config.storeBuffers = Integer.parseInt(storeField.getText());
                config.instructionQueueSize = Integer.parseInt(queueField.getText());
                close();
            } catch (NumberFormatException ex) {
                showError("Invalid input. Please enter valid integers.");
            }
        });

        Button cancelButton = new Button("Cancel");
        cancelButton.setOnAction(e -> close());

        buttonBox.getChildren().addAll(okButton, cancelButton);
        grid.add(buttonBox, 0, 10, 2, 1);

        Scene scene = new Scene(grid, 450, 400);
        setScene(scene);
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
