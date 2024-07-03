package org.guisado;

public class ControllerUnit {
    private final CPU cpu;
    private final Memory memory;

    public ControllerUnit() {
        this.cpu = new CPU();
        this.memory = new Memory64k();
    }

    /**
     * Executes program in memory
     * @throws CPU.IllegalCycleException
     * @throws CPU.IllegalAddressingModeException
     * @throws CPU.UnimplementedInstructionException
     */
    public void run()
            throws CPU.IllegalCycleException,
            CPU.IllegalAddressingModeException,
            CPU.UnimplementedInstructionException {
        // Loads first instruction
        this.cpu.setFirstInstruction(this.memory.read((short) 0));

        while (!this.cpu.breakSign) {
            byte opcode = this.memory.read(this.cpu.getAddressBus());
            this.printCurrentInstruction(opcode, this.cpu.getAddressBus());

            if (this.cpu.getReadWritePin() == CPU.ReadWrite.Read) {
                byte valueAtAddress = this.memory.read(this.cpu.getAddressBus());
                this.cpu.setDataBus(valueAtAddress);
            } else if (this.cpu.getReadWritePin() == CPU.ReadWrite.Write) {
                byte valueInDataBus = this.cpu.getDataBus();
                short address = this.cpu.getAddressBus();
                this.memory.write(valueInDataBus, address);
            }
            this.cpu.tick();
        }
        System.out.println("Register A: " + ((int) this.cpu.getAccumulator() & 0xFF));
        System.out.println("Register X: " + ((int) this.cpu.getRegisterX() & 0xFF));
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

    private void printCurrentInstruction(byte opcode, short address) {
        System.out.println("Fetching instruction "
                + String.format("0x%02X", (int) opcode & 0xFF)
                + " at "
                + String.format("0x%04X", (int) address & 0xFF));
    }
}
