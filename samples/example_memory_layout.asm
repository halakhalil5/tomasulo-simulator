# Example showing memory layout after 4-byte vs 8-byte stores
# This helps visualize the difference in memory footprint

# Register initialization:
# R1 = 0
# R2 = 100

# Create test values
DADDI R3, R1, 255   # R3 = 255
L.D F0, 0(R1)       # F0 = 10 (from preset memory)

# ============================================
# Store same value using 4-byte and 8-byte instructions
# ============================================

# Store 255 using SW (4 bytes) at address 100
SW R3, 0(R2)        
# Result: memory[100-103] = [0x00, 0x00, 0x00, 0xFF]
#         memory[104-107] = unchanged

# Store 255 using SD (8 bytes) at address 108
SD R3, 8(R2)
# Result: memory[108-115] = [0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xFF]

# Store 10 using S.S (4 bytes) at address 116
S.S F0, 16(R2)
# Result: memory[116-119] = [0x00, 0x00, 0x00, 0x0A]
#         memory[120-123] = unchanged

# Store 10 using S.D (8 bytes) at address 124
S.D F0, 24(R2)
# Result: memory[124-131] = [0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x0A]

# ============================================
# When you look at the STORE debug output:
# ============================================
# SW will show: "STORE: Address=100, Value=255 (0x00000000000000FF), Bytes[4]: 0x00 0x00 0x00 0xFF"
# SD will show: "STORE: Address=108, Value=255 (0x00000000000000FF), Bytes[8]: 0x00 0x00 0x00 0x00 0x00 0x00 0x00 0xFF"
# S.S will show: "STORE: Address=116, Value=10 (0x000000000000000A), Bytes[4]: 0x00 0x00 0x00 0x0A"
# S.D will show: "STORE: Address=124, Value=10 (0x000000000000000A), Bytes[8]: 0x00 0x00 0x00 0x00 0x00 0x00 0x00 0x0A"
