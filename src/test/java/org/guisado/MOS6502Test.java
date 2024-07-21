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

    /**
     * Tested with the examples found here:
     * <a href="https://www.righto.com/2012/12/the-6502-overflow-flag-explained.html">...</a>
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

    /**
     * Used to instantiate a CPU for simple testing.
     * Originally written for testing immediate instructions,
     * since they're the first to be tested and only depend on the status register,
     * the accumulator and an address in memory.
     * @param status status for the CPU instance.
     * @param accumulator accumulator for the CPU instance.
     * @param dataBus operand for the instruction.
     * @return CPU instance with the status, accumulator and dataBus initialized for testing.
     */
    MOS6502 setUpCPUForTesting(int status, int accumulator, int dataBus) {
        MOS6502 cpu = new MOS6502();

        cpu.setStatus(status);
        cpu.setAccumulator(accumulator);
        cpu.setDataBus(dataBus);
        return cpu;
    }

    @Test
    void test_69_0a_e1() {
        MOS6502 cpu = setUpCPUForTesting(175, 2, 10);
        cpu.adc();
        assertEquals(19, cpu.getAccumulatorAsInt());
        assertEquals(44, cpu.getStatusAsInt());
    }

    @Test
    void test_69_8f_b3() {
        MOS6502 cpu = setUpCPUForTesting(111, 227, 143);
        cpu.adc();
        assertEquals(217, cpu.getAccumulatorAsInt());
        assertEquals(109, cpu.getStatusAsInt());
    }

    @Test
    void test_69_84_43() {
        MOS6502 cpu = setUpCPUForTesting(44, 12, 132);
        cpu.adc();
        assertEquals(150, cpu.getAccumulatorAsInt());
        assertEquals(172, cpu.getStatusAsInt());
    }

    @Test
    void test_69_7b_0a() {
        MOS6502 cpu = setUpCPUForTesting(44, 28, 123);
        cpu.adc();
        assertEquals(157, cpu.getAccumulatorAsInt());
        assertEquals(236, cpu.getStatusAsInt());
    }

    @Test
    void test_e9_c4_08() {
        MOS6502 cpu = setUpCPUForTesting(109, 156, 196);
        cpu.sbc();
        assertEquals(120, cpu.getAccumulatorAsInt());
        assertEquals(172, cpu.getStatusAsInt());
    }

    @Test
    void test_c9_bb_bf() {
        MOS6502 cpu = setUpCPUForTesting(175, 16, 187);
        cpu.cmp();
        assertEquals(16, cpu.getAccumulatorAsInt());
        assertEquals(44, cpu.getStatusAsInt());
    }
}