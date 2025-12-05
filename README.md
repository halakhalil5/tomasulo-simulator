# Tomasulo Algorithm Simulator

A comprehensive cycle-by-cycle simulator implementing the Tomasulo algorithm with dynamic instruction scheduling, register renaming, and out-of-order execution. Built with Java 11 and JavaFX for an interactive GUI experience.

## 📋 Table of Contents
- [Features](#features)
- [System Requirements](#system-requirements)
- [Quick Start](#quick-start)
- [Detailed Usage Guide](#detailed-usage-guide)
- [Architecture & Implementation](#architecture--implementation)
- [Sample Programs](#sample-programs)
- [Configuration Reference](#configuration-reference)
- [Troubleshooting](#troubleshooting)

## ✨ Features

### Supported Instructions
#### Floating Point Operations
- `ADD.D Fd, Fs, Ft` - Double-precision addition
- `SUB.D Fd, Fs, Ft` - Double-precision subtraction
- `MUL.D Fd, Fs, Ft` - Double-precision multiplication
- `DIV.D Fd, Fs, Ft` - Double-precision division
- `ADD.S`, `SUB.S`, `MUL.S`, `DIV.S` - Single-precision variants

#### Integer Operations
- `DADDI Rd, Rs, immediate` - Double-word add immediate
- `DSUBI Rd, Rs, immediate` - Double-word subtract immediate

#### Memory Operations
- `L.D Fd, offset(Rs)` - Load double-word to FP register
- `LD Rd, offset(Rs)` - Load double-word to integer register
- `L.S Fd, offset(Rs)` - Load single-word to FP register
- `LW Rd, offset(Rs)` - Load word to integer register
- `S.D Fd, offset(Rs)` - Store double-word from FP register
- `SD Rd, offset(Rs)` - Store double-word from integer register
- `S.S Fd, offset(Rs)` - Store single-word from FP register
- `SW Rd, offset(Rs)` - Store word from integer register

#### Control Flow
- `BEQ Rs, Rt, label` - Branch if equal (to label or PC address)
- `BNE Rs, Rt, label` - Branch if not equal (to label or PC address)
- **No branch prediction**: Branches stall all subsequent issuing until execution completes and direction is known

### Core Simulation Features
- ✅ **Cycle-accurate execution** - Step through one cycle at a time or run to completion
- ✅ **Dynamic instruction scheduling** - Out-of-order execution with in-order commit
- ✅ **Register renaming** - Automatic via reservation station tags (eliminates WAR/WAW)
- ✅ **Hazard detection & handling**:
  - RAW (Read-After-Write) - Operands wait in reservation stations
  - WAR (Write-After-Read) - Eliminated by register renaming
  - WAW (Write-After-Write) - Latest writer wins via Qi updates
- ✅ **Common Data Bus (CDB)** - Broadcast results with configurable arbitration:
  - First-come-first-serve (by issue order)
  - Oldest instruction first
  - Random selection
- ✅ **PC-driven instruction queue** - Supports loops with iteration tracking
- ✅ **Branch handling** - Numeric PC addresses or labels supported
- ✅ **Cache simulation** - Direct-mapped with configurable size and block size
- ✅ **Memory preload** - Automatic alignment to cache block boundaries
- ✅ **Dynamic register sizing** - Register size equals cache block size

### Interactive GUI Features
#### 6 Real-Time Visualization Tabs
1. **📝 Code Editor** - Write/edit MIPS assembly with syntax support
2. **⚙️ Reservation Stations** - Monitor Add/Sub, Mul/Div, Integer, and Branch stations
3. **💾 Load/Store Buffers** - Track memory operation status
4. **📊 Register File** - View integer (R0-R31) and FP (F0-F31) registers with dependencies
5. **📋 Instruction Queue** - Complete execution timeline (issue → execute → write-back)
6. **🗄️ Cache Status** - Block contents and hit/miss access log

#### Control Panel
- **Step** - Execute one cycle
- **Run to Completion** - Execute until program finishes
- **Reset** - Clear simulation state
- **Load Program** - Parse and load instructions

#### Configuration Dialogs
- **Set Latencies** - Configure execution cycles per instruction type
- **Set Station Sizes** - Adjust reservation stations and buffer counts
- **Configure Cache** - Set cache size, block size, hit latency, miss penalty
- **Initialize Registers** - Preload register values before execution
- **Set Register File Size** - Dynamically calculated based on block size

## 🖥️ System Requirements

### Prerequisites
- **Java**: JDK 11 or higher
- **Maven**: 3.6 or higher  
- **JavaFX**: 17.0.2 (included in dependencies)
- **OS**: Windows, macOS, or Linux

### Verify Installation
```bash
java -version    # Should show Java 11+
mvn -version     # Should show Maven 3.6+
```

## 🚀 Quick Start

### 1. Clone or Download the Project
```bash
cd tomasulo-simulator-1
```

### 2. Build the Project
```bash
mvn clean compile
```

### 3. Run the Simulator
```bash
mvn javafx:run
```

### Alternative: Use the Batch Script (Windows)
```bash
run-gui.cmd
```

### Alternative: Package as Standalone JAR
```bash
mvn clean package
java -jar target/tomasulo-simulator-1.0-SNAPSHOT.jar
```

## 📖 Detailed Usage Guide

### Step 1: Load a Program

#### Option A: Write Code Directly
1. Go to the **Code Editor** tab
2. Enter MIPS assembly code (one instruction per line)
3. Use labels for branches (e.g., `LOOP:`)

#### Option B: Load Sample Programs
- Menu: **File** → **Sample Programs**
- Available samples:
  - `sample1.asm` - Basic FP operations
  - `sample2_loop.asm` - Loop with branches
  - `sample3_hazards.asm` - RAW/WAW hazards demonstration
  - `sample4_complex.asm` - Complex instruction mix
  - `sample5_cache_test.asm` - Cache behavior testing
  - `sample6_cache_simple.asm` - Simple cache operations

#### Option C: Load from File
- Menu: **File** → **Load Program**
- Select an `.asm` file from your system

### Step 2: Configure the Simulator

#### 2.1 Set Instruction Latencies
**Menu**: Configuration → Set Latencies

| Instruction Type | Default Latency | Description |
|-----------------|----------------|-------------|
| ADD.D / SUB.D / ADD.S / SUB.S | 2 cycles | FP addition/subtraction |
| MUL.D / MUL.S | 3 cycles | FP multiplication |
| DIV.D / DIV.S | 4 cycles | FP division |
| DADDI / DSUBI | 1 cycle | Integer operations |
| L.D / LD / L.S / LW | 2 cycles | Load operations (+ cache latency) |
| S.D / SD / S.S / SW | 2 cycles | Store operations (+ cache latency) |
| BEQ / BNE | 1 cycle | Branch evaluation |

#### 2.2 Configure Cache Parameters
**Menu**: Configuration → Configure Cache

| Parameter | Default | Description |
|-----------|---------|-------------|
| Cache Size | 256 bytes | Total cache capacity |
| Block Size | 4 bytes | Bytes per cache block (= register size) |
| Hit Latency | 1 cycle | Cycles added on cache hit |
| Miss Penalty | 3 cycles | Additional cycles on cache miss |

**Important**: 
- Register size = Block size (dynamically calculated)
- Total register file size = (numIntRegs + numFloatRegs) × blockSize
- Number of cache blocks = cacheSize ÷ blockSize

#### 2.3 Set Reservation Station Sizes
**Menu**: Configuration → Set Station Sizes

| Component | Default | Purpose |
|-----------|---------|---------|
| Add/Sub Stations | 3 | FP addition/subtraction |
| Mul/Div Stations | 2 | FP multiplication/division |
| Integer Stations | 2 | Integer operations |
| Branch Stations | 1 | Branch instructions |
| Load Buffers | 3 | Load operations |
| Store Buffers | 3 | Store operations |
| Instruction Queue Size | 16 | Maximum queued instructions |

#### 2.4 Initialize Registers (Optional)
**Menu**: Configuration → Initialize Registers
- Set initial values for R0-R31 (integer) and F0-F31 (floating-point)
- Quick options: "Set All to 0" or "Set Sequential Values"
- Useful for testing programs with specific initial state

### Step 3: Execute the Program

#### Load the Program
1. Click **"Load Program"** button
2. Parser validates syntax and creates instruction queue
3. Check console for any parsing errors

#### Execution Modes

##### Step-by-Step Mode (Recommended for Learning)
1. Click **"Step"** button to advance one cycle
2. Observe changes in:
   - Reservation stations filling/clearing
   - Register dependencies (Qj, Qk, Qi)
   - CDB broadcasts
   - Cache hits/misses
3. Review cycle log at bottom of window

##### Run to Completion Mode
1. Click **"Run to Completion"** button
2. Simulator executes until:
   - All instructions complete write-back, OR
   - No more instructions to issue and all units idle
3. Final state displayed in all tabs

#### Reset and Rerun
- Click **"Reset"** to clear all state
- Reconfigure if needed
- Load and execute again

### Step 4: Analyze Results

#### Reservation Stations Tab
- **Busy**: Station occupied?
- **Op**: Operation being performed
- **Vj, Vk**: Operand values (if ready)
- **Qj, Qk**: Producing station tags (if waiting)
- **A**: Address (for load/store)
- **Remaining**: Cycles left to complete

#### Load/Store Buffers Tab
- **Busy**: Buffer occupied?
- **Address**: Memory address (hex)
- **Value/Q**: Data value or producing station
- **Remaining**: Cycles until completion

#### Register File Tab
- **Register**: R0-R31, F0-F31
- **Value**: Current value
- **Qi**: Producing station (empty if ready)
- Color coding: Dependencies highlighted

#### Instruction Queue Tab
- **Iteration**: Loop iteration number (for repeated instructions)
- **Instruction**: Assembly code
- **Issue**: Cycle when issued
- **Exec Start**: Cycle when execution began
- **Exec End**: Cycle when execution completed
- **Write**: Cycle when written to CDB
- Iterations grouped with separators

#### Cache Status Tab
- **Index**: Cache block number
- **Valid**: Block contains data?
- **Tag**: Address tag
- **Block Start**: Starting memory address
- **Access Log**: Hit/Miss history with addresses

#### Cycle Log (Bottom Panel)
- Shows detailed operation of each cycle:
  - Issued instructions
  - Execution completions
  - CDB writes
  - Branch evaluations
  - Cache accesses
  - Stall conditions

## 🏗️ Architecture & Implementation

### Tomasulo Algorithm Overview
The simulator implements the classic Tomasulo algorithm with these key features:
- **Register Renaming**: Reservation station tags eliminate false dependencies
- **Out-of-Order Execution**: Instructions execute when operands ready (not program order)
- **In-Order Issue**: Instructions issue sequentially from the program
- **Common Data Bus**: Broadcasts results to all waiting units simultaneously

### 4-Stage Pipeline

#### 1. **Issue Stage**
```
For each cycle:
  - Check instruction queue for next instruction
  - Branch stall check: If branch in flight, stall non-branch issuing
  - Check for free reservation station/buffer
  - Read operands or capture dependencies (Qj, Qk)
  - For loads/stores: Calculate effective address
  - Mark destination register with producing tag (Qi)
  - Increment PC (or jump if branch taken)
```

#### 2. **Execute Stage**
```
For each reservation station/buffer:
  - Check if ready: Qj == "" AND Qk == ""
  - Decrement remaining cycles
  - When complete: Request CDB write
  - Special handling:
    - Loads: Access cache on first execution cycle
    - Stores: Access cache when data and address ready
    - Branches: Evaluate condition, determine taken/not-taken
```

#### 3. **Write-Back Stage**
```
CDB Arbitration:
  - Select winner from pending writes (by strategy)
  - Broadcast <tag, value> on CDB
```

#### 4. **Broadcast Stage**
```
For each unit (RS, buffer, register):
  - Check if waiting for broadcast tag
  - Update Qj/Qk → Vj/Vk
  - Clear Qi if matches
  - Handle conditional execution delay
```

### Memory & Cache Architecture

#### Memory Model
- **Size**: 1024 bytes (configurable)
- **Organization**: Byte-addressable
- **Endianness**: Big-endian (MSB at lower address)
- **Data sizes**:
  - Double-word (8 bytes): L.D, S.D, LD, SD
  - Word (4 bytes): L.S, S.S, LW, SW
- **Preload**: Automatic with sequential values aligned to block boundaries

#### Cache Organization (Direct-Mapped)
```
Address Structure: | Tag | Index | Block Offset |

- Block Offset: log₂(blockSize) bits (rightmost)
- Index: log₂(numBlocks) bits  
- Tag: Remaining bits (leftmost)

Example (256-byte cache, 4-byte blocks):
  - Blocks: 256 ÷ 4 = 64 blocks
  - Address 0x20 → Offset: 0, Index: 8, Tag: 0
  - Address 0x24 → Offset: 0, Index: 9, Tag: 0
```

**Cache Access**:
1. Calculate index, tag, offset from address
2. Check if `cache[index].valid AND cache[index].tag == tag`
3. **Hit**: Return data with `hitLatency` cycles
4. **Miss**: Load entire block from memory, return data with `hitLatency + missPenalty` cycles

**Important**: Each cache block holds exactly one register value (blockSize = register size)

### Register File Architecture

**Dynamic Sizing**:
```
Register Size (bytes) = Cache Block Size
Integer RF Size = numIntegerRegisters × blockSize
Float RF Size = numFloatRegisters × blockSize
Total RF Size = Integer RF Size + Float RF Size

Example (blockSize = 4, 32 regs each):
  - Each register: 4 bytes
  - Integer RF: 32 × 4 = 128 bytes
  - Float RF: 32 × 4 = 128 bytes  
  - Total: 256 bytes
```

**Register Status** (Qi):
- Empty ("") → Register value ready
- Tag ("Add1", "Mul2", etc.) → Waiting for that station to produce value

### Hazard Resolution

#### Read-After-Write (RAW)
```
ADD.D F2, F0, F4    # F2 = Add1
MUL.D F6, F2, F8    # Qj = Add1, waits for F2

Resolution:
  - MUL issues with Qj="Add1", Qk="" (or tag)
  - When ADD broadcasts, MUL receives F2 value
  - MUL becomes ready and executes
```

#### Write-After-Write (WAW)
```
ADD.D F2, F0, F4    # F2 = Add1
MUL.D F2, F6, F8    # F2 = Mul1 (overwrites Qi)

Resolution:
  - Register Qi updated to latest producer (Mul1)
  - When ADD tries to write, checks if Qi still points to it
  - If not, write is suppressed (register already renamed)
```

#### Write-After-Read (WAR)
```
ADD.D F2, F0, F4    
SUB.D F0, F6, F8    # Could overwrite F0 before ADD reads it

Resolution:
  - ADD captures F0 value at issue time (Vj)
  - SUB can write F0 anytime without affecting ADD
  - Register renaming eliminates WAR entirely
```

### Branch Handling

**Branch Execution**:
1. Branch issues immediately when at front of queue
2. Sets `branchInFlight = true` flag
3. **All subsequent issuing stalled** until branch completes
4. Branch executes: compares operands, determines taken/not-taken
5. Branch writes back: Updates PC if taken, clears `branchInFlight`
6. Normal issuing resumes

**Branch Targets**:
- Labels: `BEQ R1, R2, LOOP` - jumps to label address
- Numeric PC: `BNE R1, R2, 36` - jumps to PC address 36

**No Speculation**: Instructions after branch do NOT issue until branch result known

### CDB Arbitration Strategies

When multiple instructions complete in same cycle:

| Strategy | ID | Description |
|----------|-----|-------------|
| First-Come-First-Serve | 0 | Earliest issued instruction wins |
| Oldest First | 1 | Same as FCFS (by issue order) |
| Random | 2 | Random selection among ready |

**Current Default**: Oldest instruction first (ID = 1)

## 📝 Sample Programs

### 1. Basic Floating Point Operations (`sample1.asm`)
**Purpose**: Demonstrates simple FP arithmetic and load/store

```assembly
L.D F0, 0(R1)       # Load from memory[R1+0]
L.D F2, 8(R1)       # Load from memory[R1+8]
ADD.D F4, F0, F2    # F4 = F0 + F2
MUL.D F6, F0, F2    # F6 = F0 × F2
SUB.D F8, F4, F6    # F8 = F4 - F6
S.D F8, 16(R1)      # Store to memory[R1+16]
```

**Expected Behavior**:
- Loads execute with cache access
- ADD and MUL can execute in parallel (different stations)
- SUB waits for both ADD and MUL (RAW dependencies)

### 2. Loop with Branches (`sample2_loop.asm`)
**Purpose**: Shows branch handling and iteration tracking

```assembly
DADDI R1, R1, 24    # Initialize base address
DADDI R2, R2, 0     # Initialize counter
LOOP: L.D F0, 8(R1)
MUL.D F4, F0, F2
DSUBI R1, R1, 8     # Decrement address
BNE R1, R2, 36      # Branch to PC 36 if R1 ≠ R2
```

**Expected Behavior**:
- Instructions in loop body issued multiple times
- Each iteration tagged separately in instruction queue
- Branch stalls all issuing until evaluation complete
- PC jumps back to continue loop

### 3. Data Hazards Demonstration (`sample3_hazards.asm`)
**Purpose**: Illustrates RAW, WAR, WAW hazard handling

```assembly
L.D F0, 0(R1)        # Load F0
ADD.D F2, F0, F4     # RAW: Waits for F0
MUL.D F0, F2, F6     # WAW on F0, RAW on F2
SUB.D F8, F0, F2     # RAW: Waits for new F0 from MUL
DIV.D F10, F0, F6    # RAW: Waits for F0
S.D F0, 0(R2)        # Stores latest F0 value
```

**Expected Behavior**:
- ADD waits for L.D (RAW on F0)
- MUL waits for ADD (RAW on F2), renames F0
- SUB waits for MUL's F0, not L.D's F0 (register renaming)
- DIV also waits for MUL's F0
- Store uses MUL's result

### 4. Cache Behavior Test (`sample5_cache_test.asm`)
**Purpose**: Tests cache hits/misses with different access patterns

```assembly
# Sequential accesses (likely hits)
L.D F0, 0(R1)
L.D F2, 8(R1)
L.D F4, 16(R1)

# Strided accesses (may cause misses)
L.D F6, 256(R1)
L.D F8, 512(R1)

# Stores to test write-back
S.D F0, 0(R2)
S.D F2, 8(R2)
```

**Expected Behavior**:
- First access to each block: MISS
- Subsequent accesses to same block: HIT
- Cache replacement when blocks full
- View cache access log for hit/miss pattern

### 5. Complex Instruction Mix (`sample4_complex.asm`)
**Purpose**: Combines all instruction types

```assembly
DADDI R1, R1, 24      # Integer operation
L.D F0, 8(R1)         # Load
MUL.D F4, F0, F2      # FP multiply
ADD.D F6, F4, F0      # FP add (RAW on F4)
DSUBI R1, R1, 8       # Integer subtract
S.D F4, 8(R1)         # Store
BNE R1, R2, 36        # Branch
```

**Expected Behavior**:
- Integer and FP operations can execute in parallel
- Branch stalls subsequent issuing
- Demonstrates full pipeline utilization

### Loading Sample Programs
1. Menu: **File** → **Sample Programs**
2. Select desired sample
3. Code appears in editor
4. Click **"Load Program"** to execute

## ⚙️ Configuration Reference

### Default Configuration Values

```java
// Reservation Station Sizes
Add/Sub Stations:    3
Mul/Div Stations:    2  
Integer Stations:    2
Branch Stations:     1
Load Buffers:        3
Store Buffers:       3

// Instruction Latencies (cycles)
ADD.D, SUB.D:        2
MUL.D:               3
DIV.D:               4
DADDI, DSUBI:        1
Load (base):         2
Store (base):        2
Branch:              1

// Cache Configuration
Cache Size:          256 bytes
Block Size:          4 bytes
Hit Latency:         1 cycle
Miss Penalty:        3 cycles
Number of Blocks:    64 (calculated)

// Register File
Integer Registers:   32 (R0-R31)
Float Registers:     32 (F0-F31)
Register Size:       4 bytes (= blockSize)
Total RF Size:       256 bytes

// Memory
Memory Size:         1024 bytes
Addressing:          Byte-addressable
Preload Values:      10, 20, 30, 40, ... (per block)

// CDB
Arbitration:         Oldest instruction first (ID=1)
```

### Modifying Configuration

All configurations can be changed via GUI menus:
- **Configuration** → **Set Latencies**
- **Configuration** → **Set Station Sizes**
- **Configuration** → **Configure Cache**
- **Configuration** → **Initialize Registers**

Changes take effect on next **Reset** + **Load Program**.

## 📂 Project Structure

```
tomasulo-simulator-1/
│
├── src/main/java/com/tomasulo/          # Source code
│   ├── TomasuloSimulator.java           # ⭐ Main GUI (JavaFX)
│   ├── ExecutionEngine.java             # ⭐ Core simulation engine
│   ├── Config.java                      # Configuration manager
│   ├── Instruction.java                 # Instruction data structure
│   ├── InstructionParser.java           # MIPS assembly parser
│   ├── InstructionQueue.java            # PC-driven queue with iterations
│   ├── ReservationStation.java          # Reservation station logic
│   ├── LoadStoreBuffer.java             # Memory buffer logic
│   ├── RegisterFile.java                # Register file + Qi tracking
│   ├── CommonDataBus.java               # CDB with arbitration
│   ├── Cache.java                       # Direct-mapped cache
│   ├── Memory.java                      # Byte-addressable memory
│   ├── ConfigDialog.java                # Configuration UI dialogs
│   └── RegisterInitDialog.java          # Register init UI
│
├── samples/                              # Sample assembly programs
│   ├── sample1.asm                      # Basic FP operations
│   ├── sample2_loop.asm                 # Loop with branches
│   ├── sample3_hazards.asm              # Hazard demonstrations
│   ├── sample4_complex.asm              # Complex instruction mix
│   ├── sample5_cache_test.asm           # Cache testing
│   └── sample6_cache_simple.asm         # Simple cache ops
│
├── pom.xml                              # Maven build configuration
├── run-gui.cmd                          # Windows run script
├── mvnw-simple.cmd                      # Maven wrapper (Windows)
├── README.md                            # This file
├── ARCHITECTURE.md                      # Detailed architecture docs
├── QUICKSTART.md                        # Quick start guide
└── GUI_GUIDE.md                         # GUI usage guide
```

## 🧩 Key Components

### Backend (Simulation Engine)

| Component | Responsibility |
|-----------|----------------|
| `ExecutionEngine` | Main simulation loop, 4-stage pipeline coordination |
| `InstructionQueue` | PC-driven instruction fetching, iteration tracking |
| `ReservationStation` | Execute FP/Integer operations, track dependencies |
| `LoadStoreBuffer` | Handle memory operations, address calculation |
| `RegisterFile` | Store register values, track producers (Qi) |
| `CommonDataBus` | Broadcast results, arbitration between writers |
| `Cache` | Direct-mapped cache simulation, hit/miss tracking |
| `Memory` | Byte-addressable memory, preload with test data |
| `InstructionParser` | Parse MIPS assembly, handle labels |
| `Config` | Centralized configuration, dynamic calculations |

### Frontend (JavaFX GUI)

| Component | Responsibility |
|-----------|----------------|
| `TomasuloSimulator` | Main window, tab management, menu bar |
| `ConfigDialog` | Latency/station/cache configuration dialogs |
| `RegisterInitDialog` | Register initialization interface |
| TableViews | Real-time display of RS, buffers, registers, instructions |
| Code Editor | TextArea for writing/editing assembly code |
| Cycle Log | ScrollPane showing detailed execution log |

## 🚨 Troubleshooting

### Build Issues

#### Maven build fails
```bash
# Clean and rebuild
mvn clean install

# If dependencies missing
mvn dependency:resolve

# Check Java version
java -version  # Must be 11+
```

#### JavaFX not found
```bash
# Ensure JavaFX dependency in pom.xml
# Check Maven uses correct Java version:
mvn -version
```

### Runtime Issues

#### Application doesn't start
**Symptoms**: Window doesn't appear, crashes immediately

**Solutions**:
1. Verify Java 11+ installed: `java -version`
2. Try: `mvn clean compile javafx:run`
3. Check console for error messages
4. On Windows, try: `run-gui.cmd`

#### Instructions not issuing
**Symptoms**: "Step" button doesn't advance, instructions stuck

**Possible Causes**:
- ✅ **Reservation stations full**: Check RS tab, increase station count in config
- ✅ **Load/Store buffers full**: Check L/S buffers tab, increase buffer count
- ✅ **Branch in flight**: Branch must complete before other instructions issue
- ✅ **Waiting for operands**: Check Qj/Qk tags in RS tab
- ✅ **Same instruction pending**: An earlier instance still writing back

**Solutions**:
1. Check Reservation Stations tab for availability
2. Check instruction dependencies (Qj, Qk not empty)
3. Look for branch stall message in cycle log
4. Reset and reconfigure with more stations/buffers

#### Execution stuck/infinite loop
**Symptoms**: "Run to Completion" never finishes

**Possible Causes**:
- ✅ **Infinite loop in program**: Branch always taken
- ✅ **Deadlock**: Circular dependency (rare)
- ✅ **Very long latencies**: DIV with 40 cycles takes time

**Solutions**:
1. Use "Step" mode to debug
2. Check branch condition and target
3. Review cycle log for repeated patterns
4. Reduce latencies in configuration

#### Cache always missing
**Symptoms**: Every load/store shows MISS in cache log

**Possible Causes**:
- ✅ **Cache too small**: Not enough blocks for working set
- ✅ **Block size mismatch**: Addresses not aligned
- ✅ **Direct-mapped conflicts**: Different addresses map to same index

**Solutions**:
1. Increase cache size: Configuration → Configure Cache
2. Check memory addresses in program (should align to blocks)
3. View Cache Status tab to see block usage
4. Review access log for conflict patterns

#### Register values wrong
**Symptoms**: Register file shows unexpected values

**Possible Causes**:
- ✅ **Initial values not set**: Registers default to 0
- ✅ **Instruction not completed**: Check Qi tag
- ✅ **Wrong operand registers**: Syntax error in code

**Solutions**:
1. Initialize registers: Configuration → Initialize Registers
2. Check instruction queue for write completion times
3. Verify assembly syntax in code editor
4. Check CDB broadcasts in cycle log

#### GUI freezes
**Symptoms**: UI unresponsive, can't click buttons

**Possible Causes**:
- ✅ **Long computation**: "Run to Completion" with many instructions
- ✅ **JavaFX thread blocked**: Issue with update loop

**Solutions**:
1. Wait for computation to finish (check console progress)
2. Close and restart application
3. Use "Step" mode instead of "Run to Completion"
4. Reduce program size for testing

### Parsing Errors

#### "Unknown instruction" error
**Cause**: Instruction not in supported set

**Solution**: Use only supported instructions (see Features section)

#### "Label not found" error  
**Cause**: Branch references undefined label

**Solution**: 
```assembly
# Define label before referencing
LOOP: L.D F0, 0(R1)
BNE R1, R2, LOOP    # ✅ Correct

BNE R1, R2, LOOP    # ❌ Error: LOOP not defined yet
LOOP: L.D F0, 0(R1)
```

#### "Invalid register" error
**Cause**: Register name typo or out of range

**Solution**: Use R0-R31 for integer, F0-F31 for float

### Performance Tips

1. **Increase parallelism**: More reservation stations = more parallel execution
2. **Reduce latencies**: For faster testing, lower instruction cycle counts
3. **Optimize cache**: Set block size to common data access pattern
4. **Use step mode**: For detailed analysis, step through cycles
5. **Small programs**: Start with simple programs, add complexity gradually

## 🔒 Limitations & Assumptions

### Architectural
- ✅ **No branch prediction**: Branches stall issuing until resolved
- ✅ **Direct-mapped cache only**: Not set-associative or fully associative
- ✅ **Single CDB**: One broadcast per cycle (not multiple buses)
- ✅ **In-order issue**: Instructions issue sequentially (not superscalar)
- ✅ **Perfect instruction fetch**: No I-cache simulation

### Implementation
- ✅ **Java doubles for all values**: No true floating-point arithmetic simulation
- ✅ **Limited memory**: 1024 bytes (expandable in code)
- ✅ **No virtual memory**: Physical addresses only
- ✅ **No interrupts/exceptions**: Ideal execution environment
- ✅ **Single-threaded**: No parallel simulation

### Pedagogical
- Designed for **educational purposes** to understand Tomasulo algorithm
- Not cycle-accurate to any real processor
- Simplified cache model (no write policies, prefetching, etc.)
- No instruction-level parallelism beyond out-of-order execution

## 📚 Additional Resources

### Documentation Files
- **`ARCHITECTURE.md`**: Detailed architecture documentation
- **`QUICKSTART.md`**: Quick start guide for first-time users
- **`GUI_GUIDE.md`**: Complete GUI feature walkthrough
- **`RUN_ME_FIRST.md`**: Initial setup instructions

### Learning Resources
- **Tomasulo's Algorithm**: Original IBM 360/91 paper
- **Computer Architecture**: Hennessy & Patterson textbook
- **MIPS Assembly**: Green card reference

## 📄 License & Credits

**License**: Educational use only. Created for computer architecture coursework.

**Author**: Developed for microarchitecture simulation and education

**Version**: 1.0

**Technologies**:
- Java 11
- JavaFX 17.0.2
- Maven 3.6+

## 🤝 Contributing

This is an educational project. Contributions welcome:
1. Fork the repository
2. Create feature branch
3. Test thoroughly
4. Submit pull request with description

## 📬 Support

For issues or questions:
1. Check troubleshooting section above
2. Review sample programs for examples
3. Examine cycle log for detailed execution trace
4. Consult ARCHITECTURE.md for implementation details

---

**Happy Simulating! 🎉**
