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
                this.executeOpcode(this.currentInstruction.getOpcode());
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
            switch ((int) this.currentInstruction.getOpcode() & 0xFF) {
                // BRK
                case 0x00 -> this.brkCycleByCycle();

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

                // TRANSFER INSTRUCTIONS

                // TAX
                case 0xAA -> this.genericImpliedAddressing();

                default -> {
                    throw new UnimplementedInstructionException(
                            String.format("Opcode not implemented: 0x%02X",
                                    this.currentInstruction.getOpcode()));
                }
            }
        }
        // Checks if the current instruction has finished running.
        if (this.currentInstructionCycle == this.currentInstruction.getCycles() + this.pageCrossed) {
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
            // BRK
            case 0x00 -> this.brk();

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

            // TRANSFER INSTRUCTION

            // TAX
            case 0xAA -> this.tax();

            default -> {
                throw new UnimplementedInstructionException(
                        String.format("Opcode not implemented: 0x%02X",
                                this.currentInstruction.getOpcode()));
            }
        }
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
    private void brk() {
        this.programCounter = (short) ((this.programCounter & 0x00FF) | (this.dataBus << 8));
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
                    + this.currentInstruction.getCycles() + ", received " + this.currentInstructionCycle);
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
                    + this.currentInstruction.getCycles() + ", received " + this.currentInstructionCycle);
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
                    + this.currentInstruction.getCycles() + ", received " + this.currentInstructionCycle);
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
                    + this.currentInstruction.getCycles() + ", received " + this.currentInstructionCycle);
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
                    + this.currentInstruction.getCycles() + ", received " + this.currentInstructionCycle);
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
                    + this.currentInstruction.getCycles() + ", received " + this.currentInstructionCycle);
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
