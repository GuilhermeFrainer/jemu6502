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
        return this.ram[(int) address & 0xFFFF];
    }

    @Override
    public void write(byte value, short address) {
        // Converts to int because shorts are signed
        this.ram[(int) address & 0xFFFF] = value;
    }

    public int readAsInt(short address) {
        return this.ram[address & 0xFFFF] & 0xFF;
    }
}
