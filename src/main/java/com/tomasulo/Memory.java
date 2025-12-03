package com.tomasulo;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple memory model for the Tomasulo simulator
 * Stores word-aligned data (4 bytes per address)
 */
public class Memory {
    private Map<Integer, Double> memory;

    public Memory() {
        memory = new HashMap<>();
    }

    public double load(int address) {
        return memory.getOrDefault(address, 0.0);
    }

    public void store(int address, double value) {
        memory.put(address, value);
    }

    public void reset() {
        memory.clear();
    }

    public Map<Integer, Double> getSnapshot() {
        return new HashMap<>(memory);
    }

    public void initialize(int address, double value) {
        memory.put(address, value);
    }
}
