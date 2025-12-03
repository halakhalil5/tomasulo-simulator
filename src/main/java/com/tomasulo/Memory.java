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

    /**
     * Preload memory with a small set of hard-coded test data.
     * Addresses are word-aligned (multiples of 4) to match instruction addressing.
     */
    public void preloadWithTestData() {
        // Example pattern: store some sequence and a few arbitrary values
        for (int i = 0; i < 64; i += 4) {
            memory.put(i, (double) (i / 4));
        }

        // Some specific test values
        memory.put(100, 123.456);
        memory.put(0,90.0);
        memory.put(8,90.0);
        memory.put(16,100.0);
        memory.put(24,800.0);
        memory.put(20,400.0);
        memory.put(104, -42.0);
        memory.put(200, 3.14159);
    }
}
