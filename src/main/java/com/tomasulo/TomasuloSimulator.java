package com.tomasulo;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Separator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class TomasuloSimulator extends Application {

    private Config config;
    private ExecutionEngine engine;

    // UI Components
    private TextArea codeArea;
    private TextArea logArea;
    private Label cycleLabel;

    // Tables
    private TableView<RSTableRow> addSubTable;
    private TableView<RSTableRow> mulDivTable;
    private TableView<RSTableRow> intTable;
    private TableView<LSBufferTableRow> loadTable;
    private TableView<LSBufferTableRow> storeTable;
    private TableView<RegisterTableRow> intRegTable;
    private TableView<RegisterTableRow> fpRegTable;
    private TableView<InstructionTableRow> instructionTable;
    private TableView<CacheTableRow> cacheTable;

    private Button stepButton;
    private Button runButton;
    private Button resetButton;
    private Button loadButton;

    @Override
    public void start(Stage primaryStage) {
        config = new Config();
        engine = new ExecutionEngine(config);

        primaryStage.setTitle("Tomasulo Algorithm Simulator");

        BorderPane root = new BorderPane();

        // Top: Menu and controls
        VBox topSection = new VBox(10);
        topSection.setPadding(new Insets(10));

        MenuBar menuBar = createMenuBar(primaryStage);
        HBox controlPanel = createControlPanel();

        topSection.getChildren().addAll(menuBar, controlPanel);
        root.setTop(topSection);

        // Center: Main content area with tabs
        TabPane mainTabs = new TabPane();

        // Tab 1: Code Editor
        Tab codeTab = new Tab("Code Editor");
        codeTab.setClosable(false);
        codeArea = new TextArea();
        codeArea.setPromptText("Enter MIPS assembly code here...");
        codeArea.setPrefRowCount(20);
        codeArea.setText(InstructionParser.getSampleProgram1());

        VBox codeBox = new VBox(10);
        codeBox.setPadding(new Insets(10));
        Label codeLabel = new Label("MIPS Assembly Code:");
        codeBox.getChildren().addAll(codeLabel, codeArea);
        codeTab.setContent(codeBox);

        // Tab 2: Reservation Stations
        Tab rsTab = new Tab("Reservation Stations");
        rsTab.setClosable(false);
        rsTab.setContent(createReservationStationsView());

        // Tab 3: Load/Store Buffers
        Tab lsTab = new Tab("Load/Store Buffers");
        lsTab.setClosable(false);
        lsTab.setContent(createLoadStoreView());

        // Tab 4: Register File
        Tab regTab = new Tab("Register File");
        regTab.setClosable(false);
        regTab.setContent(createRegisterView());

        // Tab 5: Instruction Queue
        Tab instTab = new Tab("Instruction Queue");
        instTab.setClosable(false);
        instTab.setContent(createInstructionView());

        // Tab 6: Cache
        Tab cacheTab = new Tab("Cache Status");
        cacheTab.setClosable(false);
        cacheTab.setContent(createCacheView());

        mainTabs.getTabs().addAll(codeTab, rsTab, lsTab, regTab, instTab, cacheTab);
        root.setCenter(mainTabs);

        // Bottom: Log area
        VBox bottomSection = new VBox(5);
        bottomSection.setPadding(new Insets(10));
        Label logLabel = new Label("Cycle Log:");
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefRowCount(10);
        bottomSection.getChildren().addAll(logLabel, logArea);
        root.setBottom(bottomSection);

        Scene scene = new Scene(root, 1000, 500);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private MenuBar createMenuBar(Stage stage) {
        MenuBar menuBar = new MenuBar();

        // File Menu
        Menu fileMenu = new Menu("File");
        MenuItem loadFileItem = new MenuItem("Load Program...");
        loadFileItem.setOnAction(e -> loadProgramFromFile(stage));

        MenuItem exitItem = new MenuItem("Exit");
        exitItem.setOnAction(e -> stage.close());

        fileMenu.getItems().addAll(loadFileItem, new SeparatorMenuItem(), exitItem);

        // Configuration Menu
        Menu configMenu = new Menu("Configuration");
        MenuItem latencyItem = new MenuItem("Set Latencies...");
        latencyItem.setOnAction(e -> showLatencyDialog());

        MenuItem cacheItem = new MenuItem("Configure Cache...");
        cacheItem.setOnAction(e -> showCacheDialog());

        MenuItem stationItem = new MenuItem("Set Station Sizes...");
        stationItem.setOnAction(e -> showStationDialog());

        MenuItem registerItem = new MenuItem("Initialize Registers...");
        registerItem.setOnAction(e -> showRegisterDialog());

        configMenu.getItems().addAll(latencyItem, cacheItem, stationItem, registerItem);

        // Samples Menu
        Menu samplesMenu = new Menu("Sample Programs");
        MenuItem sample1 = new MenuItem("Simple FP Operations");
        sample1.setOnAction(e -> codeArea.setText(InstructionParser.getSampleProgram1()));

        MenuItem sample2 = new MenuItem("Loop Example");
        sample2.setOnAction(e -> codeArea.setText(InstructionParser.getSampleProgram2()));

        MenuItem sample3 = new MenuItem("Hazards Example");
        sample3.setOnAction(e -> codeArea.setText(InstructionParser.getSampleProgram3()));

        samplesMenu.getItems().addAll(sample1, sample2, sample3);

        // Help Menu
        Menu helpMenu = new Menu("Help");
        MenuItem aboutItem = new MenuItem("About");
        aboutItem.setOnAction(e -> showAboutDialog());

        MenuItem instructionsItem = new MenuItem("Instructions");
        instructionsItem.setOnAction(e -> showInstructionsDialog());

        helpMenu.getItems().addAll(instructionsItem, aboutItem);

        menuBar.getMenus().addAll(fileMenu, configMenu, samplesMenu, helpMenu);
        return menuBar;
    }

    private HBox createControlPanel() {
        HBox panel = new HBox(15);
        panel.setAlignment(Pos.CENTER_LEFT);
        panel.setPadding(new Insets(10));

        cycleLabel = new Label("Cycle: 0");
        cycleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        loadButton = new Button("Load Program");
        loadButton.setOnAction(e -> loadProgram());

        stepButton = new Button("Step");
        stepButton.setOnAction(e -> executeStep());
        stepButton.setDisable(true);

        runButton = new Button("Run to Completion");
        runButton.setOnAction(e -> runToCompletion());
        runButton.setDisable(true);

        resetButton = new Button("Reset");
        resetButton.setOnAction(e -> resetSimulation());
        resetButton.setDisable(true);

        panel.getChildren().addAll(cycleLabel, new Separator(),
                loadButton, stepButton, runButton, resetButton);

        return panel;
    }

    private VBox createReservationStationsView() {
        VBox box = new VBox(15);
        box.setPadding(new Insets(10));

        // Add/Sub Stations
        Label addSubLabel = new Label("Add/Sub Reservation Stations:");
        addSubLabel.setStyle("-fx-font-weight: bold;");
        addSubTable = createRSTable();

        // Mul/Div Stations
        Label mulDivLabel = new Label("Mul/Div Reservation Stations:");
        mulDivLabel.setStyle("-fx-font-weight: bold;");
        mulDivTable = createRSTable();

        // Integer Stations
        Label intLabel = new Label("Integer Reservation Stations:");
        intLabel.setStyle("-fx-font-weight: bold;");
        intTable = createRSTable();

        box.getChildren().addAll(addSubLabel, addSubTable,
                mulDivLabel, mulDivTable,
                intLabel, intTable);

        return box;
    }

    private TableView<RSTableRow> createRSTable() {
        TableView<RSTableRow> table = new TableView<>();

        TableColumn<RSTableRow, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(80);

        TableColumn<RSTableRow, String> busyCol = new TableColumn<>("Busy");
        busyCol.setCellValueFactory(new PropertyValueFactory<>("busy"));
        busyCol.setPrefWidth(60);

        TableColumn<RSTableRow, String> opCol = new TableColumn<>("Op");
        opCol.setCellValueFactory(new PropertyValueFactory<>("op"));
        opCol.setPrefWidth(80);

        TableColumn<RSTableRow, String> vjCol = new TableColumn<>("Vj");
        vjCol.setCellValueFactory(new PropertyValueFactory<>("vj"));
        vjCol.setPrefWidth(80);

        TableColumn<RSTableRow, String> vkCol = new TableColumn<>("Vk");
        vkCol.setCellValueFactory(new PropertyValueFactory<>("vk"));
        vkCol.setPrefWidth(80);

        TableColumn<RSTableRow, String> qjCol = new TableColumn<>("Qj");
        qjCol.setCellValueFactory(new PropertyValueFactory<>("qj"));
        qjCol.setPrefWidth(80);

        TableColumn<RSTableRow, String> qkCol = new TableColumn<>("Qk");
        qkCol.setCellValueFactory(new PropertyValueFactory<>("qk"));
        qkCol.setPrefWidth(80);

        TableColumn<RSTableRow, String> cyclesCol = new TableColumn<>("Remaining");
        cyclesCol.setCellValueFactory(new PropertyValueFactory<>("remaining"));
        cyclesCol.setPrefWidth(80);

        table.getColumns().addAll(nameCol, busyCol, opCol, vjCol, vkCol, qjCol, qkCol, cyclesCol);
        table.setMaxHeight(200);

        return table;
    }

    private VBox createLoadStoreView() {
        VBox box = new VBox(15);
        box.setPadding(new Insets(10));

        Label loadLabel = new Label("Load Buffers:");
        loadLabel.setStyle("-fx-font-weight: bold;");
        loadTable = createLSTable();

        Label storeLabel = new Label("Store Buffers:");
        storeLabel.setStyle("-fx-font-weight: bold;");
        storeTable = createLSTable();

        box.getChildren().addAll(loadLabel, loadTable, storeLabel, storeTable);

        return box;
    }

    private TableView<LSBufferTableRow> createLSTable() {
        TableView<LSBufferTableRow> table = new TableView<>();

        TableColumn<LSBufferTableRow, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(80);

        TableColumn<LSBufferTableRow, String> busyCol = new TableColumn<>("Busy");
        busyCol.setCellValueFactory(new PropertyValueFactory<>("busy"));
        busyCol.setPrefWidth(60);

        TableColumn<LSBufferTableRow, String> addressCol = new TableColumn<>("Address");
        addressCol.setCellValueFactory(new PropertyValueFactory<>("address"));
        addressCol.setPrefWidth(100);

        TableColumn<LSBufferTableRow, String> valueCol = new TableColumn<>("Value");
        valueCol.setCellValueFactory(new PropertyValueFactory<>("value"));
        valueCol.setPrefWidth(100);

        TableColumn<LSBufferTableRow, String> qCol = new TableColumn<>("Q");
        qCol.setCellValueFactory(new PropertyValueFactory<>("q"));
        qCol.setPrefWidth(80);

        TableColumn<LSBufferTableRow, String> cyclesCol = new TableColumn<>("Remaining");
        cyclesCol.setCellValueFactory(new PropertyValueFactory<>("remaining"));
        cyclesCol.setPrefWidth(80);

        table.getColumns().addAll(nameCol, busyCol, addressCol, valueCol, qCol, cyclesCol);
        table.setMaxHeight(200);

        return table;
    }

    private VBox createRegisterView() {
        VBox box = new VBox(15);
        box.setPadding(new Insets(10));

        Label intLabel = new Label("Integer Registers:");
        intLabel.setStyle("-fx-font-weight: bold;");
        intRegTable = createRegisterTable();

        Label fpLabel = new Label("Floating Point Registers:");
        fpLabel.setStyle("-fx-font-weight: bold;");
        fpRegTable = createRegisterTable();

        box.getChildren().addAll(intLabel, intRegTable, fpLabel, fpRegTable);

        return box;
    }

    private TableView<RegisterTableRow> createRegisterTable() {
        TableView<RegisterTableRow> table = new TableView<>();

        TableColumn<RegisterTableRow, String> nameCol = new TableColumn<>("Register");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(100);

        TableColumn<RegisterTableRow, String> valueCol = new TableColumn<>("Value");
        valueCol.setCellValueFactory(new PropertyValueFactory<>("value"));
        valueCol.setPrefWidth(150);

        TableColumn<RegisterTableRow, String> statusCol = new TableColumn<>("Status (Qi)");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setPrefWidth(150);

        table.getColumns().addAll(nameCol, valueCol, statusCol);
        table.setMaxHeight(300);

        return table;
    }

    private VBox createInstructionView() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(10));

        Label label = new Label("Instruction Queue and Execution Status:");
        label.setStyle("-fx-font-weight: bold;");

        instructionTable = new TableView<>();

        // Style separator rows (iteration headers)
        instructionTable.setRowFactory(tv -> new javafx.scene.control.TableRow<InstructionTableRow>() {
            @Override
            protected void updateItem(InstructionTableRow item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setStyle("");
                } else if (item.isSeparator()) {
                    // make separator visually distinct
                    setStyle("-fx-background-color: -fx-accent; -fx-font-weight: bold; -fx-text-fill: white;");
                } else {
                    setStyle("");
                }
            }
        });

        TableColumn<InstructionTableRow, String> instCol = new TableColumn<>("Instruction");
        instCol.setCellValueFactory(new PropertyValueFactory<>("instruction"));
        instCol.setPrefWidth(250);

    TableColumn<InstructionTableRow, String> iterCol = new TableColumn<>("Iter");
    iterCol.setCellValueFactory(new PropertyValueFactory<>("iteration"));
    iterCol.setPrefWidth(60);

        TableColumn<InstructionTableRow, String> issueCol = new TableColumn<>("Issue");
        issueCol.setCellValueFactory(new PropertyValueFactory<>("issue"));
        issueCol.setPrefWidth(80);

        TableColumn<InstructionTableRow, String> execStartCol = new TableColumn<>("Exec Start");
        execStartCol.setCellValueFactory(new PropertyValueFactory<>("execStart"));
        execStartCol.setPrefWidth(100);

        TableColumn<InstructionTableRow, String> execEndCol = new TableColumn<>("Exec End");
        execEndCol.setCellValueFactory(new PropertyValueFactory<>("execEnd"));
        execEndCol.setPrefWidth(100);

        TableColumn<InstructionTableRow, String> writeCol = new TableColumn<>("Write");
        writeCol.setCellValueFactory(new PropertyValueFactory<>("write"));
        writeCol.setPrefWidth(80);

    instructionTable.getColumns().addAll(iterCol, instCol, issueCol, execStartCol, execEndCol, writeCol);

        box.getChildren().addAll(label, instructionTable);

        return box;
    }

    private VBox createCacheView() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(10));

        Label label = new Label("Cache Status:");
        label.setStyle("-fx-font-weight: bold;");

        cacheTable = new TableView<>();

        TableColumn<CacheTableRow, String> indexCol = new TableColumn<>("Index");
        indexCol.setCellValueFactory(new PropertyValueFactory<>("index"));
        indexCol.setPrefWidth(100);

        TableColumn<CacheTableRow, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setPrefWidth(300);

        cacheTable.getColumns().addAll(indexCol, statusCol);

        TextArea cacheLogArea = new TextArea();
        cacheLogArea.setEditable(false);
        cacheLogArea.setPrefRowCount(10);
        cacheLogArea.setPromptText("Cache access log will appear here...");

        box.getChildren().addAll(label, cacheTable, new Label("Cache Access Log:"), cacheLogArea);

        return box;
    }

    private void loadProgram() {
        try {
            String code = codeArea.getText();
            List<Instruction> instructions = InstructionParser.parse(code);

            if (instructions.isEmpty()) {
                showAlert("No Instructions", "Please enter valid MIPS assembly code.");
                return;
            }

            engine = new ExecutionEngine(config);
            engine.loadProgram(instructions);

            updateAllViews();

            stepButton.setDisable(false);
            runButton.setDisable(false);
            resetButton.setDisable(false);

            logArea.setText("Program loaded successfully. " + instructions.size() + " instructions.\n");

        } catch (Exception e) {
            showAlert("Parse Error", "Error parsing instructions:\n" + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadProgramFromFile(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open MIPS Assembly File");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Assembly Files", "*.asm", "*.s", "*.txt"));

        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            try {
                List<Instruction> instructions = InstructionParser.parseFile(file);
                StringBuilder sb = new StringBuilder();
                for (Instruction inst : instructions) {
                    sb.append(inst.getOriginalInstruction()).append("\n");
                }
                codeArea.setText(sb.toString());

            } catch (Exception e) {
                showAlert("Load Error", "Error loading file:\n" + e.getMessage());
            }
        }
    }

    private void executeStep() {
        boolean hasMore = engine.executeCycle();
        updateAllViews();

        cycleLabel.setText("Cycle: " + engine.getCurrentCycle());

        // Update log
        List<String> log = engine.getCycleLog();
        if (!log.isEmpty()) {
            logArea.appendText(log.get(log.size() - 1) + "\n");
        }

        if (!hasMore) {
            stepButton.setDisable(true);
            runButton.setDisable(true);
            logArea.appendText("\n=== Simulation Complete ===\n");
        }
    }

    private void runToCompletion() {
        int maxCycles = 10000; // Safety limit
        int cycles = 0;

        while (engine.executeCycle() && cycles < maxCycles) {
            cycles++;
        }

        updateAllViews();
        cycleLabel.setText("Cycle: " + engine.getCurrentCycle());

        logArea.setText("=== Simulation Complete ===\n");
        logArea.appendText("Total cycles: " + engine.getCurrentCycle() + "\n\n");

        for (String logLine : engine.getCycleLog()) {
            logArea.appendText(logLine + "\n");
        }

        stepButton.setDisable(true);
        runButton.setDisable(true);
    }

    private void resetSimulation() {
        engine.reset();
        updateAllViews();

        cycleLabel.setText("Cycle: 0");
        logArea.clear();

        stepButton.setDisable(true);
        runButton.setDisable(true);
        resetButton.setDisable(true);
    }

    private void updateAllViews() {
        updateReservationStationTables();
        updateLoadStoreBufferTables();
        updateRegisterTables();
        updateInstructionTable();
        updateCacheTable();
    }

    private void updateReservationStationTables() {
        // Update Add/Sub table
        ObservableList<RSTableRow> addSubData = FXCollections.observableArrayList();
        for (ReservationStation rs : engine.getAddSubStations()) {
            addSubData.add(new RSTableRow(rs));
        }
        addSubTable.setItems(addSubData);

        // Update Mul/Div table
        ObservableList<RSTableRow> mulDivData = FXCollections.observableArrayList();
        for (ReservationStation rs : engine.getMulDivStations()) {
            mulDivData.add(new RSTableRow(rs));
        }
        mulDivTable.setItems(mulDivData);

        // Update Integer table
        ObservableList<RSTableRow> intData = FXCollections.observableArrayList();
        for (ReservationStation rs : engine.getIntStations()) {
            intData.add(new RSTableRow(rs));
        }
        intTable.setItems(intData);
    }

    private void updateLoadStoreBufferTables() {
        // Update Load table
        ObservableList<LSBufferTableRow> loadData = FXCollections.observableArrayList();
        for (LoadStoreBuffer buf : engine.getLoadBuffers()) {
            loadData.add(new LSBufferTableRow(buf));
        }
        loadTable.setItems(loadData);

        // Update Store table
        ObservableList<LSBufferTableRow> storeData = FXCollections.observableArrayList();
        for (LoadStoreBuffer buf : engine.getStoreBuffers()) {
            storeData.add(new LSBufferTableRow(buf));
        }
        storeTable.setItems(storeData);
    }

    private void updateRegisterTables() {
        RegisterFile rf = engine.getRegisterFile();

        // Update Integer registers
        ObservableList<RegisterTableRow> intData = FXCollections.observableArrayList();
        Map<String, Double> intRegs = rf.getIntegerRegisters();
        List<String> intRegNames = new ArrayList<>(intRegs.keySet());
        Collections.sort(intRegNames, (a, b) -> {
            int numA = Integer.parseInt(a.substring(1));
            int numB = Integer.parseInt(b.substring(1));
            return numA - numB;
        });

        for (String reg : intRegNames) {
            intData.add(new RegisterTableRow(reg, rf.getValue(reg), rf.getStatus(reg)));
        }
        intRegTable.setItems(intData);

        // Update FP registers
        ObservableList<RegisterTableRow> fpData = FXCollections.observableArrayList();
        Map<String, Double> fpRegs = rf.getFloatRegisters();
        List<String> fpRegNames = new ArrayList<>(fpRegs.keySet());
        Collections.sort(fpRegNames, (a, b) -> {
            int numA = Integer.parseInt(a.substring(1));
            int numB = Integer.parseInt(b.substring(1));
            return numA - numB;
        });

        for (String reg : fpRegNames) {
            fpData.add(new RegisterTableRow(reg, rf.getValue(reg), rf.getStatus(reg)));
        }
        fpRegTable.setItems(fpData);
    }

    private void updateInstructionTable() {
        ObservableList<InstructionTableRow> data = FXCollections.observableArrayList();

        // Prefer showing issued instruction instances (history). If none issued yet,
        // show the upcoming program window.
        List<Instruction> issued = engine.getInstructionQueue().getIssuedInstructions();
        if (issued != null && !issued.isEmpty()) {
            // Insert a separator/header row when the iteration number changes
            int lastIter = -1;
            for (Instruction inst : issued) {
                int iter = inst.getIteration();
                if (iter != lastIter) {
                    data.add(new InstructionTableRow("--- Iteration " + iter + " ---"));
                    lastIter = iter;
                }
                data.add(new InstructionTableRow(inst));
            }
        } else {
            for (Instruction inst : engine.getInstructionQueue().getQueueSnapshot()) {
                data.add(new InstructionTableRow(inst));
            }
        }

        instructionTable.setItems(data);
    }

    private void updateCacheTable() {
        ObservableList<CacheTableRow> data = FXCollections.observableArrayList();

        Map<Integer, String> cacheSnapshot = engine.getCache().getCacheSnapshot();
        List<Integer> indices = new ArrayList<>(cacheSnapshot.keySet());
        Collections.sort(indices);

        for (Integer index : indices) {
            data.add(new CacheTableRow(index, cacheSnapshot.get(index)));
        }

        cacheTable.setItems(data);
    }

    // Dialog methods
    private void showLatencyDialog() {
        ConfigDialog dialog = new ConfigDialog(config, "latency");
        dialog.showAndWait();
    }

    private void showCacheDialog() {
        ConfigDialog dialog = new ConfigDialog(config, "cache");
        dialog.showAndWait();
    }

    private void showStationDialog() {
        ConfigDialog dialog = new ConfigDialog(config, "stations");
        dialog.showAndWait();
    }

    private void showRegisterDialog() {
        RegisterInitDialog dialog = new RegisterInitDialog(engine.getRegisterFile());
        dialog.showAndWait();
        updateRegisterTables();
    }

    private void showAboutDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About");
        alert.setHeaderText("Tomasulo Algorithm Simulator");
        alert.setContentText("A cycle-by-cycle simulator for the Tomasulo algorithm.\n\n" +
                "Supports:\n" +
                "- FP operations (ADD.D, SUB.D, MUL.D, DIV.D)\n" +
                "- Integer operations (ADDI, SUBI)\n" +
                "- Memory operations (L.D, L.S, S.D, S.S, LW, SW)\n" +
                "- Branch instructions (BEQ, BNE)\n" +
                "- Configurable cache, latencies, and station sizes\n" +
                "- Handles RAW, WAR, WAW hazards\n\n" +
                "Version 1.0");
        alert.showAndWait();
    }

    private void showInstructionsDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Instructions");
        alert.setHeaderText("How to Use the Simulator");
        alert.setContentText(
                "1. Enter MIPS assembly code in the Code Editor tab or load a file\n" +
                        "2. Configure latencies, cache, and station sizes via Configuration menu\n" +
                        "3. Click 'Load Program' to load the instructions\n" +
                        "4. Use 'Step' to execute one cycle at a time\n" +
                        "5. Use 'Run to Completion' to execute until done\n" +
                        "6. View the status in different tabs:\n" +
                        "   - Reservation Stations: Shows Add/Sub, Mul/Div, Integer stations\n" +
                        "   - Load/Store Buffers: Shows memory operation buffers\n" +
                        "   - Register File: Shows register values and dependencies\n" +
                        "   - Instruction Queue: Shows all instructions and their status\n" +
                        "   - Cache Status: Shows cache blocks and access log\n\n" +
                        "Bus Arbitration: When multiple instructions finish in the same cycle,\n" +
                        "the oldest instruction (by issue order) writes to the CDB first.");
        alert.showAndWait();
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }

    // Table row classes
    public static class RSTableRow {
        private String name, busy, op, vj, vk, qj, qk, remaining;

        public RSTableRow(ReservationStation rs) {
            this.name = rs.getName();
            this.busy = rs.isBusy() ? "Yes" : "No";
            this.op = rs.getOp() != null ? rs.getOp() : "";
            this.vj = rs.isBusy() ? String.format("%.2f", rs.getVj()) : "";
            this.vk = rs.isBusy() ? String.format("%.2f", rs.getVk()) : "";
            this.qj = rs.getQj() != null ? rs.getQj() : "";
            this.qk = rs.getQk() != null ? rs.getQk() : "";
            this.remaining = rs.isBusy() ? String.valueOf(rs.getRemainingCycles()) : "";
        }

        public String getName() {
            return name;
        }

        public String getBusy() {
            return busy;
        }

        public String getOp() {
            return op;
        }

        public String getVj() {
            return vj;
        }

        public String getVk() {
            return vk;
        }

        public String getQj() {
            return qj;
        }

        public String getQk() {
            return qk;
        }

        public String getRemaining() {
            return remaining;
        }
    }

    public static class LSBufferTableRow {
        private String name, busy, address, value, q, remaining;

        public LSBufferTableRow(LoadStoreBuffer buf) {
            this.name = buf.getName();
            this.busy = buf.isBusy() ? "Yes" : "No";
            this.address = buf.isBusy() ? String.format("0x%X", buf.getAddress()) : "";
            this.value = buf.isBusy() && !buf.isLoad() ? String.format("%.2f", buf.getValue()) : "";
            this.q = buf.getQ() != null ? buf.getQ() : "";
            this.remaining = buf.isBusy() ? String.valueOf(buf.getRemainingCycles()) : "";
        }

        public String getName() {
            return name;
        }

        public String getBusy() {
            return busy;
        }

        public String getAddress() {
            return address;
        }

        public String getValue() {
            return value;
        }

        public String getQ() {
            return q;
        }

        public String getRemaining() {
            return remaining;
        }
    }

    public static class RegisterTableRow {
        private String name, value, status;

        public RegisterTableRow(String name, double value, String status) {
            this.name = name;
            this.value = String.format("%.2f", value);
            this.status = status != null ? status : "";
        }

        public String getName() {
            return name;
        }

        public String getValue() {
            return value;
        }

        public String getStatus() {
            return status;
        }
    }

    public static class InstructionTableRow {
        private String instruction, issue, execStart, execEnd, write, iteration;
        private boolean separator = false;

        // Regular row for an issued instruction instance
        public InstructionTableRow(Instruction inst) {
            this.instruction = inst.toString();
            this.issue = inst.getIssueTime() >= 0 ? String.valueOf(inst.getIssueTime()) : "";
            this.execStart = inst.getExecStartTime() >= 0 ? String.valueOf(inst.getExecStartTime()) : "";
            this.execEnd = inst.getExecEndTime() >= 0 ? String.valueOf(inst.getExecEndTime()) : "";
            this.write = inst.getWriteTime() >= 0 ? String.valueOf(inst.getWriteTime()) : "";
            this.iteration = inst.getIteration() > 0 ? String.valueOf(inst.getIteration()) : "";
            this.separator = false;
        }

        // Separator row (visual grouping for iterations)
        public InstructionTableRow(String separatorLabel) {
            this.instruction = separatorLabel;
            this.issue = "";
            this.execStart = "";
            this.execEnd = "";
            this.write = "";
            this.iteration = "";
            this.separator = true;
        }

        public boolean isSeparator() {
            return separator;
        }

        public String getInstruction() {
            return instruction;
        }

        public String getIteration() {
            return iteration;
        }

        public String getIssue() {
            return issue;
        }

        public String getExecStart() {
            return execStart;
        }

        public String getExecEnd() {
            return execEnd;
        }

        public String getWrite() {
            return write;
        }
    }

    public static class CacheTableRow {
        private String index, status;

        public CacheTableRow(int index, String status) {
            this.index = String.valueOf(index);
            this.status = status;
        }

        public String getIndex() {
            return index;
        }

        public String getStatus() {
            return status;
        }
    }
}
