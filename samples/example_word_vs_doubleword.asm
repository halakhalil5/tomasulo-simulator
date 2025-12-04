# Example demonstrating 4-byte vs 8-byte load/store operations
# Shows the difference between Word (4 bytes) and Doubleword (8 bytes)

# Register initialization needed:
# R1 = 0   (base address)
# R2 = 100 (base address for stores)

# Memory preset:
# Address 0-7: value 10 (8 bytes: 0x00 00 00 00 00 00 00 0A)
# Address 8-15: value 20 (8 bytes: 0x00 00 00 00 00 00 00 14)

# ============================================
# PART 1: Load Operations - 4 bytes vs 8 bytes
# ============================================

# LW loads 4 bytes (Word) - combines 4 bytes into value
LW R3, 0(R1)        # R3 = memory[0-3] = 0x00000000 = 0

# LD loads 8 bytes (Doubleword) - combines all 8 bytes into value
LD F0, 0(R1)        # F0 = memory[0-7] = 0x000000000000000A = 10

# L.S loads 4 bytes (Single precision)
L.S F2, 8(R1)       # F2 = memory[8-11] = 0x00000000 = 0

# L.D loads 8 bytes (Double precision)
L.D F4, 8(R1)       # F4 = memory[8-15] = 0x0000000000000014 = 20

# ============================================
# PART 2: Store Operations - 4 bytes vs 8 bytes
# ============================================

# Store value 100 using different instructions
DADDI R10, R1, 100  # R10 = 100

# SW stores 4 bytes (Word)
SW R10, 0(R2)       # memory[100-103] = 0x00000064 (4 bytes only)

# SD stores 8 bytes (Doubleword)  
SD F4, 8(R2)        # memory[108-115] = 0x0000000000000014 (8 bytes)

# S.S stores 4 bytes (Single precision)
S.S F4, 16(R2)      # memory[116-119] = 0x00000014 (4 bytes only)

# S.D stores 8 bytes (Double precision)
S.D F4, 24(R2)      # memory[124-131] = 0x0000000000000014 (8 bytes)

# ============================================
# PART 3: Verify stores by loading back
# ============================================

LW R11, 0(R2)       # R11 = memory[100-103] (4 bytes from SW)
LD F10, 8(R2)       # F10 = memory[108-115] (8 bytes from SD)
L.S F12, 16(R2)     # F12 = memory[116-119] (4 bytes from S.S)
L.D F14, 24(R2)     # F14 = memory[124-131] (8 bytes from S.D)
