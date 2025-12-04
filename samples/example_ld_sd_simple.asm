# Simple example showing 8-byte L.D/S.D operations
# This demonstrates double-precision floating-point operations (64-bit)

# Register initialization:
# R1 = 0
# R2 = 50

# Memory has: address 0-7 = 10, address 8-15 = 20

# Load 8 bytes (L.D gets all 8 bytes)
L.D F0, 0(R1)       # F0 = memory[0-7] = 10
L.D F2, 8(R1)       # F2 = memory[8-15] = 20

# Do arithmetic
ADD.D F4, F0, F2    # F4 = 10 + 20 = 30
MUL.D F6, F0, F2    # F6 = 10 * 20 = 200

# Store 8 bytes (S.D writes all 8 bytes)
S.D F4, 0(R2)       # memory[50-57] = 0x000000000000001E (stores 30 in 8 bytes)
S.D F6, 8(R2)       # memory[58-65] = 0x00000000000000C8 (stores 200 in 8 bytes)

# Load back to verify (8 bytes each)
L.D F8, 0(R2)       # F8 = 30
L.D F10, 8(R2)      # F10 = 200
