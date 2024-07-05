package org.guisado;

public class Main {
    public static void main(String[] args)
            throws MOS6502.UnimplementedInstructionException, MOS6502.IllegalAddressingModeException, MOS6502.IllegalCycleException {
        byte[] program = {
                (byte) 0xA9, // LDA (Immediate)
                (byte) 0xC0, // VALUE
                (byte) 0xAA, // TAX
                (byte) 0xE8, // INX
                0x00 //BRK
        };
        ControllerUnit emulator = new ControllerUnit();
        emulator.load(program);
        emulator.run();
    }
}