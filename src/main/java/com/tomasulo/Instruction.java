package com.tomasulo;

public class Instruction {
    public enum InstructionType {
        ADD_D, SUB_D, MUL_D, DIV_D, // FP double operations
        ADD_S, SUB_S, MUL_S, DIV_S, // FP single operations
        DADDI, DSUBI, // Integer operations
        L_D, L_S, LW, LD, // Loads
        S_D, S_S, SW, SD, // Stores
        BEQ, BNE // Branches
    }

    private InstructionType type;
    private String dest; // Destination register
    private String src1; // Source register 1
    private String src2; // Source register 2 or immediate/offset
    private int immediate; // Immediate value for ADDI/SUBI
    private int address; // Memory address for loads/stores
    private String label; // Branch label
    private int pc; // Program counter
    private String originalInstruction; // Original assembly string
    private int iteration = 0; // iteration counter for re-fetches after branches
    private boolean branchTaken = false;

    // Execution tracking
    private int issueTime = -1;
    private int execStartTime = -1;
    private int execEndTime = -1;
    private int writeTime = -1;

    public Instruction(InstructionType type, String dest, String src1, String src2) {
        this.type = type;
        this.dest = dest;
        this.src1 = src1;
        this.src2 = src2;
    }

    public Instruction(InstructionType type, String dest, String src1, int immediate) {
        this.type = type;
        this.dest = dest;
        this.src1 = src1;
        this.immediate = immediate;
    }

    public InstructionType getType() {
        return type;
    }

    public String getDest() {
        return dest;
    }

    public String getSrc1() {
        return src1;
    }

    public String getSrc2() {
        return src2;
    }

    public int getImmediate() {
        return immediate;
    }

    public int getAddress() {
        return address;
    }

    public void setAddress(int address) {
        this.address = address;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public int getPc() {
        return pc;
    }

    public void setPc(int pc) {
        this.pc = pc;
    }

    public int getIteration() {
        return iteration;
    }

    public void setIteration(int iteration) {
        this.iteration = iteration;
    }

    /**
     * Create a shallow copy of this instruction for a new dynamic instance (issued instance).
     * The returned Instruction will have the same static fields (type, operands, pc, original text)
     * but independent timing fields and the given iteration number.
     */
    public Instruction createInstanceForIteration(int iter) {
        Instruction copy;
        if (this.type == InstructionType.DADDI || this.type == InstructionType.DSUBI) {
            copy = new Instruction(this.type, this.dest, this.src1, this.immediate);
        } else if (this.isLoad() || this.isStore()) {
            // For load/store, keep src2 as string offset representation
            copy = new Instruction(this.type, this.dest, this.src1, this.src2);
        } else {
            copy = new Instruction(this.type, this.dest, this.src1, this.src2);
        }

        copy.setPc(this.pc);
        copy.setLabel(this.label);
        copy.setOriginalInstruction(this.originalInstruction);
        copy.setIteration(iter);
        copy.setBranchTaken(this.branchTaken);
        // timing fields default to -1 already
        return copy;
    }

    public boolean getBranchTaken() {
        return branchTaken;
    }

    public void setBranchTaken(boolean taken) {
        this.branchTaken = taken;
    }

    public String getOriginalInstruction() {
        return originalInstruction;
    }

    public void setOriginalInstruction(String str) {
        this.originalInstruction = str;
    }

    public int getIssueTime() {
        return issueTime;
    }

    public void setIssueTime(int time) {
        this.issueTime = time;
    }

    public int getExecStartTime() {
        return execStartTime;
    }

    public void setExecStartTime(int time) {
        this.execStartTime = time;
    }

    public int getExecEndTime() {
        return execEndTime;
    }

    public void setExecEndTime(int time) {
        this.execEndTime = time;
    }

    public int getWriteTime() {
        return writeTime;
    }

    public void setWriteTime(int time) {
        this.writeTime = time;
    }

    public boolean isFloatingPoint() {
        return type == InstructionType.ADD_D || type == InstructionType.SUB_D ||
                type == InstructionType.MUL_D || type == InstructionType.DIV_D ||
                type == InstructionType.ADD_S || type == InstructionType.SUB_S ||
                type == InstructionType.MUL_S || type == InstructionType.DIV_S ||
                type == InstructionType.L_D || type == InstructionType.S_D ||
                type == InstructionType.L_S || type == InstructionType.S_S;
    }

    public boolean isLoad() {
        return type == InstructionType.L_D || type == InstructionType.L_S ||
                type == InstructionType.LW || type == InstructionType.LD;
    }

    public boolean isStore() {
        return type == InstructionType.S_D || type == InstructionType.S_S ||
                type == InstructionType.SW || type == InstructionType.SD;
    }

    public boolean isBranch() {
        return type == InstructionType.BEQ || type == InstructionType.BNE;
    }

    public boolean isInteger() {
        return type == InstructionType.DADDI || type == InstructionType.DSUBI ||
                type == InstructionType.LW || type == InstructionType.SW;
    }

    @Override
    public String toString() {
        if (originalInstruction != null) {
            return originalInstruction;
        }

        // Convert type name: L_D -> L.D, ADD_D -> ADD.D, etc.
        String typeName = type.name().replace("_", ".");

        switch (type) {
            case ADD_D:
            case SUB_D:
            case MUL_D:
            case DIV_D:
            case ADD_S:
            case SUB_S:
            case MUL_S:
            case DIV_S:
                return String.format("%s %s, %s, %s", typeName, dest, src1, src2);
            case DADDI:
            case DSUBI:
                return String.format("%s %s, %s, %d", typeName, dest, src1, immediate);
            case L_D:
            case L_S:
            case LW:
            case LD:
                return String.format("%s %s, %s(%s)", typeName, dest, src2, src1);
            case S_D:
            case S_S:
            case SW:
            case SD:
                return String.format("%s %s, %s(%s)", typeName, dest, src2, src1);
            case BEQ:
            case BNE:
                return String.format("%s %s, %s, %s", typeName, src1, src2, label != null ? label : "");
            default:
                return typeName;
        }
    }
}
