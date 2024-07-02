package org.guisado;

public class Memory64k implements Memory {
    private byte[] memory;

    public Memory64k() {
        this.memory = new byte[0x10000];
    }

    @Override
    public byte read(short address) {
        // Converts to int because shorts are signed
        int addr = (int) address & 0xFF;
        return this.memory[addr];
    }

    @Override
    public void write(byte value, short address) {
        // Converts to int because shorts are signed
        int addr = (int) address & 0xFF;
        this.memory[addr] = value;
    }
}
