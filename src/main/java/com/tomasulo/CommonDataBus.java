// package src.main.java.com.tomasulo;
package com.tomasulo;
import java.util.ArrayList;
import java.util.List;

public class CommonDataBus {
    private List<BusEntry> pendingWrites;

    public static class BusEntry {
        public String tag;
        public double value;
        public Instruction instruction;
        public int issueOrder;

        public BusEntry(String tag, double value, Instruction instruction, int issueOrder) {
            this.tag = tag;
            this.value = value;
            this.instruction = instruction;
            this.issueOrder = issueOrder;
        }
    }

    public CommonDataBus() {
        pendingWrites = new ArrayList<>();
    }

    public void requestWrite(String tag, double value, Instruction instruction, int issueOrder) {
        pendingWrites.add(new BusEntry(tag, value, instruction, issueOrder));
    }

    public List<BusEntry> getPendingWrites() {
        return new ArrayList<>(pendingWrites);
    }

    public void clearPendingWrites() {
        pendingWrites.clear();
    }

    public BusEntry selectWinner(int strategy) {
        if (pendingWrites.isEmpty()) {
            return null;
        }

        // Strategy 0: First come first serve (by issue order)
        // Strategy 1: Oldest instruction first (also by issue order)
        // Both are the same - select instruction with smallest issue order
        BusEntry winner = pendingWrites.get(0);
        for (BusEntry entry : pendingWrites) {
            
            if (entry.instruction.getIssueTime() < winner.instruction.getIssueTime()) {
                winner = entry;
            }
        }

        pendingWrites.remove(winner);
        return winner;
    }
}
