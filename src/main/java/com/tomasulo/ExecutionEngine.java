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
    private List<ReservationStation> branchStations;
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
    private int branchBusyUntil; // Cycle until which all issuing is stalled due to branch

    public ExecutionEngine(Config config) {
        this.config = config;
        this.currentCycle = 0;
        this.issueOrder = 0;
        this.branchBusyUntil = -1; // No branch stall initially
        this.labels = new HashMap<>();
        this.cycleLog = new ArrayList<>();
        // branchStations list initialized in initializeComponents

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

        // Branch reservation stations
        branchStations = new ArrayList<>();
        for (int i = 0; i < config.branchStations; i++) {
            branchStations.add(new ReservationStation("Br" + (i + 1)));
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
        // Preload memory with hard-coded test data for cache/memory testing
        memory.preloadWithTestData();
        // Set memory reference in cache so it can load blocks
        cache.setMemory(memory);
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
        reset(false);
    }

    public void reset(boolean preserveRegisters) {
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

        if (!preserveRegisters) {
            registerFile.reset();
        } else {
            // Only clear register status, keep values
            registerFile.clearAllStatus();
        }
        instructionQueue.reset();
        cdb.clearPendingWrites();
        cache.reset();
        memory.reset();
    }

    public boolean executeCycle() {
        currentCycle++;
        cycleLog.add("=== Cycle " + currentCycle + " ===");

        // 1. Write Result (CDB) - select winner and broadcast
        CommonDataBus.BusEntry winner = writeResultStage();

        // 2. Commit write - update registers and clear buffers BEFORE issuing new instructions
        //    This prevents buffer reuse in the same cycle
        if (winner != null) {
            write(winner);
        }

        // 3. Execute
        executeStage();

        // 4. Issue
        issueStage();

        // Check if simulation is complete
        return !isComplete();
    }

    private void issueStage() {
        if (instructionQueue.isEmpty()) {
            return;
        }

        Instruction inst = instructionQueue.peek();
        if (inst == null)
            return;
        // Prevent issuing the same static instruction while a previous instance
        // of it is still pending write-back. This avoids an instruction being
        // in both "write-back" and "issue" in the same cycle.
        for (CommonDataBus.BusEntry pending : cdb.getPendingWrites()) {
            if (pending != null && pending.instruction != null && pending.instruction.getPc() == inst.getPc()) {
                // Stall issuing this instruction until the older instance completes
                return;
            }
        }

        // Check if this is a branch - branches can issue immediately
        boolean isBranch = (inst.getType() == Instruction.InstructionType.BEQ ||
                inst.getType() == Instruction.InstructionType.BNE);

        // If there's an active branch stall and this is NOT a branch, stall all issuing
        if (!isBranch && currentCycle <= branchBusyUntil) {
            cycleLog.add("Stalled issuing due to branch in execution");
            return;
        }
        // issuance handled per-case below

        switch (inst.getType()) {
            case ADD_D:
            case SUB_D:
            case ADD_S:
            case SUB_S:
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
            case MUL_S:
            case DIV_S:
                if (findFreeStation(mulDivStations) != null) {
                    Instruction issuedInst = instructionQueue.issue();
                    if (issuedInst != null && issueToMulDiv(issuedInst)) {
                        issuedInst.setIssueTime(currentCycle);
                        cycleLog.add("Issued: " + issuedInst.toString());
                    }
                }
                break;
            case DADDI:
            case DSUBI:
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
                // Branch issues immediately (captures Qi if operands not ready)
                if (findFreeStation(branchStations) != null) {
                    Instruction issuedInst = instructionQueue.issue();
                    if (issuedInst != null) {
                        boolean issuedOk = issueBranch(issuedInst);
                        if (issuedOk) {
                            cycleLog.add("Issued: " + issuedInst.toString());
                        }
                    }
                }
                break;
        }
    }

    private boolean instructionReadyForLoad(Instruction inst) {
        String base = inst.getSrc1();
        // Base register must be ready and a free load buffer must exist
        if (!registerFile.getStatus(base).isEmpty())
            return false;
        if (findFreeBuffer(loadBuffers) == null)
            return false;

        // Try to compute address (offset should be parseable)
        try {
            int offset = Integer.parseInt(inst.getSrc2());
            int address = (int) registerFile.getValue(base) + offset;

            // If any store buffer already holds this address, stall to preserve memory
            // ordering
            for (LoadStoreBuffer sb : storeBuffers) {
                if (sb.isBusy() && sb.getAddress() == address)
                    return false;
            }

            return true;
        } catch (NumberFormatException ex) {
            // Can't compute address now -> stall
            return false;
        }
    }

    private boolean instructionReadyForStore(Instruction inst) {
        String base = inst.getSrc1();
        // value-to-store register is intentionally not required to be ready
        // at issue time; store buffer will record its Qi if needed.

        // Base and source registers must be ready and a free store buffer must exist
        // Base register must be ready (to compute address). The source register
        // value may be produced later; we allow issuing a store with Q set so
        // it will receive the value when ready.
        if (!registerFile.getStatus(base).isEmpty())
            return false;
        if (findFreeBuffer(storeBuffers) == null)
            return false;

        // Compute address and check both load and store buffers for conflicts
        try {
            int offset = Integer.parseInt(inst.getSrc2());
            int address = (int) registerFile.getValue(base) + offset;

            for (LoadStoreBuffer sb : storeBuffers) {
                if (sb.isBusy() && sb.getAddress() == address)
                    return false;
            }
            for (LoadStoreBuffer lb : loadBuffers) {
                if (lb.isBusy() && lb.getAddress() == address)
                    return false;
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

        int latency;
        if (inst.getType() == Instruction.InstructionType.ADD_D
                || inst.getType() == Instruction.InstructionType.ADD_S) {
            latency = config.addLatency;
        } else {
            latency = config.subLatency;
        }

        // Convert operation name: ADD_D -> ADD.D
        String opName = inst.getType().name().replace("_", ".");
        rs.setInstruction(inst, opName, vj, vk, qj, qk, latency);
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

        int latency;
        if (inst.getType() == Instruction.InstructionType.MUL_D
                || inst.getType() == Instruction.InstructionType.MUL_S) {
            latency = config.mulLatency;
        } else {
            latency = config.divLatency;
        }

        // Convert operation name: MUL_D -> MUL.D
        String opName = inst.getType().name().replace("_", ".");
        rs.setInstruction(inst, opName, vj, vk, qj, qk, latency);
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

        int latency = inst.getType() == Instruction.InstructionType.DADDI ? config.intAddLatency : config.intSubLatency;

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

        // Cache will be accessed during execution stage, not issue
        // Set initial latency to load latency (cache latency added during execution)
        int latency = config.loadLatency;

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

        // Cache will be accessed during execution stage, not issue
        // Set initial latency to store latency (cache latency added during execution)
        int latency = config.storeLatency;

        buf.setStoreInstruction(inst, address, value, q, latency);

        return true;
    }

    private boolean issueBranch(Instruction inst) {
        // Simple branch - no prediction, wait for operands
        String src1 = inst.getSrc1();
        String src2 = inst.getSrc2();

        // Need a free branch reservation station
        ReservationStation rs = findFreeStation(branchStations);
        if (rs == null)
            return false;

        // Prepare operand values or Qi tags
        double vj = 0;
        double vk = 0;
        String qj = registerFile.getStatus(src1);
        String qk = registerFile.getStatus(src2);

        if (qj.isEmpty())
            vj = registerFile.getValue(src1);
        if (qk.isEmpty())
            vk = registerFile.getValue(src2);

        // Set branch into branch RS with branch latency
        rs.setInstruction(inst, inst.getType().name(), vj, vk, qj, qk, config.branchLatency);

        inst.setIssueTime(currentCycle);

        // Stall all subsequent issuing until this branch completes execution
        this.branchBusyUntil = currentCycle + config.branchLatency;
        cycleLog.add("Branch issued - stalling all issuing until cycle " + branchBusyUntil);

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
                // Access cache at the start of execution (first cycle only)
                if (!buf.isCacheAccessed()) {
                    // Use appropriate load method based on instruction type
                    Instruction inst = buf.getInstruction();
                    boolean isWord = (inst.getType() == Instruction.InstructionType.LW ||
                            inst.getType() == Instruction.InstructionType.L_S);
                    double memoryValue = isWord ? memory.loadWord(buf.getAddress()) : memory.load(buf.getAddress());
                    int cacheLatency = cache.accessLoad(buf.getAddress(), memoryValue);
                    buf.addCacheLatency(cacheLatency);
                    buf.setCacheAccessed(true);
                    // Log cache access to cycle log
                    String lastCacheLog = cache.getLastAccess();
                    if (!lastCacheLog.isEmpty()) {
                        cycleLog.add(lastCacheLog);
                    }
                }
                buf.decrementCycles();
                if (buf.isComplete()) {
                    // Use appropriate load method based on instruction type
                    Instruction inst = buf.getInstruction();
                    boolean isWord = (inst.getType() == Instruction.InstructionType.LW ||
                            inst.getType() == Instruction.InstructionType.L_S);
                    double value = isWord ? memory.loadWord(buf.getAddress()) : memory.load(buf.getAddress());
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
                // Access cache at the start of execution (first cycle only)
                if (!buf.isCacheAccessed()) {
                    int cacheLatency = cache.accessStore(buf.getAddress(), buf.getValue());
                    buf.addCacheLatency(cacheLatency);
                    buf.setCacheAccessed(true);
                    // Log cache access to cycle log
                    String lastCacheLog = cache.getLastAccess();
                    if (!lastCacheLog.isEmpty()) {
                        cycleLog.add(lastCacheLog);
                    }
                }
                buf.decrementCycles();
                if (buf.isComplete()) {
                    // Do not perform the store immediately; schedule a CDB write so the
                    // actual memory.store and buffer clear happen in the next cycle's
                    // writeResultStage (writes happen after execute stage).
                    buf.getInstruction().setExecEndTime(currentCycle);
                    cdb.requestWrite(buf.getName(), buf.getValue(), buf.getInstruction(), issueOrder++);
                }
            }
        }

        // Execute branch reservation stations
        for (ReservationStation rs : branchStations) {
            if (rs.isReady() && rs.getRemainingCycles() > 0) {
                if (rs.getInstruction().getExecStartTime() == -1) {
                    rs.getInstruction().setExecStartTime(currentCycle);
                }
                rs.decrementCycles();
                if (rs.isComplete()) {
                    // When branch completes execution, evaluate the condition using
                    // the RS operands (vj/vk) and request a CDB write so the jump
                    // is applied during the write stage.
                    Instruction b = rs.getInstruction();
                    b.setExecEndTime(currentCycle);
                    double val1 = rs.getVj();
                    double val2 = rs.getVk();
                    boolean taken = b.getType() == Instruction.InstructionType.BEQ ? (val1 == val2) : (val1 != val2);
                    b.setBranchTaken(taken);
                    cycleLog.add(String.format("Branch eval: %s comparing %.2f %s %.2f = %s, target: %s",
                            b.getType(), val1, (b.getType() == Instruction.InstructionType.BEQ ? "==" : "!="),
                            val2, taken, b.getLabel()));
                    cdb.requestWrite(rs.getName(), 0.0, b, issueOrder++);
                }
            }
        }
    }

    private CommonDataBus.BusEntry writeResultStage() {
        if (cdb.getPendingWrites().isEmpty()) {
            return null;
        }

        // Select winner based on arbitration strategy
        CommonDataBus.BusEntry winner = cdb.selectWinner(config.busArbitrationStrategy);
        if (winner == null)
            return null;

        cycleLog.add("CDB Write: " + winner.tag + " = " + winner.value + " (instruction: " +
                (winner.instruction != null ? winner.instruction.toString() : "null") + ")");

        // Update reservation stations

        // Update register file FIRST, before clearing any buffers

        // Update load/store buffers
        for (LoadStoreBuffer buf : loadBuffers) {
            if (buf.getName().equals(winner.tag)) {
                buf.clear();
            }
        }

        // If the write corresponds to a branch, apply the branch effect now (jump on
        // write-back)
        if (winner.instruction != null && winner.instruction.isBranch()) {
            Instruction br = winner.instruction;
            br.setWriteTime(currentCycle);
            String label = br.getLabel();
            cycleLog.add("Branch write-back: label/target = '" + label + "', taken = " + br.getBranchTaken());

            if (br.getBranchTaken()) {
                int targetPc;

                // Check if the label is actually a numeric PC address
                try {
                    targetPc = Integer.parseInt(label);
                    cycleLog.add("Branch TAKEN: Jumping to numeric PC " + targetPc);
                } catch (NumberFormatException e) {
                    // It's a label name, look it up in the labels map
                    targetPc = labels.getOrDefault(label, 0);
                    cycleLog.add("Branch TAKEN: Jumping to label '" + label + "' resolved to PC " + targetPc);
                }

                instructionQueue.jumpTo(targetPc);
                cycleLog.add("PC updated from " + (targetPc - 4) + " to " + instructionQueue.getPc());
            } else {
                cycleLog.add("Branch NOT taken - continuing sequential execution");
            }
        }

        // Set write time for the instruction
        if (winner.instruction != null) {
            winner.instruction.setWriteTime(currentCycle);
        }
        return winner;
    }

    public void write(CommonDataBus.BusEntry winner) {
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

        for (ReservationStation rs : branchStations) {
            rs.updateOperand(winner.tag, winner.value);
            if (rs.getName().equals(winner.tag)) {
                rs.clear();
            }
        }
        for (LoadStoreBuffer buf : storeBuffers) {
            // If this write corresponds to a store buffer producing its value,
            // update the buffer's value/Q as usual.
            buf.updateValue(winner.tag, winner.value);
            // If the CDB winner is the store buffer tag itself, perform the actual memory
            // store
            // and clear the buffer now (this makes stores commit in the write stage).
            if (buf.getName().equals(winner.tag)) {
                // Use appropriate store method based on instruction type
                Instruction inst = buf.getInstruction();
                int address = buf.getAddress();  // Save address before clearing
                boolean isWord = (inst.getType() == Instruction.InstructionType.SW ||
                        inst.getType() == Instruction.InstructionType.S_S);
                if (isWord) {
                    memory.storeWord(address, buf.getValue());
                } else {
                    memory.store(address, buf.getValue());
                }
                buf.getInstruction().setWriteTime(currentCycle);
                buf.clear();
                cycleLog.add("Store completed to address " + address);
            }
        }
        boolean foundRegister = false;
        for (String reg : registerFile.getRegisterStatus().keySet()) {
            String regStatus = registerFile.getStatus(reg);
            if (regStatus != null && !regStatus.isEmpty() && regStatus.equals(winner.tag)) {
                cycleLog.add("Updating register " + reg + " with value " + winner.value + " (clearing Qi: " + winner.tag
                        + ")");
                registerFile.setValue(reg, winner.value);
                registerFile.clearStatus(reg);
                foundRegister = true;
            }
        }
        if (!foundRegister && winner.instruction != null && !winner.instruction.isBranch()
                && !winner.instruction.isStore()) {
            cycleLog.add("WARNING: No register found with Qi=" + winner.tag + " for instruction "
                    + winner.instruction.toString());
        }
    }

    private boolean isComplete() {
        // Simulation is complete when:
        // 1. All instructions have been issued (no more in queue to fetch)
        // 2. All reservation stations are empty
        // 3. All load/store buffers are empty
        // 4. All branch stations are empty

        // Check if there are more instructions to issue
        if (instructionQueue.hasMoreInstructions()) {
            return false;
        }

        // Check all reservation stations
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
        for (ReservationStation rs : branchStations) {
            if (rs.isBusy())
                return false;
        }

        // Check all load/store buffers
        for (LoadStoreBuffer buf : loadBuffers) {
            if (buf.isBusy())
                return false;
        }
        for (LoadStoreBuffer buf : storeBuffers) {
            if (buf.isBusy())
                return false;
        }

        return true;
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

    public List<ReservationStation> getBranchStations() {
        return branchStations;
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
