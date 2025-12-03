package com.tomasulo;

import java.util.*;

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

    private static class CacheBlock {
        boolean valid;
        int tag;
        byte[] data;

        CacheBlock(int blockSize) {
            this.valid = false;
            this.tag = -1;
            this.data = new byte[blockSize];
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

        // Initialize cache blocks
        for (int i = 0; i < numBlocks; i++) {
            cache.put(i, new CacheBlock(blockSize));
        }
    }

    /**
     * Access cache for load operation
     * Returns latency (hitLatency for hit, hitLatency + missPenalty for miss)
     */
    public int accessLoad(int address) {
        int blockOffset = address % blockSize;
        int index = (address / blockSize) % numBlocks;
        int tag = address / (blockSize * numBlocks);

        CacheBlock block = cache.get(index);

        if (block.valid && block.tag == tag) {
            // Cache hit
            accessLog.add(String.format("Cycle: Load HIT - Addr: 0x%X (Tag: %d, Index: %d, Offset: %d)",
                    address, tag, index, blockOffset));
            return hitLatency;
        } else {
            // Cache miss
            accessLog.add(String.format("Cycle: Load MISS - Addr: 0x%X (Tag: %d, Index: %d, Offset: %d)",
                    address, tag, index, blockOffset));

            // Load block from memory (simulated)
            block.valid = true;
            block.tag = tag;

            return hitLatency + missPenalty;
        }
    }

    /**
     * Access cache for store operation
     * Returns latency (hitLatency for hit, hitLatency + missPenalty for miss)
     */
    public int accessStore(int address) {
        int blockOffset = address % blockSize;
        int index = (address / blockSize) % numBlocks;
        int tag = address / (blockSize * numBlocks);

        CacheBlock block = cache.get(index);

        if (block.valid && block.tag == tag) {
            // Cache hit
            accessLog.add(String.format("Cycle: Store HIT - Addr: 0x%X (Tag: %d, Index: %d, Offset: %d)",
                    address, tag, index, blockOffset));
            return hitLatency;
        } else {
            // Cache miss
            accessLog.add(String.format("Cycle: Store MISS - Addr: 0x%X (Tag: %d, Index: %d, Offset: %d)",
                    address, tag, index, blockOffset));

            // Load block from memory (simulated)
            block.valid = true;
            block.tag = tag;

            return hitLatency + missPenalty;
        }
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
