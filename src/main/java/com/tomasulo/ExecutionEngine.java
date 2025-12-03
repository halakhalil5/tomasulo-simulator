package com.tomasulo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExecutionEngine {
    private Config config;
    private List<ReservationStation> addSubStations;
    private List<ReservationStation> mulDivStations;
    private List<ReservationStation> intStations;
    private List<LoadStoreBuffer> loadBuffers;
    private List<LoadStoreBuffer> storeBuffers;
    private RegisterFile registerFile;
    private InstructionQueue instructionQueue;
    private CommonDataBus cdb;
    private Cache cache;
    private Memory memory;
    private int currentCycle;
    private int issueOrder;
    private Map<String, Integer> labels;
    private List<String> cycleLog;
    private int branchBusyUntil;

    public ExecutionEngine(Config config) {
        this.config = config;
        this.currentCycle = 0;
        this.issueOrder = 0;
        this.labels = new HashMap<>();
        this.cycleLog = new ArrayList<>();
        this.branchBusyUntil = -1;

        initializeComponents();
    }

    private void initializeComponents() {
        // Initialize reservation stations
        addSubStations = new ArrayList<>();
        for (int i = 0; i < config.addSubStations; i++) {
            addSubStations.add(new ReservationStation("Add" + (i + 1)));
        }

        mulDivStations = new ArrayList<>();
        for (int i = 0; i < config.mulDivStations; i++) {
            mulDivStations.add(new ReservationStation("Mul" + (i + 1)));
        }

        intStations = new ArrayList<>();
        for (int i = 0; i < config.integerStations; i++) {
            intStations.add(new ReservationStation("Int" + (i + 1)));
        }

        // Initialize load/store buffers
        loadBuffers = new ArrayList<>();
        for (int i = 0; i < config.loadBuffers; i++) {
            loadBuffers.add(new LoadStoreBuffer("Load" + (i + 1)));
        }

        storeBuffers = new ArrayList<>();
        for (int i = 0; i < config.storeBuffers; i++) {
            storeBuffers.add(new LoadStoreBuffer("Store" + (i + 1)));
        }

        // Initialize other components
        registerFile = new RegisterFile(config.numIntegerRegisters, config.numFloatRegisters);
        instructionQueue = new InstructionQueue(config.instructionQueueSize);
        cdb = new CommonDataBus();
        cache = new Cache(config.cacheSize, config.blockSize, config.cacheHitLatency, config.cacheMissPenalty);
        memory = new Memory();
    }

    public void loadProgram(List<Instruction> instructions) {
        // Parse labels
        labels.clear();
        for (int i = 0; i < instructions.size(); i++) {
            Instruction inst = instructions.get(i);
            String line = inst.getOriginalInstruction();
            if (line != null && line.contains(":")) {
                String label = line.substring(0, line.indexOf(":")).trim();
                labels.put(label, i * 4);
            }
        }

        instructionQueue.loadInstructions(instructions);
    }

    public void reset() {
        currentCycle = 0;
        issueOrder = 0;
        cycleLog.clear();

        for (ReservationStation rs : addSubStations)
            rs.clear();
        for (ReservationStation rs : mulDivStations)
            rs.clear();
        for (ReservationStation rs : intStations)
            rs.clear();
        for (LoadStoreBuffer buf : loadBuffers)
            buf.clear();
        for (LoadStoreBuffer buf : storeBuffers)
            buf.clear();

        registerFile.reset();
        instructionQueue.reset();
        cdb.clearPendingWrites();
        cache.reset();
        memory.reset();
    }

    public boolean executeCycle() {
        cycleLog.add("=== Cycle " + currentCycle + " ===");

        // 1. Write Result (CDB)
        writeResultStage();

        // 2. Execute
        executeStage();

        // 3. Issue
        issueStage();

        currentCycle++;

        // Check if simulation is complete
        return !isComplete();
    }

    private void issueStage() {
        // Stall issuing while a branch is in-flight (waiting to complete)
        if (currentCycle < branchBusyUntil) {
            // do not issue until branch finishes
            return;
        }

        if (instructionQueue.isEmpty()) {
            return;
        }

        Instruction inst = instructionQueue.peek();
        if (inst == null)
            return;
    // issuance handled per-case below

        switch (inst.getType()) {
            case ADD_D:
            case SUB_D:
                // Need a free Add/Sub RS
                if (findFreeStation(addSubStations) != null) {
                    Instruction issuedInst = instructionQueue.issue();
                    if (issuedInst != null && issueToAddSub(issuedInst)) {
                        issuedInst.setIssueTime(currentCycle);
                        cycleLog.add("Issued: " + issuedInst.toString());
                    }
                }
                break;
            case MUL_D:
            case DIV_D:
                if (findFreeStation(mulDivStations) != null) {
                    Instruction issuedInst = instructionQueue.issue();
                    if (issuedInst != null && issueToMulDiv(issuedInst)) {
                        issuedInst.setIssueTime(currentCycle);
                        cycleLog.add("Issued: " + issuedInst.toString());
                    }
                }
                break;
            case ADDI:
            case SUBI:
                if (findFreeStation(intStations) != null) {
                    Instruction issuedInst = instructionQueue.issue();
                    if (issuedInst != null && issueToInteger(issuedInst)) {
                        issuedInst.setIssueTime(currentCycle);
                        cycleLog.add("Issued: " + issuedInst.toString());
                    }
                }
                break;
            case L_D:
            case L_S:
            case LW:
            case LD:
                // For loads, base register must be ready
                if (instructionReadyForLoad(inst)) {
                    Instruction issuedInst = instructionQueue.issue();
                    if (issuedInst != null && issueLoad(issuedInst)) {
                        issuedInst.setIssueTime(currentCycle);
                        cycleLog.add("Issued: " + issuedInst.toString());
                    }
                }
                break;
            case S_D:
            case S_S:
            case SW:
            case SD:
                if (instructionReadyForStore(inst)) {
                    Instruction issuedInst = instructionQueue.issue();
                    if (issuedInst != null && issueStore(issuedInst)) {
                        issuedInst.setIssueTime(currentCycle);
                        cycleLog.add("Issued: " + issuedInst.toString());
                    }
                }
                break;
            case BEQ:
            case BNE:
                // Branch only issues if operands ready
                if (registerFile.getStatus(inst.getSrc1()).isEmpty() && registerFile.getStatus(inst.getSrc2()).isEmpty()) {
                    Instruction issuedInst = instructionQueue.issue();
                    if (issuedInst != null && issueBranch(issuedInst)) {
                        // issueBranch now handles setting times and jump
                        cycleLog.add("Issued: " + issuedInst.toString());
                    }
                }
                break;
        }
    }

    private boolean instructionReadyForLoad(Instruction inst) {
        String base = inst.getSrc1();
        // Base register must be ready and a free load buffer must exist
        if (!registerFile.getStatus(base).isEmpty()) return false;
        if (findFreeBuffer(loadBuffers) == null) return false;

        // Try to compute address (offset should be parseable)
        try {
            int offset = Integer.parseInt(inst.getSrc2());
            int address = (int) registerFile.getValue(base) + offset;

            // If any store buffer already holds this address, stall to preserve memory ordering
            for (LoadStoreBuffer sb : storeBuffers) {
                if (sb.isBusy() && sb.getAddress() == address) return false;
            }

            return true;
        } catch (NumberFormatException ex) {
            // Can't compute address now -> stall
            return false;
        }
    }

    private boolean instructionReadyForStore(Instruction inst) {
        String base = inst.getSrc1();
        String srcReg = inst.getDest(); // value to store

        // Base and source registers must be ready and a free store buffer must exist
        if (!registerFile.getStatus(base).isEmpty()) return false;
        if (!registerFile.getStatus(srcReg).isEmpty()) return false;
        if (findFreeBuffer(storeBuffers) == null) return false;

        // Compute address and check both load and store buffers for conflicts
        try {
            int offset = Integer.parseInt(inst.getSrc2());
            int address = (int) registerFile.getValue(base) + offset;

            for (LoadStoreBuffer sb : storeBuffers) {
                if (sb.isBusy() && sb.getAddress() == address) return false;
            }
            for (LoadStoreBuffer lb : loadBuffers) {
                if (lb.isBusy() && lb.getAddress() == address) return false;
            }

            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private boolean issueToAddSub(Instruction inst) {
        ReservationStation rs = findFreeStation(addSubStations);
        if (rs == null)
            return false;

        String src1 = inst.getSrc1();
        String src2 = inst.getSrc2();

        double vj = 0;
        double vk = 0;
        String qj = registerFile.getStatus(src1);
        String qk = registerFile.getStatus(src2);

        if (qj.isEmpty())
            vj = registerFile.getValue(src1);
        if (qk.isEmpty())
            vk = registerFile.getValue(src2);

        int latency = inst.getType() == Instruction.InstructionType.ADD_D ? config.addLatency : config.subLatency;

        rs.setInstruction(inst, inst.getType().name(), vj, vk, qj, qk, latency);
        registerFile.setStatus(inst.getDest(), rs.getName());

        return true;
    }

    private boolean issueToMulDiv(Instruction inst) {
        ReservationStation rs = findFreeStation(mulDivStations);
        if (rs == null)
            return false;

        String src1 = inst.getSrc1();
        String src2 = inst.getSrc2();

        double vj = 0;
        double vk = 0;
        String qj = registerFile.getStatus(src1);
        String qk = registerFile.getStatus(src2);

        if (qj.isEmpty())
            vj = registerFile.getValue(src1);
        if (qk.isEmpty())
            vk = registerFile.getValue(src2);

        int latency = inst.getType() == Instruction.InstructionType.MUL_D ? config.mulLatency : config.divLatency;

        rs.setInstruction(inst, inst.getType().name(), vj, vk, qj, qk, latency);
        registerFile.setStatus(inst.getDest(), rs.getName());

        return true;
    }

    private boolean issueToInteger(Instruction inst) {
        ReservationStation rs = findFreeStation(intStations);
        if (rs == null)
            return false;

        String src1 = inst.getSrc1();
        double vj = 0;
        String qj = registerFile.getStatus(src1);

        if (qj.isEmpty())
            vj = registerFile.getValue(src1);

        double vk = inst.getImmediate();

        int latency = inst.getType() == Instruction.InstructionType.ADDI ? config.intAddLatency : config.intSubLatency;

        rs.setInstruction(inst, inst.getType().name(), vj, vk, qj, "", latency);
        registerFile.setStatus(inst.getDest(), rs.getName());

        return true;
    }

    private boolean issueLoad(Instruction inst) {
        LoadStoreBuffer buf = findFreeBuffer(loadBuffers);
        if (buf == null)
            return false;

        // Calculate address: offset(base) -> base + offset
        String base = inst.getSrc1();
        int offset = Integer.parseInt(inst.getSrc2());

        String qBase = registerFile.getStatus(base);
        if (!qBase.isEmpty()) {
            // Base register not ready - wait
            return false;
        }

        int address = (int) registerFile.getValue(base) + offset;
        inst.setAddress(address);

        // Check cache and set latency
        int latency = cache.accessLoad(address);

        buf.setLoadInstruction(inst, address, latency);
        registerFile.setStatus(inst.getDest(), buf.getName());

        return true;
    }

    private boolean issueStore(Instruction inst) {
        LoadStoreBuffer buf = findFreeBuffer(storeBuffers);
        if (buf == null)
            return false;

        // Calculate address
        String base = inst.getSrc1();
        int offset = Integer.parseInt(inst.getSrc2());

        String qBase = registerFile.getStatus(base);
        if (!qBase.isEmpty()) {
            return false;
        }

        int address = (int) registerFile.getValue(base) + offset;
        inst.setAddress(address);

        // Get value to store
        String srcReg = inst.getDest(); // For stores, dest is actually the source
        double value = 0;
        String q = registerFile.getStatus(srcReg);

        if (q.isEmpty()) {
            value = registerFile.getValue(srcReg);
        }

        int latency = cache.accessStore(address);

        buf.setStoreInstruction(inst, address, value, q, latency);

        return true;
    }

    private boolean issueBranch(Instruction inst) {
        // Simple branch - no prediction, wait for operands
        String src1 = inst.getSrc1();
        String src2 = inst.getSrc2();

        if (!registerFile.getStatus(src1).isEmpty() || !registerFile.getStatus(src2).isEmpty()) {
            return false; // Wait for operands
        }

        double val1 = registerFile.getValue(src1);
        double val2 = registerFile.getValue(src2);

        boolean taken = inst.getType() == Instruction.InstructionType.BEQ ? (val1 == val2) : (val1 != val2);

        inst.setIssueTime(currentCycle);
        inst.setExecStartTime(currentCycle);
        inst.setExecEndTime(currentCycle + config.branchLatency);
        inst.setWriteTime(currentCycle + config.branchLatency);

        // Stall issuing until branch completes
        this.branchBusyUntil = currentCycle + config.branchLatency;

        if (taken) {
            int targetPc = labels.getOrDefault(inst.getLabel(), 0);
            instructionQueue.jumpTo(targetPc);
            cycleLog.add("Branch taken to " + inst.getLabel());
        } else {
            cycleLog.add("Branch not taken");
        }

        return true;
    }

    private void executeStage() {
        // Execute in reservation stations
        for (ReservationStation rs : addSubStations) {
            if (rs.isReady() && rs.getRemainingCycles() > 0) {
                if (rs.getInstruction().getExecStartTime() == -1) {
                    rs.getInstruction().setExecStartTime(currentCycle);
                }
                rs.decrementCycles();
                if (rs.isComplete()) {
                    double result = rs.executeOperation();
                    rs.getInstruction().setExecEndTime(currentCycle);
                    cdb.requestWrite(rs.getName(), result, rs.getInstruction(), issueOrder++);
                }
            }
        }

        for (ReservationStation rs : mulDivStations) {
            if (rs.isReady() && rs.getRemainingCycles() > 0) {
                if (rs.getInstruction().getExecStartTime() == -1) {
                    rs.getInstruction().setExecStartTime(currentCycle);
                }
                rs.decrementCycles();
                if (rs.isComplete()) {
                    double result = rs.executeOperation();
                    rs.getInstruction().setExecEndTime(currentCycle);
                    cdb.requestWrite(rs.getName(), result, rs.getInstruction(), issueOrder++);
                }
            }
        }

        for (ReservationStation rs : intStations) {
            if (rs.isReady() && rs.getRemainingCycles() > 0) {
                if (rs.getInstruction().getExecStartTime() == -1) {
                    rs.getInstruction().setExecStartTime(currentCycle);
                }
                rs.decrementCycles();
                if (rs.isComplete()) {
                    double result = rs.executeOperation();
                    rs.getInstruction().setExecEndTime(currentCycle);
                    cdb.requestWrite(rs.getName(), result, rs.getInstruction(), issueOrder++);
                }
            }
        }

        // Execute load buffers
        for (LoadStoreBuffer buf : loadBuffers) {
            if (buf.isReady() && buf.getRemainingCycles() > 0) {
                if (buf.getInstruction().getExecStartTime() == -1) {
                    buf.getInstruction().setExecStartTime(currentCycle);
                }
                buf.decrementCycles();
                if (buf.isComplete()) {
                    double value = memory.load(buf.getAddress());
                    buf.getInstruction().setExecEndTime(currentCycle);
                    cdb.requestWrite(buf.getName(), value, buf.getInstruction(), issueOrder++);
                }
            }
        }

        // Execute store buffers
        for (LoadStoreBuffer buf : storeBuffers) {
            if (buf.isReady() && buf.getRemainingCycles() > 0) {
                if (buf.getInstruction().getExecStartTime() == -1) {
                    buf.getInstruction().setExecStartTime(currentCycle);
                }
                buf.decrementCycles();
                if (buf.isComplete()) {
                    memory.store(buf.getAddress(), buf.getValue());
                    buf.getInstruction().setExecEndTime(currentCycle);
                    buf.getInstruction().setWriteTime(currentCycle);
                    buf.clear();
                    cycleLog.add("Store completed to address " + buf.getAddress());
                }
            }
        }
    }

    private void writeResultStage() {
        if (cdb.getPendingWrites().isEmpty()) {
            return;
        }

        // Select winner based on arbitration strategy
        CommonDataBus.BusEntry winner = cdb.selectWinner(config.busArbitrationStrategy);
        if (winner == null)
            return;

        cycleLog.add("CDB Write: " + winner.tag + " = " + winner.value);

        // Update reservation stations
        for (ReservationStation rs : addSubStations) {
            rs.updateOperand(winner.tag, winner.value);
            if (rs.getName().equals(winner.tag)) {
                rs.clear();
            }
        }

        for (ReservationStation rs : mulDivStations) {
            rs.updateOperand(winner.tag, winner.value);
            if (rs.getName().equals(winner.tag)) {
                rs.clear();
            }
        }

        for (ReservationStation rs : intStations) {
            rs.updateOperand(winner.tag, winner.value);
            if (rs.getName().equals(winner.tag)) {
                rs.clear();
            }
        }

        // Update load/store buffers
        for (LoadStoreBuffer buf : loadBuffers) {
            if (buf.getName().equals(winner.tag)) {
                buf.clear();
            }
        }

        for (LoadStoreBuffer buf : storeBuffers) {
            buf.updateValue(winner.tag, winner.value);
        }

        // Update register file
        for (String reg : registerFile.getRegisterStatus().keySet()) {
            if (registerFile.getStatus(reg).equals(winner.tag)) {
                registerFile.setValue(reg, winner.value);
                registerFile.clearStatus(reg);
            }
        }

        winner.instruction.setWriteTime(currentCycle);
    }

    private boolean isComplete() {
        // Check if all stations and buffers are free and queue is empty
        for (ReservationStation rs : addSubStations) {
            if (rs.isBusy())
                return false;
        }
        for (ReservationStation rs : mulDivStations) {
            if (rs.isBusy())
                return false;
        }
        for (ReservationStation rs : intStations) {
            if (rs.isBusy())
                return false;
        }
        for (LoadStoreBuffer buf : loadBuffers) {
            if (buf.isBusy())
                return false;
        }
        for (LoadStoreBuffer buf : storeBuffers) {
            if (buf.isBusy())
                return false;
        }

        return !instructionQueue.hasMoreInstructions();
    }

    private ReservationStation findFreeStation(List<ReservationStation> stations) {
        for (ReservationStation rs : stations) {
            if (!rs.isBusy())
                return rs;
        }
        return null;
    }

    private LoadStoreBuffer findFreeBuffer(List<LoadStoreBuffer> buffers) {
        for (LoadStoreBuffer buf : buffers) {
            if (!buf.isBusy())
                return buf;
        }
        return null;
    }

    // Getters
    public List<ReservationStation> getAddSubStations() {
        return addSubStations;
    }

    public List<ReservationStation> getMulDivStations() {
        return mulDivStations;
    }

    public List<ReservationStation> getIntStations() {
        return intStations;
    }

    public List<LoadStoreBuffer> getLoadBuffers() {
        return loadBuffers;
    }

    public List<LoadStoreBuffer> getStoreBuffers() {
        return storeBuffers;
    }

    public RegisterFile getRegisterFile() {
        return registerFile;
    }

    public InstructionQueue getInstructionQueue() {
        return instructionQueue;
    }

    public Cache getCache() {
        return cache;
    }

    public Memory getMemory() {
        return memory;
    }

    public int getCurrentCycle() {
        return currentCycle;
    }

    public List<String> getCycleLog() {
        return new ArrayList<>(cycleLog);
    }
}
