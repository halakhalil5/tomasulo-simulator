# Simple example showing 4-byte LW/SW operations
# This demonstrates integer word operations (32-bit)

# Register initialization:
# R1 = 0
# R2 = 50

# Memory has: address 0-7 = 255, address 8-15 = 1000

# Load 4 bytes (LW gets first 4 bytes only)
LW R3, 0(R1)        # R3 = memory[0-3] = 0x00000000 = 0
LW R4, 8(R1)        # R4 = memory[8-11] = 0x00000000 = 0

# Do arithmetic
DADDI R5, R3, 50    # R5 = 0 + 50 = 50
DADDI R6, R4, 100   # R6 = 0 + 100 = 100

# Store 4 bytes (SW writes only 4 bytes)
SW R5, 0(R2)        # memory[50-53] = 0x00000032 (stores 50 in 4 bytes)
SW R6, 4(R2)        # memory[54-57] = 0x00000064 (stores 100 in 4 bytes)

# Load back to verify (4 bytes each)
LW R7, 0(R2)        # R7 = 50
LW R8, 4(R2)        # R8 = 100
