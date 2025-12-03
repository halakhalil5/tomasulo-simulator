# Simple FP operations example
L.D F0, 0(R1)
L.D F2, 0(R2)
MUL.D F4, F0, F2
ADD.D F6, F0, F2
S.D F4, 0(R3)
S.D F6, 8(R3)
