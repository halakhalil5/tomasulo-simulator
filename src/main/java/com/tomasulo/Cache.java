package com.tomasulo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cache implementation for Tomasulo simulator
 * Uses direct-mapped cache with configurable block size
 * 
 * Address mapping:
 * - Block offset: log2(blockSize) bits (rightmost)
 * - Index: log2(numBlocks) bits
 * - Tag: remaining bits (leftmost)
 */
public class Cache {
    private int cacheSize; // Total cache size in bytes
    private int blockSize; // Block size in bytes
    private int numBlocks; // Number of cache blocks
    private int hitLatency; // Cycles for cache hit
    private int missPenalty; // Additional cycles for cache miss

    private Map<Integer, CacheBlock> cache;
    private List<String> accessLog;
    private Memory memory; // Reference to memory for loading blocks

    private static class CacheBlock {
        boolean valid;
        int tag;
        byte[] data; // Store individual bytes
        int blockStartAddress; // Starting address of this cached block

        CacheBlock(int blockSize) {
            this.valid = false;
            this.tag = -1;
            this.data = new byte[blockSize];
            this.blockStartAddress = -1;
        }
    }

    public Cache(int cacheSize, int blockSize, int hitLatency, int missPenalty) {
        this.cacheSize = cacheSize;
        this.blockSize = blockSize;
        this.numBlocks = cacheSize / blockSize;
        this.hitLatency = hitLatency;
        this.missPenalty = missPenalty;

        this.cache = new HashMap<>();
        this.accessLog = new ArrayList<>();
        this.memory = null;

        // Initialize cache blocks
        for (int i = 0; i < numBlocks; i++) {
            cache.put(i, new CacheBlock(blockSize));
        }
    }

    public void setMemory(Memory memory) {
        this.memory = memory;
    }

    /**
     * Access cache for load operation (NO spatial locality - exact address range)
     * Returns latency (hitLatency for hit, hitLatency + missPenalty for miss)
     */
    public int accessLoad(int address, double memoryValue) {
        int index = (address / blockSize) % numBlocks;
        int tag = address / (blockSize * numBlocks);

        CacheBlock block = cache.get(index);

        if (block.valid && block.tag == tag && block.blockStartAddress == address) {
            // Cache hit - exact address match
            accessLog.add(String.format("Cycle: Load HIT - Addr: 0x%X (Tag: %d, Index: %d)",
                    address, tag, index));
            return hitLatency;
        } else {
            // Cache miss - load exact bytes from access address (no block alignment)
            accessLog.add(String.format("Cycle: Load MISS - Addr: 0x%X (Tag: %d, Index: %d)",
                    address, tag, index));

            // Store exact access address (no alignment)
            block.valid = true;
            block.tag = tag;
            block.blockStartAddress = address;

            // Load exact blockSize bytes starting from access address
            if (memory != null) {
                for (int i = 0; i < blockSize; i++) {
                    block.data[i] = memory.getByte(address + i);
                }
            }

            return hitLatency + missPenalty;
        }
    }

    /**
     * Check cache for store operation and return latency (NO spatial locality)
     * On miss: loads exact bytes from access address (making it visible in GUI)
     * Does NOT write the store value yet - that happens in writeStoreValue()
     * Returns latency (hitLatency for hit, hitLatency + missPenalty for miss)
     */
    public int checkStoreLatency(int address, boolean isWordStore) {
        int index = (address / blockSize) % numBlocks;
        int tag = address / (blockSize * numBlocks);
        int numBytes = isWordStore ? 4 : 8;

        CacheBlock block = cache.get(index);

        if (block.valid && block.tag == tag && block.blockStartAddress == address) {
            // Cache hit - exact address match
            accessLog.add(String.format("Cycle: Store HIT - Addr: 0x%X (Tag: %d, Index: %d) [%d bytes]",
                    address, tag, index, numBytes));
            return hitLatency;
        } else {
            // Cache miss - load exact bytes from access address (no block alignment)
            accessLog.add(String.format("Cycle: Store MISS - Addr: 0x%X (Tag: %d, Index: %d) [%d bytes]",
                    address, tag, index, numBytes));

            // Store exact access address (no alignment)
            block.valid = true;
            block.tag = tag;
            block.blockStartAddress = address;

            // Load exact blockSize bytes starting from access address
            if (memory != null) {
                for (int i = 0; i < blockSize; i++) {
                    block.data[i] = memory.getByte(address + i);
                }
            }

            return hitLatency + missPenalty;
        }
    }

    /**
     * Write only the store value to cache (assumes block is already loaded)
     * Should be called on the last cycle of store execution
     */
    public void writeStoreValue(int address, double memoryValue, boolean isWordStore) {
        int index = (address / blockSize) % numBlocks;
        int numBytes = isWordStore ? 4 : 8;

        CacheBlock block = cache.get(index);

        // Update bytes starting from position 0 (since we store exact address range)
        long intValue = (long) memoryValue;
        int shiftStart = (numBytes == 4) ? 24 : 56;
        for (int i = 0; i < numBytes && i < blockSize; i++) {
            block.data[i] = (byte) ((intValue >> (shiftStart - i * 8)) & 0xFF);
        }
    }

    /**
     * Write data to cache for store operation (NO spatial locality)
     * Should be called AFTER checkStoreLatency
     * Handles both cache hit (update existing block) and miss (load exact bytes
     * then update)
     */
    public void writeToCache(int address, double memoryValue, boolean isWordStore) {
        int index = (address / blockSize) % numBlocks;
        int tag = address / (blockSize * numBlocks);
        int numBytes = isWordStore ? 4 : 8;

        CacheBlock block = cache.get(index);

        // If cache miss, load exact bytes from access address (no alignment)
        if (!block.valid || block.tag != tag || block.blockStartAddress != address) {
            // Store exact access address (no alignment)
            block.valid = true;
            block.tag = tag;
            block.blockStartAddress = address;

            // Load exact blockSize bytes starting from access address
            if (memory != null) {
                for (int i = 0; i < blockSize; i++) {
                    block.data[i] = memory.getByte(address + i);
                }
            }
        }

        // Update bytes starting from position 0 (since we store exact address range)
        long intValue = (long) memoryValue;
        int shiftStart = (numBytes == 4) ? 24 : 56;
        for (int i = 0; i < numBytes && i < blockSize; i++) {
            block.data[i] = (byte) ((intValue >> (shiftStart - i * 8)) & 0xFF);
        }
    }

    /**
     * Access cache for store operation (combined check + write)
     * Returns latency (hitLatency for hit, hitLatency + missPenalty for miss)
     * This is a convenience method that calls checkStoreLatency then writeToCache
     */
    public int accessStore(int address, double memoryValue, boolean isWordStore) {
        int latency = checkStoreLatency(address, isWordStore);
        writeToCache(address, memoryValue, isWordStore);
        return latency;
    }

    /**
     * Check if address would hit without modifying cache state
     */
    public boolean wouldHit(int address) {
        int index = (address / blockSize) % numBlocks;
        int tag = address / (blockSize * numBlocks);

        CacheBlock block = cache.get(index);
        return block.valid && block.tag == tag;
    }

    public void invalidate(int address) {
        int index = (address / blockSize) % numBlocks;
        CacheBlock block = cache.get(index);
        block.valid = false;
    }

    public void reset() {
        for (CacheBlock block : cache.values()) {
            block.valid = false;
            block.tag = -1;
        }
        accessLog.clear();
    }

    public List<String> getAccessLog() {
        return new ArrayList<>(accessLog);
    }

    public String getLastAccess() {
        if (accessLog.isEmpty()) {
            return "";
        }
        return accessLog.get(accessLog.size() - 1);
    }

    public Map<Integer, String> getCacheSnapshot() {
        Map<Integer, String> snapshot = new HashMap<>();
        for (int i = 0; i < numBlocks; i++) {
            CacheBlock block = cache.get(i);
            if (block.valid) {
                snapshot.put(i, String.format("Tag: %d, Valid: true", block.tag));
            } else {
                snapshot.put(i, "Valid: false");
            }
        }
        return snapshot;
    }

    /**
     * Detailed snapshot exposing per-block fields: valid, tag, and raw data.
     */
    public static class CacheBlockInfo {
        public final boolean valid;
        public final int tag;
        public final byte[] data;
        public final int blockStartAddress; // Block start address

        public CacheBlockInfo(boolean valid, int tag, byte[] data, int blockStartAddress) {
            this.valid = valid;
            this.tag = tag;
            this.data = data != null ? data.clone() : new byte[0];
            this.blockStartAddress = blockStartAddress;
        }

        public String getDataHex() {
            if (data == null || data.length == 0)
                return "";
            StringBuilder sb = new StringBuilder();
            for (byte b : data) {
                sb.append(String.format("%02X", b));
            }
            return sb.toString();
        }

        public String getDataValue() {
            if (!valid || data == null || data.length == 0)
                return "";
            // Format as hex array: [0x01, 0x02, ...]
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < data.length; i++) {
                if (i > 0)
                    sb.append(", ");
                sb.append(String.format("0x%02X", data[i] & 0xFF));
            }
            sb.append("]");
            return sb.toString();
        }

        public String getBlockAddress() {
            if (!valid || blockStartAddress < 0)
                return "";
            return String.format("0x%X", blockStartAddress);
        }
    }

    /**
     * Returns a detailed snapshot for GUI consumption.
     */
    public Map<Integer, CacheBlockInfo> getDetailedSnapshot() {
        Map<Integer, CacheBlockInfo> snap = new HashMap<>();
        for (int i = 0; i < numBlocks; i++) {
            CacheBlock block = cache.get(i);
            if (block != null) {
                snap.put(i,
                        new CacheBlockInfo(block.valid, block.tag, block.data,
                                block.blockStartAddress));
            } else {
                snap.put(i, new CacheBlockInfo(false, -1, new byte[0], -1));
            }
        }
        return snap;
    }

    // Getters
    public int getCacheSize() {
        return cacheSize;
    }

    public int getBlockSize() {
        return blockSize;
    }

    public int getNumBlocks() {
        return numBlocks;
    }

    public int getHitLatency() {
        return hitLatency;
    }

    public int getMissPenalty() {
        return missPenalty;
    }

    // Setters for reconfiguration
    public void reconfigure(int cacheSize, int blockSize, int hitLatency, int missPenalty) {
        this.cacheSize = cacheSize;
        this.blockSize = blockSize;
        this.numBlocks = cacheSize / blockSize;
        this.hitLatency = hitLatency;
        this.missPenalty = missPenalty;

        this.cache.clear();
        for (int i = 0; i < numBlocks; i++) {
            cache.put(i, new CacheBlock(blockSize));
        }
        accessLog.clear();
    }
}
