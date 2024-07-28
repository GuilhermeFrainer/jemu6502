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

    public boolean breakSign;

    public enum ReadWrite {
        Read,
        Write;
    }

    // Memory
    final protected Memory memory;
    private ReadWrite readWritePin;
    private byte dataBus;
    private short addressBus;

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

    void writeToMemory() {
        this.readWritePin = ReadWrite.Write;
        this.memory.write(this.dataBus, this.addressBus);
    }

    void readFromMemory() {
        this.readWritePin = ReadWrite.Read;
        this.dataBus = this.memory.read(this.addressBus);
    }

    /**
     * Loads program into memory.
     * @param program byte array the size of RAM to essentially replace the current RAM status.
     */
    void load(byte[] program) {
        for (int i = 0; i < program.length; i++) {
            this.memory.write(program[i], (short) i);
        }
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
     * and read on the next one (following Cherkov's Gun principle).
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

    protected void setCurrentInstruction(Instruction instruction) {
        this.currentInstruction = instruction;
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
        this.memory = new Memory64k();
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
        this.writeToMemory();
        this.stackPointer--;
    }

    /**
     * Pops value from the stack.
     * Value will be written to the data bus.
     */
    private void stackPop() {
        this.stackPointer++;
        this.addressBus = (short) (STACK_PAGE + (this.stackPointer & 0xFF));
        this.readFromMemory();
    }

    /**
     * Executes the next cycle.
     */
    public void tick()
            throws UnimplementedInstructionException, IllegalCycleException {
        /*
         * The last cycle in an instruction.
         * Moved here (from the bottom of this function)
         * otherwise Read-Modify-Write instructions wouldn't work properly.
         * Might have to change the "null" condition later.
         */
        if (this.currentInstruction == null || this.currentInstructionCycle > this.currentInstruction.cycles() + this.pageCrossed) {
            // This might have to be fixed later
            //this.addressBus = this.programCounter;
            this.pageCrossed = 0;
            this.currentInstructionCycle = 1;
            this.readWritePin = ReadWrite.Read;
            // First cycle of every instruction
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
            this.readFromMemory();
            this.currentInstruction = this.fetchInstruction(this.dataBus);
            this.addressBus = this.programCounter;
            this.programCounter++;
        } else if (this.currentInstructionCycle > 1) {
            switch ((int) this.currentInstruction.opcode() & 0xFF) {
                // Implied addressing mode only instructions
                // INX, NOP, TAX, DEX, DEY, INY
                // Clear flag instructions: CLC, CLD, CLI, CLV
                // Set flag instructions: SEC, SED, SEI
                case 0xEA, 0xAA, 0xE8, 0x18, 0xD8, 0x58, 0xB8, 0xCA, 0x88, 0xC8, 0x38, 0xF8, 0x78
                        -> this.impliedAddressingInstruction();
                // Read instructions
                // ADC, AND, BIT, CMP, EOR, LDA, LDX, LDY, ORA, SBC, CPX, CPY
                case 0x69, 0x29, 0xC9, 0x49, 0xA9, 0xA2, 0xA0, 0x09, 0xE9, 0xE0, 0xC0
                        -> this.immediateAddressingInstruction();
                case 0x65, 0x25, 0x24, 0xC5, 0x45, 0xA5, 0xA6, 0xA4, 0x05, 0xE5, 0xE4, 0xC4
                        -> this.zeroPageReadInstruction();
                case 0x75, 0x35, 0xD5, 0x55, 0xB5, 0xB4, 0x15, 0xF5
                        -> this.zeroPageIndexedReadInstruction(this.registerX);
                case 0xB6 -> this.zeroPageIndexedReadInstruction(this.registerY); // LDY is the only one here
                case 0x6D, 0x2D, 0x2C, 0xCD, 0x4D, 0xAD, 0xAE, 0xAC, 0x0D, 0xED, 0xEC, 0xCC
                        -> this.absoluteReadInstruction();
                case 0x7D, 0x3D, 0xDD, 0x5D, 0xBD, 0xBC, 0x1D, 0xFD
                        -> this.absoluteIndexedReadInstruction(this.registerX);
                case 0x79, 0x39, 0xD9, 0x59, 0xB9, 0xBE, 0x19, 0xF9
                        -> this.absoluteIndexedReadInstruction(this.registerY);
                case 0x61, 0x21, 0xC1, 0x41, 0xA1, 0x01, 0xE1
                        -> this.indexedIndirectReadInstruction();
                case 0x71, 0x31, 0xD1, 0x51, 0xB1, 0x11, 0xF1
                        -> this.indirectIndexedReadInstruction();

                // Read-Modify-Write instructions
                // ASL, LSR, ROL, ROR, INC, DEC
                // Accumulator addressing mode uses the same function as Implied
                case 0x0A, 0x4A, 0x2A, 0x6A
                        -> this.impliedAddressingInstruction();
                case 0x06, 0x46, 0x26, 0x66, 0xE6, 0xC6
                        -> this.zeroPageModifyInstruction();
                case 0x16, 0x56, 0x36, 0x76, 0xF6, 0xD6
                        -> this.zeroPageIndexedModifyInstruction(this.registerX);
                case 0x0E, 0x4E, 0x2E, 0x6E, 0xEE, 0xCE
                        -> this.absoluteModifyInstruction();
                case 0x1E, 0x5E, 0x3E, 0x7E, 0xFE, 0xDE
                        -> this.absoluteIndexedModifyInstruction();

                // Write instructions
                // STA, STX, STY
                case 0x85, 0x86, 0x84
                    -> this.zeroPageWriteInstruction();
                case 0x95, 0x94
                    -> this.zeroPageIndexedWriteInstruction(this.registerX);
                case 0x96
                    -> this.zeroPageIndexedWriteInstruction(this.registerY);
                case 0x8D, 0x8E, 0x8C
                    -> this.absoluteWriteInstruction();
                case 0x9D
                    -> this.absoluteIndexedWriteInstruction(this.registerX);
                case 0x99
                    -> this.absoluteIndexedWriteInstruction(this.registerY);
                case 0x81
                    -> this.indexedIndirectWriteInstruction();
                case 0x91
                    -> this.indirectIndexedWriteInstruction();

                // BRK
                case 0x00 -> this.brkCycleByCycle();

                // Branch instructions
                case 0x90 -> this.relativeInstruction((this.status & CARRY) == 0);
                case 0xB0 -> this.relativeInstruction((this.status & CARRY) != 0);
                case 0xF0 -> this.relativeInstruction((this.status & ZERO) != 0);
                case 0x30 -> this.relativeInstruction((this.status & NEGATIVE) != 0);
                case 0xD0 -> this.relativeInstruction((this.status & ZERO) == 0);
                case 0x10 -> this.relativeInstruction((this.status & NEGATIVE) == 0);
                case 0x50 -> this.relativeInstruction((this.status & OVERFLOW) == 0);
                case 0x70 -> this.relativeInstruction((this.status & OVERFLOW) != 0);

                // JMP
                case 0x4C -> this.absoluteJumpInstruction();
                case 0x6C -> this.indirectInstruction();

                // RTI
                case 0x40 -> this.rtiCycleToCycle();

                // RTS
                case 0x60 -> this.rtsCycleToCycle();

                // PHA, PLP
                case 0x48 -> this.pushRegisterCycleByCycle();
                case 0x08 -> this.pushRegisterCycleByCycle();

                // PLA, PLP
                case 0x68, 0x28 -> this.pullRegisterCycleByCycle();

                // JSR
                case 0x20 -> this.jsrCycleByCycle();

                default -> throw new UnimplementedInstructionException(
                        String.format("Opcode not implemented in tick function: 0x%02X",
                                this.currentInstruction.opcode()));
            }
        }
        if (this.currentInstructionCycle == this.currentInstruction.cycles() + this.pageCrossed) {
            this.executeOpcode(this.currentInstruction.opcode());
        }
        // Checks if the current instruction has finished running.
        // TRY PUTTING THIS AT THE BEGINNING OF THE NEXT CYCLE INSTEAD
        /*
        if (this.currentInstructionCycle == this.currentInstruction.cycles() + this.pageCrossed) {
            // This might have to be fixed later
            //this.addressBus = this.programCounter;
            this.pageCrossed = 0;
            this.currentInstructionCycle = 0; // Set to 0 because it'll be incremented before the next iteration.
            this.readWritePin = ReadWrite.Read;
        }
         */
        this.currentInstructionCycle++;
        this.currentCycle++;
    }

    /**
     * Executes an opcode at the end of a cycle.
     * Should only be used either for Read instructions or for accumulator or implied
     * addressing modes.
     * @param opcode opcode to execute.
     * @throws UnimplementedInstructionException in case the opcode hasn't been implemented.
     */
    protected void executeOpcode(byte opcode) throws UnimplementedInstructionException {
        switch ((int) opcode & 0xFF) {
            // ADC
            case 0x69, 0x65, 0x75, 0x6D, 0x7D, 0x79, 0x61, 0x71 -> this.adc();

            // AND
            case 0x29, 0x25, 0x35, 0x2D, 0x3D, 0x39, 0x21, 0x31 -> this.and();

            // ASL
            case 0x0A, 0x06, 0x16, 0x0E, 0x1E -> this.asl();

            // BIT
            case 0x24, 0x2C -> this.bit();

            // BRK
            case 0x00 -> this.brk();

            // BRANCH INSTRUCTIONS
            case 0x90, 0xB0, 0xF0, 0x30, 0xD0, 0x10, 0x50, 0x70 -> { }

            // CLEAR FLAG INSTRUCTIONS
            case 0x18 -> this.clc();
            case 0xD8 -> this.cld();
            case 0x58 -> this.cli();
            case 0xB8 -> this.clv();

            // COMPARISON INSTRUCTIONS

            // CMP
            case 0xC9, 0xC5, 0xD5, 0xCD, 0xDD, 0xD9, 0xC1, 0xD1 -> this.cmp();

            // CPX
            case 0xE0, 0xE4, 0xEC -> this.cpx();

            // CPY
            case 0xC0, 0xC4, 0xCC -> this.cpy();

            // DEC
            case 0xC6, 0xD6, 0xCE, 0xDE -> this.dec();

            // DEX
            case 0xCA -> this.dex();

            // DEY
            case 0x88 -> this.dey();

            // EOR
            case 0x49, 0x45, 0x55, 0x4D, 0x5D, 0x59, 0x41, 0x51 -> this.eor();

            // INCREMENT INSTRUCTIONS

            // INC
            case 0xE6, 0xF6, 0xEE, 0xFE -> this.inc();

            // INX
            case 0xE8 -> this.inx();

            // INY
            case 0xC8 -> this.iny();

            // JMP
            case 0x4C, 0x6C -> this.jmp();

            // JSR
            case 0x20 -> { }

            // LOAD INSTRUCTIONS

            // LDA
            case 0xA9, 0xA5, 0xB5, 0xAD, 0xBD, 0xB9, 0xA1, 0xB1 -> this.lda();

            // LDX
            case 0xA2, 0xA6, 0xB6, 0xAE, 0xBE -> this.ldx();

            // LDY
            case 0xA0, 0xA4, 0xB4, 0xAC, 0xBC -> this.ldy();

            // LSR
            case 0x4A, 0x46, 0x56, 0x4E, 0x5E -> this.lsr();

            // NOP
            case 0xEA -> { /* This does nothing. */}

            // ORA
            case 0x09, 0x05, 0x15, 0x0D, 0x1D, 0x19, 0x01, 0x11 -> this.ora();

            // PHA, PHP
            case 0x48, 0x08 -> { }

            // PLA
            case 0x68 -> this.pla();

            // PLP
            case 0x28 -> this.plp();

            // ROL
            case 0x2A, 0x26, 0x36, 0x2E, 0x3E -> this.rol();

            // ROR
            case 0x6A, 0x66, 0x76, 0x6E, 0x7E -> this.ror();

            // RTI, RTS
            case 0x40, 0x60 -> { }

            // SBC
            case 0xE9, 0xE5, 0xF5, 0xED, 0xFD, 0xF9, 0xE1, 0xF1 -> this.sbc();

            // SET FLAG INSTRUCTIONS

            // SEC
            case 0x38 -> this.sec();

            // SED
            case 0xF8 -> this.sed();

            // SEI
            case 0x78 -> this.sei();

            // STORE INSTRUCTIONS

            // STA
            case 0x85, 0x95, 0x8D, 0x9D, 0x99, 0x81, 0x91 -> this.sta();

            // STX
            case 0x86, 0x96, 0x8E -> this.stx();

            // STY
            case 0x84, 0x94, 0x8C -> this.sty();

            // TRANSFER INSTRUCTIONS

            // TAX
            case 0xAA -> this.tax();

            default -> throw new UnimplementedInstructionException(
                            String.format("Unimplemented instruction: 0x%02X",
                                    this.currentInstruction.opcode()));
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

    /**
     * Shifts the contents of either the accumulator or the address in memory one bit to the left.
     * Bit 0 is set to 0 and bit 7 is placed in the carry flag.
     */
    private void asl() {
        if (this.currentInstruction.addressingMode() == Instruction.AddressingMode.Accumulator) {
            if ((this.accumulator & 0b10000000) != 0) {
                this.setCarry();
            } else {
                this.unsetCarry();
            }
            this.accumulator <<= 1;
        } else {
            if ((this.dataBus & 0x10000000) != 0) {
                this.setCarry();
            } else {
                this.unsetCarry();
            }
            this.dataBus <<= 1;
            this.writeToMemory();
        }
    }

    /* =
     * B
     === */

    /**
     * Tests bits in a memory location.
     * Bitwise ANDs value in memory with A to set (if = 0) or clear (otherwise) the zero flag.
     * Sets V and N to the value of the 6th and 7th bits of the value in memory respectively.
     */
    private void bit() {
        if ((this.accumulator & this.dataBus) == 0) {
            this.setZero();
        } else {
            this.unsetZero();
        }
        if ((this.dataBus & 0b10000000) != 0) {
            this.setNegative();
        } else {
            this.unsetNegative();
        }
        if ((this.dataBus & 0b01000000) == 0) {
            this.setOverflow();
        } else {
            this.unsetOverflow();
        }
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
        final int unsignedDataBus = dataBus & 0xFF;
        final int highByte = unsignedDataBus << 8;
        this.programCounter = (short) ((this.retainedByte & 0xFF) | highByte);
    }

    /* =
     * C
     === */

    /* =======================
     * CLEAR FLAG INSTRUCTIONS
     ========================= */

    /**
     * Sets the carry flag to 0.
     */
    private void clc() {
        this.status ^= CARRY;
    }

    /**
     * Sets the decimal flag to 0.
     */
    private void cld() {
        this.status ^= DECIMAL;
    }

    /**
     * Sets the interrupt disable flag to 0.
     */
    private void cli() {
        this.status ^= INTERRUPT_DISABLE;
    }

    /**
     * Sets the overflow flag to 0.
     */
    private void clv() {
        this.status ^= OVERFLOW;
    }
    /* =======================
     * COMPARISON INSTRUCTIONS
     ========================= */

    /**
     * Compares the (unsigned) values in the accumulator and in an address in memory.
     * Doesn't write to the accumulator.
     * Sets the zero, negative and carry flags as appropriate.
     */
    void cmp() {
        if ((this.accumulator & 0xFF) >= (this.dataBus & 0xFF)) {
            this.setCarry();
        } else {
            this.unsetCarry();
        }
        this.updateZeroAndNegativeFlags((byte) ((this.accumulator & 0xFF) - (this.dataBus & 0xFF)));
    }

    /**
     * Compares the (unsigned) values in the X register and in an address in memory.
     * Doesn't write to the register.
     * Sets the zero, negative and carry flags as appropriate.
     */
    private void cpx() {
        if ((this.registerX & 0xFF) >= (this.dataBus & 0xFF)) {
            this.setCarry();
        } else {
            this.unsetCarry();
        }
        this.updateZeroAndNegativeFlags((byte) ((this.registerX & 0xFF) - (this.dataBus & 0xFF)));
    }

    /**
     * Compares the (unsigned) values in the Y register and in an address in memory.
     * Doesn't write to the register.
     * Sets the zero, negative and carry flags as appropriate.
     */
    private void cpy() {
        if ((this.registerY & 0xFF) >= (this.dataBus & 0xFF)) {
            this.setCarry();
        } else {
            this.unsetCarry();
        }
        this.updateZeroAndNegativeFlags((byte) ((this.registerY & 0xFF) - (this.dataBus & 0xFF)));
    }

    /* =
     * D
     === */

    /**
     * Subtracts one from the value held at a specified memory location
     * setting the zero and negative flags as appropriate.
     */
    void dec() {
        this.dataBus--;
        this.updateZeroAndNegativeFlags(this.dataBus);
        this.writeToMemory();
    }

    /**
     * Subtracts one from the X register
     * setting the zero and negative flags as appropriate.
     */
    void dex() {
        this.registerX--;
        this.updateZeroAndNegativeFlags(this.registerX);
    }

    /**
     * Subtracts one from the value held at a specified memory location
     * setting the zero and negative flags as appropriate.
     */
    void dey() {
        this.registerY--;
        this.updateZeroAndNegativeFlags(this.registerY);
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
     * Adds one to the value held at a specified memory location
     * setting the zero and negative flags as appropriate.
     */
    void inc() {
        this.dataBus++;
        this.updateZeroAndNegativeFlags(this.dataBus);
        this.writeToMemory();
    }

    /**
     * Adds one to register X and sets the zero and negative flags as appropriate.
     */
    void inx() {
        this.registerX++;
        this.updateZeroAndNegativeFlags(this.registerX);
    }

    /**
     * Adds one to the Y register,
     * setting the zero and negative flags as appropriate.
     */
    void iny() {
        this.registerY++;
        this.updateZeroAndNegativeFlags(this.registerY);
    }

    /* =
     * J
     === */

    /**
     * Sets the program counter to the address specified by the operand.
     */
    void jmp() {
        this.programCounter = (short) (((this.dataBus & 0xFF) << 8) | (this.retainedByte & 0xFF));
    }

    /* =================
     * LOAD INSTRUCTIONS
     =================== */

    /**
     * Loads byte into accumulator register and updates de zero and negative flags as appropriate.
     */
    void lda() {
        this.accumulator = this.dataBus;
        this.updateZeroAndNegativeFlags(this.accumulator);
    }

    /**
     * Loads byte into register X and updates zero and negative flags as appropriate.
     */
    void ldx() {
        this.registerX = this.dataBus;
        this.updateZeroAndNegativeFlags(this.registerX);
    }

    /**
     * Loads byte into register Y and updates zero and negative flags as appropriate.
     */
    void ldy() {
        this.registerY = this.dataBus;
        this.updateZeroAndNegativeFlags(this.registerY);
    }

    /**
     * Shifts the bits in the accumulator or in memory to the right.
     * The bit that was in bit 0 is pushed onto the carry flag.
     * Bit 7 is set to 0.
     * Updates the zero and negative flags accordingly.
     */
    void lsr() {
        if (this.currentInstruction.addressingMode() == Instruction.AddressingMode.Accumulator) {
            if ((this.accumulator & 1) != 0) {
                this.setCarry();
            } else {
                this.unsetCarry();
            }
            this.accumulator >>= 1;
            this.accumulator &= 0b01111111;
        } else {
            if ((this.dataBus & 1) != 0) {
                this.setCarry();
            } else {
                this.unsetCarry();
            }
            this.dataBus >>= 1;
            this.dataBus &= 0b01111111;
            this.writeToMemory();
        }
    }

    /*
     * This is where a NOP function would go
     */

    /**
     * Performs inclusive bitwise OR on the accumulator contents using data bus contents.
     * Updates zero and negative flags accordingly.
     */
    void ora() {
        this.accumulator = (byte) ((this.accumulator & 0xFF) | (this.dataBus & 0xFF));
        this.updateZeroAndNegativeFlags(this.accumulator);
    }

    /**
     * Pulls an 8 bit value from the stack and into the accumulator.
     * Updates zero and negative flags as appropriate.
     */
    void pla() {
        this.accumulator = this.dataBus;
        this.updateZeroAndNegativeFlags(this.accumulator);
    }

    /**
     * Pulls an 8 bit value from the stack and into the processor flags.
     * The flags will take on new states as determined by the value pulled.
     */
    void plp() {
        this.status = this.dataBus;
        this.updateZeroAndNegativeFlags(this.status);
    }

    /* =====================
     * ROTATION INSTRUCTIONS
     ======================= */

    /**
     * Move each of the bits in either A or M one place to the left.
     * Bit 0 is filled with the current value of the carry flag,
     * whilst the old bit 7 becomes the new carry flag value.
     */
    void rol() {
        final int carryIn = this.status & CARRY;
        if (this.currentInstruction.addressingMode() == Instruction.AddressingMode.Accumulator) {
            if ((this.accumulator & 0b10000000) != 0) {
                this.setCarry();
            } else {
                this.unsetCarry();
            }
            this.accumulator <<= 1;
            this.accumulator |= (byte) carryIn;
        } else {
            if ((this.dataBus & 0b10000000) != 0) {
                this.setCarry();
            } else {
                this.unsetCarry();
            }
            this.dataBus <<= 1;
            this.dataBus |= (byte) carryIn;
            this.writeToMemory();
        }
    }

    /**
     * Move each of the bits in either A or M one place to the right.
     * Bit 7 is filled with the current value of the carry flag,
     * whilst the old bit 0 becomes the new carry flag value.
     */
    void ror() {
        final int carryIn = this.status & CARRY;
        if (this.currentInstruction.addressingMode() == Instruction.AddressingMode.Accumulator) {
            if ((this.accumulator & 1) != 0) {
                this.setCarry();
            } else {
                this.unsetCarry();
            }
            this.accumulator >>>= 1;
            // Must zero out the first bit, cause apparently Java pads it with 1s
            this.accumulator &= 0b01111111;
            this.accumulator |= (byte) (carryIn << 7);
        } else {
            if ((this.dataBus & 1) != 0) {
                this.setCarry();
            } else {
                this.unsetCarry();
            }
            this.dataBus >>>= 1;
            // Must zero out the first bit, cause apparently Java pads it with 1s
            this.dataBus &= 0b01111111;
            this.dataBus |= (byte) (carryIn << 7);
            this.writeToMemory();
        }
    }

    /**
     * Subtracts the contents of an address in memory to the accumulator with the inverse of the carry bit.
     * Updates zero, negative, overflow and carry flags.
     * Might operate in decimal mode.
     */
    void sbc() {
        if ((this.status & DECIMAL) != 0) {
            final int unsignedAccumulator = this.accumulator & 0xFF;
            final int unsignedOperand = this.dataBus & 0xFF;
            final int carryIn = this.status & CARRY;
            // Flags are set based on binary addition
            final int binarySubtraction = unsignedAccumulator - unsignedOperand + carryIn - 1;
            // Carry works in the opposite way of how it works in ADC
            if (binarySubtraction < 0) {
                this.unsetCarry();
            } else {
                this.setCarry();
            }

            final byte result = (byte) ((byte) binarySubtraction & 0xFF);
            if (((unsignedAccumulator ^ result) & (unsignedOperand ^ result) & 0x80) != 0) {
                this.setOverflow();
            } else {
                this.unsetOverflow();
            }
            this.updateZeroAndNegativeFlags((byte) binarySubtraction);

            // Now we do proper decimal subtraction
            final int lowNibble = (unsignedAccumulator & 0xF) - (unsignedOperand & 0xF) + carryIn - 1;
            int subtraction = lowNibble;
            if (lowNibble < 0) {
                subtraction = ((lowNibble - 0x6) & 0xF) - 0x10;
            }
            subtraction += (unsignedAccumulator & 0xF0) - (unsignedOperand & 0xF0);
            if (subtraction < 0) {
                subtraction -= 0x60;
            }
            this.accumulator = (byte) subtraction;
        } else {
            this.accumulator = this.addWithCarry(this.accumulator, (byte) ~this.dataBus);
        }
    }

    /* =====================
     * SET FLAG INSTRUCTIONS
     ======================= */

    /**
     * Sets the carry flag to one.
     */
    void sec() {
        this.status |= CARRY;
    }

    /**
     * Sets the decimal flag to one.
     */
    void sed() {
        this.status |= DECIMAL;
    }

    /**
     * Sets the interrupt disable flag to one.
     */
    void sei() {
        this.status |= INTERRUPT_DISABLE;
    }

    /* ==================
     * STORE INSTRUCTIONS
     ==================== */

    /**
     * Stores the contents of the accumulator in memory.
     */
    void sta() {
        this.dataBus = this.accumulator;
        this.writeToMemory();
    }

    /**
     * Stores the contents of the X register in memory.
     */
    void stx() {
        this.dataBus = this.registerX;
        this.writeToMemory();
    }

    /**
     * Stores the contents of the Y register in memory.
     */
    void sty() {
        this.dataBus = this.registerY;
        this.writeToMemory();
    }

    /* =====================
     * TRANSFER INSTRUCTIONS
     ======================= */

    /**
     * Transfers accumulator to register X.
     * Updates zero and negative flags if the new value for register X
     * is either zero or negative respectively.
     */
    void tax() {
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
     * Function to generically represent the cycle-to-cycle behavior of the MOS6502
     * for instructions with implied or accumulator addressing mode.
     * @throws IllegalCycleException in case the instruction reaches an unimplemented cycle.
     */
    private void impliedAddressingInstruction() throws IllegalCycleException {
        // "Read next instruction byte (and throw it away)"
        if (this.currentInstructionCycle == 2) {
            this.addressBus = this.programCounter;
            this.readFromMemory();
        } else {
            throw new IllegalCycleException(this);
        }
    }

    /**
     * Function that generically represents the cycle-to-cycle behavior of Immediate addressing mode
     * instructions.
     * @throws IllegalCycleException in case the function reaches an unimplemented cycle.
     */
    private void immediateAddressingInstruction() throws IllegalCycleException {
        // "Fetch value, increment PC"
        if (this.currentInstructionCycle == 2) {
            this.addressBus = this.programCounter;
            this.readFromMemory();
            this.programCounter++;
        } else {
            throw new IllegalCycleException("This instruction accepts at most "
                    + this.currentInstruction.cycles() + ", received " + this.currentInstructionCycle);
        }
    }

    /* =================
     * READ INSTRUCTIONS
     =================== */

    /**
     * Implements cycle-to-cycle behavior of Zero Page read instructions (LDA, LDX, LDY,
     * EOR, AND, ORA, ADC, SBC, CMP, BIT, LAX, NOP)
     * @throws IllegalCycleException if the instruction is called at a cycle it shouldn't be.
     */
    private void zeroPageReadInstruction() throws IllegalCycleException {
        switch (this.currentInstructionCycle) {
            // "Fetch address, increment PC"
            case 2 -> {
                this.addressBus = this.programCounter;
                this.readFromMemory();
                this.programCounter++;
            }
            // "Read from effective address"
            case 3 -> {
                this.addressBus = (short) (this.dataBus & 0xFF);
                this.readFromMemory();
            }
            default -> throw new IllegalCycleException("This instruction accepts at most "
                    + this.currentInstruction.cycles() + ", received " + this.currentInstructionCycle);
        }
    }

    /**
     * Implements cycle-to-cycle behavior of Zero Page Indexed (either by X or Y)
     * read instructions (LDA, LDX, LDY, EOR, AND, ORA, ADC, SBC, CMP, BIT, LAX, NOP).
     * @param indexRegister the register used for indexing. Either register X or Y.
     * @throws IllegalCycleException if the instruction is called at a cycle it shouldn't be.
     */
    private void zeroPageIndexedReadInstruction(byte indexRegister) throws IllegalCycleException {
        switch (this.currentInstructionCycle) {
            // "Fetch address, increment PC"
            case 2 -> {
                this.addressBus = this.programCounter;
                this.readFromMemory();
                this.programCounter++;
            }
            // "Read from address, add index register to it"
            case 3 -> {
                this.addressBus = (short) (this.dataBus & 0xFF);
                this.readFromMemory();
            }
            // "Read from effective address"
            case 4 -> {
                this.addressBus += (short) (indexRegister & 0xFF);
                // Zeros out highest byte
                this.addressBus &= 0x00FF;
                this.readFromMemory();
            }
            default -> throw new IllegalCycleException("This instruction accepts at most "
                    + this.currentInstruction.cycles() + ", received " + this.currentInstructionCycle);
        }
    }

    /**
     * Implements cycle-to-cycle behavior of Absolute addressing read instructions
     * (LDA, LDX, LDY, EOR, AND, ORA, ADC, SBC, CMP, BIT, LAX, NOP)
     * @throws IllegalCycleException if the instruction is called at a cycle it shouldn't be.
     */
    private void absoluteReadInstruction() throws IllegalCycleException {
        switch (this.currentInstructionCycle) {
            // "Fetch low byte of address, increment PC"
            case 2 -> {
                this.addressBus = this.programCounter;
                this.readFromMemory();
                this.programCounter++;
            }
            // "Fetch high byte of address, increment PC"
            case 3 -> {
                this.retainedByte = this.dataBus;
                this.addressBus = this.programCounter;
                this.readFromMemory();
                this.programCounter++;
            }
            // "Read from effective address"
            case 4 -> {
                // Retails low byte and reads high byte from data bus
                this.addressBus = (short) (this.dataBus << 8 | (this.retainedByte & 0xFF));
                this.readFromMemory();
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
     * @throws IllegalCycleException if the instruction is called at a cycle it shouldn't be.
     */
    private void absoluteIndexedReadInstruction(byte indexRegister) throws IllegalCycleException {
        switch (this.currentInstructionCycle) {
            // "Fetch low byte of address, increment PC"
            case 2 -> {
                this.addressBus = this.programCounter;
                this.readFromMemory();
                this.programCounter++;
            }
            // "Fetch high byte of address, add index register to low address byte, increment PC"
            case 3 -> {
                // Retain low byte
                this.retainedByte = this.dataBus;
                this.addressBus = programCounter;
                this.readFromMemory();
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
                this.readFromMemory();
            }
            // "Re-read from effective address"
            // Only run if there's page crossing
            case 5 -> {
                if (this.pageCrossed == 0) {
                    throw new IllegalCycleException("This cycle should only be run if a page crossing is" +
                            " identified, but that didn't happen.");
                }
                this.addressBus += 0x100;
                this.readFromMemory();
            }

            default -> throw new IllegalCycleException("This instruction accepts at most "
                    + this.currentInstruction.cycles() + ", received " + this.currentInstructionCycle);
        }
    }

    /**
     * Implements the cycle-by-cycle behavior of Indexed Indirect (A.K.A Indexed,X) addressing mode read instructions
     * (LDA, ORA, EOR, AND, ADC, CMP, SBC, LAX)
     * @throws IllegalCycleException if the instruction is called at a cycle it shouldn't be.
     */
    private void indexedIndirectReadInstruction() throws IllegalCycleException {
        switch (this.currentInstructionCycle) {
            // "Fetch pointer address, increment PC"
            case 2 -> {
                this.addressBus = this.programCounter;
                this.readFromMemory();
                this.programCounter++;
            }
            // "Read from the address, add X to it"
            case 3 -> {
                // Fetched data in this cycle is discarded
                this.addressBus = (short) (this.dataBus & 0xFF);
                this.readFromMemory();
            }
            // "Fetch effective address low"
            case 4 -> {
                this.addressBus += (short) (this.registerX & 0xFF);
                this.addressBus &= 0xFF; // Zeroes out most significant byte
                this.readFromMemory();
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
                this.readFromMemory();
            }

            // "Read from effective address"
            case 6 -> {
                this.addressBus = (short) ((short) (this.dataBus << 8) | ((short) (this.retainedByte & 0xFF)));
                this.readFromMemory();
            }
            default -> throw new IllegalCycleException("This instruction accepts at most "
                    + this.currentInstruction.cycles() + ", received " + this.currentInstructionCycle);
        }
    }

    /**
     * Implements the cycle-by-cycle behavior of the Indirect Indexed (A.K.A. Indexed,Y) addressing mode
     * for read instructions (LDA, EOR, AND, ORA, ADC, SBC, CMP).
     * Takes an extra cycle in case of page crossing.
     * @throws IllegalCycleException if the instruction is called at a cycle it shouldn't be.
     */
    private void indirectIndexedReadInstruction() throws IllegalCycleException {
        switch (this.currentInstructionCycle) {
            // "Fetch pointer address, increment PC"
            case 2 -> {
                this.addressBus = this.programCounter;
                this.readFromMemory();
                this.programCounter++;
            }
            // "Fetch effective address low"
            case 3 -> {
                this.addressBus = (short) (this.dataBus & 0xFF);
                this.readFromMemory();
            }
            // "Fetch effective address high, add Y to low byte of effective address"
            case 4 -> {
                this.retainedByte = this.dataBus;
                this.addressBus++;
                this.addressBus &= 0xFF; // Effective address is always fetched from zero page
                this.readFromMemory();
            }
            // "Read from effective address, fix high byte of effective address"
            case 5 -> {
                final short sum = (short) ((short) (this.retainedByte & 0xFF) + (short) (this.registerY & 0xFF));
                // Detects page crossing
                if ((sum & 0x100) != 0) {
                    this.pageCrossed = 1;
                }
                this.addressBus = (short) (this.dataBus << 8 | (short) (sum & 0xFF));
                this.readFromMemory();
            }
            // "Read from effective address"
            case 6 -> {
                if (this.pageCrossed == 0) {
                    throw new IllegalCycleException("This cycle should only be run if a page crossing is" +
                            " identified, but that didn't happen.");
                }
                this.addressBus += 0x100;
                this.readFromMemory();
            }
            default -> throw new IllegalCycleException("This instruction accepts at most "
                    + this.currentInstruction.cycles() + ", received " + this.currentInstructionCycle);
        }
    }

    /* ==============================
     * READ-MODIFY-WRITE INSTRUCTIONS
     ================================ */

    /**
     * Represents the behavior of Zero Page Read-Modify-Write instructions.
     * (ASL, LSR, ROL, ROR, INC, DEC, SLO, SRE, RLA, RRA, ISB, DCP)
     * @throws IllegalCycleException in case the instruction reaches a cycle it's not supposed to.
     */
    private void zeroPageModifyInstruction() throws IllegalCycleException, UnimplementedInstructionException {
        switch (this.currentInstructionCycle) {
            // "Fetch address, increment PC"
            case 2 -> {
                this.addressBus = this.programCounter;
                this.readFromMemory();
                this.programCounter++;
            }
            // "Read from effective address"
            case 3 -> {
                this.addressBus = (short) (this.dataBus & 0xFF);
                this.readFromMemory();
            }
            // "Write the value back to effective address, and do the operation on it"
            case 4 -> {
                this.writeToMemory();
            }
            // "Write the new value to the effective address"
            case 5 -> { }
            default -> throw new IllegalCycleException("This instruction accepts at most "
                    + this.currentInstruction.cycles() + ", received " + this.currentInstructionCycle);
        }
    }

    /**
     * Represents the behavior of Zero Page Indexed Read-Modify-Write instructions.
     * (ASL, LSR, ROL, ROR, INC, DEC, SLO, SRE, RLA, RRA, ISB, DCP)
     * @param indexRegister the register used for indexing (either X or Y)
     * @throws IllegalCycleException if the instruction reaches a cycle it's not supposed to.
     * @throws UnimplementedInstructionException if the CPU calls this function when an instruction not listed above
     *                                           is running.
     */
    private void zeroPageIndexedModifyInstruction(byte indexRegister)
            throws IllegalCycleException, UnimplementedInstructionException {
        switch (this.currentInstructionCycle) {
            // "Fetch address, increment PC"
            case 2 -> {
                this.addressBus = this.programCounter;
                this.readFromMemory();
                this.programCounter++;
            }
            // "Read from address, add index register X to it"
            case 3 -> {
                this.addressBus = (short) ((short) this.dataBus & 0xFF);
                this.readFromMemory();
            }
            // "Read from effective address"
            case 4 -> {
                this.addressBus += indexRegister;
                this.addressBus &= 0xFF; // The effective address is always at page zero.
                this.readFromMemory();
            }
            // "Write the value back to effective address, and do the operation on it"
            case 5 -> this.writeToMemory();
            // "Write the new value to effective address"
            case 6 -> { }
            default -> throw new IllegalCycleException("This instruction accepts at most "
                    + this.currentInstruction.cycles() + ", received " + this.currentInstructionCycle);
        }
    }

    /**
     * Implements cycle-by-cycle behavior of absolute addressing read-modify-write instructions.
     * (ASL, LSR, ROL, ROR, INC, DEC, SLO, SRE, RLA, RRA, ISB, DCP)
     * @throws IllegalCycleException if the instruction runs for longer than it should.
     * @throws UnimplementedInstructionException if this function is called during the execution
     *                                           of an instruction not listed above.
     */
    private void absoluteModifyInstruction()
            throws IllegalCycleException, UnimplementedInstructionException {
        switch (this.currentInstructionCycle) {
            // "Fetch low byte of address, increment PC"
            case 2 -> {
                this.addressBus = this.programCounter;
                this.readFromMemory();
                this.programCounter++;
            }
            // "Fetch high byte of address, increment PC"
            case 3 -> {
                this.retainedByte = this.dataBus; // Saves low byte of address for later
                this.addressBus = this.programCounter;
                this.readFromMemory();
                this.programCounter++;
            }
            // "Read from effective address"
            case 4 -> {
                this.addressBus = (short) ((short) (this.dataBus << 8) | (this.retainedByte & 0xFF));
                this.readFromMemory();
            }
            // "Write the value back to effective address, and do the operation on it"
            case 5 -> this.writeToMemory();
            // "Write the new value to effective address"
            case 6 -> { }
            default -> throw new IllegalCycleException("This instruction accepts at most "
                    + this.currentInstruction.cycles() + ", received " + this.currentInstructionCycle);
        }
    }

    /**
     * Implements the cycle-to-cycle behavior of absolute indexed read-modify-write instructions.
     * (ASL, LSR, ROL, ROR, INC, DEC, SLO, SRE, RLA, RRA, ISB, DCP)
     * @throws IllegalCycleException if the instruction reaches a cycle it's not supposed to.
     * @throws UnimplementedInstructionException if this function is called by an instruction it's not supposed to.
     */
    private void absoluteIndexedModifyInstruction()
            throws IllegalCycleException, UnimplementedInstructionException{
        switch (this.currentInstructionCycle) {
            // "Fetch low byte of address, increment PC"
            case 2 -> {
                this.addressBus = this.programCounter;
                this.readFromMemory();
                this.programCounter++;
            }
            // "Fetch high byte of address, add index register X to low address byte, increment PC"
            case 3 -> {
                this.retainedByte = this.dataBus;
                this.addressBus = this.programCounter;
                this.readFromMemory();
                this.programCounter++;
            }
            // "Read from effective address, fix the high byte of effective address"
            case 4 -> {
                final int sum = (this.retainedByte & 0xFF) + (this.registerX & 0xFF);
                // Page crossing is ignored now, but will be fixed on the next cycle
                if (sum >= 0x100) {
                    this.pageCrossed = 1;
                }
                this.addressBus = (short) ((short) (this.dataBus << 8) | (sum & 0xFF));
                this.readFromMemory();
            }
            // "Re-read from effective address"
            case 5 -> {
                if (this.pageCrossed == 1) {
                    this.pageCrossed = 0;
                    this.addressBus += 0x100;
                    this.readFromMemory();
                }
            }
            // "Write the value back to effective address, and do the operation on it"
            case 6 -> this.writeToMemory();
            // "Write the new value to effective address"
            case 7 -> { }
            default -> throw new IllegalCycleException("This instruction accepts at most "
                    + this.currentInstruction.cycles() + ", received " + this.currentInstructionCycle);
        }
    }

    /* ==================
     * WRITE INSTRUCTIONS
     ==================== */

    /**
     * Implements the cycle-to-cycle behavior of Zero Page write instructions.
     * (STA, STX, STY, SAX).
     * @throws IllegalCycleException if the instruction reaches a cycle it's not supposed to.
     */
    private void zeroPageWriteInstruction() throws IllegalCycleException {
        switch (this.currentInstructionCycle) {
            // "Fetch address, increment PC"
            case 2 -> {
                this.addressBus = this.programCounter;
                this.readFromMemory();
                this.programCounter++;
            }
            // "Write register to effective address"
            case 3 -> this.addressBus = (short) (this.dataBus & 0xFF);
            default -> throw new IllegalCycleException("This instruction accepts at most "
                    + this.currentInstruction.cycles() + ", received " + this.currentInstructionCycle);
        }
    }

    /**
     * Implements the cycle-to-cycle behavior of Zero Page Indexed write instructions.
     * (STA, STX, STY, SAX)
     * @param indexRegister the register to add to the zero page address.
     * @throws IllegalCycleException if the instruction reaches a cycle it's not supposed to.
     */
    private void zeroPageIndexedWriteInstruction(byte indexRegister) throws IllegalCycleException {
        switch (this.currentInstructionCycle) {
            // "Fetch address, increment PC"
            case 2 -> {
                this.addressBus = this.programCounter;
                this.readFromMemory();
                this.programCounter++;
            }
            // "Read from address, add index register to it"
            case 3 -> {
                this.addressBus = (short) (this.dataBus & 0xFF);
                this.readFromMemory();
            }
            // "Write to effective address"
            case 4 -> {
                this.addressBus += (short) (indexRegister & 0xFF);
                this.addressBus &= 0xFF; // The effective address is always in page zero
            }
            default -> throw new IllegalCycleException("This instruction accepts at most "
                    + this.currentInstruction.cycles() + ", received " + this.currentInstructionCycle);
        }
    }

    /**
     * Implements cycle-to-cycle behavior of absolute addressing mode write instructions.
     * (STA, STX, STY, SAX)
     * @throws IllegalCycleException if the instruction reaches a cycle it's not supposed to.
     */
    private void absoluteWriteInstruction() throws IllegalCycleException {
        switch (this.currentInstructionCycle) {
            // "Fetch low byte of address, increment PC"
            case 2 -> {
                this.addressBus = this.programCounter;
                this.readFromMemory();
                this.retainedByte = this.dataBus;
                this.programCounter++;
            }
            // "Fetch high byte of address, increment PC"
            case 3 -> {
                this.addressBus = this.programCounter;
                this.readFromMemory();
                this.programCounter++;
            }
            // "Write register to effective address"
            case 4 -> {
                final int effectiveAddress = (this.dataBus << 8) | (this.retainedByte & 0xFF);
                this.addressBus = (short) effectiveAddress;
            }
            default -> throw new IllegalCycleException("This instruction accepts at most "
                    + this.currentInstruction.cycles() + ", received " + this.currentInstructionCycle);
        }
    }

    /**
     * Implements cycle-to-cycle behavior of absolute indexed addressing mode write instructions.
     * (STA, STX, STY, SAX)
     * @param indexRegister the register used as indexing (either X or Y).
     * @throws IllegalCycleException if the instruction reaches a cycle it's not supposed to.
     */
    private void absoluteIndexedWriteInstruction(byte indexRegister) throws IllegalCycleException {
        switch (this.currentInstructionCycle) {
            // "Fetch low byte of address, increment PC"
            case 2 -> {
                this.addressBus = this.programCounter;
                this.readFromMemory();
                this.retainedByte = this.dataBus;
                this.programCounter++;
            }
            // "Fetch high byte of address, add index register to low address byte, increment PC"
            case 3 -> {
                this.addressBus = this.programCounter;
                this.readFromMemory();
                this.programCounter++;
            }
            // "Read from effective address, fix the high byte of effective address"
            case 4 -> {
                int lowByteSum = (this.retainedByte & 0xFF) + (indexRegister & 0xFF);
                if (lowByteSum >= 0x100) {
                    this.pageCrossed = 1;
                    lowByteSum -= 0x100;
                }
                final int effectiveAddress = (this.dataBus << 8) + lowByteSum;
                this.addressBus = (short) effectiveAddress;
                this.readFromMemory();
            }
            // "Write to effective address"
            case 5 -> {
                if (this.pageCrossed == 1) {
                    this.pageCrossed = 0;
                    this.addressBus += 0x100;
                }
            }
            default -> throw new IllegalCycleException("This instruction accepts at most "
                    + this.currentInstruction.cycles() + ", received " + this.currentInstructionCycle);
        }
    }

    /**
     * Implements cycle-to-cycle behavior of indexed indirect addressing mode write instructions.
     * (STA, SAX)
     * @throws IllegalCycleException if the instruction reaches a cycle it's not supposed to.
     */
    private void indexedIndirectWriteInstruction() throws IllegalCycleException {
        switch (this.currentInstructionCycle) {
            // "Fetch pointer address, increment PC"
            case 2 -> {
                this.addressBus = this.programCounter;
                this.readFromMemory();
                this.programCounter++;
            }
            // "Read from the address, add X to it"
            case 3 -> {
                this.addressBus = (short) (this.dataBus & 0xFF);
                this.readFromMemory();
            }
            // "Fetch effective address low"
            case 4 -> {
                this.addressBus += (short) (this.registerX & 0xFF);
                this.addressBus &= 0xFF; // Zero page boundary crossing not handled
                this.readFromMemory();
                this.retainedByte = this.dataBus;
            }
            // "Fetch effective address high"
            case 5 -> {
                this.addressBus++;
                this.addressBus &= 0xFF; // Zero page boundary crossing not handled
                this.readFromMemory();
            }
            // "Write to effective address"
            case 6 -> {
                this.addressBus = (short) ((this.dataBus << 8) | (this.retainedByte & 0xFF));
            }
            default -> throw new IllegalCycleException("This instruction accepts at most "
                    + this.currentInstruction.cycles() + ", received " + this.currentInstructionCycle);
        }
    }

    /**
     * Implements cycle-to-cycle behavior of indirect indexed addressing mode write instructions.
     * (STA, SHA)
     * @throws IllegalCycleException if the instruction reaches a cycle it's not supposed to.
     */
    private void indirectIndexedWriteInstruction() throws IllegalCycleException {
        switch (this.currentInstructionCycle) {
            // "Fetch pointer address, increment PC"
            case 2 -> {
                this.addressBus = this.programCounter;
                this.readFromMemory();
                this.programCounter++;
            }
            // "Fetch effective address low"
            case 3 -> {
                this.addressBus = (short) (this.dataBus & 0xFF);
                this.readFromMemory();
                this.retainedByte = this.dataBus;
            }
            // "Fetch effective address high, add Y to low byte of effective address"
            case 4 -> {
                this.addressBus++;
                this.addressBus &= 0xFF; // Effective address is always fetched from zero page
                this.readFromMemory();
            }
            // "Read from effective address, fix high byte of effective address"
            case 5 -> {
                int lowByteOfAddress = (this.retainedByte & 0xFF) + (this.registerY & 0xFF);
                if (lowByteOfAddress >= 0x100) {
                    this.pageCrossed = 1;
                    lowByteOfAddress -= 0x100;
                }
                this.addressBus = (short) ((this.dataBus << 8) + lowByteOfAddress);
                this.readFromMemory();
            }
            // "Write to effective address"
            case 6 -> {
                if (this.pageCrossed == 1) {
                    this.pageCrossed = 0;
                    this.addressBus += 0x100;
                }
            }
            default -> throw new IllegalCycleException("This instruction accepts at most "
                    + this.currentInstruction.cycles() + ", received " + this.currentInstructionCycle);
        }
    }

    /**
     * Implements cycle-to-cycle behavior of relative addressing mode instructions.
     * (BCC, BCS, BEQ, BMI, BNE, BPL, BVC, BVS)
     * @param condition determines if branch is taken.
     * @throws IllegalCycleException if the instruction reaches a cycle it's not supposed to.
     */
    private void relativeInstruction(boolean condition) throws IllegalCycleException {
        switch (this.currentInstructionCycle) {
            // "Fetch operand, increment PC"
            case 2 -> {
                this.addressBus = this.programCounter;
                this.readFromMemory();
                this.programCounter++;
                if (condition) {
                    this.pageCrossed = 1; // Instruction takes one more cycle if branch is taken
                }
            }
            // "Fetch opcode of next instruction.
            // If branch is taken, add operand to PCL
            // Otherwise increment PC"
            case 3 -> {
                this.addressBus = this.programCounter;
                final int pageBefore = this.programCounter & 0xFF00;
                this.programCounter += this.dataBus;
                final int pageAfter = this.programCounter & 0xFF00;
                // Reverts page crossing to correct later
                if (pageBefore != pageAfter) {
                    this.pageCrossed = 2;
                    this.programCounter &= 0x00FF;
                    this.programCounter |= (short) pageBefore;
                }
                this.retainedByte = (byte) (pageAfter >> 8);
                this.readFromMemory(); // Read from memory and discard
            }
            // "Fetch opcode of next instruction
            // Fix PCH. If it did not change, increment PC."
            case 4 -> {
                this.addressBus = this.programCounter;
                this.readFromMemory();
                // Set high byte to contents of retained byte
                this.programCounter &= 0xFF;
                this.programCounter |= (short) (this.retainedByte << 8);
            }
            default -> throw new IllegalCycleException("This instruction accepts at most "
                    + this.currentInstruction.cycles() + ", received " + this.currentInstructionCycle);
        }
    }

    /**
     * Implements cycle-to-cycle behavior of the absolute addressing jump instruction.
     * @throws IllegalCycleException if the instruction reaches a cycle it's not supposed to.
     */
    private void absoluteJumpInstruction() throws IllegalCycleException {
        switch (this.currentInstructionCycle) {
            // "Fetch low address byte, increment PC"
            case 2 -> {
                this.addressBus = this.programCounter;
                this.readFromMemory();
                this.programCounter++;
            }
            // "Copy low address byte to PCL, fetch high address byte to PCH"
            case 3 -> {
                this.retainedByte = this.dataBus;
                this.addressBus = this.programCounter;
                this.readFromMemory();
                this.programCounter++;
            }
            default -> throw new IllegalCycleException("This instruction accepts at most "
                    + this.currentInstruction.cycles() + ", received " + this.currentInstructionCycle);
        }
    }

    /**
     * Implements cycle-to-cycle behavior of indirect addressing mode instructions.
     * JMP is the only instruction to implement this addressing mode.
     * @throws IllegalCycleException if the instruction reaches a cycle it's not supposed to.
     */
    private void indirectInstruction() throws IllegalCycleException {
        switch (this.currentInstructionCycle) {
            // "Fetch pointer address low, increment PC"
            case 2 -> {
                this.addressBus = this.programCounter;
                this.readFromMemory();
                this.programCounter++;
            }
            // "Fetch pointer address high, increment PC"
            case 3 -> {
                this.retainedByte = this.dataBus;
                this.addressBus = this.programCounter;
                this.readFromMemory();
                this.programCounter++;
            }
            // "Fetch low address to latch"
            case 4 -> {
                this.addressBus = (short) (((this.dataBus & 0xFF) << 8) | (this.retainedByte & 0xFF));
                this.readFromMemory();
            }
            // "Fetch PCH copy latch to PCL"
            case 5 -> {
                this.retainedByte = this.dataBus;
                int incrementedLowByte = (this.addressBus & 0xFF) + 1;
                /*
                 * Address is always fetched from the same page,
                 * i.e., page crossing is not handled.
                 */
                incrementedLowByte &= 0xFF;
                this.addressBus &= (short) 0xFF00;
                this.addressBus |= (short) incrementedLowByte;
                this.readFromMemory();
            }
            default -> throw new IllegalCycleException("This instruction accepts at most "
                    + this.currentInstruction.cycles() + ", received " + this.currentInstructionCycle);
        }
    }


    /* ==================================
     * INSTRUCTIONS THAT ACCESS THE STACK
     ==================================== */

    /**
     * Forces interrupt request. PC and processor status are pushed to the stack
     * and IRQ interrupt vector at $FFFE/F is loaded into PC and the break flag
     * in the status is set to one.
     * @throws IllegalCycleException if the instruction is called at a cycle it shouldn't be.
     */
    private void brkCycleByCycle() throws IllegalCycleException {
        switch (this.currentInstructionCycle) {
            /* "Read next instruction byte and throw it away,
             * increment PC."
             */
            case 2 -> {
                this.addressBus = this.programCounter;
                this.readFromMemory();
                this.programCounter++;
            }
            // "Push PCH on stack (with B flag set), decrement S"
            case 3 -> this.stackPush((byte) (this.programCounter >> 8));
            // "Push PCL on stack, decrement S"
            case 4 -> this.stackPush((byte) (this.programCounter & 0xFF));
            // "Push P on stack, decrement S"
            case 5 -> this.stackPush((byte) (this.status | BREAK));
            // "Fetch PCL"
            case 6 -> {
                this.addressBus = IRQ_VECTOR_LOW;
                this.readFromMemory();
            }
            // "Fetch PCH"
            case 7 -> {
                this.retainedByte = this.dataBus;
                this.addressBus = IRQ_VECTOR_HIGH;
                this.readFromMemory();
            }
            default -> throw new IllegalCycleException(this);
        }
    }

    /**
     * Implements the cycle-to-cycle behavior of the RTI instruction.
     * @throws IllegalCycleException if the instruction reaches a cycle it's not supposed to.
     */
    private void rtiCycleToCycle() throws IllegalCycleException {
        switch (this.currentInstructionCycle) {
            // "Read next instruction byte (and throw it away)"
            case 2 -> {
                this.addressBus = this.programCounter;
                this.readFromMemory();
            }
            // "Increment S"
            case 3 -> {
                this.stackPointer--;
                stackPop();
            }
            // "Pull P from stack, increment S"
            case 4 -> {
                this.stackPop();
                this.status = this.dataBus;
            }
            // "Pull PCL from stack, increment S"
            case 5 -> {
                this.stackPop();
                this.programCounter &= (short) 0xFF00;
                this.programCounter |= this.dataBus;
            }
            // "Pull PCH from stack"
            case 6 -> {
                this.stackPop();
                this.programCounter &= 0x00FF;
                this.programCounter |= (short) ((this.dataBus & 0xFF) << 8);
            }
            default -> throw new IllegalCycleException(this);
        }
    }

    private void rtsCycleToCycle() throws IllegalCycleException {
        switch (this.currentInstructionCycle) {
            // "Read next instruction byte (and throw it away"
            case 2 -> {
                this.addressBus = this.programCounter;
                this.readFromMemory();
                this.programCounter++;
            }
            // "Increment S"
            case 3 -> {
                this.stackPointer--;
                this.stackPop();
            }
            // "Pull PCL from stack, increment S"
            case 4 -> {
                this.stackPop();
                this.programCounter &= (short) 0xFF00;
                this.programCounter |= (short) (this.dataBus & 0xFF);
            }
            // "Pull PCH from stack"
            case 5 -> {
                this.stackPop();
                this.programCounter &= 0x00FF;
                this.programCounter |= (short) ((this.dataBus & 0xFF) << 8);
            }
            // "Increment PC"
            case 6 -> {
                this.addressBus = this.programCounter;
                this.readFromMemory();
                this.programCounter++;
            }
            default -> throw new IllegalCycleException(this);
        }
    }

    /**
     * Implements cycle-to-cycle behavior of instructions that push a register to the stack.
     * (PHA, PHP)
     * @throws IllegalCycleException if the instruction reaches a cycle it's not supposed to.
     */
    private void pushRegisterCycleByCycle() throws IllegalCycleException {
        switch (this.currentInstructionCycle) {
            // "Read next instruction byte (and throw it away)
            case 2 -> {
                this.addressBus = this.programCounter;
                this.readFromMemory();
            }
            // "Push register on stack, decrement S"
            case 3 -> {
                // PHP pushes P with the B flag set
                if (this.currentInstruction.opcode() == 0x08) {
                    this.stackPush((byte) (this.status | BREAK));
                } else {
                    this.stackPush(this.accumulator);
                }
            }
            default -> throw new IllegalCycleException(this);
        }
    }

    /**
     * Implements cycle-to-cycle behavior of instructions that pull a register from the stack.
     * (PLA, PLP)
     * @throws IllegalCycleException if the instruction reaches a cycle it's not supposed to.
     */
    private void pullRegisterCycleByCycle() throws IllegalCycleException {
        switch (this.currentInstructionCycle) {
            // "Read next instruction byte (and throw it away)"
            case 2 -> {
                this.addressBus = this.programCounter;
                this.readFromMemory();
            }
            // "Increment S"
            case 3 -> {
                this.stackPointer--;
                this.stackPop();
            }
            // "Pull register from stack"
            case 4 -> this.stackPop();
            default -> throw new IllegalCycleException(this);
        }
    }

    /**
     * Implements cycle-to-cycle behavior of the JSR instruction.
     * @throws IllegalCycleException if the instruction reaches a cycle it's not supposed to.
     */
    private void jsrCycleByCycle() throws IllegalCycleException {
        switch (this.currentInstructionCycle) {
            // "Fetch low address byte, increment PC"
            case 2 -> {
                this.addressBus = this.programCounter;
                this.readFromMemory();
                this.retainedByte = this.dataBus;
                this.programCounter++;
            }
            // "Internal operation (pre-decrement S?)"
            case 3 -> {
                this.stackPointer--;
                this.stackPop();
            }
            // "Push PCH on stack, decrement S"
            case 4 -> {
                this.stackPush((byte) ((this.programCounter & 0xFF00) >> 8));
            }
            // "Push PCL on stack, decrement S"
            case 5 -> {
                this.stackPush((byte) (this.programCounter & 0xFF));
            }
            // "Copy low address byte to PCL, fetch high address byte to PCH"
            case 6 -> {
                this.addressBus = this.programCounter;
                this.readFromMemory();
                this.programCounter = (short) (((this.dataBus & 0xFF) << 8) | (this.retainedByte & 0xFF));
            }
            default -> throw new IllegalCycleException(this);
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
            sum = ((lowNibble + 0x06) & 0xF) + 0x10;
        }

        // Add most significant nibble (highest digit)
        sum += (unsignedAccumulator & 0xF0) + (unsignedOperand & 0xF0);
        if (sum >= 0xA0) {
            sum += 0x60;
        }

        // Carry is set based on the final result
        if (sum >= 0x100) {
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
