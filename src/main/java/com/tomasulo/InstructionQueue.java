package com.tomasulo;

import java.util.ArrayList;
import java.util.List;

public class InstructionQueue {
    private List<Instruction> allInstructions;
    private int maxSize;
    private int pc;
    private int iteration;
    private List<Instruction> issuedInstances;

    public InstructionQueue(int maxSize) {
        this.maxSize = maxSize;
        this.allInstructions = new ArrayList<>();
        this.pc = 0;
        this.iteration = 1;
        this.issuedInstances = new ArrayList<>();
    }

    public void loadInstructions(List<Instruction> instructions) {
        allInstructions.clear();
        allInstructions.addAll(instructions);
        pc = 0;
        this.iteration = 1;
        this.issuedInstances.clear();

        // Set PC for each instruction
        for (int i = 0; i < allInstructions.size(); i++) {
            allInstructions.get(i).setPc(i * 4); // MIPS uses 4-byte instructions
        }
    }

    public Instruction peek() {
        int idx = pc / 4;
        if (idx < 0 || idx >= allInstructions.size()) return null;
        return allInstructions.get(idx);
    }

    public Instruction issue() {
        int idx = pc / 4;
        if (idx < 0 || idx >= allInstructions.size()) return null;
        Instruction template = allInstructions.get(idx);
        Instruction inst = template.createInstanceForIteration(this.iteration);
        // record as issued instance for UI/history
        issuedInstances.add(inst);
        pc += 4;
        // mark that this specific issued instance will later receive timing values
        return inst;
    }

    public boolean isEmpty() {
        return peek() == null;
    }

    public boolean hasMoreInstructions() {
        // There are more instructions if PC is still pointing to a valid instruction
        // (even if it was issued before in a previous iteration, we can issue it again)
        return pc / 4 < allInstructions.size();
    }

    public List<Instruction> getQueueSnapshot() {
        List<Instruction> window = new ArrayList<>();
        int start = pc / 4;
        for (int i = start; i < Math.min(allInstructions.size(), start + maxSize); i++) {
            window.add(allInstructions.get(i));
        }
        return window;
    }

    public List<Instruction> getAllInstructions() {
        return new ArrayList<>(allInstructions);
    }

    public void reset() {
        pc = 0;
        // Reset instruction issue/execution metadata
        for (Instruction inst : allInstructions) {
            inst.setIssueTime(-1);
            inst.setExecStartTime(-1);
            inst.setExecEndTime(-1);
            inst.setWriteTime(-1);
            inst.setIteration(0);
        }
        this.iteration = 1;
        this.issuedInstances.clear();
    }

    public void jumpTo(int targetPc) {
        this.pc = targetPc;
        // New fetch sequence due to branch -> increment iteration so newly issued
        // instances are tagged as a different iteration and not overwrite history.
        this.iteration++;
    }

    /**
     * Return the list of issued instruction instances (history), in order of issue.
     */
    public List<Instruction> getIssuedInstructions() {
        return new ArrayList<>(issuedInstances);
    }
    
    /**
     * Get the current program counter (PC) value
     */
    public int getPc() {
        return pc;
    }
    
    /**
     * Get the current iteration number
     */
    public int getIteration() {
        return iteration;
    }
}
