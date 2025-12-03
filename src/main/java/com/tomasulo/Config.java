package com.tomasulo;

public class Config {
    // Reservation Station sizes
    public int addSubStations = 3;
    public int mulDivStations = 2;
    public int integerStations = 2;
    public int branchStations = 1;
    public int loadBuffers = 3;
    public int storeBuffers = 3;

    // Instruction latencies (in cycles)
    public int addLatency = 2;
    public int subLatency = 2;
    public int mulLatency = 10;
    public int divLatency = 40;
    public int loadLatency = 2;
    public int storeLatency = 2;
    public int intAddLatency = 1;
    public int intSubLatency = 1;
    public int branchLatency = 1;

    // Cache configuration
    public int cacheSize = 256; // in bytes
    public int blockSize = 16; // in bytes
    public int cacheHitLatency = 1;
    public int cacheMissPenalty = 50;

    // Register file configuration
    public int numIntegerRegisters = 32;
    public int numFloatRegisters = 32;

    // Instruction queue size
    public int instructionQueueSize = 16;

    // Bus arbitration strategy
    // When multiple instructions want to write to CDB in same cycle:
    // 0 = First come first serve (by issue order)
    // 1 = Oldest instruction first
    // 2 = Random selection
    public int busArbitrationStrategy = 1;

    public Config() {
    }

    public Config copy() {
        Config c = new Config();
        c.addSubStations = this.addSubStations;
        c.mulDivStations = this.mulDivStations;
        c.integerStations = this.integerStations;
    c.branchStations = this.branchStations;
        c.loadBuffers = this.loadBuffers;
        c.storeBuffers = this.storeBuffers;

        c.addLatency = this.addLatency;
        c.subLatency = this.subLatency;
        c.mulLatency = this.mulLatency;
        c.divLatency = this.divLatency;
        c.loadLatency = this.loadLatency;
        c.storeLatency = this.storeLatency;
        c.intAddLatency = this.intAddLatency;
        c.intSubLatency = this.intSubLatency;
        c.branchLatency = this.branchLatency;

        c.cacheSize = this.cacheSize;
        c.blockSize = this.blockSize;
        c.cacheHitLatency = this.cacheHitLatency;
        c.cacheMissPenalty = this.cacheMissPenalty;

        c.numIntegerRegisters = this.numIntegerRegisters;
        c.numFloatRegisters = this.numFloatRegisters;
        c.instructionQueueSize = this.instructionQueueSize;
        c.busArbitrationStrategy = this.busArbitrationStrategy;

        return c;
    }
}
