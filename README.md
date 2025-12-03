# Tomasulo Algorithm Simulator

A comprehensive cycle-by-cycle simulator for the Tomasulo algorithm implemented in Java with JavaFX GUI.

## Features

### Supported Instructions
- **Floating Point Operations**: ADD.D, SUB.D, MUL.D, DIV.D
- **Integer Operations**: ADDI, SUBI (for loops)
- **Load Instructions**: L.D, L.S, LW
- **Store Instructions**: S.D, S.S, SW
- **Branch Instructions**: BEQ, BNE (no branch prediction)

### Core Functionality
- ✅ Cycle-by-cycle execution with step mode
- ✅ Run to completion mode
- ✅ Configurable instruction latencies
- ✅ Configurable reservation station and buffer sizes
- ✅ Register file with dependency tracking
- ✅ Common Data Bus (CDB) with arbitration
- ✅ Cache simulation with configurable parameters
- ✅ Handles RAW, WAR, and WAW hazards
- ✅ Address clash handling in load/store operations
- ✅ Instruction queue visualization

### GUI Features
- **Code Editor**: Enter MIPS assembly code or load from file
- **Reservation Stations View**: Real-time status of Add/Sub, Mul/Div, and Integer stations
- **Load/Store Buffers View**: Status of memory operation buffers
- **Register File View**: Integer and floating-point registers with values and dependencies
- **Instruction Queue View**: Shows issue, execution start/end, and write times
- **Cache Status View**: Cache blocks and access log (hit/miss tracking)
- **Cycle Log**: Detailed log of operations each cycle

## How to Build and Run

### Prerequisites
- Java 11 or higher
- Maven 3.6 or higher

### Building the Project

1. Navigate to the project directory:
```bash
cd "c:\Users\EL MAHDY 01007778867\microproj"
```

2. Build with Maven:
```bash
mvn clean compile
```

3. Run the application:
```bash
mvn javafx:run
```

### Alternative: Package as JAR
```bash
mvn clean package
java -jar target/tomasulo-simulator-1.0-SNAPSHOT.jar
```

## Usage Guide

### 1. Loading a Program
- Enter MIPS assembly code in the Code Editor tab, OR
- Load a sample program from the "Sample Programs" menu, OR
- Load a file using File → Load Program

### 2. Configuration
Before running, configure the simulator:

#### Set Instruction Latencies
- Configuration → Set Latencies
- Configure execution cycles for each instruction type
- Default values:
  - ADD.D/SUB.D: 2 cycles
  - MUL.D: 10 cycles
  - DIV.D: 40 cycles
  - ADDI/SUBI: 1 cycle
  - Load/Store: 2 cycles (base, plus cache latency)

#### Configure Cache
- Configuration → Configure Cache
- Set cache size (bytes), block size (bytes)
- Set hit latency and miss penalty
- Uses direct-mapped cache
- Default: 256 bytes cache, 16 bytes per block, 1 cycle hit, 50 cycles miss

#### Set Station Sizes
- Configuration → Set Station Sizes
- Configure number of:
  - Add/Sub reservation stations (default: 3)
  - Mul/Div reservation stations (default: 2)
  - Integer stations (default: 2)
  - Load buffers (default: 3)
  - Store buffers (default: 3)
  - Instruction queue size (default: 16)

#### Initialize Registers
- Configuration → Initialize Registers
- Set initial values for integer (R0-R31) and FP (F0-F31) registers
- Quick set options: all zeros or sequential values

### 3. Running the Simulation
1. Click "Load Program" to parse and load instructions
2. Use "Step" to execute one cycle at a time
3. Use "Run to Completion" to execute until all instructions complete
4. Use "Reset" to restart the simulation

### 4. Viewing Results
Switch between tabs to view:
- **Reservation Stations**: Shows busy status, operation, operands (Vj, Vk), dependencies (Qj, Qk), remaining cycles
- **Load/Store Buffers**: Shows address, value/Q tag, remaining cycles
- **Register File**: Current values and producing stations (Qi)
- **Instruction Queue**: Timing information (issue, exec start/end, write)
- **Cache Status**: Block contents and access log

## Architecture Documentation

### Addressing Strategy
**Cache Organization** (Direct-Mapped):
- Address bits divided into: Tag | Index | Block Offset
- Block Offset: log₂(block_size) bits (rightmost)
- Index: log₂(num_blocks) bits
- Tag: remaining bits (leftmost)
- Example: 256-byte cache, 16-byte blocks = 16 blocks
  - Address 0x100 → Index: (0x100/16) % 16 = 0, Tag: 0x100/(16*16) = 1

**Memory Addressing**:
- Word-aligned (4 bytes per address)
- Load/Store format: `INSTR Rd, offset(base)` → address = R[base] + offset
- Addresses stored and displayed in hexadecimal

### Bus Arbitration Strategy
When multiple instructions complete in the same cycle:
- **Strategy Used**: Oldest instruction first (by issue order)
- The instruction issued earliest gets priority to write to CDB
- Other instructions wait for the next cycle
- Configurable via `config.busArbitrationStrategy` (currently set to oldest-first)

### Hazard Handling

**Read-After-Write (RAW)**:
- Detected via Qj/Qk tags in reservation stations
- Instructions wait until dependencies are resolved
- CDB broadcast updates all waiting stations

**Write-After-Write (WAW)**:
- Handled by register renaming via reservation station tags
- Register status (Qi) always points to the latest producer
- Earlier writes are ignored when they reach CDB if register status has changed

**Write-After-Read (WAR)**:
- Eliminated by register renaming
- Values are captured in reservation stations at issue time

### Address Clashes
- Load/Store buffers check addresses before committing
- Stores wait until address calculation is complete
- In-order memory access within each buffer type
- No speculative loads past unresolved stores to same address

## Sample Programs

### 1. Simple FP Operations
```assembly
L.D F0, 0(R1)
L.D F2, 0(R2)
MUL.D F4, F0, F2
ADD.D F6, F0, F2
S.D F4, 0(R3)
S.D F6, 8(R3)
```

### 2. Loop Example
```assembly
ADDI R1, R0, 0      # i = 0
ADDI R2, R0, 100    # base address
LOOP:
L.D F0, 0(R2)
MUL.D F4, F0, F0
S.D F4, 0(R2)
ADDI R2, R2, 8
ADDI R1, R1, 1
SUBI R3, R1, 10     # Check if i < 10
BNE R3, R0, LOOP
```

### 3. Hazards Example
```assembly
L.D F0, 0(R1)       # Load F0
ADD.D F2, F0, F4    # RAW on F0
MUL.D F0, F2, F6    # WAW on F0, RAW on F2
SUB.D F8, F0, F2    # RAW on F0 and F2
DIV.D F10, F0, F6   # RAW on F0
S.D F0, 0(R2)       # Store F0
```

## Implementation Details

### File Structure
```
src/main/java/com/tomasulo/
├── TomasuloSimulator.java    # Main GUI application
├── Config.java                # Configuration parameters
├── Instruction.java           # Instruction representation
├── InstructionParser.java     # MIPS assembly parser
├── ExecutionEngine.java       # Main simulation engine
├── ReservationStation.java    # Reservation station logic
├── LoadStoreBuffer.java       # Load/store buffer logic
├── RegisterFile.java          # Register file with status
├── InstructionQueue.java      # Instruction queue management
├── CommonDataBus.java         # CDB with arbitration
├── Cache.java                 # Cache simulation
├── Memory.java                # Memory model
├── ConfigDialog.java          # Configuration dialogs
└── RegisterInitDialog.java    # Register initialization dialog
```

### Key Algorithms

**Issue Stage**:
1. Check if appropriate reservation station/buffer is available
2. Read register values or capture dependencies (Qj, Qk)
3. For loads/stores: calculate effective address
4. Mark destination register with producing station tag

**Execute Stage**:
1. Check if operands are ready (Qj and Qk empty)
2. Decrement remaining cycles
3. When complete, request CDB write

**Write-Back Stage**:
1. CDB arbitration selects winner among competing instructions
2. Broadcast result on CDB
3. Update all reservation stations and buffers waiting for this tag
4. Update register file if it's still waiting for this tag
5. Clear the producing reservation station/buffer

## Limitations and Assumptions

- No branch prediction (branches resolved when operands ready)
- Direct-mapped cache only (not set-associative)
- No cache simulation for instruction fetch (only data)
- Single-threaded execution
- No pipelining within functional units
- Memory is perfect (no bank conflicts, infinite size)
- No floating point precision simulation (uses Java doubles)

## Troubleshooting

**Issue**: Application doesn't start
- Ensure Java 11+ and JavaFX are properly installed
- Try: `mvn clean install` then `mvn javafx:run`

**Issue**: Instructions not issuing
- Check if reservation stations are full
- Check if load/store buffers are full
- Verify register dependencies are being resolved

**Issue**: Cache always missing
- Check cache size vs. block size configuration
- Verify addresses are within reasonable range
- Review cache access log in Cache Status tab

## License
Educational use only. Created for computer architecture coursework.

## Version
1.0 - Initial Release
