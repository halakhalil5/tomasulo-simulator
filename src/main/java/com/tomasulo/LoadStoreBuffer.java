package com.tomasulo;

public class LoadStoreBuffer {
    private String name;
    private boolean busy;
    private int address;
    private double value; // For stores
    private String q; // Tag of instruction producing value (for stores)
    private Instruction instruction;
    private int remainingCycles;
    private boolean isLoad;

    public LoadStoreBuffer(String name) {
        this.name = name;
        this.busy = false;
        this.q = "";
    }

    public void clear() {
        busy = false;
        address = 0;
        value = 0;
        q = "";
        instruction = null;
        remainingCycles = 0;
        isLoad = false;
    }

    public void setLoadInstruction(Instruction inst, int address, int latency) {
        this.busy = true;
        this.instruction = inst;
        this.address = address;
        this.remainingCycles = latency;
        this.isLoad = true;
        this.q = "";
    }

    public void setStoreInstruction(Instruction inst, int address, double value, String q, int latency) {
        this.busy = true;
        this.instruction = inst;
        this.address = address;
        this.value = value;
        this.q = q;
        this.remainingCycles = latency;
        this.isLoad = false;
    }

    public boolean isReady() {
        if (isLoad) {
            return busy;
        } else {
            return busy && q.isEmpty();
        }
    }

    public void decrementCycles() {
        if (remainingCycles > 0) {
            remainingCycles--;
        }
    }

    public boolean isComplete() {
        return busy && remainingCycles == 0;
    }

    public void updateValue(String tag, double val) {
        if (q.equals(tag)) {
            value = val;
            q = "";
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

    public int getAddress() {
        return address;
    }

    public void setAddress(int address) {
        this.address = address;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public String getQ() {
        return q;
    }

    public void setQ(String q) {
        this.q = q;
    }

    public Instruction getInstruction() {
        return instruction;
    }

    public void setInstruction(Instruction inst) {
        this.instruction = inst;
    }

    public int getRemainingCycles() {
        return remainingCycles;
    }

    public void setRemainingCycles(int cycles) {
        this.remainingCycles = cycles;
    }

    public boolean isLoad() {
        return isLoad;
    }

    public void setIsLoad(boolean isLoad) {
        this.isLoad = isLoad;
    }
}
