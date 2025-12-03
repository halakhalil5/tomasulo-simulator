package com.tomasulo;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.Map;

public class RegisterInitDialog extends Stage {

    private RegisterFile registerFile;

    public RegisterInitDialog(RegisterFile registerFile) {
        this.registerFile = registerFile;

        initModality(Modality.APPLICATION_MODAL);
        setTitle("Initialize Register Values");

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));

        Label infoLabel = new Label("Set initial values for registers (leave blank to keep current value):");

        TabPane tabPane = new TabPane();

        // Integer registers tab
        Tab intTab = new Tab("Integer Registers");
        intTab.setClosable(false);
        ScrollPane intScroll = new ScrollPane();
        intScroll.setFitToWidth(true);
        GridPane intGrid = createRegisterGrid(registerFile.getIntegerRegisters(), true);
        intScroll.setContent(intGrid);
        intTab.setContent(intScroll);

        // FP registers tab
        Tab fpTab = new Tab("Floating Point Registers");
        fpTab.setClosable(false);
        ScrollPane fpScroll = new ScrollPane();
        fpScroll.setFitToWidth(true);
        GridPane fpGrid = createRegisterGrid(registerFile.getFloatRegisters(), false);
        fpScroll.setContent(fpGrid);
        fpTab.setContent(fpScroll);

        tabPane.getTabs().addAll(intTab, fpTab);

        // Quick set options
        HBox quickSetBox = new HBox(10);
        Label quickLabel = new Label("Quick Set:");

        Button setZeroButton = new Button("Set All to 0");
        setZeroButton.setOnAction(e -> {
            for (int i = 0; i < 32; i++) {
                registerFile.setValue("R" + i, 0.0);
                registerFile.setValue("F" + i, 0.0);
            }
            close();
        });

        Button setSequentialButton = new Button("Set Sequential (0-31)");
        setSequentialButton.setOnAction(e -> {
            for (int i = 0; i < 32; i++) {
                registerFile.setValue("R" + i, i);
                registerFile.setValue("F" + i, i);
            }
            close();
        });

        quickSetBox.getChildren().addAll(quickLabel, setZeroButton, setSequentialButton);

        // Buttons
        HBox buttonBox = new HBox(10);
        Button okButton = new Button("OK");
        okButton.setOnAction(e -> {
            // Values are updated as they're typed via listeners
            close();
        });

        Button cancelButton = new Button("Cancel");
        cancelButton.setOnAction(e -> close());

        buttonBox.getChildren().addAll(okButton, cancelButton);

        root.getChildren().addAll(infoLabel, tabPane, quickSetBox, buttonBox);

        Scene scene = new Scene(root, 600, 500);
        setScene(scene);
    }

    private GridPane createRegisterGrid(Map<String, Double> registers, boolean isInteger) {
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(10));
        grid.setHgap(10);
        grid.setVgap(5);

        int row = 0;
        for (int i = 0; i < 32; i++) {
            String regName = isInteger ? "R" + i : "F" + i;
            double currentValue = registers.get(regName);

            Label label = new Label(regName + ":");
            TextField field = new TextField(String.format("%.2f", currentValue));
            field.setPrefWidth(120);

            // Update register value as user types
            field.textProperty().addListener((obs, oldVal, newVal) -> {
                try {
                    if (!newVal.isEmpty()) {
                        double value = Double.parseDouble(newVal);
                        registerFile.setValue(regName, value);
                    }
                } catch (NumberFormatException ex) {
                    // Ignore invalid input
                }
            });

            grid.add(label, (i % 2) * 2, row);
            grid.add(field, (i % 2) * 2 + 1, row);

            if (i % 2 == 1) {
                row++;
            }
        }

        return grid;
    }
}
