package org.guisado;

/**
 * Represents the MOS6502.
 * Registers should all be interpreted as unsigned 8-bit ints.
 */
public class MOS6502 {
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

    private static final short STACK_PAGE = 0x100;
    private static final byte STARTING_STACK_POINTER = (byte) 0xFF;
    private static final short IRQ_VECTOR_LOW = (short) 0xFFFE;
    private static final short IRQ_VECTOR_HIGH = (short) 0xFFFF;

    private static final Instruction[] instructionSet = Instruction.initializeInstructionSet();

    // Registers
    private short programCounter;
    private byte stackPointer;
    private byte status;
    private byte accumulator;
    private byte registerX;
    private byte registerY;

    /* Getters and setters for the CPU register fields.
     * For now, they're only used for testing purposes.
     */

    protected short getProgramCounter() {
        return this.programCounter;
    }

    protected int getProgramCounterAsInt() {
        return (int) this.programCounter & 0xFFFF;
    }

    protected void setProgramCounter(short address) {
        this.programCounter = address;
    }

    protected byte getStackPointer() {
        return this.stackPointer;
    }

    protected int getStackPointerAsInt() {
        return (int) this.stackPointer & 0xFF;
    }

    protected void setStackPointer(byte value) {
        this.stackPointer = value;
    }

    protected byte getStatus() {
        return this.status;
    }

    protected int getStatusAsInt() {
        return (int) this.status & 0xFF;
    }

    protected void setStatus(byte value) {
        this.status = value;
    }

    protected byte getAccumulator() {
        return this.accumulator;
    }

    protected int getAccumulatorAsInt() {
        return (int) this.accumulator & 0xFF;
    }

    protected void setAccumulator(byte value) {
        this.accumulator = value;
    }

    protected byte getRegisterX() {
        return this.registerX;
    }

    protected int getRegisterXAsInt() {
        return (int) this.registerX & 0xFF;
    }

    protected void setRegisterX(byte value) {
        this.registerX = value;
    }

    protected byte getRegisterY() {
        return this.registerY;
    }

    protected int getRegisterYAsInt() {
        return (int) this.registerY & 0xFF;
    }

    protected void setRegisterY(byte value) {
        this.registerY = value;
    }

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

    public short getAddressBus() {
        return this.addressBus;
    }

    public void setAddressBus(short value) {
        this.addressBus = value;
    }

    public byte getDataBus() {
        return dataBus;
    }

    public void setDataBus(byte number) {
        this.dataBus = number;
    }

    // Abstractions
    private Instruction currentInstruction;
    private int currentInstructionCycle;
    private int currentCycle;

    protected Instruction getCurrentInstruction() {
        return this.currentInstruction;
    }

    public void setCurrentInstructionCycle(int cycle) {
        this.currentInstructionCycle = cycle;
    }

    public int getCurrentCycle() {
        return this.currentCycle;
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
        private IllegalAddressingModeException(MOS6502 cpu) {
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
        private IllegalCycleException(MOS6502 cpu) {
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

    public MOS6502() {
        this.programCounter = 0;
        this.stackPointer = 0;
        this.status = STARTING_STACK_POINTER;
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

    /**
     * Initializes MOS6502, preparing it to run the program.
     * @param opcode Opcode of the first instruction.
     */
    public void init(byte opcode) {
        this.dataBus = opcode;
        this.currentInstructionCycle = 1;
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

    /* ===============
     * STACK FUNCTIONS
     ================= */

    /**
     * Pushes value onto the stack.
     * @param value Value to be pushed.
     */
    private void stackPush(byte value) {
        this.addressBus = (short) (STACK_PAGE + this.stackPointer);
        this.dataBus = value;
        this.readWritePin = ReadWrite.Write;
        this.stackPointer--;
    }

    /**
     * Pops value from the stack.
     * Value will be written to the data bus.
     */
    private void stackPop() {
        this.stackPointer++;
        this.addressBus = (short) (STACK_PAGE + this.stackPointer);
        this.readWritePin = ReadWrite.Read;
    }

    /**
     * Executes the next cycle.
     */
    public void tick()
            throws UnimplementedInstructionException, IllegalAddressingModeException, IllegalCycleException {
        /* The first cycle in every single instruction
         * consists of fetching the next opcode and incrementing the program counter.
         */
        if (this.currentInstructionCycle == 1) {
            this.currentInstruction = this.fetchInstruction();
            this.programCounter++;
            this.addressBus = this.programCounter;
        }

        if (this.currentInstruction == null) {
            throw new UnimplementedInstructionException("Unimplemented opcode: "
                    + String.format("0x%02X", (int) this.dataBus & 0xFF));
        }

        if (this.currentInstructionCycle > 1) {
            switch ((int) this.currentInstruction.getOpcode() & 0xFF) {
                case 0x00 -> this.brk();
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
    }

    /* =====================
     * INSTRUCTION FUNCTIONS
     ======================= */

    /* =====
     * BREAK
     ======= */

    /**
     * Forces interrupt request. PC and processor status are pushed to the stack
     * and IRQ interrupt vector at $FFFE/F is loaded into PC and the break flag
     * in the status is set to one.
     */
    private void brk() throws IllegalCycleException {
        switch (this.currentInstructionCycle) {
            /* "Read next instruction byte and throw it away,
             * increment PC."
             */
            case 2 -> {
                this.programCounter++;
            }
            // "Push PCH on stack (with B flag set), decrement S"
            case 3 -> {
                this.setBreak();
                this.stackPush((byte) (this.programCounter >> 8));
            }
            // "Push PCL on stack, decrement S"
            case 4 -> {
                this.stackPush((byte) (this.programCounter & 0xFF));
            }
            // "Push P on stack, decrement S"
            case 5 -> {
                this.stackPush(this.status);
            }
            // "Fetch PCL"
            case 6 -> {
                this.addressBus = IRQ_VECTOR_LOW;
                this.readWritePin = ReadWrite.Read;
            }
            // "Fetch PCH"
            case 7 -> {
                this.addressBus = IRQ_VECTOR_HIGH;
            }
            default -> throw new IllegalCycleException(this);
        }
    }


    /* ======================
     * INCREMENT INSTRUCTIONS
     ======================== */

    /**
     * Adds one to register X and sets the zero and negative flags as appropriate.
     * @throws IllegalCycleException
     */
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
     * Loads byte into accumulator register.
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
     * Function to generically represent the cycle-to-cycle behavior of the MOS6502
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
