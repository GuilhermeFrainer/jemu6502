package org.guisado;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;

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
    void testReadAndWrite() {
        /**
         * Extracted from test case 00 3f f7.
         */
        int[][] program = {{35714, 0}, {35715, 63}, {35716, 247}, {65534, 212}, {65535, 37}, {9684, 237}};
        MOS6502 cpu = new MOS6502();

        for (int i = 0; i < program.length; i++) {
            short address = (short) program[i][0];
            byte value = (byte) program[i][1];
            //System.out.println("Writing " + value + " at " + address);
            cpu.setAddressBus(address);
            cpu.setDataBus(value);
            cpu.writeToMemory();
        }

        assertEquals(0, cpu.memory.readAsInt((short) 35714));
        assertEquals(63, cpu.memory.readAsInt((short) 35715));
        assertEquals(247, cpu.memory.readAsInt((short) 35716));
        assertEquals(212, cpu.memory.readAsInt((short) 65534));
        assertEquals(37, cpu.memory.readAsInt((short) 65535));
        assertEquals(237, cpu.memory.readAsInt((short) 9684));
    }

    @Test
    void testLoad() {
        byte[] program  = new byte[0x10000];
        program[45930] = (byte) 169;
        program[45931] = (byte) 204;
        program[45932] = (byte) 33;

        MOS6502 cpu = new MOS6502();
        cpu.load(program);

        assertEquals((byte) 169, cpu.memory.read((short) 45930));
        assertEquals((byte) 204, cpu.memory.read((short) 45931));
        assertEquals((byte) 33, cpu.memory.read((short) 45932));
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

    MOS6502 setUpCPUForTesting(int status, int accumulator, int dataBus, int opcode) {
        MOS6502 cpu = new MOS6502();
        final Instruction[] instructionSet = Instruction.initializeInstructionSet();

        cpu.setStatus(status);
        cpu.setAccumulator(accumulator);
        cpu.setDataBus(dataBus);
        cpu.setCurrentInstruction(instructionSet[opcode]);
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

    @Test
    void test_6a_5d_58() {
        MOS6502 cpu = setUpCPUForTesting(100, 168, 93, 0x6A);
        cpu.ror();
        assertEquals(84, cpu.getAccumulatorAsInt());
        assertEquals(100, cpu.getStatusAsInt());
    }

    /*
     * Below here are functions which run Tom Harte's test suite.
     */

    private static final String pathToTests = "..\\65x02\\6502\\v1";

    private class JsonTestCase {
        String name;
        MOS6502Test.CPUState before;
        MOS6502Test.CPUState after;
        ArrayList<MOS6502Test.CycleAction> cycles;

        JsonTestCase(JSONObject jsonObject) {
            this.name = jsonObject.getString("name");
            this.before = new MOS6502Test.CPUState(jsonObject.getJSONObject("initial"));
            this.after = new MOS6502Test.CPUState(jsonObject.getJSONObject("final"));

            this.cycles = new ArrayList<MOS6502Test.CycleAction>();
            JSONArray cycleArray = jsonObject.getJSONArray("cycles");
            for (Object c: cycleArray) {
                this.cycles.add(new MOS6502Test.CycleAction((JSONArray) c));
            }
        }
    }

    class CPUState {
        short programCounter;
        byte stackPointer; // S
        byte accumulator;
        byte registerX;
        byte registerY;
        byte status; // P
        byte[] ram;

        CPUState(JSONObject jsonObject) {
            this.programCounter = (short) jsonObject.getInt("pc");
            this.stackPointer = (byte) jsonObject.getInt("s");
            this.accumulator = (byte) jsonObject.getInt("a");
            this.registerX = (byte) jsonObject.getInt("x");
            this.registerY = (byte) jsonObject.getInt("y");
            this.status = (byte) jsonObject.getInt("p");

            this.ram = new byte[0x10000];
            JSONArray jsonRam = jsonObject.getJSONArray("ram");
            for (Object m : jsonRam) {
                MemoryByte mB = new MemoryByte((JSONArray) m);
                this.ram[(int) mB.address & 0xFFFF] = mB.value;
            }
        }
    }

    class MemoryByte {
        short address;
        byte value;

        MemoryByte(JSONArray array) {
            this.address = (short) array.getInt(0);
            this.value = (byte) array.getInt(1);
        }
    }

    class CycleAction {
        short address;
        byte value;
        MOS6502.ReadWrite type;

        CycleAction(JSONArray array) {
            this.address = (short) array.getInt(0);
            this.value = (byte) array.getInt(1);
            if (array.getString(2).equals("read")) {
                this.type = MOS6502.ReadWrite.Read;
            } else if (array.getString(2).equals("write")) {
                this.type = MOS6502.ReadWrite.Write;
            } else {
                throw new IllegalArgumentException("Invalid action in JSON array: " + array.get(2));
            }
        }

        int getAddressAsInt() {
            return this.address & 0xFFFF;
        }

        int getValueAsInt() {
            return this.value & 0xFF;
        }
    }

    /**
     * Created to represent cycle-by-cycle bus action for the CPU.
      */
    public class Log {
        private final short address;
        private final byte value;
        private final MOS6502.ReadWrite action;

        public Log(short address, byte value, MOS6502.ReadWrite action) {
            this.address = address;
            this.value = value;
            this.action = action;
        }

        short getAddress() {
            return this.address;
        }

        int getAddressAsInt() {
            return this.address & 0xFFFF;
        }

        byte getValue() {
            return this.value;
        }

        int getValueAsInt() {
            return this.value & 0xFF;
        }

        MOS6502.ReadWrite getAction() {
            return this.action;
        }

        public String toString() {
            return "Address " + String.format("0x%04X (%d)", this.address, this.getAddressAsInt())
                    + " Value " + String.format("0x%02X (%d)", this.value, this.getValueAsInt())
                    + " in " + this.action + " mode";
        }
    }

    /**
     * Testing support function.
     * Runs one instruction and logs CPU state before, during and after.
     * CPU state must already be set.
     * @param cpu a CPU state to run the instruction on.
     * @return a list of logs with the CPU bus state on each cycle.
     * @throws MOS6502.UnimplementedInstructionException if the instruction hasn't been implemented.
     * @throws MOS6502.IllegalCycleException if the instruction reaches a cycle it's not supposed to.
     */
    ArrayList<Log> runOneInstructionWithLogging(MOS6502 cpu)
            throws MOS6502.UnimplementedInstructionException, MOS6502.IllegalCycleException {
        final ArrayList<Log> logArray = new ArrayList<>();

        // Loads first opcode into data bus
        cpu.setAddressBus(cpu.getProgramCounter());
        cpu.readFromMemory();

        byte opcode = cpu.getDataBus();
        Instruction[] instructionSet = Instruction.initializeInstructionSet();
        Instruction instruction = instructionSet[(int) opcode & 0xFF];

        for (int i = 0; i < instruction.cycles() + cpu.getPageCrossed(); i++) {
            cpu.tick();
            logArray.add(new Log(
                    cpu.getAddressBus(),
                    cpu.getDataBus(),
                    cpu.getReadWritePin()
            ));
        }
        return logArray;
    }

    /**
     * Runs all tests for a single opcode in one of the files in Tom Harte's test suite.
     * @param testFile file handler for the file with all the test instances.
     * @throws FileNotFoundException if the file isn't found.
     * @throws MOS6502.UnimplementedInstructionException if the opcode hasn't been implemented.
     * @throws MOS6502.IllegalCycleException if the instruction reaches a cycle it's not supposed to.
     */
    void testRunInstruction(File testFile)
            throws FileNotFoundException,
            MOS6502.UnimplementedInstructionException,
            MOS6502.IllegalCycleException {
        Scanner reader = new Scanner(testFile);
        reader.nextLine(); // Skips the first line, which only contains '['

        while (reader.hasNextLine()) {
            String line = reader.nextLine();
            try {
                this.runTestCase(line);
            } catch (JSONException e) {
                if (line.equals("]")) {
                    return;
                }
                else {
                    throw e;
                }
            }
        }
        reader.close();
    }

    /**
     * Runs a single test case from Tom Harte's test suite.
     * @param line represents the state of the CPU before and after the test.
     * @throws MOS6502.UnimplementedInstructionException if the instruction hasn't been implemented.
     * @throws MOS6502.IllegalCycleException if the function reaches an illegal cycle.
     */
    void runTestCase(String line)
            throws MOS6502.UnimplementedInstructionException,
            MOS6502.IllegalCycleException {
        JsonTestCase testCase = new JsonTestCase(new JSONObject(line));
        MOS6502 cpu = new MOS6502();

        // Load test case into emulator
        cpu.setProgramCounter(testCase.before.programCounter);
        cpu.setStackPointer(testCase.before.stackPointer);
        cpu.setAccumulator(testCase.before.accumulator);
        cpu.setRegisterX(testCase.before.registerX);
        cpu.setRegisterY(testCase.before.registerY);
        cpu.setStatus(testCase.before.status);

        // Load program into memory
        cpu.load(testCase.before.ram);

        // Runs test case
        ArrayList<Log> logList = runOneInstructionWithLogging(cpu);

        // Comparison
        String message = "Test case " + testCase.name
                + String.format(" of opcode 0x%02X", cpu.getCurrentInstruction().opcode());

        /*
        System.out.println("Logs:");
        for (Log log : logList) {
            System.out.println(log.toString());
        }
         */
        for (int i = 0; i < logList.size(); i++) {
            assertEquals(
                    testCase.cycles.get(i).getAddressAsInt(),
                    logList.get(i).getAddressAsInt(),
                    message + " at address bus at cycle " + (i + 1)
            );
            assertEquals(
                    testCase.cycles.get(i).getValueAsInt(),
                    logList.get(i).getValueAsInt(),
                    message + " at data bus at cycle " + (i + 1)
                            + " " + logList.get(i).getValueAsInt()
            );
            assertEquals(
                    testCase.cycles.get(i).type,
                    logList.get(i).getAction(),
                    message + " at R/W pin at cycle " + (i + 1)
            );
        }

        assertEquals(
                (int) testCase.after.programCounter & 0xFFFF,
                cpu.getProgramCounterAsInt(),
                message + " at program counter"
        );
        assertEquals(
                (int) testCase.after.stackPointer & 0xFF,
                cpu.getStackPointerAsInt(),
                message + " at stack pointer (S)"
        );
        assertEquals(
                (int) testCase.after.accumulator & 0xFF,
                cpu.getAccumulatorAsInt(),
                message + " at accumulator"
        );
        assertEquals(
                (int) testCase.after.registerX & 0xFF,
                cpu.getRegisterXAsInt(),
                message + " at register X"
        );
        assertEquals(
                (int) testCase.after.registerY & 0xFF,
                cpu.getRegisterYAsInt(),
                message + " at register Y"
        );
        assertArrayEquals(
                testCase.after.ram,
                cpu.memory.getRam(),
                message + " at RAM"
        );
    }

    /**
     * Used in the process of developing the CPU.
     * This function runs the tests for all the opcodes listed in its internal array
     * defined at the top of the function body.
     * @throws MOS6502.UnimplementedInstructionException if the opcode hasn't been implemented.
     * @throws FileNotFoundException if the file containing the test suite isn't found.
     * @throws MOS6502.IllegalCycleException if the instruction reaches a cycle it's not supposed to.
     */
    @Test
    void testSomeInstructions()
            throws MOS6502.UnimplementedInstructionException,
            FileNotFoundException,
            MOS6502.IllegalCycleException {
        String[] instructions = {
                "20",
                "48", "08", "68", "28",
                "60", "40",
                "4c", "6c",
                "ca", "88", "c8",
                "90", "b0", "f0", "30", "d0", "10", "50", "70",
                "86", "96", "8e",
                "84", "94", "8c",
                "85", "95", "8d", "9d", "99", "81", "91",
                "c6", "d6", "ce", "de",
                "e6", "f6", "ee", "fe",
                "6a", "66", "76", "6e", "7e",
                "2a", "26", "36", "2e", "3e",
                "a9", "a5", "b5", "ad", "bd", "b9", "a1", "b1",
                "00",
                "4a", "46", "56", "4e", "5e",
                "e0", "e4", "ec",
                "c0", "c4", "cc",
                "18", "d8", "58", "b8",
                "0a", "06", "16", "0e", "1e",
                "ea",
                "24", "2c",
                "c9", "c5", "d5", "cd", "dd", "d9", "c1", "d1",
                "aa", "e8",
                "a2", "a6", "b6", "ae", "be",
                "a0", "a4", "b4", "ac", "bc",
                "49", "45", "55", "4d", "5d", "59", "41", "51",
                "29", "25", "35", "2D", "3D", "39", "21", "31",
                "09", "05", "15", "0d", "1d", "19", "01", "11",
                "69", "65", "75", "6d", "7d", "79", "61", "71",
                "e9", "e5", "f5", "ed", "fd", "f9", "e1", "f1",
        };
        for (String instruction: instructions) {
            System.out.println("Testing opcode 0x" + instruction);
            File testFile = new File(pathToTests + "\\" + instruction + ".json");
            testRunInstruction(testFile);
        }
    }
}