# Loop example with integer operations
DADDI R1, R0, 0      # i = 0
DADDI R2, R0, 100    # base address = 100
LOOP:
L.D F0, 0(R2)       # Load from array
MUL.D F4, F0, F0    # Square the value
S.D F4, 0(R2)       # Store back
DADDI R2, R2, 8      # Increment address by 8
DADDI R1, R1, 1      # i++
DSUBI R3, R1, 10     # R3 = i - 10
BNE R3, R0, LOOP    # if R3 != 0, continue loop
