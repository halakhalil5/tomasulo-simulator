package com.tomasulo;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple memory model for the Tomasulo simulator
 * Stores data as bytes - each address holds one byte (0x00-0xFF)
 */
public class Memory {
    private static final int MEMORY_SIZE = 1024; // 1024 bytes
    private byte[] memory;

    public Memory() {
        memory = new byte[MEMORY_SIZE];
    }

    /**
     * Load 8 bytes starting from address and combine into a double
     * Bytes are combined in big-endian order: byte[addr] is MSB
     * The combined value is treated as an integer (no floating-point conversion)
     */
    public double load(int address) {
        if (address < 0 || address + 7 >= MEMORY_SIZE) {
            return 0.0;
        }

        // Load 8 bytes for double precision (L.D instruction)
        // Combine bytes: addr is MSB, addr+7 is LSB
        // Result is treated as integer value, stored in double for compatibility
        long combined = 0;
        for (int i = 0; i < 8; i++) {
            combined = (combined << 8) | (memory[address + i] & 0xFF);
        }

        // Return as double but represents integer value
        return (double) combined;
    }

    /**
     * Store a double value by splitting it into 8 bytes
     * Value is treated as integer, not floating-point
     * Bytes stored in big-endian order: MSB at addr, LSB at addr+7
     */
    public void store(int address, double value) {
        if (address < 0 || address + 7 >= MEMORY_SIZE) {
            return;
        }

        // Convert double (treated as integer) to 8 bytes
        long intValue = (long) value;
        System.out.printf("STORE: Address=%d, Value=%.0f (0x%016X), Bytes: ", address, value, intValue);
        
        // Store in big-endian order: MSB at address, LSB at address+7
        for (int i = 0; i < 8; i++) {
            memory[address + i] = (byte) ((intValue >> (56 - i * 8)) & 0xFF);
        }
        
        // Print bytes in order
        for (int i = 0; i < 8; i++) {
            System.out.printf("0x%02X ", memory[address + i] & 0xFF);
        }
        System.out.println();
    }

    /**
     * Get a single byte from memory (for cache block loading)
     */
    public byte getByte(int address) {
        if (address < 0 || address >= MEMORY_SIZE) {
            return 0;
        }
        return memory[address];
    }

    /**
     * Set a single byte in memory
     */
    public void setByte(int address, byte value) {
        if (address >= 0 && address < MEMORY_SIZE) {
            memory[address] = value;
        }
    }

    public void reset() {
        Arrays.fill(memory, (byte) 0);
    }

    public Map<Integer, Double> getSnapshot() {
        Map<Integer, Double> snapshot = new HashMap<>();
        // Display memory in 8-byte chunks
        for (int i = 0; i < MEMORY_SIZE - 7; i += 8) {
            // Check if any byte in this 8-byte chunk is non-zero
            boolean hasData = false;
            for (int j = 0; j < 8; j++) {
                if (memory[i + j] != 0) {
                    hasData = true;
                    break;
                }
            }
            if (hasData) {
                snapshot.put(i, load(i));
            }
        }
        return snapshot;
    }

    public void initialize(int address, double value) {
        store(address, value);
    }

    /**
     * Preload memory with test data as individual bytes in hex
     * Using smaller values for easier verification
     */
    public void preloadWithTestData() {
        // Initialize memory with small hex byte values
        memory[0] = (byte) 0x00;
        memory[1] = (byte) 0x00;
        memory[2] = (byte) 0x00;
        memory[3] = (byte) 0x00;
        memory[4] = (byte) 0x00;
        memory[5] = (byte) 0x00;
        memory[6] = (byte) 0x00;
        memory[7] = (byte) 0x0A; // = 10
        memory[8] = (byte) 0x00;
        memory[9] = (byte) 0x00;
        memory[10] = (byte) 0x00;
        memory[11] = (byte) 0x00;
        memory[12] = (byte) 0x00;
        memory[13] = (byte) 0x00;
        memory[14] = (byte) 0x00;
        memory[15] = (byte) 0x14; // = 20
    }

    /**
     * Print memory contents for debugging (shows non-zero 8-byte chunks)
     */
    public void printMemory() {
        System.out.println("\n=== Final Memory State ===");
        for (int i = 0; i < MEMORY_SIZE; i += 8) {
            // Check if this 8-byte chunk has any non-zero data
            boolean hasData = false;
            for (int j = 0; j < 8 && (i + j) < MEMORY_SIZE; j++) {
                if (memory[i + j] != 0) {
                    hasData = true;
                    break;
                }
            }
            
            if (hasData) {
                System.out.printf("Address %3d-%3d: [", i, i + 7);
                for (int j = 0; j < 8 && (i + j) < MEMORY_SIZE; j++) {
                    System.out.printf("0x%02X", memory[i + j] & 0xFF);
                    if (j < 7) System.out.print(", ");
                }
                System.out.printf("] = %.0f\n", load(i));
            }
        }
        System.out.println("=========================\n");
    }
}
