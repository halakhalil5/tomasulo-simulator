package com.tomasulo;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class InstructionQueue {
    private Queue<Instruction> queue;
    private List<Instruction> allInstructions;
    private int maxSize;
    private int pc;

    public InstructionQueue(int maxSize) {
        this.maxSize = maxSize;
        this.queue = new LinkedList<>();
        this.allInstructions = new ArrayList<>();
        this.pc = 0;
    }

    public void loadInstructions(List<Instruction> instructions) {
        allInstructions.clear();
        allInstructions.addAll(instructions);
        queue.clear();
        pc = 0;

        // Set PC for each instruction
        for (int i = 0; i < allInstructions.size(); i++) {
            allInstructions.get(i).setPc(i * 4); // MIPS uses 4-byte instructions
        }

        // Fill queue up to maxSize
        fillQueue();
    }

    private void fillQueue() {
        while (queue.size() < maxSize && pc / 4 < allInstructions.size()) {
            queue.offer(allInstructions.get(pc / 4));
            pc += 4;
        }
    }

    public Instruction peek() {
        return queue.peek();
    }

    public Instruction issue() {
        Instruction inst = queue.poll();
        fillQueue();
        return inst;
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public boolean hasMoreInstructions() {
        return !queue.isEmpty() || pc / 4 < allInstructions.size();
    }

    public List<Instruction> getQueueSnapshot() {
        return new ArrayList<>(queue);
    }

    public List<Instruction> getAllInstructions() {
        return new ArrayList<>(allInstructions);
    }

    public void reset() {
        queue.clear();
        pc = 0;
        fillQueue();
    }

    public void jumpTo(int targetPc) {
        queue.clear();
        this.pc = targetPc;
        fillQueue();
    }
}
