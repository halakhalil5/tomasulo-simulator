# Example demonstrating RAW, WAR, and WAW hazards
L.D F0, 0(R1)        # Load F0
ADD.D F2, F0, F4     # RAW hazard on F0 (must wait for L.D to complete)
MUL.D F0, F2, F6     # WAW hazard on F0 (overwrites F0), RAW on F2
SUB.D F8, F0, F2     # RAW hazards on both F0 and F2
DIV.D F10, F0, F6    # RAW hazard on F0 (from MUL.D)
S.D F0, 0(R2)        # Store F0 (final value from MUL.D)
