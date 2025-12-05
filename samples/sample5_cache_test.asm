# Cache Behavior Test Program
# This program demonstrates cache hits, misses, and tag checking
# 
# Default cache config: 256 bytes total, 16 bytes/block = 16 blocks (indices 0-15)
# Address mapping: Tag = addr/(blockSize*numBlocks), Index = (addr/blockSize) % numBlocks
#
# With blockSize=16, numBlocks=16:
# - Addresses 0-15 map to index 0, tag 0
# - Addresses 16-31 map to index 1, tag 0
# - Addresses 256-271 map to index 0, tag 1 (same index, different tag - causes replacement)
# - Addresses 512-527 map to index 0, tag 2 (same index, different tag)

# Initialize base addresses in registers
DADDI R1, R0, 0      # R1 = 0 (base address for first block)
DADDI R2, R0, 256    # R2 = 256 (base address for second block, same index, different tag)
DADDI R3, R0, 512    # R3 = 512 (base address for third block, same index, different tag)
DADDI R4, R0, 128    # R4 = 128 (base address for different index)

# Test 1: First access - CACHE MISS (index 0, tag 0)
# Expected: Miss, loads block from memory, sets valid=Yes, tag=0, address=0x0
L.D F0, 0(R1)       # Load from address 0 - MISS

# Test 2: Access same block - CACHE HIT (index 0, tag 0)
# Expected: Hit, tag matches (0==0), uses cached value
L.D F1, 0(R1)       # Load from address 0 again - HIT

# Test 3: Access within same block - CACHE HIT (index 0, tag 0)
# Expected: Hit, same block (addresses 0-7 in same block)
L.D F2, 4(R1)       # Load from address 4 - HIT (same block as address 0)

# Test 4: Access different index - CACHE MISS (index 8, tag 0)
# Expected: Miss, different index, loads new block
L.D F3, 0(R4)       # Load from address 64 - MISS (index 8)

# Test 5: Access same index but different tag - CACHE MISS + REPLACEMENT
# Expected: Miss, index 0 but tag 1 != stored tag 0, replaces old block
L.D F4, 0(R2)       # Load from address 256 - MISS (index 0, tag 1, replaces tag 0)

# Test 6: Try to access original block again - CACHE MISS (was replaced)
# Expected: Miss, block was evicted by previous access
L.D F5, 0(R1)       # Load from address 0 - MISS (block was replaced)

# Test 7: Access third block, same index - CACHE MISS + REPLACEMENT
# Expected: Miss, index 0 but tag 2, replaces previous block
L.D F6, 0(R3)       # Load from address 512 - MISS (index 0, tag 2)

# Test 8: Store operation - tests write behavior
# Expected: Miss if not in cache, then updates value
S.D F0, 0(R4)       # Store to address 128 - MISS (index 8, tag 0)

# Test 9: Load after store to same location - CACHE HIT
# Expected: Hit, just stored to this location
L.D F7, 0(R4)       # Load from address 128 - HIT

# Test 10: Multiple accesses to show hit pattern
L.D F8, 8(R1)       # Load from address 8 - HIT (same block as address 0)
L.D F9, 4(R4)       # Load from address 132 - HIT (same block as 128)

# Summary of expected cache behavior:
# Address 0:   Index=0,  Tag=0  (initially loaded, then replaced, then loaded again)
# Address 128: Index=8,  Tag=0  (loaded by store, then hit by load)
# Address 256: Index=0,  Tag=1  (replaces address 0's block)
# Address 512: Index=0,  Tag=2  (replaces address 256's block)
#
# Watch the Cache Status tab to see:
# - Valid bits changing from No to Yes
# - Tag values being updated
# - Block addresses showing which memory regions are cached
# - Values being loaded from memory
