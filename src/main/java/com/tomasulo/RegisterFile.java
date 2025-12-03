package com.tomasulo;

import java.util.HashMap;
import java.util.Map;

public class RegisterFile {
    private Map<String, Double> integerRegisters;
    private Map<String, Double> floatRegisters;
    private Map<String, String> registerStatus; // Maps register to producing reservation station

    public RegisterFile(int numIntRegs, int numFloatRegs) {
        integerRegisters = new HashMap<>();
        floatRegisters = new HashMap<>();
        registerStatus = new HashMap<>();

        // Initialize integer registers R0-R31
        for (int i = 0; i < numIntRegs; i++) {
            integerRegisters.put("R" + i, 0.0);
            registerStatus.put("R" + i, "");
        }
        // Initialize floating point registers F0-F31
        for (int i = 0; i < numFloatRegs; i++) {
            floatRegisters.put("F" + i, 0.0);
            registerStatus.put("F" + i, "");
        }
    }

    public double getValue(String register) {
        if (register.startsWith("F")) {
            return floatRegisters.getOrDefault(register, 0.0);
        } else {
            return integerRegisters.getOrDefault(register, 0.0);
        }
    }

    public void setValue(String register, double value) {
        if (register.startsWith("F")) {
            floatRegisters.put(register, value);
        } else {
            integerRegisters.put(register, value);
        }
    }

    public String getStatus(String register) {
        return registerStatus.getOrDefault(register, "");
    }

    public void setStatus(String register, String tag) {
        registerStatus.put(register, tag);
    }

    public void clearStatus(String register) {
        registerStatus.put(register, "");
    }

    public void clearAllStatus() {
        for (String reg : registerStatus.keySet()) {
            registerStatus.put(reg, "");
        }
    }

    public Map<String, Double> getIntegerRegisters() {
        return new HashMap<>(integerRegisters);
    }

    public Map<String, Double> getFloatRegisters() {
        return new HashMap<>(floatRegisters);
    }

    public Map<String, String> getRegisterStatus() {
        return new HashMap<>(registerStatus);
    }

    public void reset() {
        for (String reg : integerRegisters.keySet()) {
            integerRegisters.put(reg, 0.0);
            registerStatus.put(reg, "");
        }
        for (String reg : floatRegisters.keySet()) {
            floatRegisters.put(reg, 0.0);
            registerStatus.put(reg, "");
        }
    }
}
