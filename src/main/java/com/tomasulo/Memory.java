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
     * Used by: L.D (Load Double), LD (Load Doubleword)
     */
    public double load(int address) {
        return load(address, 8);
    }

    /**
     * Load 4 bytes starting from address and combine into a double
     * Used by: LW (Load Word), L.S (Load Single)
     */
    public double loadWord(int address) {
        return load(address, 4);
    }

    /**
     * Load specified number of bytes (4 or 8) from address
     * Bytes are combined in big-endian order: byte[addr] is MSB
     */
    private double load(int address, int numBytes) {
        if (address < 0 || address + numBytes - 1 >= MEMORY_SIZE) {
            return 0.0;
        }

        // Combine bytes: addr is MSB, addr+(numBytes-1) is LSB
        long combined = 0;
        for (int i = 0; i < numBytes; i++) {
            combined = (combined << 8) | (memory[address + i] & 0xFF);
        }

        // Return as double but represents integer value
        return (double) combined;
    }

    /**
     * Store a double value by splitting it into 8 bytes
     * Used by: S.D (Store Double), SD (Store Doubleword)
     */
    public void store(int address, double value) {
        store(address, value, 8);
    }

    /**
     * Store a value by splitting it into 4 bytes
     * Used by: SW (Store Word), S.S (Store Single)
     */
    public void storeWord(int address, double value) {
        store(address, value, 4);
    }

    /**
     * Store a value by splitting it into specified number of bytes (4 or 8)
     * Bytes stored in big-endian order: MSB at addr
     */
    private void store(int address, double value, int numBytes) {
        if (address < 0 || address + numBytes - 1 >= MEMORY_SIZE) {
            return;
        }

        // Convert double (treated as integer) to bytes
        long intValue = (long) value;
        System.out.printf("STORE: Address=%d, Value=%.0f (0x%016X), Bytes[%d]: ",
                address, value, intValue, numBytes);

        // Store in big-endian order: MSB at address
        int shiftStart = (numBytes - 1) * 8;
        for (int i = 0; i < numBytes; i++) {
            memory[address + i] = (byte) ((intValue >> (shiftStart - i * 8)) & 0xFF);
        }

        // Print bytes in order
        for (int i = 0; i < numBytes; i++) {
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
        // Address 0-7: value 10
        memory[0] = (byte) 0x00;
        memory[1] = (byte) 0x00;
        memory[2] = (byte) 0x00;
        memory[3] = (byte) 0x00;
        memory[4] = (byte) 0x00;
        memory[5] = (byte) 0x00;
        memory[6] = (byte) 0x00;
        memory[7] = (byte) 0x0A; // = 10

        // Address 8-15: value 20
        memory[8] = (byte) 0x00;
        memory[9] = (byte) 0x00;
        memory[10] = (byte) 0x00;
        memory[11] = (byte) 0x00;
        memory[12] = (byte) 0x00;
        memory[13] = (byte) 0x00;
        memory[14] = (byte) 0x00;
        memory[15] = (byte) 0x14; // = 20

        // Address 16-23: value 30
        memory[16] = (byte) 0x00;
        memory[17] = (byte) 0x00;
        memory[18] = (byte) 0x00;
        memory[19] = (byte) 0x00;
        memory[20] = (byte) 0x00;
        memory[21] = (byte) 0x00;
        memory[22] = (byte) 0x00;
        memory[23] = (byte) 0x1E; // = 30

        // Address 24-31: value 40
        memory[24] = (byte) 0x00;
        memory[25] = (byte) 0x00;
        memory[26] = (byte) 0x00;
        memory[27] = (byte) 0x00;
        memory[28] = (byte) 0x00;
        memory[29] = (byte) 0x00;
        memory[30] = (byte) 0x00;
        memory[31] = (byte) 0x28; // = 40
    }

    /**
     * Print memory contents for debugging
     * Shows memory in 4-byte and 8-byte aligned chunks where data exists
     */
    public void printMemory() {
        System.out.println("\n=== Final Memory State ===");

        // Track which addresses have been processed
        boolean[] processed = new boolean[MEMORY_SIZE];

        for (int i = 0; i < MEMORY_SIZE; i++) {
            if (processed[i] || memory[i] == 0)
                continue;

            // Check if this starts an 8-byte value (all 8 bytes have some data pattern)
            boolean is8ByteValue = false;
            if (i % 8 == 0 && i + 7 < MEMORY_SIZE) {
                // Check if at least one byte in next 8 is non-zero
                for (int j = 0; j < 8; j++) {
                    if (memory[i + j] != 0) {
                        is8ByteValue = true;
                        break;
                    }
                }
            }

            // Check if this starts a 4-byte value
            boolean is4ByteValue = false;
            if (!is8ByteValue && i % 4 == 0 && i + 3 < MEMORY_SIZE) {
                // Check if at least one byte in next 4 is non-zero
                for (int j = 0; j < 4; j++) {
                    if (memory[i + j] != 0) {
                        is4ByteValue = true;
                        break;
                    }
                }
            }

            if (is8ByteValue) {
                // Print 8-byte value
                System.out.printf("Address %3d-%3d: [", i, i + 7);
                for (int j = 0; j < 8; j++) {
                    System.out.printf("0x%02X", memory[i + j] & 0xFF);
                    if (j < 7)
                        System.out.print(", ");
                    processed[i + j] = true;
                }
                System.out.printf("] = %.0f (8-byte value)\n", load(i, 8));
                i += 7; // Skip the next 7 bytes
            } else if (is4ByteValue) {
                // Print 4-byte value
                System.out.printf("Address %3d-%3d: [", i, i + 3);
                for (int j = 0; j < 4; j++) {
                    System.out.printf("0x%02X", memory[i + j] & 0xFF);
                    if (j < 3)
                        System.out.print(", ");
                    processed[i + j] = true;
                }
                System.out.printf("] = %.0f (4-byte value)\n", load(i, 4));
                i += 3; // Skip the next 3 bytes
            }
        }
        System.out.println("=========================\n");
    }
}
