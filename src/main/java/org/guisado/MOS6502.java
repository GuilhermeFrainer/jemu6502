package org.guisado;

/**
 * Represents the MOS6502.
 * Registers should all be interpreted as unsigned 8-bit ints.
 */
public class MOS6502 {
    // Status register flags
    static final byte NEGATIVE = (byte) 0b10000000;
    static final byte OVERFLOW = 0b01000000;
    /**
     * Called "expansion" bit in the official documentation.
     */
    static final byte EXPANSION = 0b00100000;
    static final byte BREAK = 0b00010000;
    static final byte DECIMAL = 0b00001000;
    static final byte INTERRUPT_DISABLE = 0b00000100;
    static final byte ZERO = 0b00000010;
    static final byte CARRY = 0b00000001;

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

    protected void setProgramCounter(int address) {
        this.programCounter = (short) address;
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

    protected void setStackPointer(int value) {
        this.stackPointer = (byte) value;
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

    protected void setStatus(int value) {
        this.status = (byte) value;
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

    protected void setAccumulator(int value) {
        this.accumulator = (byte) value;
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

    protected void setRegisterX(int value) {
        this.registerX = (byte) value;
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

    protected void setRegisterY(int value) {
        this.registerY = (byte) value;
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

    public void setDataBus(int number) {
        this.dataBus = (byte) number;
    }

    // Abstractions
    private Instruction currentInstruction;
    private int currentInstructionCycle;
    private int currentCycle;

    /*
     * Stupid abstraction necessitated by two facts:
     * 1. Java doesn't have static variables like in C;
     * 2. When fetching the address in absolute addressing, we have to store the first (low) byte
     * of the address somewhere, and it can't be in the address bus, since we need to read the
     * high byte of the new address.
     * This *shouldn't* be too big of a problem, since it *should* be written to in one cycle
     * and read on the next one.
     */
    private byte retainedByte;

    /*
     * Wonky abstraction necessitated by the fact that instructions which page cross
     * need to run for an additional cycle.
     * Should be 0 most of the time, set to 1 when a page cross is detected,
     * then set back to 0 after the instruction is finished running.
     */
    private int pageCrossed;

    int getPageCrossed() {
        return this.pageCrossed;
    }

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
              "Instruction " + cpu.currentInstruction.mnemonic() +
                      " received unimplemented addressing mode " +
                      cpu.currentInstruction.addressingMode().toString());
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
            super("Instruction " + cpu.currentInstruction.mnemonic() +
                    " with opcode " +
                    String.format("0x%02X", cpu.currentInstruction.opcode()) +
                    " accepts at most " + cpu.currentInstruction.cycles() +
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
        this.stackPointer = STARTING_STACK_POINTER;
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
        this.pageCrossed = 0;
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
     * @param opcode the opcode of the instruction to be fetched.
     * @throws UnimplementedInstructionException in case the instruction isn't in the array.
     */
    private Instruction fetchInstruction(byte opcode) throws UnimplementedInstructionException {
        Instruction instruction = instructionSet[(int) opcode & 0xFF];
        if (instruction == null) {
            throw new UnimplementedInstructionException("Unimplemented opcode: "
                    + String.format("0x%02X", (int) opcode & 0xFF));
        } else {
            return instruction;
        }
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
    protected void stackPush(byte value) {
        // Must convert stack pointer to positive int
        this.addressBus = (short) (STACK_PAGE + (this.stackPointer & 0xFF));
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
            // Executes the previous instruction before fetching the new one
            try {
                this.executeOpcode(this.currentInstruction.opcode());
            } catch (NullPointerException e) {
                /* Ignores the exception if this is the first cycle,
                 * as there wouldn't be a "previous" instruction in this situation
                 */
                if (this.currentCycle != 0) {
                    throw e;
                }
            }
            this.currentInstruction = this.fetchInstruction(this.dataBus);
            this.addressBus = this.programCounter;
            this.programCounter++;
        } else if (this.currentInstructionCycle > 1) {
            switch ((int) this.currentInstruction.opcode() & 0xFF) {
                // ADC
                case 0x69 -> this.genericImmediateAddressing();
                case 0x65 -> this.zeroPageReadInstruction();
                case 0x75 -> this.zeroPageIndexedReadInstruction(this.registerX);
                case 0x6D -> this.absoluteReadInstruction();
                case 0x7D -> this.absoluteIndexedReadInstruction(this.registerX);
                case 0x79 -> this.absoluteIndexedReadInstruction(this.registerY);
                case 0x61 -> this.indexedIndirectReadInstruction();
                case 0x71 -> this.indirectIndexedReadInstruction();

                // AND
                case 0x29 -> this.genericImmediateAddressing();
                case 0x25 -> this.zeroPageReadInstruction();
                case 0x35 -> this.zeroPageIndexedReadInstruction(this.registerX);
                case 0x2D -> this.absoluteReadInstruction();
                case 0x3D -> this.absoluteIndexedReadInstruction(this.registerX);
                case 0x39 -> this.absoluteIndexedReadInstruction(this.registerY);
                case 0x21 -> this.indexedIndirectReadInstruction();
                case 0x31 -> this.indirectIndexedReadInstruction();

                // BRK
                case 0x00 -> this.brkCycleByCycle();

                // EOR
                case 0x49 -> this.genericImmediateAddressing();
                case 0x45 -> this.zeroPageReadInstruction();
                case 0x55 -> this.zeroPageIndexedReadInstruction(this.registerX);
                case 0x4D -> this.absoluteReadInstruction();
                case 0x5D -> this.absoluteIndexedReadInstruction(this.registerX);
                case 0x59 -> this.absoluteIndexedReadInstruction(this.registerY);
                case 0x41 -> this.indexedIndirectReadInstruction();
                case 0x51 -> this.indirectIndexedReadInstruction();

                // INCREMENT INSTRUCTIONS

                // INX
                case 0xE8 -> this.genericImpliedAddressing();

                // LOAD INSTRUCTIONS

                // LDA
                case 0xA9 -> this.genericImmediateAddressing();
                case 0xA5 -> this.zeroPageReadInstruction();
                case 0xB5 -> this.zeroPageIndexedReadInstruction(this.registerX);
                case 0xAD -> this.absoluteReadInstruction();
                case 0xBD -> this.absoluteIndexedReadInstruction(this.registerX);
                case 0xB9 -> this.absoluteIndexedReadInstruction(this.registerY);
                case 0xA1 -> this.indexedIndirectReadInstruction();
                case 0xB1 -> this.indirectIndexedReadInstruction();

                // LDX
                case 0xA2 -> this.genericImmediateAddressing();
                case 0xA6 -> this.zeroPageReadInstruction();
                case 0xB6 -> this.zeroPageIndexedReadInstruction(this.registerY);
                case 0xAE -> this.absoluteReadInstruction();
                case 0xBE -> this.absoluteIndexedReadInstruction(this.registerY);

                // LDY
                case 0xA0 -> this.genericImmediateAddressing();
                case 0xA4 -> this.zeroPageReadInstruction();
                case 0xB4 -> this.zeroPageIndexedReadInstruction(this.registerX);
                case 0xAC -> this.absoluteReadInstruction();
                case 0xBC -> this.absoluteIndexedReadInstruction(this.registerX);

                // ORA
                case 0x09 -> this.genericImmediateAddressing();
                case 0x05 -> this.zeroPageReadInstruction();
                case 0x15 -> this.zeroPageIndexedReadInstruction(this.registerX);
                case 0x0D -> this.absoluteReadInstruction();
                case 0x1D -> this.absoluteIndexedReadInstruction(this.registerX);
                case 0x19 -> this.absoluteIndexedReadInstruction(this.registerY);
                case 0x01 -> this.indexedIndirectReadInstruction();
                case 0x11 -> this.indirectIndexedReadInstruction();

                // TRANSFER INSTRUCTIONS

                // TAX
                case 0xAA -> this.genericImpliedAddressing();

                default -> {
                    throw new UnimplementedInstructionException(
                            String.format("Opcode not implemented: 0x%02X",
                                    this.currentInstruction.opcode()));
                }
            }
        }
        // Checks if the current instruction has finished running.
        if (this.currentInstructionCycle == this.currentInstruction.cycles() + this.pageCrossed) {
            // This might have to be fixed later
            //this.addressBus = this.programCounter;
            this.pageCrossed = 0;
            this.currentInstructionCycle = 0; // Set to 0 because it'll be incremented before the next iteration.
            this.readWritePin = ReadWrite.Read;
        }
        this.currentInstructionCycle++;
        this.currentCycle++;
    }

    /**
     * Executes the opcode.
     * @param opcode opcode to execute.
     * @throws UnimplementedInstructionException in case the opcode hasn't been implemented.
     */
    protected void executeOpcode(byte opcode) throws UnimplementedInstructionException {
        switch ((int) opcode & 0xFF) {
            // ADC
            case 0x69, 0x65, 0x75, 0x6D, 0x7D, 0x79, 0x61, 0x71 -> this.adc();

            // AND
            case 0x29, 0x25, 0x35, 0x2D, 0x3D, 0x39, 0x21, 0x31 -> this.and();

            // BRK
            case 0x00 -> this.brk();

            // EOR
            case 0x49, 0x45, 0x55, 0x4D, 0x5D, 0x59, 0x41, 0x51 -> this.eor();

            // INCREMENT INSTRUCTIONS

            // INX
            case 0xE8 -> this.inx();

            // LOAD INSTRUCTIONS

            // LDA
            case 0xA9, 0xA5, 0xB5, 0xAD, 0xBD, 0xB9, 0xA1, 0xB1 -> this.lda();

            // LDX
            case 0xA2, 0xA6, 0xB6, 0xAE, 0xBE -> this.ldx();

            // LDY
            case 0xA0, 0xA4, 0xB4, 0xAC, 0xBC -> this.ldy();

            // ORA
            case 0x09, 0x05, 0x15, 0x0D, 0x1D, 0x19, 0x01, 0x11 -> this.ora();

            // TRANSFER INSTRUCTION

            // TAX
            case 0xAA -> this.tax();

            default -> {
                throw new UnimplementedInstructionException(
                        String.format("Opcode not implemented: 0x%02X",
                                this.currentInstruction.opcode()));
            }
        }
    }

    /* =====================
     * INSTRUCTION FUNCTIONS
     ======================= */

    /* =
     * A
     === */

    /**
     * Adds the contents of memory to the accumulator + the carry bit. If overflow occurs, the carry is set.
     * May treat operands either as decimal or hex number.
     * Sets zero and negative flags accordingly.
     */
    void adc() {
        // Interprets operands as decimal numbers.
        if ((this.status & DECIMAL) != 0) {
            this.accumulator = this.addWithCarryDecimal(this.accumulator, this.dataBus);
        // Interprets them as hex numbers.
        } else {
            this.accumulator = this.addWithCarry(this.accumulator, this.dataBus);
            this.updateZeroAndNegativeFlags(this.accumulator);
        }
    }

    /**
     * Performs a bitwise and between the accumulator and the contents of the data bus
     * and stores the result in the accumulator.
     * Updates zero and negative flags accordingly.
     */
    private void and() {
        this.accumulator = (byte) ((this.accumulator & 0xFF) & (this.dataBus & 0xFF));
        this.updateZeroAndNegativeFlags(this.accumulator);
    }


    /* =====
     * BREAK
     ======= */

    /**
     * Forces interrupt request. PC and processor status are pushed to the stack
     * and IRQ interrupt vector at $FFFE/F is loaded into PC and the break flag
     * in the status is set to one.
     */
    private void brk() {
        this.programCounter = (short) ((this.programCounter & 0x00FF) | (this.dataBus << 8));
    }


    /* ===
     * EOR
     ===== */

    /**
     * XORs the contents of the accumulator with what's on the data bus.
     * Updates zero and negative flags as needed.
     */
    private void eor() {
        this.accumulator = (byte) ((this.accumulator & 0xFF) ^ (this.dataBus & 0xFF));
        this.updateZeroAndNegativeFlags(this.accumulator);
    }


    /* ======================
     * INCREMENT INSTRUCTIONS
     ======================== */

    /**
     * Adds one to register X and sets the zero and negative flags as appropriate.
     */
    private void inx() {
        this.registerX++;
        this.updateZeroAndNegativeFlags(this.registerX);
    }

    /* =================
     * LOAD INSTRUCTIONS
     =================== */

    /**
     * Loads byte into accumulator register and updates de zero and negative flags as appropriate.
     */
    private void lda() {
        this.accumulator = this.dataBus;
        this.updateZeroAndNegativeFlags(this.accumulator);
    }

    /**
     * Loads byte into register X and updates zero and negative flags as appropriate.
     */
    private void ldx() {
        this.registerX = this.dataBus;
        this.updateZeroAndNegativeFlags(this.registerX);
    }

    /**
     * Loads byte into register Y and updates zero and negative flags as appropriate.
     */
    private void ldy() {
        this.registerY = this.dataBus;
        this.updateZeroAndNegativeFlags(this.registerY);
    }

    /**
     * Performs inclusive bitwise OR on the accumulator contents using data bus contents.
     * Updates zero and negative flags accordingly.
     */
    private void ora() {
        this.accumulator = (byte) ((this.accumulator & 0xFF) | (this.dataBus & 0xFF));
        this.updateZeroAndNegativeFlags(this.accumulator);
    }

    /* =====================
     * TRANSFER INSTRUCTIONS
     ======================= */

    /**
     * Transfers accumulator to register X.
     * Updates zero and negative flags if the new value for register X
     * is either zero or negative respectively.
     */
    private void tax() {
        this.registerX = this.accumulator;
        this.updateZeroAndNegativeFlags(this.registerX);
    }

    /* ====================================
     * CYCLE-BY-CYCLE INSTRUCTION FUNCTIONS
     ====================================== */

    /* At each case, the command above it between quotation marks
     * are extracted from this cycle-by-cycle guide of the NES dev wiki:
     * https://www.nesdev.org/6502_cpu.txt
     */
    /**
     * Forces interrupt request. PC and processor status are pushed to the stack
     * and IRQ interrupt vector at $FFFE/F is loaded into PC and the break flag
     * in the status is set to one.
     */
    private void brkCycleByCycle() throws IllegalCycleException {
        switch (this.currentInstructionCycle) {
            /* "Read next instruction byte and throw it away,
             * increment PC."
             */
            case 2 -> {
                this.addressBus = this.programCounter;
                this.programCounter++;
            }
            // "Push PCH on stack (with B flag set), decrement S"
            case 3 -> {
                this.stackPush((byte) (this.programCounter >> 8));
            }
            // "Push PCL on stack, decrement S"
            case 4 -> {
                this.stackPush((byte) (this.programCounter & 0xFF));
            }
            // "Push P on stack, decrement S"
            case 5 -> {
                this.stackPush((byte) (this.status | BREAK));
            }
            // "Fetch PCL"
            case 6 -> {
                this.addressBus = IRQ_VECTOR_LOW;
                this.readWritePin = ReadWrite.Read;
            }
            // "Fetch PCH"
            case 7 -> {
                this.programCounter = (short) ((this.programCounter & 0xFF00) | this.dataBus);
                this.addressBus = IRQ_VECTOR_HIGH;
            }
            default -> throw new IllegalCycleException(this);
        }
    }

    /**
     * Function to generically represent the cycle-to-cycle behavior of the MOS6502
     * for instructions with implied addressing mode.
     * @throws IllegalCycleException in case the instruction reaches an unimplemented cycle.
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

    /**
     * Function that generically represents the cycle-to-cycle behavior of Immediate addressing mode
     * instructions.
     * @throws IllegalCycleException in case the function reaches an unimplemented cycle.
     */
    private void genericImmediateAddressing() throws IllegalCycleException {
        switch (this.currentInstructionCycle) {
            // "Fetch value, increment PC"
            case 2 -> {
                this.addressBus = this.programCounter;
                this.programCounter++;
            }
            default -> throw new IllegalCycleException("This instruction accepts at most "
                    + this.currentInstruction.cycles() + ", received " + this.currentInstructionCycle);
        }
    }

    /**
     * Implements cycle-to-cycle behavior of Zero Page read instructions (LDA, LDX, LDY,
     * EOR, AND, ORA, ADC, SBC, CMP, BIT, LAX, NOP)
     * @throws IllegalCycleException
     */
    private void zeroPageReadInstruction() throws IllegalCycleException {
        switch (this.currentInstructionCycle) {
            // "Fetch address, increment PC"
            case 2 -> {
                this.addressBus = this.programCounter;
                this.programCounter++;
            }
            // "Read from effective address"
            case 3 -> {
                this.addressBus = (short) (this.dataBus & 0xFF);
            }
            default -> throw new IllegalCycleException("This instruction accepts at most "
                    + this.currentInstruction.cycles() + ", received " + this.currentInstructionCycle);
        }
    }

    /**
     * Implements cycle-to-cycle behavior of Zero Page Indexed (either by X or Y)
     * read instructions (LDA, LDX, LDY, EOR, AND, ORA, ADC, SBC, CMP, BIT, LAX, NOP).
     * @param indexRegister the register used for indexing. Either register X or Y.
     * @throws IllegalCycleException
     */
    private void zeroPageIndexedReadInstruction(byte indexRegister) throws IllegalCycleException {
        switch (this.currentInstructionCycle) {
            // "Fetch address, increment PC"
            case 2 -> {
                this.addressBus = this.programCounter;
                this.programCounter++;
            }
            // "Read from address, add index register to it"
            case 3 -> {
                this.addressBus = (short) (this.dataBus & 0xFF);
            }
            // "Read from effective address"
            case 4 -> {
                this.addressBus += (short) (indexRegister & 0xFF);
                // Zeros out highest byte
                this.addressBus &= 0x00FF;
            }
            default -> throw new IllegalCycleException("This instruction accepts at most "
                    + this.currentInstruction.cycles() + ", received " + this.currentInstructionCycle);
        }
    }

    /**
     * Implements cycle-to-cycle behavior of Absolute addressing read instructions
     * (LDA, LDX, LDY, EOR, AND, ORA, ADC, SBC, CMP, BIT, LAX, NOP)
     * @throws IllegalCycleException
     */
    private void absoluteReadInstruction() throws IllegalCycleException {
        switch (this.currentInstructionCycle) {
            // "Fetch low byte of address, increment PC"
            case 2 -> {
                this.addressBus = this.programCounter;
                this.programCounter++;
            }
            // "Fetch high byte of address, increment PC"
            case 3 -> {
                this.retainedByte = this.dataBus;
                this.addressBus = this.programCounter;
                this.programCounter++;
            }
            // "Read from effective address"
            case 4 -> {
                // Retails low byte and reads high byte from data bus
                this.addressBus = (short) (this.dataBus << 8 | (this.retainedByte & 0xFF));
            }
            default -> throw new IllegalCycleException("This instruction accepts at most "
                    + this.currentInstruction.cycles() + ", received " + this.currentInstructionCycle);
        }
    }

    /**
     * Implements cycle-to-cycle behavior of Absolute Indexed (either by register X or Y) read instructions
     * (LDA, LDX, LDY, EOR, AND, ORA, ADC, SBC, CMP, BIT, LAX, LAE, SHS, NOP).
     * Runs for an extra cycle if page crossing.
     * @param indexRegister register containing the index. Either register X or Y.
     * @throws IllegalCycleException
     */
    private void absoluteIndexedReadInstruction(byte indexRegister) throws IllegalCycleException {
        switch (this.currentInstructionCycle) {
            // "Fetch low byte of address, increment PC"
            case 2 -> {
                this.addressBus = this.programCounter;
                this.programCounter++;
            }
            // "Fetch high byte of address, add index register to low address byte, increment PC"
            case 3 -> {
                // Retain low byte
                this.retainedByte = this.dataBus;
                this.addressBus = programCounter;
                this.programCounter++;
            }
            // "Read from effective address, fix the high byte of effective address"
            case 4 -> {
                // Adds low byte of address and index register to check for page crossing
                short sum = (short) ((short) (this.retainedByte & 0xFF) + (short) (indexRegister & 0xFF));
                if ((sum & 0x100) != 0) {
                    this.pageCrossed = 1;
                }
                this.addressBus = (short) (this.dataBus << 8 | (short) (sum & 0xFF));
            }
            // "Re-read from effective address"
            // Only run if there's page crossing
            case 5 -> {
                if (this.pageCrossed == 0) {
                    throw new IllegalCycleException("This cycle should only be run if a page crossing is" +
                            " identified, but that didn't happen.");
                }
                this.addressBus += 0x100;
            }

            default -> throw new IllegalCycleException("This instruction accepts at most "
                    + this.currentInstruction.cycles() + ", received " + this.currentInstructionCycle);
        }
    }

    /**
     * Implements the cycle-by-cycle behavior of Indexed Indirect (A.K.A Indexed,X) addressing mode read instructions
     * (LDA, ORA, EOR, AND, ADC, CMP, SBC, LAX)
     * @throws IllegalCycleException
     */
    private void indexedIndirectReadInstruction() throws IllegalCycleException {
        switch (this.currentInstructionCycle) {
            // "Fetch pointer address, increment PC"
            case 2 -> {
                this.addressBus = this.programCounter;
                this.programCounter++;
            }
            // "Read from the address, add X to it"
            case 3 -> {
                // Fetched data in this cycle is discarded
                this.addressBus = (short) (this.dataBus & 0xFF);
            }
            // "Fetch effective address low"
            case 4 -> {
                this.addressBus += (short) (this.registerX & 0xFF);
                this.addressBus &= 0xFF; // Zeroes out most significant byte
            }
            // "Fetch effective address high"
            case 5 -> {
                this.retainedByte = this.dataBus;
                this.addressBus++;
                /*
                 * The effective address is always fetched from the zero page,
                 * so the page crossing is ignored.
                 * In practice this means we have to zero out the most significant byte.
                 */
                this.addressBus &= 0xFF;
            }

            // "Read from effective address"
            case 6 -> {
                this.addressBus = (short) ((short) (this.dataBus << 8) | ((short) (this.retainedByte & 0xFF)));
            }
            default -> throw new IllegalCycleException("This instruction accepts at most "
                    + this.currentInstruction.cycles() + ", received " + this.currentInstructionCycle);
        }
    }

    /**
     * Implements the cycle-by-cycle behavior of the Indirect Indexed (A.K.A. Indexed,Y) addressing mode
     * for read instructions (LDA, EOR, AND, ORA, ADC, SBC, CMP).
     * Takes an extra cycle in case of page crossing.
     * @throws IllegalCycleException
     */
    private void indirectIndexedReadInstruction() throws IllegalCycleException {
        switch (this.currentInstructionCycle) {
            // "Fetch pointer address, increment PC"
            case 2 -> {
                this.addressBus = this.programCounter;
                this.programCounter++;
            }
            // "Fetch effective address low"
            case 3 -> {
                this.addressBus = (short) (this.dataBus & 0xFF);
            }
            // "Fetch effective address high, add Y to low byte of effective address"
            case 4 -> {
                this.retainedByte = this.dataBus;
                this.addressBus++;
                this.addressBus &= 0xFF; // Effective address is always fetched from zero page
            }
            // "Read from effective address, fix high byte of effective address"
            case 5 -> {
                final short sum = (short) ((short) (this.retainedByte & 0xFF) + (short) (this.registerY & 0xFF));
                // Detects page crossing
                if ((sum & 0x100) != 0) {
                    this.pageCrossed = 1;
                }
                this.addressBus = (short) (this.dataBus << 8 | (short) (sum & 0xFF));
            }
            // "Read from effective address"
            case 6 -> {
                if (this.pageCrossed == 0) {
                    throw new IllegalCycleException("This cycle should only be run if a page crossing is" +
                            " identified, but that didn't happen.");
                }
                this.addressBus += 0x100;
            }
            default -> throw new IllegalCycleException("This instruction accepts at most "
                    + this.currentInstruction.cycles() + ", received " + this.currentInstructionCycle);
        }
    }

    /* ================
     * HELPER FUNCTIONS
     ================== */


    /**
     * Performs addition between the accumulator and the operand.
     * Used as a helper function for the ADC and SBC instructions.
     * Updates the carry and overflow flags.
     * @param x accumulator.
     * @param y number to add to the accumulator.
     *                It's always a byte held in this.dataBus.
     * @return result of the addition.
     */
    private byte addWithCarry(byte x, byte y) {
        final int carryIn = this.status & CARRY; // Carry bit is the least significant bit
        final int unsignedAccumulator = x & 0xFF;
        final int unsignedOperand = y & 0xFF;
        final int sum = carryIn + unsignedOperand + unsignedAccumulator;
        if (sum > 0xFF) {
            this.setCarry();
        } else {
            this.unsetCarry();
        }

        final byte result = (byte) ((byte) sum & 0xFF);
        if (((unsignedAccumulator ^ result) & (unsignedOperand ^ result) & 0x80) != 0) {
            this.setOverflow();
        } else {
            this.unsetOverflow();
        }
        return result;
    }

    /**
     * Performs addition between the two operands.
     * One of them should be the accumulator.
     * Used as a helper function for the ADC and SBC instruction when the CPU is in decimal mode.
     * Updates the carry and overflow flags.
     * For an explanation on how this works, check this:
     * <a href="http://www.6502.org/tutorials/decimal_mode.html#A">...</a>
     * @param x accumulator
     * @param y number to add to the accumulator.
     *          It's always the byte in the data bus.
     * @return result of the addition.
     */
    private byte addWithCarryDecimal(byte x, byte y) {
        final int unsignedAccumulator = x & 0xFF;
        final int unsignedOperand = y & 0xFF;
        final int carryIn = this.status & CARRY; // Carry flag is the least significant bit

        // The zero flag is set as if it were regular binary addition
        if (((unsignedAccumulator + unsignedOperand + carryIn) & 0xFF) == 0) {
            this.setZero();
        } else {
            this.unsetZero();
        }

        // Add least significant nibble (lowest digit)
        final int lowNibble = (unsignedAccumulator & 0xF) + (unsignedOperand & 0xF) + carryIn;
        int sum = lowNibble; // This is the value that will be returned
        if (lowNibble > 0x9) {
            sum = ((sum + 0x06) & 0xF) + 0x10;
        }

        // Add most significant nibble (highest digit)
        sum += (unsignedAccumulator & 0xF0) + (unsignedOperand & 0xF0);
        if (sum > 0x90) {
            sum += 0x60;
        }

        // Carry is set based on the final result
        if (sum > 0x99) {
            this.setCarry();
        } else {
            this.unsetCarry();
        }

        // Negative and Overflow flags are set based on signed addition
        final int signedSum = (byte) (unsignedAccumulator & 0xF0)
                + (byte) (unsignedOperand & 0xF0) + (byte) (lowNibble);
        if ((byte) signedSum < 0) {
            this.setNegative();
        } else {
            this.unsetNegative();
        }

        // Overflow is set if this result is > 127 or < -128
        if (signedSum > 127 || signedSum < -128) {
            this.setOverflow();
        } else {
            this.unsetOverflow();
        }
        return (byte) sum;
    }

    /**
     * Converts decimal number to hex.
     * Essentially reverts the conversion done with hexToDecimal.
     * This is needed because the CPU will still be in decimal mode.
     * E.g. in test 69 0a e1 we're adding 10 + 2 + carry = 13, but it needs to be stored as 19 decimal (0x13).
     * @param n the number to be converted.
     * @return the converted number.
     */
    byte decimalToHex(byte n) {
        final int unsignedN = n & 0xFF;
        final int tens = unsignedN / 10;
        final int units = unsignedN % 10;
        return (byte) (tens * 16 + units);
    }

    /**
     * Converts hex number to binary coded decimal (BCD).
     * Values 0x00-0x09 are equal to 0-9 (0b0000-0b1111). Values 0xA-0xF are invalid.
     * 0x10-0x90 are equal to 10-90. Values 0xA0-0xF0 are invalid.
     * This way, a byte can represent values 00-99.
     * @param n value to be converted.
     * @return the converted number.
     */
    byte hexToDecimal(byte n) {
        final int lowNibble = n & 0xF;
        final int highNibble = n & 0xF0;

        final int carry = lowNibble > 9 ? 10 : 0;
        final int decimalLowNibble = Math.min(lowNibble, 9);
        final int decimalHighNibble = Math.min(((highNibble + carry) >> 4), 90);
        return (byte) (decimalHighNibble * 10 + decimalLowNibble);
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
        } else {
            this.unsetZero();
        }
        // Reminder: Java's 'byte' types are signed
        if (number < 0) {
            this.setNegative();
        } else {
            this.unsetNegative();
        }
    }
}
