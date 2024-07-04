package org.guisado;

/**
 * Represents the CPU.
 * Registers should all be interpreted as unsigned 8-bit ints.
 */
public class CPU {
    // Status register flags
    private static final byte NEGATIVE = (byte) 0b10000000;
    private static final byte OVERFLOW = 0b01000000;
    /**
     * Called "expansion" bit in the official documentation.
     */
    private static final byte EXPANSION = 0b00100000;
    private static final byte BREAK = 0b00010000;
    private static final byte DECIMAL = 0b00001000;
    private static final byte INTERRUPT_DISABLE = 0b00000100;
    private static final byte ZERO = 0b00000010;
    private static final byte CARRY = 0b00000001;

    private static final Instruction[] instructionSet = Instruction.initializeInstructionSet();

    // Registers
    private short programCounter;
    private byte stackPointer;
    private byte status;
    private byte accumulator;
    private byte registerX;
    private byte registerY;

    public enum ReadWrite {
        Read,
        Write;
    }

    // Bus
    private ReadWrite readWritePin;
    private byte dataBus;
    private short addressBus;
    public boolean breakSign;

    public ReadWrite getReadWritePin() {
        return this.readWritePin;
    }

    public byte getDataBus() {
        return dataBus;
    }

    public short getAddressBus() {
        return this.addressBus;
    }

    public void setDataBus(byte number) {
        this.dataBus = number;
    }

    // Abstractions
    private Instruction currentInstruction;
    private int currentInstructionCycle;
    private int currentCycle;

    public void setCurrentInstructionCycle(int cycle) {
        this.currentInstructionCycle = cycle;
    }

    /* ==================
     * SPECIAL EXCEPTIONS
     ==================== */

    /**
     * Used when an instruction receives an addressing mode that isn't implemented for it.
     */
    public class IllegalAddressingModeException extends Exception {
        private IllegalAddressingModeException() {}
        private IllegalAddressingModeException(String message) {
            super(message);
        }
        private IllegalAddressingModeException(CPU cpu) {
            super(
              "Instruction " + cpu.currentInstruction.getMnemonic() +
                      " received unimplemented addressing mode " +
                      cpu.currentInstruction.getAddressingMode().toString());
        }
    }

    /**
     * Used when an exception reaches a cycle it's not meant to.
     * E.g. an instruction which is supposed to run for 5 cycles runs for 6.
     */
    public class IllegalCycleException extends Exception {
        private IllegalCycleException() {}
        private IllegalCycleException(String message) {
            super(message);
        }
        private IllegalCycleException(CPU cpu) {
            super("Instruction " + cpu.currentInstruction.getMnemonic() +
                    " with opcode " +
                    String.format("0x%02X", cpu.currentInstruction.getOpcode()) +
                    " accepts at most " + cpu.currentInstruction.getCycles() +
                    " but received " + cpu.currentInstructionCycle);
        }
    }

    public class UnimplementedInstructionException extends Exception {
        public UnimplementedInstructionException() {}

        public UnimplementedInstructionException(String message) {
            super(message);
        }
    }

    public CPU() {
        this.programCounter = 0;
        this.stackPointer = 0;
        this.status = 0;
        this.accumulator = 0;
        this.registerX = 0;
        this.registerY = 0;
        this.currentInstruction = null;
        this.dataBus = 0;
        this.addressBus = 0;
        this.currentInstructionCycle = 1;
        this.currentCycle = 0;
        this.breakSign = false;
        this.readWritePin = ReadWrite.Read;
    }

    public byte getAccumulator() {
        return this.accumulator;
    }

    public byte getRegisterX() {
        return this.registerX;
    }

    /**
     * Fetches the instruction from the 'instructionSet' based on opcode
     * currently in the data bus.
     * @return the fetched instruction.
     */
    private Instruction fetchInstruction() {
        return instructionSet[(int) this.dataBus & 0xFF];
    }

    /* ================
     * MEMORY FUNCTIONS
     ================== */


    /* =========================
     * SET STATUS FLAG FUNCTIONS
     =========================== */

    private void setNegative() {this.status |= NEGATIVE;}

    private void setOverflow() {this.status |= OVERFLOW;}

    private void setExpansion() {this.status |= EXPANSION;}

    private void setBreak() {this.status |= BREAK;}

    private void setDecimal() {this.status |= DECIMAL;}

    private void setInterruptDisable() {this.status |= INTERRUPT_DISABLE;}

    private void setZero() {this.status |= ZERO;}

    private void setCarry() {this.status |= CARRY;}

    /* ===========================
     * UNSET STATUS FLAG FUNCTIONS
     ============================= */

    private void unsetNegative() {this.status &= ~NEGATIVE;}

    private void unsetOverflow() {this.status &= ~OVERFLOW;}

    private void unsetExpansion() {this.status &= ~EXPANSION;}

    private void unsetBreak() {this.status &= ~BREAK;}

    private void unsetDecimal() {this.status &= ~DECIMAL;}

    private void unsetInterruptDisable() {this.status &= ~INTERRUPT_DISABLE;}

    private void unsetZero() {this.status &= ~ZERO;}

    private void unsetCarry() {this.status &= ~CARRY;}

    /**
     * Executes the next cycle.
     */
    public void tick()
            throws UnimplementedInstructionException, IllegalAddressingModeException, IllegalCycleException {
        System.out.println("Current instruction cycle: " + this.currentInstructionCycle);
        System.out.println("Current cycle: " + this.currentCycle);
        System.out.println("Accumulator: " + String.format("0x%02X", this.accumulator));
        System.out.println("Register X: " + String.format("0x%02X", this.registerX));
        /* The first cycle in every single instruction
         * consists of fetching the next opcode and incrementing the program counter.
         */
        if (this.currentInstructionCycle == 1) {
            System.out.println("Fetching instruction: " + this.fetchInstruction().getMnemonic());
            this.currentInstruction = this.fetchInstruction();
            this.programCounter++;
            this.addressBus = this.programCounter;
        }

        if (this.currentInstruction == null) {
            throw new UnimplementedInstructionException("Unimplemented opcode: "
                    + String.format("0x%02X", (int) this.dataBus & 0xFF));
        }

        if (this.currentInstructionCycle > 1) {
            System.out.println("Running instruction: " + this.currentInstruction.getMnemonic());
            switch ((int) this.currentInstruction.getOpcode() & 0xFF) {
                case 0x00 -> {
                    System.out.println("CPU IS GOING FOR A HALT NOW.");
                    this.breakSign = true;
                }
                case 0xE8 -> this.inx();
                case 0xA9 -> this.lda();
                case 0xAA -> this.tax();
                default -> {
                    throw new UnimplementedInstructionException(
                            "Instruction not implemented: " + this.currentInstruction.getOpcode());
                }
            }
        }
        // Checks if the current instruction has finished running.
        if (this.currentInstructionCycle == this.currentInstruction.getCycles()) {
            this.addressBus = this.programCounter;
            this.currentInstructionCycle = 0;
            this.readWritePin = ReadWrite.Read;
        }
        this.currentInstructionCycle++;
        this.currentCycle++;
        System.out.println("***********\n");
    }

    /* =====================
     * INSTRUCTION FUNCTIONS
     ======================= */


    /* ======================
     * INCREMENT INSTRUCTIONS
     ======================== */

    private void inx() throws IllegalCycleException {
        this.genericImpliedAddressing();
        if (this.currentInstructionCycle == this.currentInstruction.getCycles()) {
            this.registerX++;
            this.updateZeroAndNegativeFlags(this.registerX);
        }
    }

    /* =================
     * LOAD INSTRUCTIONS
     =================== */

    /**
     * Loads byte into accumulator register
     * @throws IllegalAddressingModeException in case the instruction has an unimplemented addressing mode.
     * @throws IllegalCycleException in case the instruction reaches an unimplemented cycle.
     */
    private void lda() throws IllegalAddressingModeException, IllegalCycleException {
        switch (this.currentInstruction.getAddressingMode()) {
            case Immediate -> this.genericImmediateAddressing();
            default -> throw new IllegalAddressingModeException(this);
        }
        if (this.currentInstructionCycle == this.currentInstruction.getCycles()) {
            this.accumulator = this.dataBus;
            this.updateZeroAndNegativeFlags(this.accumulator);
        }
    }

    /* =====================
     * TRANSFER INSTRUCTIONS
     ======================= */

    /**
     * Transfers accumulator to register X.
     * Updates zero and negative flags if the new value for register X
     * is either zero or negative respectively.
     * @throws IllegalCycleException in case the function reaches an unimplemented cycle.
     */
    private void tax() throws IllegalCycleException {
        this.genericImpliedAddressing();
        if (this.currentInstructionCycle == this.currentInstruction.getCycles()) {
            this.registerX = this.accumulator;
            this.updateZeroAndNegativeFlags(this.registerX);
        }
    }

    /* =============================
     * GENERIC INSTRUCTION FUNCTIONS
     =============================== */

    /* At each case, the command above it between quotation marks
     * are extracted from this cycle-by-cycle guide of the NES dev wiki:
     * https://www.nesdev.org/6502_cpu.txt
     */


    /**
     * Function to generically represent the cycle-to-cycle behavior of the CPU
     * for instructions with implied addressing mode.
     * @throws IllegalCycleException in case the instruction reaches and unimplemented cycle.
     */
    private void genericImpliedAddressing() throws IllegalCycleException {
        switch (this.currentInstructionCycle) {
            // "Read next instruction byte (and throw it away)"
            case 2 -> {
                this.addressBus = this.programCounter;
            }
            default -> throw new IllegalCycleException(this);
        }
    }

    private void genericImmediateAddressing() throws IllegalCycleException {
        switch (this.currentInstructionCycle) {
            // "Fetch value, increment PC"
            case 2 -> {
                this.addressBus = this.programCounter;
                this.programCounter++;
            }
            default -> throw new IllegalCycleException("This instruction accepts at most "
                    + this.currentInstruction.getCycles() + ", received " + this.currentInstructionCycle);
        }
    }

    /* ================
     * HELPER FUNCTIONS
     ================== */

    /**
     * Sets zero flag if 'number' == 0,
     * otherwise sets negative flag if 'number' < 0.
     * @param number number based on which the flags will be updated.
     *               Generally a register.
     */
    private void updateZeroAndNegativeFlags(byte number) {
        if (number == 0) {
            this.setZero();
        }
        else {
            this.unsetZero();
        }
        // Reminder: Java's 'byte' types are signed
        if (number < 0) {
            this.setNegative();
        }
        else {
            this.unsetNegative();
        }
    }
}
