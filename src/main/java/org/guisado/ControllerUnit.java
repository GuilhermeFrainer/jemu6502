package org.guisado;

import java.util.ArrayList;

public class ControllerUnit {
    protected final MOS6502 cpu;
    protected final Memory memory;

    public ControllerUnit() {
        this.cpu = new MOS6502();
        this.memory = new Memory64k();
    }

    /**
     * Executes program in memory
     * @throws MOS6502.IllegalCycleException
     * @throws MOS6502.IllegalAddressingModeException
     * @throws MOS6502.UnimplementedInstructionException
     */
    public void run()
            throws MOS6502.IllegalCycleException,
            MOS6502.IllegalAddressingModeException,
            MOS6502.UnimplementedInstructionException {
        // Initialize MOS6502
        byte firstOpcode = this.memory.read(this.cpu.getAddressBus());
        this.cpu.init(firstOpcode);

        while (!this.cpu.breakSign) {
            if (this.cpu.getReadWritePin() == MOS6502.ReadWrite.Read) {
                byte valueAtAddress = this.memory.read(this.cpu.getAddressBus());
                this.cpu.setDataBus(valueAtAddress);
            } else if (this.cpu.getReadWritePin() == MOS6502.ReadWrite.Write) {
                byte valueInDataBus = this.cpu.getDataBus();
                short address = this.cpu.getAddressBus();
                this.memory.write(valueInDataBus, address);
            }
            this.cpu.tick();
        }
    }

    /**
     * Loads program into memory.
     * @param program byte array representing program to be loaded in memory.
     */
    public void load(byte[] program) {
        for (int i = 0; i < program.length; i++) {
            this.memory.write(program[i], (short) i);
        }
    }

    /**
     * Runs a single instruction.
     * As of now, exists solely for testing.
     * @throws MOS6502.IllegalCycleException
     * @throws MOS6502.IllegalAddressingModeException
     * @throws MOS6502.UnimplementedInstructionException
     */
    protected void runOneInstruction()
            throws MOS6502.IllegalCycleException,
            MOS6502.IllegalAddressingModeException,
            MOS6502.UnimplementedInstructionException {
        // Initialize MOS6502
        byte opcode = this.memory.read(this.cpu.getAddressBus());
        Instruction[] instructionSet = Instruction.initializeInstructionSet();
        Instruction instruction = instructionSet[(int) opcode & 0xFF];
        this.cpu.init(opcode);

        int i = 0;
        while (i < instruction.cycles()) {
            if (this.cpu.getReadWritePin() == MOS6502.ReadWrite.Read) {
                byte valueAtAddress = this.memory.read(this.cpu.getAddressBus());
                this.cpu.setDataBus(valueAtAddress);
            } else if (this.cpu.getReadWritePin() == MOS6502.ReadWrite.Write) {
                byte valueInDataBus = this.cpu.getDataBus();
                short address = this.cpu.getAddressBus();
                this.memory.write(valueInDataBus, address);
            }
            this.cpu.tick();
            i++;
        }
    }

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
     * Runs one instruction and returns array of object containing the state
     * of the address and data buses and of the read/write pin cycle-by-cycle.
     * Exists solely for testing.
     * @return ArrayList containing cycle-by-cycle behavior for the R/W, address and data pins.
     * @throws MOS6502.UnimplementedInstructionException in case an unimplemented instruction is run.
     * @throws MOS6502.IllegalCycleException in case an instruction reaches a cycle it's not supposed to.
     */
    protected ArrayList<Log> runOneInstructionWithLogging()
            throws MOS6502.UnimplementedInstructionException,
            MOS6502.IllegalCycleException {
        ArrayList<Log> logArray = new ArrayList<>();

        // Loads first instruction into data bus
        this.cpu.setAddressBus(this.cpu.getProgramCounter());
        this.cpu.setDataBus(this.memory.read(this.cpu.getProgramCounter()));

        byte opcode = this.cpu.getDataBus();
        Instruction[] instructionSet = Instruction.initializeInstructionSet();
        Instruction instruction = instructionSet[(int) opcode & 0xFF];

        for (int i = 0; i < instruction.cycles() + this.cpu.getPageCrossed(); i++) {
            this.cpu.tick();
            if (this.cpu.getReadWritePin() == MOS6502.ReadWrite.Read) {
                final byte valueAtAddress = this.memory.read(this.cpu.getAddressBus());
                this.cpu.setDataBus(valueAtAddress);
            } else if (this.cpu.getReadWritePin() == MOS6502.ReadWrite.Write) {
                final byte valueInDataBus = this.cpu.getDataBus();
                final short address = this.cpu.getAddressBus();
                this.memory.write(valueInDataBus, address);
            }
            logArray.add(new Log(
                    this.cpu.getAddressBus(),
                    this.cpu.getDataBus(),
                    this.cpu.getReadWritePin()
            ));
        }
        // A last read/write is needed to update the data and address pins
        if (this.cpu.getReadWritePin() == MOS6502.ReadWrite.Read) {
            final byte valueAtAddress = this.memory.read(this.cpu.getAddressBus());
            this.cpu.setDataBus(valueAtAddress);
        } else if (this.cpu.getReadWritePin() == MOS6502.ReadWrite.Write) {
            final byte valueInDataBus = this.cpu.getDataBus();
            final short address = this.cpu.getAddressBus();
            this.memory.write(valueInDataBus, address);
        }
        // Read instructions are executed at the end
        //this.cpu.executeOpcode(this.cpu.getCurrentInstruction().opcode());
        return logArray;
    }
}