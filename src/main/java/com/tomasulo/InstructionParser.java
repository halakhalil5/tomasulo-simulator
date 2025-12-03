package com.tomasulo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class InstructionParser {

    /**
     * Parse MIPS assembly instructions from a string
     * Supports:
     * - FP double operations: ADD.D, SUB.D, MUL.D, DIV.D
     * - FP single operations: ADD.S, SUB.S, MUL.S, DIV.S
     * - Integer operations: DADDI, DSUBI
     * - Loads: L.D, L.S, LW, LD
     * - Stores: S.D, S.S, SW, SD
     * - Branches: BEQ, BNE
     * - Labels: LOOP:, END:, etc.
     */
    public static List<Instruction> parse(String code) throws Exception {
        List<Instruction> instructions = new ArrayList<>();
        String[] lines = code.split("\n");

        for (String line : lines) {
            line = line.trim();

            // Remove comments
            if (line.contains("#")) {
                line = line.substring(0, line.indexOf("#")).trim();
            }
            if (line.contains(";")) {
                line = line.substring(0, line.indexOf(";")).trim();
            }

            if (line.isEmpty())
                continue;

            // Handle labels
            if (line.contains(":")) {
                String labelPart = line.substring(0, line.indexOf(":")).trim();
                String rest = line.substring(line.indexOf(":") + 1).trim();

                if (!rest.isEmpty()) {
                    Instruction inst = parseLine(rest);
                    if (inst != null) {
                        inst.setOriginalInstruction(line);
                        instructions.add(inst);
                    }
                }
                continue;
            }

            Instruction inst = parseLine(line);
            if (inst != null) {
                inst.setOriginalInstruction(line);
                instructions.add(inst);
            }
        }

        return instructions;
    }

    private static Instruction parseLine(String line) throws Exception {
        String[] tokens = line.split("[\\s,()]+");
        if (tokens.length == 0)
            return null;

        String opcode = tokens[0].toUpperCase();

        try {
            switch (opcode) {
                case "ADD.D":
                    return new Instruction(Instruction.InstructionType.ADD_D,
                            tokens[1], tokens[2], tokens[3]);

                case "SUB.D":
                    return new Instruction(Instruction.InstructionType.SUB_D,
                            tokens[1], tokens[2], tokens[3]);

                case "MUL.D":
                    return new Instruction(Instruction.InstructionType.MUL_D,
                            tokens[1], tokens[2], tokens[3]);

                case "DIV.D":
                    return new Instruction(Instruction.InstructionType.DIV_D,
                            tokens[1], tokens[2], tokens[3]);

                case "ADD.S":
                    return new Instruction(Instruction.InstructionType.ADD_S,
                            tokens[1], tokens[2], tokens[3]);

                case "SUB.S":
                    return new Instruction(Instruction.InstructionType.SUB_S,
                            tokens[1], tokens[2], tokens[3]);

                case "MUL.S":
                    return new Instruction(Instruction.InstructionType.MUL_S,
                            tokens[1], tokens[2], tokens[3]);

                case "DIV.S":
                    return new Instruction(Instruction.InstructionType.DIV_S,
                            tokens[1], tokens[2], tokens[3]);

                case "DADDI":
                    return new Instruction(Instruction.InstructionType.DADDI,
                            tokens[1], tokens[2], Integer.parseInt(tokens[3]));

                case "DSUBI":
                    return new Instruction(Instruction.InstructionType.DSUBI,
                            tokens[1], tokens[2], Integer.parseInt(tokens[3]));

                case "L.D":
                    return parseLoadStore(Instruction.InstructionType.L_D, tokens);

                case "L.S":
                    return parseLoadStore(Instruction.InstructionType.L_S, tokens);

                case "LW":
                    return parseLoadStore(Instruction.InstructionType.LW, tokens);

                case "LD":
                    return parseLoadStore(Instruction.InstructionType.LD, tokens);

                case "S.D":
                    return parseLoadStore(Instruction.InstructionType.S_D, tokens);

                case "S.S":
                    return parseLoadStore(Instruction.InstructionType.S_S, tokens);

                case "SW":
                    return parseLoadStore(Instruction.InstructionType.SW, tokens);

                case "SD":
                    return parseLoadStore(Instruction.InstructionType.SD, tokens);

                case "BEQ":
                    Instruction beq = new Instruction(Instruction.InstructionType.BEQ,
                            null, tokens[1], tokens[2]);
                    beq.setLabel(tokens[3]);
                    return beq;

                case "BNE":
                    Instruction bne = new Instruction(Instruction.InstructionType.BNE,
                            null, tokens[1], tokens[2]);
                    bne.setLabel(tokens[3]);
                    return bne;

                default:
                    System.err.println("Unknown opcode: " + opcode);
                    return null;
            }
        } catch (Exception e) {
            throw new Exception("Error parsing line: " + line + " - " + e.getMessage());
        }
    }

    private static Instruction parseLoadStore(Instruction.InstructionType type, String[] tokens) {
        // Format: LW R1, 0(R2) or LW R1, offset(base)
        // tokens[0] = opcode, tokens[1] = dest/src, tokens[2] = offset, tokens[3] =
        // base

        String destSrc = tokens[1];
        String offset = tokens[2];
        String base = tokens[3];

        Instruction inst = new Instruction(type, destSrc, base, offset);
        return inst;
    }

    /**
     * Load instructions from a file
     */
    public static List<Instruction> parseFile(File file) throws Exception {
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line;

        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();

        return parse(sb.toString());
    }

    /**
     * Generate sample programs for testing
     */
    public static String getSampleProgram1() {
        return "# Simple FP operations\n" +
                "L.D F0, 0(R1)\n" +
                "L.D F2, 0(R2)\n" +
                "MUL.D F4, F0, F2\n" +
                "ADD.D F6, F0, F2\n" +
                "S.D F4, 0(R3)\n" +
                "S.D F6, 8(R3)\n";
    }

    public static String getSampleProgram2() {
        return "# Loop example\n" +
                "ADDI R1, R0, 0    # i = 0\n" +
                "ADDI R2, R0, 100  # base address\n" +
                "LOOP:\n" +
                "L.D F0, 0(R2)\n" +
                "MUL.D F4, F0, F0\n" +
                "S.D F4, 0(R2)\n" +
                "ADDI R2, R2, 8\n" +
                "ADDI R1, R1, 1\n" +
                "SUBI R3, R1, 10   # Check if i < 10\n" +
                "BNE R3, R0, LOOP\n";
    }

    public static String getSampleProgram3() {
        return "# RAW, WAR, WAW hazards\n" +
                "L.D F0, 0(R1)     # Load F0\n" +
                "ADD.D F2, F0, F4  # RAW on F0\n" +
                "MUL.D F0, F2, F6  # WAW on F0, RAW on F2\n" +
                "SUB.D F8, F0, F2  # RAW on F0 and F2\n" +
                "DIV.D F10, F0, F6 # RAW on F0\n" +
                "S.D F0, 0(R2)     # Store F0\n";
    }
    
    public static String getSampleProgram4() {
        return "# New instruction types demo\n" +
                "# Single precision FP operations\n" +
                "L.S F0, 0(R1)     # Load single\n" +
                "L.S F2, 4(R1)     # Load single\n" +
                "ADD.S F4, F0, F2  # Add single\n" +
                "SUB.S F6, F0, F2  # Subtract single\n" +
                "MUL.S F8, F0, F2  # Multiply single\n" +
                "DIV.S F10, F0, F2 # Divide single\n" +
                "S.S F4, 0(R2)     # Store single\n" +
                "# Double word loads/stores\n" +
                "LD F12, 0(R3)     # Load double word\n" +
                "SD F12, 8(R3)     # Store double word\n";
    }
}
