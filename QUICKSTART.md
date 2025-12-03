# Quick Start Guide - Tomasulo Simulator

## Installation & Running

1. **Install Prerequisites:**
   - Java JDK 11 or higher
   - Maven 3.6 or higher

2. **Navigate to Project Directory:**
   ```powershell
   cd "c:\Users\EL MAHDY 01007778867\microproj"
   ```

3. **Build and Run:**
   ```powershell
   mvn clean javafx:run
   ```

## First Time Usage

### Step 1: Configure the Simulator (Optional)
- Go to **Configuration** menu
- **Set Latencies**: Configure execution cycles for each instruction type
  - Defaults are reasonable for learning
- **Configure Cache**: Set cache size, block size, hit/miss latencies
  - Default: 256 bytes, 16-byte blocks, 1 cycle hit, 50 cycle miss
- **Set Station Sizes**: Configure number of reservation stations and buffers
  - Defaults: 3 Add/Sub, 2 Mul/Div, 2 Integer, 3 Load, 3 Store buffers

### Step 2: Load a Program
Choose one of these methods:
- **Use Sample**: Menu → Sample Programs → Choose one
- **Type Code**: Enter MIPS assembly in Code Editor tab
- **Load File**: File → Load Program → Select .asm file from samples folder

### Step 3: Initialize Registers (Optional)
- Configuration → Initialize Registers
- Set initial values for R0-R31 (integer) and F0-F31 (floating point)
- Quick options: Set all to 0 or sequential values

### Step 4: Run Simulation
1. Click **"Load Program"** button
2. Choose execution mode:
   - **"Step"**: Execute one cycle at a time (recommended for learning)
   - **"Run to Completion"**: Execute all cycles automatically

### Step 5: View Results
Switch between tabs to observe:
- **Reservation Stations**: See which operations are executing
- **Load/Store Buffers**: Track memory operations
- **Register File**: Monitor register values and dependencies
- **Instruction Queue**: View instruction timing (issue/execute/write)
- **Cache Status**: See cache hits/misses
- **Cycle Log** (bottom): Read detailed cycle-by-cycle operations

## Example Workflow

1. Start the application: `mvn javafx:run`
2. Select: Sample Programs → "Hazards Example"
3. Click: "Load Program"
4. Click: "Step" repeatedly and watch:
   - Instructions issue to reservation stations
   - Dependencies tracked via Qj/Qk
   - Operations execute (remaining cycles count down)
   - Results broadcast on CDB
   - Registers update
5. Review timing in "Instruction Queue" tab

## Understanding the Display

### Reservation Station Table Columns:
- **Name**: Station identifier (Add1, Mul1, etc.)
- **Busy**: Yes if station occupied
- **Op**: Operation being performed
- **Vj, Vk**: Operand values (if ready)
- **Qj, Qk**: Tags of producing stations (if waiting)
- **Remaining**: Cycles left to complete

### Register File Table:
- **Register**: Register name (R0-R31, F0-F31)
- **Value**: Current register value
- **Status (Qi)**: Producing station tag (empty if ready)

### Instruction Table:
- **Issue**: Cycle when instruction issued
- **Exec Start**: Cycle when execution started
- **Exec End**: Cycle when execution completed
- **Write**: Cycle when result written to CDB

## Common Scenarios

### RAW Hazard Example:
```assembly
L.D F0, 0(R1)     # Loads F0
ADD.D F2, F0, F4  # Must wait for F0 (RAW)
```
- ADD.D issues with Qj pointing to Load buffer
- ADD.D waits until L.D completes and broadcasts
- Then ADD.D executes

### WAW Hazard Example:
```assembly
MUL.D F0, F2, F4  # Writes F0
ADD.D F0, F6, F8  # Also writes F0 (WAW)
```
- Both issue, F0's Qi updated to Add station
- Even if MUL completes first, register uses ADD's result
- Tomasulo handles this automatically via register renaming

### Cache Miss:
- Load/Store to new address causes cache miss
- Latency = Hit Latency + Miss Penalty
- Subsequent access to same block = cache hit

## Troubleshooting

**Nothing happens when I click Step:**
- Ensure you clicked "Load Program" first
- Check if all stations are busy (structural hazard)
- Verify instruction syntax is correct

**Instructions stuck:**
- Check Register File tab for unresolved dependencies (Qi column)
- Some instructions may be waiting for earlier operations
- Long-latency operations (DIV) take many cycles

**Cache always missing:**
- Cache is direct-mapped, may have conflicts
- Check addresses in Load/Store Buffers tab
- Review cache configuration (Configuration → Configure Cache)

## Tips for Learning

1. **Start Simple**: Use sample1.asm to understand basics
2. **Step Mode**: Always use Step initially to see each cycle
3. **Watch Dependencies**: Focus on Qj/Qk columns in reservation stations
4. **Compare Timing**: After completion, check Instruction Queue tab
5. **Experiment**: Try changing latencies to see impact on execution time

## Need Help?

- Click: Help → Instructions (in the application)
- Click: Help → About
- Read: README.md for detailed documentation
