# GUI Usage Guide - Tomasulo Simulator

## Application Layout

```
┌─────────────────────────────────────────────────────────────────┐
│ File  Configuration  Sample Programs  Help                      │
├─────────────────────────────────────────────────────────────────┤
│ Cycle: 5    [Load Program] [Step] [Run to Completion] [Reset]  │
├─────────────────────────────────────────────────────────────────┤
│ ┌───────────────────────────────────────────────────────────┐  │
│ │ [Code Editor] [Reservation Stations] [Load/Store Buffers] │  │
│ │ [Register File] [Instruction Queue] [Cache Status]        │  │
│ │                                                             │  │
│ │  Tab Content Area (Tables and Views)                       │  │
│ │                                                             │  │
│ │                                                             │  │
│ │                                                             │  │
│ └───────────────────────────────────────────────────────────┘  │
├─────────────────────────────────────────────────────────────────┤
│ Cycle Log:                                                      │
│ === Cycle 0 ===                                                 │
│ Issued: L.D F0, 0(R1)                                          │
│ === Cycle 1 ===                                                 │
│ Issued: ADD.D F2, F0, F4                                       │
│ ...                                                             │
└─────────────────────────────────────────────────────────────────┘
```

## Menu Bar Guide

### File Menu
```
File
├── Load Program...     → Open .asm/.s/.txt file
├── ───────────────
└── Exit               → Close application
```

### Configuration Menu
```
Configuration
├── Set Latencies...          → Configure instruction execution cycles
├── Configure Cache...        → Set cache size, block size, hit/miss latency
├── Set Station Sizes...      → Configure RS and buffer quantities
└── Initialize Registers...   → Set initial register values
```

### Sample Programs Menu
```
Sample Programs
├── Simple FP Operations    → Basic ADD.D, MUL.D, etc.
├── Loop Example           → BNE loop with ADDI/SUBI
└── Hazards Example        → RAW, WAR, WAW demonstrations
```

### Help Menu
```
Help
├── Instructions    → How to use the simulator
└── About          → Version and feature information
```

## Tab Descriptions

### 1. Code Editor Tab

```
┌─────────────────────────────────────────────────┐
│ MIPS Assembly Code:                             │
│ ┌─────────────────────────────────────────────┐ │
│ │ L.D F0, 0(R1)                               │ │
│ │ L.D F2, 0(R2)                               │ │
│ │ MUL.D F4, F0, F2                            │ │
│ │ ADD.D F6, F0, F2                            │ │
│ │ S.D F4, 0(R3)                               │ │
│ │ S.D F6, 8(R3)                               │ │
│ │                                              │ │
│ └─────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────┘
```

**Usage**:
- Type or paste MIPS assembly code
- Use # for comments
- Labels format: `LABEL:`
- Click "Load Program" when done

### 2. Reservation Stations Tab

```
Add/Sub Reservation Stations:
┌──────┬──────┬────────┬──────┬──────┬──────┬──────┬───────────┐
│ Name │ Busy │   Op   │  Vj  │  Vk  │  Qj  │  Qk  │ Remaining │
├──────┼──────┼────────┼──────┼──────┼──────┼──────┼───────────┤
│ Add1 │ Yes  │ ADD.D  │ 3.50 │ 4.20 │      │      │     1     │
│ Add2 │ No   │        │      │      │      │      │           │
│ Add3 │ No   │        │      │      │      │      │           │
└──────┴──────┴────────┴──────┴──────┴──────┴──────┴───────────┘

Mul/Div Reservation Stations:
┌──────┬──────┬────────┬──────┬──────┬──────┬──────┬───────────┐
│ Name │ Busy │   Op   │  Vj  │  Vk  │  Qj  │  Qk  │ Remaining │
├──────┼──────┼────────┼──────┼──────┼──────┼──────┼───────────┤
│ Mul1 │ Yes  │ MUL.D  │ 0.00 │ 2.00 │ Load1│      │     8     │
│ Mul2 │ No   │        │      │      │      │      │           │
└──────┴──────┴────────┴──────┴──────┴──────┴──────┴───────────┘

Integer Reservation Stations:
┌──────┬──────┬────────┬──────┬──────┬──────┬──────┬───────────┐
│ Name │ Busy │   Op   │  Vj  │  Vk  │  Qj  │  Qk  │ Remaining │
├──────┼──────┼────────┼──────┼──────┼──────┼──────┼───────────┤
│ Int1 │ No   │        │      │      │      │      │           │
│ Int2 │ No   │        │      │      │      │      │           │
└──────┴──────┴────────┴──────┴──────┴──────┴──────┴───────────┘
```

**Column Meanings**:
- **Name**: Station identifier
- **Busy**: Yes if occupied, No if free
- **Op**: Operation being performed (ADD.D, MUL.D, etc.)
- **Vj, Vk**: Operand values (if ready)
- **Qj, Qk**: Tags of stations producing operands (if waiting)
- **Remaining**: Cycles until completion

**What to Watch**:
- Qj/Qk empty → instruction ready to execute
- Remaining counting down → instruction executing
- Remaining = 0 → instruction complete, waiting for CDB

### 3. Load/Store Buffers Tab

```
Load Buffers:
┌───────┬──────┬──────────┬───────┬──────┬───────────┐
│ Name  │ Busy │ Address  │ Value │  Q   │ Remaining │
├───────┼──────┼──────────┼───────┼──────┼───────────┤
│ Load1 │ Yes  │  0x64    │       │      │     2     │
│ Load2 │ No   │          │       │      │           │
│ Load3 │ No   │          │       │      │           │
└───────┴──────┴──────────┴───────┴──────┴───────────┘

Store Buffers:
┌────────┬──────┬──────────┬───────┬──────┬───────────┐
│ Name   │ Busy │ Address  │ Value │  Q   │ Remaining │
├────────┼──────┼──────────┼───────┼──────┼───────────┤
│ Store1 │ Yes  │  0x0     │ 12.00 │      │     1     │
│ Store2 │ No   │          │       │      │           │
│ Store3 │ No   │          │       │      │           │
└────────┴──────┴──────────┴───────┴──────┴───────────┘
```

**Column Meanings**:
- **Name**: Buffer identifier
- **Busy**: Yes if occupied
- **Address**: Memory address (hex)
- **Value**: Data value (for stores)
- **Q**: Tag if waiting for value (stores only)
- **Remaining**: Cycles including cache latency

**What to Watch**:
- Address calculated at issue
- Remaining includes cache hit/miss latency
- Stores need Q empty before proceeding

### 4. Register File Tab

```
Integer Registers:
┌──────────┬─────────┬──────────────┐
│ Register │  Value  │ Status (Qi)  │
├──────────┼─────────┼──────────────┤
│   R0     │  0.00   │              │
│   R1     │  100.00 │              │
│   R2     │  200.00 │              │
│   R3     │  5.00   │   Add1       │ ← Waiting for Add1
│   ...    │  ...    │              │
└──────────┴─────────┴──────────────┘

Floating Point Registers:
┌──────────┬─────────┬──────────────┐
│ Register │  Value  │ Status (Qi)  │
├──────────┼─────────┼──────────────┤
│   F0     │  3.50   │              │
│   F1     │  0.00   │              │
│   F2     │  4.20   │              │
│   F4     │  0.00   │   Mul1       │ ← Being produced by Mul1
│   ...    │  ...    │              │
└──────────┴─────────┴──────────────┘
```

**Column Meanings**:
- **Register**: Register name (R0-R31 or F0-F31)
- **Value**: Current register value
- **Status (Qi)**: Empty if ready, Tag if being produced

**What to Watch**:
- Empty Status → value is current and ready
- Tag in Status → value not yet available
- After CDB write → Status clears, Value updates

### 5. Instruction Queue Tab

```
Instruction Queue and Execution Status:
┌──────────────────────────┬───────┬────────────┬──────────┬───────┐
│      Instruction         │ Issue │ Exec Start │ Exec End │ Write │
├──────────────────────────┼───────┼────────────┼──────────┼───────┤
│ L.D F0, 0(R1)           │   0   │     0      │    2     │   3   │
│ L.D F2, 0(R2)           │   1   │     1      │    3     │   4   │
│ MUL.D F4, F0, F2        │   2   │     3      │    13    │  14   │
│ ADD.D F6, F0, F2        │   3   │     3      │    5     │   6   │
│ S.D F4, 0(R3)           │   4   │    14      │    16    │  16   │
│ S.D F6, 8(R3)           │   5   │     6      │    8     │   8   │
└──────────────────────────┴───────┴────────────┴──────────┴───────┘
```

**Column Meanings**:
- **Instruction**: Full instruction text
- **Issue**: Cycle when instruction issued
- **Exec Start**: Cycle when execution began
- **Exec End**: Cycle when execution completed
- **Write**: Cycle when result written to CDB

**What to Watch**:
- Empty cells → instruction hasn't reached that stage yet
- Gaps between stages → waiting for dependencies
- Compare Issue to Write → total instruction latency

### 6. Cache Status Tab

```
Cache Status:
┌───────┬──────────────────────────┐
│ Index │         Status           │
├───────┼──────────────────────────┤
│   0   │ Tag: 1, Valid: true     │
│   1   │ Valid: false            │
│   2   │ Tag: 0, Valid: true     │
│  ...  │ ...                     │
└───────┴──────────────────────────┘

Cache Access Log:
┌─────────────────────────────────────────────────────────┐
│ Cycle: Load HIT - Addr: 0x64 (Tag: 1, Index: 6, ...)  │
│ Cycle: Load MISS - Addr: 0xC8 (Tag: 3, Index: 12, ...) │
│ Cycle: Store HIT - Addr: 0x0 (Tag: 0, Index: 0, ...)  │
│ ...                                                     │
└─────────────────────────────────────────────────────────┘
```

**What to Watch**:
- Valid: false → cache block empty
- HIT → fast access (hit latency only)
- MISS → slow access (hit latency + miss penalty)
- Address pattern → spatial/temporal locality

## Control Panel

```
┌────────────────────────────────────────────────────────┐
│ Cycle: 5  │  [Load Program] [Step] [Run to Completion] [Reset] │
└────────────────────────────────────────────────────────┘
```

**Buttons**:
- **Load Program**: Parse code and initialize simulation
- **Step**: Execute one cycle (useful for learning)
- **Run to Completion**: Execute all cycles automatically
- **Reset**: Clear simulation, keep code

## Cycle Log (Bottom Panel)

```
=== Cycle 0 ===
Issued: L.D F0, 0(R1)

=== Cycle 1 ===
Issued: L.D F2, 0(R2)

=== Cycle 2 ===

=== Cycle 3 ===
CDB Write: Load1 = 3.50
Issued: MUL.D F4, F0, F2
Issued: ADD.D F6, F0, F2

=== Cycle 4 ===
CDB Write: Load2 = 4.20
```

**What to Watch**:
- "Issued" → instruction entered reservation station
- "CDB Write" → result broadcast, dependencies resolved
- Empty cycle → no issue/write that cycle
- Pattern reveals parallelism and bottlenecks

## Configuration Dialogs

### Set Latencies Dialog
```
┌────────────────────────────────────────┐
│ Set Instruction Latencies              │
├────────────────────────────────────────┤
│ Floating Point Operations:             │
│   ADD.D latency:  [2     ]             │
│   SUB.D latency:  [2     ]             │
│   MUL.D latency:  [10    ]             │
│   DIV.D latency:  [40    ]             │
│                                        │
│ Integer Operations:                    │
│   ADDI latency:   [1     ]             │
│   SUBI latency:   [1     ]             │
│                                        │
│ Memory Operations:                     │
│   Load base:      [2     ]             │
│   Store base:     [2     ]             │
│                                        │
│ Branch Operations:                     │
│   Branch latency: [1     ]             │
│                                        │
│          [OK]  [Cancel]                │
└────────────────────────────────────────┘
```

### Configure Cache Dialog
```
┌────────────────────────────────────────┐
│ Configure Cache                        │
├────────────────────────────────────────┤
│ Cache size (bytes):     [256  ]        │
│ Block size (bytes):     [16   ]        │
│ Hit latency (cycles):   [1    ]        │
│ Miss penalty (cycles):  [50   ]        │
│                                        │
│ Note: Direct-mapped cache is used.    │
│ Number of blocks = Cache size / Block │
│                                        │
│          [OK]  [Cancel]                │
└────────────────────────────────────────┘
```

### Set Station Sizes Dialog
```
┌────────────────────────────────────────┐
│ Set Reservation Station & Buffer Sizes │
├────────────────────────────────────────┤
│ Reservation Stations:                  │
│   Add/Sub stations:    [3     ]        │
│   Mul/Div stations:    [2     ]        │
│   Integer stations:    [2     ]        │
│                                        │
│ Load/Store Buffers:                    │
│   Load buffers:        [3     ]        │
│   Store buffers:       [3     ]        │
│                                        │
│ Instruction Queue:                     │
│   Queue size:          [16    ]        │
│                                        │
│ Note: Changing these requires reload.  │
│                                        │
│          [OK]  [Cancel]                │
└────────────────────────────────────────┘
```

### Initialize Registers Dialog
```
┌────────────────────────────────────────────────────┐
│ Initialize Register Values                         │
├────────────────────────────────────────────────────┤
│ [Integer Registers] [Floating Point Registers]    │
│                                                    │
│ R0: [0.00  ]    R1: [100.00]                      │
│ R2: [200.00]    R3: [0.00  ]                      │
│ ...                                                │
│                                                    │
│ Quick Set: [Set All to 0] [Set Sequential (0-31)] │
│                                                    │
│                    [OK]  [Cancel]                  │
└────────────────────────────────────────────────────┘
```

## Typical Workflow

### Learning Mode (Step by Step)
```
1. Load sample program
2. Click "Load Program"
3. Click "Step" repeatedly
4. After each step:
   - Check Reservation Stations tab
   - Check Register File tab
   - Read Cycle Log
5. Observe how dependencies resolve
6. Note timing in Instruction Queue tab
```

### Testing Mode (Run to Completion)
```
1. Enter/load your program
2. Configure latencies and cache
3. Initialize registers if needed
4. Click "Load Program"
5. Click "Run to Completion"
6. Review results:
   - Final register values
   - Instruction timing
   - Cache access log
   - Total cycles
```

## Tips for Effective Use

### Understanding Dependencies
1. Watch **Qj/Qk columns** in Reservation Stations
2. Watch **Status (Qi)** in Register File
3. Match tags: Load1, Add1, etc.

### Tracking Execution
1. Use **Step mode** initially
2. Focus on one instruction at a time
3. Follow it through Issue → Execute → Write

### Debugging Programs
1. Check **Cycle Log** for what happened
2. Check **Instruction Queue** for timing
3. Look for unexpected stalls (large gaps)

### Analyzing Performance
1. Compare different latency settings
2. Try different station quantities
3. Observe cache hit/miss patterns
4. Note CDB contention (multiple completes)

## Common Display Patterns

### Normal Execution
```
Reservation Station: Remaining counts down
Register Status: Tag appears, then clears
Cycle Log: Regular issue and write messages
```

### RAW Hazard
```
Reservation Station: Qj or Qk has tag, waits
Register Status: Producer tag visible
Eventually: CDB write, tag clears, execution resumes
```

### Structural Hazard
```
Instruction Queue: Instruction stuck at head
Reservation Station: All stations busy
Cycle Log: No issue messages
Eventually: Station frees, issue resumes
```

### Cache Miss
```
Load/Store Buffer: High Remaining count
Cache Log: "MISS" message
Eventually: Access completes, CDB write
```

### CDB Contention
```
Multiple stations: Remaining = 0
Cycle Log: Only one "CDB Write"
Next cycle: Another "CDB Write"
(Instructions take turns)
```

## Keyboard Shortcuts

Currently, no keyboard shortcuts are implemented. Use mouse for all interactions.

## Window Management

- Minimum recommended resolution: 1400x900
- Resizable window
- Tabs prevent clutter
- Scroll bars appear when needed

## Troubleshooting Visual Issues

**Tables don't update**:
- Click "Step" or "Run to Completion"
- Tables update after each cycle

**Can't see all columns**:
- Resize window wider
- Scroll horizontally in table

**Log area too small**:
- Resize window taller
- Scroll within log area

**Tab content cut off**:
- Make window larger
- Use scroll bars within tabs

This guide should help you navigate and understand all visual elements of the Tomasulo Simulator!
