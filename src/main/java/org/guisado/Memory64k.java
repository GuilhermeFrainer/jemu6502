package org.guisado;

public class Memory64k implements Memory {
    private final byte[] ram;

    public Memory64k() {
        this.ram = new byte[0x10000];
    }

    public byte[] getRam() {
        return this.ram;
    }

    @Override
    public byte read(short address) {
        // Converts to int because shorts are signed
        int addr = (int) address & 0xFFFF;
        return this.ram[addr];
    }

    @Override
    public void write(byte value, short address) {
        // Converts to int because shorts are signed
        int addr = (int) address & 0xFFFF;
        this.ram[addr] = value;
    }
}
