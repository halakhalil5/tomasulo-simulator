package com.tomasulo;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple memory model for the Tomasulo simulator
 * Stores word-aligned data as a simple array
 */
public class Memory {
    private static final int MEMORY_SIZE = 1024; // 1024 addresses (each holds a double)
    private double[] memory;
    

    public Memory() {
        memory = new double[MEMORY_SIZE];
         memory[32] = 1.0;
    }

    public double load(int address) {
        if (address < 0 || address >= MEMORY_SIZE) {
            return 0.0;
        }
        return memory[address];
    }

    public void store(int address, double value) {
        if (address >= 0 && address < MEMORY_SIZE) {
            memory[address] = value;
        }
    }

    public void reset() {
        Arrays.fill(memory, 0.0);
    }

    public Map<Integer, Double> getSnapshot() {
        Map<Integer, Double> snapshot = new HashMap<>();
        for (int i = 0; i < MEMORY_SIZE; i++) {
            if (memory[i] != 0.0) {
                snapshot.put(i, memory[i]);
            }
        }
        return snapshot;
    }

    public void initialize(int address, double value) {
        if (address >= 0 && address < MEMORY_SIZE) {
            memory[address] = value;
        }
    }

    /**
     * Preload memory with a small set of hard-coded test data.
     */
    public void preloadWithTestData() {
        // Initialize memory with test values
        double[] testValues = { 1.0, 1.2, 3.0, 5.0, 7.0, 6.0, 10.0, 5.5, 3.0 };

        for (int i = 0; i < testValues.length && i < MEMORY_SIZE; i++) {
            memory[i] = testValues[i];
        }
    }
}
