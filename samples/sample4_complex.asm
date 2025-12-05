# Complex example with multiple operation types
# Initialize some addresses
DADDI R1, R0, 100     # Base address 1
DADDI R2, R0, 200     # Base address 2

# Load values
L.D F0, 0(R1)
L.D F2, 8(R1)
LW R3, 0(R2)

# FP arithmetic with dependencies
ADD.D F4, F0, F2     # RAW: depends on F0, F2
MUL.D F6, F4, F0     # RAW: depends on F4, F0
SUB.D F8, F6, F2     # RAW: depends on F6, F2
DIV.D F10, F8, F4    # RAW: depends on F8, F4

# Integer operations
DADDI R4, R3, 10
DSUBI R5, R4, 5

# More loads/stores
L.S F12, 16(R1)
S.D F10, 0(R2)
S.D F6, 8(R2)

# Final arithmetic
ADD.D F14, F10, F12
MUL.D F16, F14, F8

# Store results
S.D F16, 16(R2)
SW R5, 24(R2)
