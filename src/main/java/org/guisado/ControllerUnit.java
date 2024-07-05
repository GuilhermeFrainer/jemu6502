package org.guisado;

public class ControllerUnit {
    protected final MOS6502 cpu;
    protected final Memory memory;

    public ControllerUnit() {
        this.cpu = new MOS6502();
        this.memory = new Memory64k();
    }

    /**
     * Executes program in memory
     *
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
     * Loads program in memory.
     * @param program byte array representing program to be loaded in memory.
     */
    public void load(byte[] program) {
        for (int i = 0; i < program.length; i++) {
            this.memory.write(program[i], (short) i);
        }
    }

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
        while (i < instruction.getCycles()) {
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
}