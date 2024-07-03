package org.guisado;

public class Main {
    public static void main(String[] args)
            throws CPU.UnimplementedInstructionException, CPU.IllegalAddressingModeException, CPU.IllegalCycleException {
        byte[] program = {(byte) 0xA9, (byte) 0xC0, (byte) 0xAA, (byte) 0xE8, 0x00};
        ControllerUnit emulator = new ControllerUnit();
        emulator.load(program);
        emulator.run();
    }
}