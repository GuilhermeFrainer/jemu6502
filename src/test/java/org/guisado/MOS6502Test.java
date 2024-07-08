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
}