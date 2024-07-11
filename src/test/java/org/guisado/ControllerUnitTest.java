package org.guisado;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.io.File;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ControllerUnitTest {
    private static final String pathToTests = "..\\65x02\\6502\\v1";
    private ControllerUnit emulator;

    private class JsonTestCase {
        String name;
        CPUState before;
        CPUState after;
        ArrayList<CycleAction> cycles;

        JsonTestCase(JSONObject jsonObject) {
            this.name = jsonObject.getString("name");
            this.before = new CPUState(jsonObject.getJSONObject("initial"));
            this.after = new CPUState(jsonObject.getJSONObject("final"));

            this.cycles = new ArrayList<CycleAction>();
            JSONArray cycleArray = jsonObject.getJSONArray("cycles");
            for (Object c: cycleArray) {
                this.cycles.add(new CycleAction((JSONArray) c));
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
            for (Object m: jsonRam) {
                MemoryByte mB = new MemoryByte((JSONArray) m);
                this.ram[(int) mB.address & 0xFFFF] = mB.value;
            }
        }

        int getProgramCounterAsInt() {
            return this.programCounter & 0xFFFF;
        }

        int getStackPointerAsInt() {
            return this.stackPointer & 0xFF;
        }

        int getAccumulatorAsInt() {
            return this.accumulator & 0xFF;
        }

        int getRegisterXAsInt() {
            return this.registerX & 0xFF;
        }

        int getRegisterYAsInt() {
            return this.registerY & 0xFF;
        }

        int getStatusAsInt() {
            return this.status & 0xFF;
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

    @Test
    void testReadAndWrite() {
        /**
         * Extracted from test case 00 3f f7.
         */
        int[][] program = {{35714, 0}, {35715, 63}, {35716, 247}, {65534, 212}, {65535, 37}, {9684, 237}};
        ControllerUnit emulator = new ControllerUnit();

        for (int i = 0; i < program.length; i++) {
            short address = (short) program[i][0];
            byte value = (byte) program[i][1];
            //System.out.println("Writing " + value + " at " + address);
            emulator.memory.write(value, address);
        }

        assertEquals(0, emulator.memory.readAsInt((short) 35714));
        assertEquals(63, emulator.memory.readAsInt((short) 35715));
        assertEquals(247, emulator.memory.readAsInt((short) 35716));
        assertEquals(212, emulator.memory.readAsInt((short) 65534));
        assertEquals(37, emulator.memory.readAsInt((short) 65535));
        assertEquals(237, emulator.memory.readAsInt((short) 9684));
    }

    @Test
    void testLoad() {
        byte[] program  = new byte[0x10000];
        program[45930] = (byte) 169;
        program[45931] = (byte) 204;
        program[45932] = (byte) 33;

        ControllerUnit emu = new ControllerUnit();
        emu.load(program);

        assertEquals((byte) 169, emu.memory.read((short) 45930));
        assertEquals((byte) 204, emu.memory.read((short) 45931));
        assertEquals((byte) 33, emu.memory.read((short) 45932));
    }

    //@Test
    void testAllInstructions()
            throws MOS6502.UnimplementedInstructionException,
            FileNotFoundException, MOS6502.IllegalAddressingModeException,
            MOS6502.IllegalCycleException {
        File testDir = new File(pathToTests);
        for (File file: testDir.listFiles())
            testRunInstruction(file);
    }

    @Test
    void testSomeInstructions()
        throws MOS6502.UnimplementedInstructionException,
            FileNotFoundException,
            MOS6502.IllegalCycleException,
            MOS6502.IllegalAddressingModeException {
        String[] instructions = {"00", "a9", "a5", "b5", "ad", "bd", "b9", "a1", "b1",
                                 "aa", "e8"};
        for (String instruction: instructions) {
            System.out.println("Testing opcode 0x" + instruction);
            File testFile = new File(pathToTests + "\\" + instruction + ".json");
            testRunInstruction(testFile);
        }
    }

    void testRunInstruction(File testFile)
            throws FileNotFoundException,
            MOS6502.UnimplementedInstructionException,
            MOS6502.IllegalAddressingModeException,
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

    void runTestCase(String line)
            throws MOS6502.UnimplementedInstructionException,
            MOS6502.IllegalAddressingModeException,
            MOS6502.IllegalCycleException {
        JsonTestCase testCase = new JsonTestCase(new JSONObject(line));
        ControllerUnit emulator = new ControllerUnit();

        // Load test case into emulator
        emulator.cpu.setProgramCounter(testCase.before.programCounter);
        emulator.cpu.setStackPointer(testCase.before.stackPointer);
        emulator.cpu.setAccumulator(testCase.before.accumulator);
        emulator.cpu.setRegisterX(testCase.before.registerX);
        emulator.cpu.setRegisterY(testCase.before.registerY);
        emulator.cpu.setStatus(testCase.before.status);

        // Load program into memory
        emulator.load(testCase.before.ram);

        // Runs test case
        ArrayList<ControllerUnit.Log> logList = emulator.runOneInstructionWithLogging();

        // Comparison
        String message = "Test case " + testCase.name
                + String.format(" of opcode 0x%02X", emulator.cpu.getCurrentInstruction().getOpcode());

        /*
        System.out.println("Logs:");
        for (int i = 0; i < logList.size(); i++) {
            System.out.println(logList.get(i).toString());
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
                emulator.cpu.getProgramCounterAsInt(),
                message + " at program counter"
        );
        assertEquals(
                (int) testCase.after.stackPointer & 0xFF,
                emulator.cpu.getStackPointerAsInt(),
                message + " at stack pointer (S)"
        );
        assertEquals(
                (int) testCase.after.accumulator & 0xFF,
                emulator.cpu.getAccumulatorAsInt(),
                message + " at accumulator"
        );
        assertEquals(
                (int) testCase.after.registerX & 0xFF,
                emulator.cpu.getRegisterXAsInt(),
                message + " at register X"
        );
        assertEquals(
                (int) testCase.after.registerY & 0xFF,
                emulator.cpu.getRegisterYAsInt(),
                message + " at register Y"
        );
        assertArrayEquals(
                testCase.after.ram,
                emulator.memory.getRam(),
                message + " at RAM"
        );
    }
}