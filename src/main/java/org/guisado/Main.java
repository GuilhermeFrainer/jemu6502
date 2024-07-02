package org.guisado;

public class Main {
    public static void main(String[] args)
            throws UnimplementedInstructionException, CPU.IllegalAddressingModeException, CPU.IllegalCycleException {
        System.out.println("Hello world!");
        CPU cpu = new CPU();
        byte[] program = {(byte) 0xA9, (byte) 0xC0, (byte) 0xAA, (byte) 0xE8, 0x00};
        cpu.load(program);
        cpu.run();
        System.out.println("Register A: " + ((int) cpu.getAccumulator() & 0xFF));
        System.out.println("Register X: " + ((int) cpu.getRegisterX() & 0xFF));
    }
}