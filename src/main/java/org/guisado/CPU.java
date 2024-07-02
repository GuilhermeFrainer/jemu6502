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

    // Bus
    // private Memory memory;
    /**
     * True for read,
     * false for write.
     */
    // MAKE THIS AN ENUM
    private boolean readWrite;
    private byte dataBus;
    private short addressBus;

    public boolean getReadWrite() {
        return this.readWrite;
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

    /* probably useless
    public void setAddressBus(short address) {
        this.addressBus = address;
    }
    */

    // Abstractions
    private Instruction currentInstruction;
    private int currentInstructionCycle;
    private int currentCycle;

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
        this.currentInstructionCycle = 0;
        this.currentCycle = 0;
        //this.memory = new Memory64k();
    }

    public byte getAccumulator() {
        return this.accumulator;
    }

    public byte getRegisterX() {
        return this.registerX;
    }

    /**
     * Runs the program loaded in memory.
     */
    public void run() throws UnimplementedInstructionException, IllegalAddressingModeException, IllegalCycleException {
        do {
            if (this.currentInstruction == null || this.instructionFinishedRunning()) {
                System.out.println("Fetching instruction " + ((int) this.readAtProgramCounter() & 0xFF) + " at " + this.programCounter);
                this.currentInstruction = this.fetchInstruction(this.readAtProgramCounter());

                this.currentInstructionCycle = 1;
            }
            this.interpret();
        } while (this.currentInstruction.getOpcode() != 0x00);
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

    /**
     * Fetches instruction at 'instructionSet'.
     * @param opcode opcodes are used as indexes in the array.
     * @return the fetched instruction.
     */
    private Instruction fetchInstruction(byte opcode) {
        return instructionSet[(int) opcode & 0xFF];
    }

    /**
     * Reads memory at the address stored in the program counter register
     * and returns the value therein.
     * @return byte stored at 'programCounter'.
     */
    private byte readAtProgramCounter() {
        return this.memory.read(this.programCounter);
    }

    /**
     * Interprets the current instruction.
     */
    private void interpret()
            throws UnimplementedInstructionException, IllegalAddressingModeException, IllegalCycleException {
        switch ((int) this.currentInstruction.getOpcode() & 0xFF) {
            case 0x00 -> {
                return;
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

    /* =====================
     * INSTRUCTION FUNCTIONS
     ======================= */


    /* ======================
     * INCREMENT INSTRUCTIONS
     ======================== */

    private void inx() throws IllegalCycleException {
        this.genericImpliedAddressing();
        if (this.instructionFinishedRunning()) {
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
        if (this.instructionFinishedRunning()) {
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
        if (this.instructionFinishedRunning()) {
            this.registerX = this.accumulator;
            this.updateZeroAndNegativeFlags(this.registerX);
        }
    }

    /* =============================
     * GENERIC INSTRUCTION FUNCTIONS
     =============================== */

    /**
     * Function to generically represent the cycle-to-cycle behavior of the CPU
     * for instructions with implied addressing mode.
     * @throws IllegalCycleException in case the instruction reaches and unimplemented cycle.
     */
    private void genericImpliedAddressing() throws IllegalCycleException {
        switch (this.currentInstructionCycle) {
            case 1 -> {
                this.addressBus = this.programCounter;
                this.dataBus = this.currentInstruction.getOpcode();
                this.programCounter++;
                this.incrementCycles();
            }
            case 2 -> {
                this.addressBus = this.programCounter;
                this.programCounter++;
                // Fetches next OP code and ignores it
                this.dataBus = this.readAtProgramCounter();
                this.incrementCycles();
            }
            default -> throw new IllegalCycleException(this);
        }
    }

    private void genericImmediateAddressing() throws IllegalCycleException {
        switch (this.currentInstructionCycle) {
            case 1 -> {
                this.addressBus = this.programCounter;
                this.dataBus = this.currentInstruction.getOpcode();
                this.programCounter++;
                this.incrementCycles();
            }
            case 2 -> {
                this.dataBus = this.readAtProgramCounter();
                this.addressBus = this.programCounter;
                this.programCounter++;
                this.incrementCycles();
            }
            default -> throw new IllegalCycleException("This instruction accepts at most "
            + this.currentInstruction.getCycles() + ", received " + this.currentInstructionCycle);
        }
    }

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

    /* ================
     * HELPER FUNCTIONS
     ================== */

    /**
     * Checks if the instruction has finished running (i.e. it's past its last cycle)
     * This is important cause instructions which use generic functions will perform its action
     * only in the last cycle.
     * @return true if current instruction is in its last cycle, false otherwise.
     */
    private boolean instructionFinishedRunning() {
        if (this.currentInstructionCycle == this.currentInstruction.getCycles() + 1) return true;
        else return false;
    }

    /**
     * Increments both the overall cycle counter and the current instruction cycle counter.
     */
    private void incrementCycles() {
        this.currentInstructionCycle++;
        this.currentCycle++;
    }

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
