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

    @Test
    void testHexToDecimal() {
        MOS6502 cpu = new MOS6502();

        assertEquals(40, cpu.hexToDecimal((byte) 0x40) & 0xFF);
        assertEquals(10, cpu.hexToDecimal((byte) 0x10) & 0xFF);
        assertEquals(64, cpu.hexToDecimal((byte) 0x64) & 0xFF);
        assertEquals(58, cpu.hexToDecimal((byte) 0x58) & 0xFF);
        assertEquals(46, cpu.hexToDecimal((byte) 0x46) & 0xFF);
        assertEquals(12, cpu.hexToDecimal((byte) 0x12) & 0xFF);
        assertEquals(34, cpu.hexToDecimal((byte) 0x34) & 0xFF);
        assertEquals(15, cpu.hexToDecimal((byte) 0x15) & 0xFF);
        assertEquals(26, cpu.hexToDecimal((byte) 0x26) & 0xFF);
    }
    /**
     * Tested with the examples found here: https://www.righto.com/2012/12/the-6502-overflow-flag-explained.html
     */
    @Test
    void testADC() {
        MOS6502 cpu = new MOS6502();

        cpu.setAccumulator(0x50);
        cpu.setDataBus(0x10);
        cpu.adc();
        assertEquals(0x60, cpu.getAccumulatorAsInt());
        assertEquals(0, cpu.getStatusAsInt() & MOS6502.OVERFLOW);
        assertEquals(0, cpu.getStatusAsInt() & MOS6502.CARRY);

        cpu.setStatus(0);
        cpu.setAccumulator(0x50);
        cpu.setDataBus(0x50);
        cpu.adc();
        assertEquals(0xA0, cpu.getAccumulatorAsInt());
        assertNotEquals(0, cpu.getStatusAsInt() & MOS6502.OVERFLOW);
        assertEquals(0, cpu.getStatusAsInt() & MOS6502.CARRY);

        cpu.setStatus(0);
        cpu.setAccumulator(0x50);
        cpu.setDataBus(0x90);
        cpu.adc();
        assertEquals(0xE0, cpu.getAccumulatorAsInt());
        assertEquals(0, cpu.getStatusAsInt() & MOS6502.OVERFLOW);
        assertEquals(0, cpu.getStatusAsInt() & MOS6502.CARRY);

        cpu.setStatus(0);
        cpu.setAccumulator(0x50);
        cpu.setDataBus(0xD0);
        cpu.adc();
        assertEquals(0x120 & 0xFF, cpu.getAccumulatorAsInt());
        assertEquals(0, cpu.getStatusAsInt() & MOS6502.OVERFLOW);
        assertNotEquals(0, cpu.getStatusAsInt() & MOS6502.CARRY);

        cpu.setStatus(0);
        cpu.setAccumulator(0xD0);
        cpu.setDataBus(0x10);
        cpu.adc();
        assertEquals(0xE0, cpu.getAccumulatorAsInt());
        assertEquals(0, cpu.getStatusAsInt() & MOS6502.OVERFLOW);
        assertEquals(0, cpu.getStatusAsInt() & MOS6502.CARRY);

        cpu.setStatus(0);
        cpu.setAccumulator(0xD0);
        cpu.setDataBus(0x50);
        cpu.adc();
        assertEquals(0x120 & 0xFF, cpu.getAccumulatorAsInt());
        assertEquals(0, cpu.getStatusAsInt() & MOS6502.OVERFLOW);
        assertNotEquals(0, cpu.getStatusAsInt() & MOS6502.CARRY);

        cpu.setStatus(0);
        cpu.setAccumulator(0xD0);
        cpu.setDataBus(0x90);
        cpu.adc();
        assertEquals(0x160 & 0xFF, cpu.getAccumulatorAsInt());
        assertNotEquals(0, cpu.getStatusAsInt() & MOS6502.OVERFLOW);
        assertNotEquals(0, cpu.getStatusAsInt() & MOS6502.CARRY);

        cpu.setStatus(0);
        cpu.setAccumulator(0xD0);
        cpu.setDataBus(0xD0);
        cpu.adc();
        assertEquals(0x1A0 & 0xFF, cpu.getAccumulatorAsInt());
        assertEquals(0, cpu.getStatusAsInt() & MOS6502.OVERFLOW);
        assertNotEquals(0, cpu.getStatusAsInt() & MOS6502.CARRY);
    }

    /*
     * The following tests were based on failed tests from Tom Harte's test suite.
     */

    @Test
    void test_69_0a_e1() {
        MOS6502 cpu = new MOS6502();

        cpu.setStatus(175); // D is set
        cpu.setAccumulator(2);
        cpu.setDataBus(10);
        cpu.adc();
        assertEquals(19, cpu.getAccumulatorAsInt());
        assertEquals(44, cpu.getStatusAsInt());
    }

    @Test
    void test_69_8f_b3() {
        MOS6502 cpu = new MOS6502();

        cpu.setStatus(111); // D is set
        cpu.setAccumulator(227);
        cpu.setDataBus(143);
        cpu.adc();
        assertEquals(217, cpu.getAccumulatorAsInt());
        assertEquals(109, cpu.getStatusAsInt());
    }
}