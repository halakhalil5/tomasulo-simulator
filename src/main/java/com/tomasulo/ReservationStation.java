package com.tomasulo;

public class ReservationStation {
    private String name;
    private boolean busy;
    private String op;
    private double vj;
    private double vk;
    private String qj;
    private String qk;
    private int remainingCycles;
    private Instruction instruction;
    private int address; // For load/store

    public ReservationStation(String name) {
        this.name = name;
        this.busy = false;
        this.qj = "";
        this.qk = "";
    }

    public void clear() {
        busy = false;
        op = null;
        vj = 0;
        vk = 0;
        qj = "";
        qk = "";
        remainingCycles = 0;
        instruction = null;
        address = 0;
    }

    public void setInstruction(Instruction inst, String operation,
            double vj, double vk, String qj, String qk, int latency) {
        this.busy = true;
        this.instruction = inst;
        this.op = operation;
        this.vj = vj;
        this.vk = vk;
        this.qj = qj;
        this.qk = qk;
        this.remainingCycles = latency;
    }

    public boolean isReady() {
        return busy && qj.isEmpty() && qk.isEmpty();
    }

    public void decrementCycles() {
        if (remainingCycles > 0) {
            remainingCycles--;
        }
    }

    public boolean isComplete() {
        return busy && remainingCycles == 0;
    }

    public void updateOperand(String tag, double value) {
        if (qj.equals(tag)) {
            vj = value;
            qj = "";
        }
        if (qk.equals(tag)) {
            vk = value;
            qk = "";
        }
    }

    public double executeOperation() {
        switch (op) {
            case "ADD":
            case "ADD.D":
            case "ADD.S":
            case "DADDI":
                return vj + vk;
            case "SUB":
            case "SUB.D":
            case "SUB.S":
            case "DSUBI":
                return vj - vk;
            case "MUL":
            case "MUL.D":
            case "MUL.S":
                return vj * vk;
            case "DIV":
            case "DIV.D":
            case "DIV.S":
                return vj / vk;
            default:
                return 0;
        }
    }

    // Getters and setters
    public String getName() {
        return name;
    }

    public boolean isBusy() {
        return busy;
    }

    public void setBusy(boolean busy) {
        this.busy = busy;
    }

    public String getOp() {
        return op;
    }

    public void setOp(String op) {
        this.op = op;
    }

    public double getVj() {
        return vj;
    }

    public void setVj(double vj) {
        this.vj = vj;
    }

    public double getVk() {
        return vk;
    }

    public void setVk(double vk) {
        this.vk = vk;
    }

    public String getQj() {
        return qj;
    }

    public void setQj(String qj) {
        this.qj = qj;
    }

    public String getQk() {
        return qk;
    }

    public void setQk(String qk) {
        this.qk = qk;
    }

    public int getRemainingCycles() {
        return remainingCycles;
    }

    public void setRemainingCycles(int cycles) {
        this.remainingCycles = cycles;
    }

    public Instruction getInstruction() {
        return instruction;
    }

    public void setInstruction(Instruction inst) {
        this.instruction = inst;
    }

    public int getAddress() {
        return address;
    }

    public void setAddress(int address) {
        this.address = address;
    }
}
