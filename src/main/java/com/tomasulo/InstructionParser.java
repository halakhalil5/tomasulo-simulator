package com.tomasulo;

import java.io.*;
import java.util.*;

public class InstructionParser {

    /**
     * Parse MIPS assembly instructions from a string
     * Supports:
     * - FP operations: ADD.D, SUB.D, MUL.D, DIV.D
     * - Integer operations: ADDI, SUBI
     * - Loads: L.D, L.S, LW
     * - Stores: S.D, S.S, SW
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

                case "ADDI":
                    return new Instruction(Instruction.InstructionType.ADDI,
                            tokens[1], tokens[2], Integer.parseInt(tokens[3]));

                case "SUBI":
                    return new Instruction(Instruction.InstructionType.SUBI,
                            tokens[1], tokens[2], Integer.parseInt(tokens[3]));

                case "L.D":
                    return parseLoadStore(Instruction.InstructionType.L_D, tokens);

                case "L.S":
                    return parseLoadStore(Instruction.InstructionType.L_S, tokens);

                case "LW":
                    return parseLoadStore(Instruction.InstructionType.LW, tokens);

                case "S.D":
                    return parseLoadStore(Instruction.InstructionType.S_D, tokens);

                case "S.S":
                    return parseLoadStore(Instruction.InstructionType.S_S, tokens);

                case "SW":
                    return parseLoadStore(Instruction.InstructionType.SW, tokens);

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
}
