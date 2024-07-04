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
        // Initialize CPU
        byte firstOpcode = this.memory.read(this.cpu.getAddressBus());
        this.cpu.setDataBus(firstOpcode);
        this.cpu.setCurrentInstructionCycle(1);

        while (!this.cpu.breakSign) {
            byte opcode = this.memory.read(this.cpu.getAddressBus());
            this.printCurrentInstruction(opcode, this.cpu.getAddressBus());

            if (this.cpu.getReadWritePin() == CPU.ReadWrite.Read) {
                byte valueAtAddress = this.memory.read(this.cpu.getAddressBus());
                System.out.println("Inserting into data bus: " + String.format("0x%02X", valueAtAddress));
                this.cpu.setDataBus(valueAtAddress);
            } else if (this.cpu.getReadWritePin() == CPU.ReadWrite.Write) {
                byte valueInDataBus = this.cpu.getDataBus();
                short address = this.cpu.getAddressBus();
                this.memory.write(valueInDataBus, address);
            }
            this.cpu.tick();
        }
        System.out.println("Register A: " + String.format("0x%02X", (int) this.cpu.getAccumulator() & 0xFF));
        System.out.println("Register X: " + String.format("0x%02X", (int) this.cpu.getRegisterX() & 0xFF));
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
        System.out.println("Value at address bus address: "
                + String.format("0x%02X", (int) opcode & 0xFF)
                + "\nAddress bus: "
                + String.format("0x%04X", (int) address & 0xFFFF));
    }
}
