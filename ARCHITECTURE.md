# Tomasulo Architecture - Detailed Documentation

## Overview

This document provides in-depth technical details about the Tomasulo algorithm implementation in this simulator.

## 1. Tomasulo Algorithm Basics

### What is Tomasulo?
Tomasulo's algorithm is a hardware-based dynamic scheduling technique that:
- Allows out-of-order execution
- Eliminates false dependencies (WAR, WAW)
- Uses register renaming via reservation stations
- Distributes hazard detection

### Key Innovation
Instead of referring to registers, instructions refer to the values they need or the reservation stations producing those values. This decouples instruction scheduling from register allocation.

## 2. Component Details

### 2.1 Reservation Stations (RS)

**Purpose**: Hold instructions waiting for operands and functional units

**Structure**:
```
┌──────────────────────────────────────┐
│ Reservation Station                  │
├──────────────────────────────────────┤
│ Name:      Add1, Mul1, Int1, etc.   │
│ Busy:      true/false                │
│ Op:        ADD.D, MUL.D, etc.        │
│ Vj, Vk:    Operand values (if ready) │
│ Qj, Qk:    Source RS tags (if wait)  │
│ Remaining: Cycles until complete     │
│ Address:   For load/store (if applic)│
└──────────────────────────────────────┘
```

**Types**:
1. **Add/Sub Stations**: FP addition and subtraction
2. **Mul/Div Stations**: FP multiplication and division
3. **Integer Stations**: Integer operations (ADDI, SUBI)

**Lifecycle**:
1. **Issue**: Instruction allocated to free RS
2. **Wait**: If Qj or Qk not empty, wait for CDB
3. **Execute**: When ready (Qj, Qk empty), start execution
4. **Complete**: Countdown reaches 0, request CDB
5. **Write**: After CDB write, RS freed

### 2.2 Load/Store Buffers

**Purpose**: Handle memory operations with address calculation

**Structure**:
```
┌──────────────────────────────────────┐
│ Load/Store Buffer                    │
├──────────────────────────────────────┤
│ Name:      Load1, Store1, etc.      │
│ Busy:      true/false                │
│ Address:   Effective memory address  │
│ Value:     Data value (for stores)   │
│ Q:         Source tag (for stores)   │
│ IsLoad:    true for loads            │
│ Remaining: Cycles (includes cache)   │
└──────────────────────────────────────┘
```

**Load Process**:
1. Calculate address: base + offset
2. Issue to load buffer
3. Access cache (get latency)
4. Wait for cache access
5. Broadcast value on CDB

**Store Process**:
1. Calculate address: base + offset
2. Wait for value (if Q not empty)
3. Access cache (get latency)
4. Write to memory when ready
5. No CDB broadcast needed

**Address Calculation**:
```
Format: INSTR Rd, offset(base)
Address = R[base] + offset

Example: L.D F0, 8(R2)
- base = R2 (register value)
- offset = 8
- address = R[base] + 8
```

### 2.3 Register File

**Purpose**: Store register values and track dependencies

**Structure**:
```
┌────────────────────────────────┐
│ Register File                  │
├────────────────────────────────┤
│ Registers:                     │
│   - Integer: R0-R31            │
│   - Float:   F0-F31            │
│                                │
│ Register Status (Qi):          │
│   - Empty: Value ready         │
│   - Tag:   Waiting for RS/buf  │
└────────────────────────────────┘
```

**Operations**:
- **Read**: Return value if Qi empty, else return Qi tag
- **Write**: Update value and clear Qi
- **Rename**: Set Qi to producing RS tag

**Special Case - R0**:
In MIPS, R0 is hardwired to 0. This simulator allows R0 to be modified for flexibility in testing.

### 2.4 Common Data Bus (CDB)

**Purpose**: Broadcast results from completing instructions

**Structure**:
```
┌─────────────────────────────────────┐
│ Common Data Bus                     │
├─────────────────────────────────────┤
│ Pending Writes:                     │
│   - Tag (source RS/buffer)          │
│   - Value (result)                  │
│   - Instruction (for tracking)      │
│   - Issue Order (for arbitration)   │
└─────────────────────────────────────┘
```

**Arbitration Strategy**:
When multiple instructions complete in the same cycle:
1. Collect all pending writes
2. Sort by issue order (earliest first)
3. Select oldest instruction
4. Broadcast its result
5. Others retry next cycle

**Broadcast Effect**:
1. Update all RS with matching Qj/Qk
2. Update all buffers with matching Q
3. Update register file if Qi matches
4. Clear producing RS/buffer

### 2.5 Instruction Queue

**Purpose**: Buffer instructions waiting to issue

**Properties**:
- Configurable size (default 16)
- FIFO structure
- Refills as instructions issue
- Supports PC manipulation for branches

**Operations**:
- **Peek**: Look at next instruction
- **Issue**: Remove and return next instruction
- **Jump**: Set PC for branches (flush and refill)

### 2.6 Cache

**Type**: Direct-Mapped

**Address Mapping**:
```
Virtual Address (32 bits):
┌────────────┬─────────┬──────────────┐
│    Tag     │  Index  │ Block Offset │
└────────────┴─────────┴──────────────┘
     bits        bits         bits
```

**Calculations**:
```
Block Offset bits = log₂(Block Size)
Index bits = log₂(Number of Blocks)
Tag bits = remaining bits

Number of Blocks = Cache Size / Block Size

Example: 256-byte cache, 16-byte blocks
- Blocks = 256/16 = 16
- Block Offset = log₂(16) = 4 bits
- Index = log₂(16) = 4 bits
- Tag = 32 - 4 - 4 = 24 bits
```

**Block Structure**:
```
┌────────────────────────┐
│ Cache Block            │
├────────────────────────┤
│ Valid: 1 bit           │
│ Tag:   variable bits   │
│ Data:  block_size bytes│
└────────────────────────┘
```

**Access Process**:
1. Extract index from address
2. Read block at index
3. Compare tag
4. If match and valid: HIT (return data)
5. If no match or invalid: MISS (load from memory)

**Latency**:
- Hit: `hit_latency` cycles
- Miss: `hit_latency + miss_penalty` cycles

**Write Policy**:
- Not explicitly modeled (simplified)
- Stores update memory directly
- Cache updated on write

## 3. Execution Stages

### 3.1 Issue Stage

**Order**: Every cycle, attempt to issue ONE instruction

**Steps**:
1. **Check Queue**: Is there an instruction?
2. **Check Resources**: Is appropriate RS/buffer free?
3. **Read Operands**:
   - If Qi empty → read value (Vj or Vk)
   - If Qi not empty → copy tag (Qj or Qk)
4. **Reserve Destination**: Set register Qi to this RS/buffer
5. **Allocate**: Assign instruction to RS/buffer
6. **Advance Queue**: Remove instruction from queue

**Issue Conditions**:
- Must have free reservation station/buffer
- Structural hazards prevent issue
- No other conditions (no WAR/WAW checks needed)

### 3.2 Execute Stage

**Order**: Every cycle, all ready instructions execute

**Steps**:
1. **Check Ready**: Qj and Qk both empty?
2. **Start Execution**: Begin counting down cycles
3. **Decrement**: Reduce remaining cycles by 1
4. **Complete**: When remaining = 0:
   - Compute result (for arithmetic)
   - Access memory (for loads/stores)
   - Request CDB write

**Parallel Execution**:
- Multiple RS can execute simultaneously
- Each functional unit operates independently
- No conflicts as long as resources available

**Special Cases**:
- **Loads**: Access cache, may take variable cycles
- **Stores**: Write memory, no CDB needed
- **Branches**: Evaluate condition, update PC

### 3.3 Write-Back Stage

**Order**: Every cycle, ONE result writes to CDB

**Steps**:
1. **Collect Requests**: Find all completed instructions
2. **Arbitrate**: Select winner (oldest issue order)
3. **Broadcast**:
   - Send tag and value on CDB
   - Update all RS waiting for this tag
   - Update all buffers waiting for this tag
   - Update register file if Qi matches tag
4. **Free Resource**: Clear winning RS/buffer
5. **Defer Others**: Losers retry next cycle

**CDB Contention**:
```
Cycle N:
- Add1 completes: wants CDB
- Mul1 completes: wants CDB
- Load1 completes: wants CDB

Selection:
- Check issue order: Add1=5, Mul1=3, Load1=7
- Winner: Mul1 (issued earliest)
- Add1 and Load1 wait until Cycle N+1
```

## 4. Hazard Handling

### 4.1 RAW (Read-After-Write) / True Dependency

**Example**:
```assembly
MUL.D F0, F2, F4   # Writes F0
ADD.D F6, F0, F8   # Reads F0 (RAW on F0)
```

**Handling**:
1. MUL.D issues, F0.Qi = Mul1
2. ADD.D issues, reads F0
   - F0.Qi = Mul1 (not empty)
   - Set ADD's Qj = Mul1
3. ADD waits until Mul1 broadcasts
4. When Mul1 writes CDB:
   - ADD receives value
   - ADD's Qj cleared
   - ADD can now execute

**Table View**:
```
After Issue:
┌──────┬──────┬────────┬────┬────┬─────┬────┐
│ Name │ Busy │   Op   │ Vj │ Vk │ Qj  │ Qk │
├──────┼──────┼────────┼────┼────┼─────┼────┤
│ Mul1 │ Yes  │ MUL.D  │3.0 │4.0 │     │    │
│ Add1 │ Yes  │ ADD.D  │0.0 │8.0 │Mul1 │    │
└──────┴──────┴────────┴────┴────┴─────┴────┘

Register Status:
F0: Mul1  (F0 being produced by Mul1)

After Mul1 Writes:
┌──────┬──────┬────────┬────┬────┬────┬────┐
│ Name │ Busy │   Op   │ Vj │ Vk │ Qj │ Qk │
├──────┼──────┼────────┼────┼────┼────┼────┤
│ Mul1 │ No   │        │    │    │    │    │
│ Add1 │ Yes  │ ADD.D  │12.0│8.0 │    │    │ ← Vj updated
└──────┴──────┴────────┴────┴────┴────┴────┘

Register Status:
F0: 12.0  (value now available)
```

### 4.2 WAR (Write-After-Read) / Anti-Dependency

**Example**:
```assembly
ADD.D F6, F0, F8   # Reads F0
MUL.D F0, F2, F4   # Writes F0 (WAR on F0)
```

**Handling**:
- **Not a Problem!** Values captured at issue
- ADD reads F0's value when it issues
- MUL can immediately update F0.Qi
- No conflict because ADD has F0's value

**Key**: Register renaming eliminates WAR

### 4.3 WAW (Write-After-Write) / Output Dependency

**Example**:
```assembly
MUL.D F0, F2, F4   # Writes F0
ADD.D F0, F6, F8   # Also writes F0 (WAW on F0)
```

**Handling**:
1. MUL.D issues, F0.Qi = Mul1
2. ADD.D issues, F0.Qi = Add1 (overwrite)
3. Both execute independently
4. MUL completes first, writes to CDB
   - Checks F0.Qi: now Add1, not Mul1
   - Does NOT update F0 (wrong tag)
5. ADD completes, writes to CDB
   - Checks F0.Qi: still Add1 (match!)
   - Updates F0 with correct value

**Result**: Final value is from ADD (program order preserved)

**Key**: Register status always points to latest writer

## 5. Memory System

### 5.1 Memory Model

**Simplifications**:
- Word-aligned addresses (4 bytes)
- Infinite size
- No bank conflicts
- Perfect store (never fails)

**Storage**:
- HashMap<Integer, Double>
- Address → Value mapping

### 5.2 Cache Model

**Configuration**:
- Direct-mapped only
- Configurable size and block size
- Write-through implied

**Limitations**:
- No write buffer
- No dirty bits (write-through assumed)
- Single cycle for actual read/write (after latency)

### 5.3 Address Spaces

**Separate Spaces** (programmer's responsibility):
- Code: Instructions (not cached)
- Data: Load/store targets (cached)

**Typical Layout**:
```
0x0000 - 0x00FF: Stack/data
0x0100 - 0x01FF: Heap
0x0200+:         Program-specific
```

## 6. Branch Handling

### No Branch Prediction

**Process**:
1. Branch instruction issues
2. Wait for operands (check Qi of both sources)
3. Evaluate condition when ready
4. Update PC if taken
5. Flush instruction queue
6. Refill from new PC

**Branch Types**:
- **BEQ**: Branch if equal
- **BNE**: Branch if not equal

**Label Resolution**:
- Labels parsed during load
- Stored in label → PC map
- Branch target looked up by label

**Example**:
```assembly
      ADDI R1, R0, 0
LOOP: L.D F0, 0(R2)
      ADDI R1, R1, 1
      SUBI R3, R1, 10
      BNE R3, R0, LOOP

Labels: {LOOP: 4}  (PC = 4 for second instruction)
```

## 7. Implementation Notes

### 7.1 Cycle Execution Order

Within each cycle:
```
1. Write-Back (CDB broadcasts)
   ↓
2. Execute (operations proceed)
   ↓
3. Issue (new instruction enters)
```

**Rationale**: 
- Write first to free resources
- Execute to make progress
- Issue last to fill freed slots

### 7.2 Timing Details

**Issue**: Happens in same cycle as decision
- Instruction at cycle N → issues at cycle N
- Recorded as issue time = N

**Execute**: Starts next cycle after issue
- Issue at N → execute starts at N+1
- Latency counts from N+1

**Write**: Happens when countdown = 0
- If execute ends at N → write at N

**Example Timeline**:
```
ADD.D F0, F2, F4  (latency = 2)

Cycle 0: Issue (record issue=0)
Cycle 1: Execute (remaining=2)
Cycle 2: Execute (remaining=1)
Cycle 3: Complete (remaining=0), write to CDB
```

### 7.3 Structural Hazards

**Causes**:
- All stations of required type busy
- All buffers of required type busy
- Instruction queue full

**Effect**:
- Issue stalls
- Instruction remains at head of queue
- Will retry next cycle

### 7.4 Data Structures

**Key Design Decisions**:
- List for RS/buffers (iteration efficiency)
- HashMap for registers (lookup efficiency)
- HashMap for cache (sparse storage)
- Queue for instructions (FIFO)

## 8. Performance Considerations

### Factors Affecting Execution Time

1. **Instruction Mix**:
   - Long latency (DIV) vs. short (ADD)
   - Load/store percentage

2. **Dependencies**:
   - RAW chains increase time
   - Independent instructions overlap

3. **Resource Availability**:
   - More RS → more parallelism
   - Structural hazards → stalls

4. **Cache Performance**:
   - Hit rate critical for loads/stores
   - Misses add significant latency

5. **CDB Contention**:
   - Multiple completions → serialization
   - Arbitration causes waiting

### Optimization Strategies

1. **Increase Stations**: Reduce structural hazards
2. **Increase Cache**: Better hit rate
3. **Reduce Latencies**: Faster functional units
4. **Code Scheduling**: Minimize dependencies

## 9. Validation

### Correctness Criteria

1. **Program Order for Same Register**:
   - If two instructions write same register, later one's value persists

2. **Dependency Satisfaction**:
   - RAW: Reader gets correct value
   - WAR: Eliminated by renaming
   - WAW: Latest write wins

3. **Memory Consistency**:
   - Loads return values from last store
   - No reordering visible to memory

### Testing Approach

1. **Simple Programs**: Verify basic operations
2. **Hazard Programs**: Test RAW/WAR/WAW
3. **Loop Programs**: Test branches and iteration
4. **Cache Programs**: Test hit/miss patterns
5. **Complex Programs**: Combined scenarios

## 10. Comparison with Hardware

### Similarities
✅ Register renaming
✅ Out-of-order execution
✅ Dynamic scheduling
✅ Hazard elimination

### Differences
❌ Simplified cache (single level, direct-mapped)
❌ No instruction cache
❌ Single CDB (real: multiple)
❌ No speculative execution
❌ No ROB (reorder buffer)
❌ Perfect memory

### Modern Variants
Real processors extend Tomasulo with:
- Reorder Buffer (ROB) for precise exceptions
- Speculative execution with branch prediction
- Multiple issue/execution ports
- Hierarchical cache
- Store buffers
- Memory disambiguation

## Conclusion

This implementation provides a faithful simulation of the core Tomasulo algorithm with modern educational enhancements. While simplified compared to real hardware, it captures the essential mechanisms and trade-offs that make dynamic scheduling effective.
