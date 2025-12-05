# Simple Cache Test - Easy to Follow
# Watch the Cache Status tab to see cache behavior!
#
# Setup: Initialize memory with known values first using:
# Configuration → Initialize Memory
# Enter these values:
#   0: 10.0
#   8: 20.0
#   64: 30.0
#   128: 40.0
#   256: 50.0

# Step 1: Load from address 0
# Expected: CACHE MISS, Index=0, Tag=0, Block Addr=0x0, Value=10
DADDI R1, R0, 0
L.D F0, 0(R1)       # Address 0 - First access = MISS

# Step 2: Load from address 0 again
# Expected: CACHE HIT, same index (0) and tag (0), reads from cache
L.D F1, 0(R1)       # Address 0 - Second access = HIT!

# Step 3: Load from address 8
# Expected: CACHE MISS, Index=1, Tag=0, Block Addr=0x8, Value=20
DADDI R2, R0, 8
L.D F2, 0(R2)       # Address 8 - New block = MISS

# Step 4: Load from address 128
# Expected: CACHE MISS, Index=0, Tag=1, Block Addr=0x80, Value=40
# NOTE: This REPLACES the block at index 0 (address 0) because same index, different tag!
DADDI R3, R0, 128
L.D F3, 0(R3)       # Address 128 - Same index as addr 0, but different tag = MISS

# Step 5: Try loading from address 0 again
# Expected: CACHE MISS! The block was evicted by address 128
L.D F4, 0(R1)       # Address 0 - Block was replaced = MISS again!

# Step 6: Store to address 64
# Expected: CACHE MISS, Index=8, Tag=0, Block Addr=0x40
DADDI R4, R0, 64
S.D F0, 0(R4)       # Address 64 - Store causes MISS, loads block then stores

# Step 7: Load from address 64
# Expected: CACHE HIT, block is now in cache from previous store
L.D F5, 0(R4)       # Address 64 - Block in cache = HIT!

# INSTRUCTIONS FOR TESTING:
# 1. Set up memory values using Configuration → Initialize Memory (see values above)
# 2. Load this program
# 3. Go to Cache Status tab
# 4. Click "Step" button repeatedly and watch:
#    - Valid column: changes from "No" to "Yes" when blocks are loaded
#    - Tag column: shows the tag value for address mapping
#    - Block Addr column: shows which memory block is cached (0x0, 0x8, 0x40, 0x80, etc.)
#    - Value column: shows the actual data value from memory
# 5. Watch the Cycle Log at bottom to see "HIT" vs "MISS" messages
# 6. Notice how address 128 replaces address 0 in cache (same index, different tag)
