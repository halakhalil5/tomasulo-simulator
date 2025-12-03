# 🚀 Tomasulo Simulator - Quick Start Guide

## ✅ Setup Complete!

Your Tomasulo Simulator is now ready to run! All dependencies have been downloaded and the application has been compiled successfully.

## 🎯 How to Run the Application

### **Option 1: Using the Simple Script (Recommended)**
```powershell
.\run-gui.cmd
```
This will start the application and show any error messages if they occur.

### **Option 2: Using Maven Wrapper Directly**
```powershell
.\mvnw-simple.cmd javafx:run
```

### **Option 3: Manual Java Execution**
If the above don't work, you can run directly:
```powershell
& "C:\Program Files\Eclipse Adoptium\jdk-17.0.17.10-hotspot\bin\java.exe" --module-path "C:\Users\EL MAHDY 01007778867\.m2\repository\org\openjfx" --add-modules javafx.controls,javafx.fxml -cp "target\classes" com.tomasulo.TomasuloSimulator
```

## 📝 What to Expect

When you run the application, you should see:
1. A window titled "Tomasulo Algorithm Simulator"
2. Six tabs across the top:
   - **Code Editor** - Enter your MIPS assembly code here
   - **Reservation Stations** - View Add/Sub, Mul/Div, and Integer stations
   - **Load/Store Buffers** - Monitor memory operations
   - **Register File** - Track register values (R0-R31, F0-F31)
   - **Instruction Queue** - See instruction flow
   - **Cache Status** - Monitor cache hits/misses

3. Control buttons at the bottom:
   - **Load Instructions** - Parse your code
   - **Step** - Execute one cycle
   - **Run** - Execute until completion
   - **Reset** - Clear all state

## 🎓 Quick Tutorial

### 1. Load a Sample Program
- Click **Samples** menu → **Sample 1 - Basic FP Operations**
- This loads a simple program with floating-point operations

### 2. Configure Settings (Optional)
- Click **Configuration** menu to adjust:
  - Instruction latencies
  - Cache parameters
  - Reservation station sizes
  - Initial register values

### 3. Run the Simulation
- Click **Load Instructions** to parse the code
- Click **Step** to execute cycle-by-cycle (watch the tables update!)
- OR click **Run** to execute until completion

### 4. Observe the Results
- Watch instructions move through the pipeline
- See reservation stations become busy/free
- Monitor register dependencies (Qi tags)
- Track cache hits and misses

## 📚 Sample Programs Included

1. **Sample 1** - Basic floating-point operations (ADD.D, SUB.D, MUL.D, DIV.D)
2. **Sample 2** - Loop with branches (BEQ for loop control)
3. **Sample 3** - Hazard demonstrations (RAW, WAR, WAW)
4. **Sample 4** - Complex mixed operations (FP + Integer + Memory)

## 🔧 Supported Instructions

### Floating-Point
- `ADD.D F1, F2, F3` - Add double
- `SUB.D F1, F2, F3` - Subtract double  
- `MUL.D F1, F2, F3` - Multiply double
- `DIV.D F1, F2, F3` - Divide double

### Integer
- `ADDI R1, R2, 100` - Add immediate
- `SUBI R1, R2, 50` - Subtract immediate

### Memory Operations
- `L.D F1, 0(R2)` - Load double to FP register
- `L.S F1, 4(R2)` - Load single to FP register
- `LW R1, 8(R2)` - Load word to integer register
- `S.D F1, 0(R2)` - Store double from FP register
- `S.S F1, 4(R2)` - Store single from FP register
- `SW R1, 8(R2)` - Store word from integer register

### Branches (No Prediction)
- `BEQ R1, R2, LABEL` - Branch if equal
- `BNE R1, R2, LABEL` - Branch if not equal

## ❓ Troubleshooting

### Application doesn't start?
1. Check that Java 17 is installed: `java -version`
2. Make sure JAVA_HOME is set correctly
3. Try running with error output: `.\run-gui.cmd`

### Window opens but crashes?
- Check the terminal for error messages
- Try resetting configuration: **Configuration** → **Latencies** → Use defaults

### Can't see tables?
- The window might be too small - maximize it or resize
- Tables populate after clicking **Load Instructions**

## 📖 More Documentation

- `README.md` - Complete feature documentation
- `ARCHITECTURE.md` - Technical implementation details
- `GUI_GUIDE.md` - Visual walkthrough of all GUI components
- `QUICKSTART.md` - Detailed first-time user guide

## 🎉 Enjoy!

You now have a fully functional Tomasulo algorithm simulator! Experiment with different programs, adjust configurations, and watch how the algorithm handles hazards and dependencies.

---
**Created with:** Java 17 + JavaFX 17
**Build Tool:** Maven (via Maven Wrapper)
