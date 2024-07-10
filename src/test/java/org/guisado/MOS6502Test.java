package org.guisado;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MOS6502Test {
    @Test
    void testSetDataBus() {
        MOS6502 cpu = new MOS6502();
        final byte exampleNumber = 105;
        cpu.setDataBus(exampleNumber);
        assertEquals(exampleNumber, cpu.getDataBus());
    }

    @Test
    void testStackPush() {
        MOS6502 cpu = new MOS6502();
        final byte stackPointer = 81;
        final byte value = 0x30;

        cpu.setStackPointer(stackPointer);
        cpu.stackPush(value);
        assertEquals(stackPointer - 1, cpu.getStackPointer());
        assertEquals(0x151, cpu.getAddressBus());
        assertEquals(value, cpu.getDataBus());
        assertEquals(MOS6502.ReadWrite.Write, cpu.getReadWritePin());
    }
}