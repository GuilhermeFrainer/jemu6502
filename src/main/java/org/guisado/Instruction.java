package org.guisado;

/**
 * Represents an instruction.
 */
public record Instruction(org.guisado.Instruction.AddressingMode addressingMode, byte opcode, int bytes, int cycles,
                          String mnemonic, InstructionType type) {
    /**
     * Represents different memory addressing modes for instructions.
     */
    public enum AddressingMode {
        Accumulator,
        Implied,
        Immediate,
        ZeroPage,
        ZeroPageX,
        ZeroPageY,
        Absolute,
        AbsoluteX,
        AbsoluteY,
        Indirect,
        IndirectX,
        IndirectY
    }

    public enum InstructionType {
        Read,
        Write,
        ReadModifyWrite,
    }

    /**
     * @param addressingMode Enum value representing the instruction's addressing mode.
     * @param opcode Byte representing the instruction's opcode.
     * @param bytes Number of bytes the instruction and its arguments occupy in memory.
     * @param cycles The minimum number of cycles the instruction takes to run.
     *               Might take more in case of page crossing or successfully branching.
     * @param mnemonic       the instruction's mnemonic. Must be string with three letters plus an optional '*' if
     *                       it's an "unofficial" instruction.
     * @param type Represents the type of the instruction.
     * @throws IllegalArgumentException in case invalid mnemonics (i.e. length > 4), 'bytes' (> 3) or 'cycles' (>7)
     *                                  are passed.
     */
    public Instruction {
        if (mnemonic.length() > 4) {
            throw new IllegalArgumentException("String length must be at most 4. Received string of length " + mnemonic.length());
        } else if (bytes > 3 || bytes < 1) {
            throw new IllegalArgumentException("'bytes' must be in the range [1, 3]. Got " + bytes);
        } else if (cycles > 7 || cycles < 1) {
            throw new IllegalArgumentException("´cycles' must be in the range [1, 7]. Got " + cycles);

        }
    }

    /**
     * @return this instruction's addressing mode.
     */
    @Override
    public AddressingMode addressingMode() {
        return this.addressingMode;
    }

    /**
     * @return this instruction's opcode.
     */
    @Override
    public byte opcode() {
        return this.opcode;
    }

    /**
     * @return this instruction's number of bytes.
     * This means the instruction byte + the operands.
     */
    @Override
    public int bytes() {
        return this.bytes;
    }

    /**
     * @return this instruction's cycle count ignoring page crossing.
     */
    @Override
    public int cycles() {
        return this.cycles;
    }

    /**
     * @return this instructions mnemonic.
     */
    @Override
    public String mnemonic() {
        return this.mnemonic;
    }

    /**
     * Initializes array with the entire 6502 instruction set.
     * @return the array.
     */
    public static Instruction[] initializeInstructionSet() {
        Instruction[] instructionSet = new Instruction[256];

        /* =
         * A
         === */

        // ADC
        instructionSet[0x69] = new Instruction(AddressingMode.Immediate, (byte) 0x69, 2, 2, "ADC", InstructionType.Read);
        instructionSet[0x65] = new Instruction(AddressingMode.ZeroPage, (byte) 0x65, 2, 3, "ADC", InstructionType.Read);
        instructionSet[0x75] = new Instruction(AddressingMode.ZeroPageX, (byte) 0x75, 2, 4, "ADC", InstructionType.Read);
        instructionSet[0x6d] = new Instruction(AddressingMode.Absolute, (byte) 0x6d, 3, 4, "ADC", InstructionType.Read);
        instructionSet[0x7d] = new Instruction(AddressingMode.AbsoluteX, (byte) 0x7d, 3, 4, "ADC", InstructionType.Read);
        instructionSet[0x79] = new Instruction(AddressingMode.AbsoluteY, (byte) 0x79, 3, 4, "ADC", InstructionType.Read);
        instructionSet[0x61] = new Instruction(AddressingMode.IndirectX, (byte) 0x61, 2, 6, "ADC", InstructionType.Read);
        instructionSet[0x71] = new Instruction(AddressingMode.IndirectY, (byte) 0x71, 2, 5, "ADC", InstructionType.Read);

        // AND
        instructionSet[0x29] = new Instruction(AddressingMode.Immediate, (byte) 0x29, 2, 2, "AND", InstructionType.Read);
        instructionSet[0x25] = new Instruction(AddressingMode.ZeroPage, (byte) 0x25, 2, 3, "AND", InstructionType.Read);
        instructionSet[0x35] = new Instruction(AddressingMode.ZeroPageX, (byte) 0x35, 2, 4, "AND", InstructionType.Read);
        instructionSet[0x2d] = new Instruction(AddressingMode.Absolute, (byte) 0x2d, 3, 4, "AND", InstructionType.Read);
        instructionSet[0x3d] = new Instruction(AddressingMode.AbsoluteX, (byte) 0x3d, 3, 4, "AND", InstructionType.Read);
        instructionSet[0x39] = new Instruction(AddressingMode.AbsoluteY, (byte) 0x39, 3, 4, "AND", InstructionType.Read);
        instructionSet[0x21] = new Instruction(AddressingMode.IndirectX, (byte) 0x21, 2, 6, "AND", InstructionType.Read);
        instructionSet[0x31] = new Instruction(AddressingMode.IndirectY, (byte) 0x31, 2, 5, "AND", InstructionType.Read);

        // ASL
        instructionSet[0x0A] = new Instruction(AddressingMode.Accumulator, (byte) 0x0A, 1, 2, "ASL", InstructionType.ReadModifyWrite);
        instructionSet[0x06] = new Instruction(AddressingMode.ZeroPage, (byte) 0x06, 2, 5, "ASL", InstructionType.ReadModifyWrite);
        instructionSet[0x16] = new Instruction(AddressingMode.ZeroPageX, (byte) 0x16, 2, 6, "ASL", InstructionType.ReadModifyWrite);
        instructionSet[0x0E] = new Instruction(AddressingMode.Absolute, (byte) 0x0E, 3, 6, "ASL", InstructionType.ReadModifyWrite);
        instructionSet[0x1E] = new Instruction(AddressingMode.AbsoluteX, (byte) 0x1E, 3, 7, "ASL", InstructionType.ReadModifyWrite);

        /* =
         * B
         === */

        // BIT
        instructionSet[0x24] = new Instruction(AddressingMode.ZeroPage, (byte) 0x24, 2, 3, "BIT", InstructionType.Read);
        instructionSet[0x2C] = new Instruction(AddressingMode.Absolute, (byte) 0x2C, 3, 4, "BIT", InstructionType.Read);

        // BRK
        instructionSet[0x00] = new Instruction(AddressingMode.Implied, (byte) 0x00, 1, 7, "BRK", InstructionType.Read);

        // CMP
        instructionSet[0xc9] = new Instruction(AddressingMode.Immediate, (byte) 0xc9, 2, 2, "CMP", InstructionType.Read);
        instructionSet[0xc5] = new Instruction(AddressingMode.ZeroPage, (byte) 0xc5, 2, 3, "CMP", InstructionType.Read);
        instructionSet[0xd5] = new Instruction(AddressingMode.ZeroPageX, (byte) 0xd5, 2, 4, "CMP", InstructionType.Read);
        instructionSet[0xcd] = new Instruction(AddressingMode.Absolute, (byte) 0xcd, 3, 4, "CMP", InstructionType.Read);
        instructionSet[0xdd] = new Instruction(AddressingMode.AbsoluteX, (byte) 0xdd, 3, 4, "CMP", InstructionType.Read);
        instructionSet[0xd9] = new Instruction(AddressingMode.AbsoluteY, (byte) 0xd9, 3, 4, "CMP", InstructionType.Read);
        instructionSet[0xc1] = new Instruction(AddressingMode.IndirectX, (byte) 0xc1, 2, 6, "CMP", InstructionType.Read);
        instructionSet[0xd1] = new Instruction(AddressingMode.IndirectY, (byte) 0xd1, 2, 5, "CMP", InstructionType.Read);

        // EOR
        instructionSet[0x49] = new Instruction(AddressingMode.Immediate, (byte) 0x49, 2, 2, "EOR", InstructionType.Read);
        instructionSet[0x45] = new Instruction(AddressingMode.ZeroPage, (byte) 0x45, 2, 3, "EOR", InstructionType.Read);
        instructionSet[0x55] = new Instruction(AddressingMode.ZeroPageX, (byte) 0x55, 2, 4, "EOR", InstructionType.Read);
        instructionSet[0x4d] = new Instruction(AddressingMode.Absolute, (byte) 0x4d, 3, 4, "EOR", InstructionType.Read);
        instructionSet[0x5d] = new Instruction(AddressingMode.AbsoluteX, (byte) 0x5d, 3, 4, "EOR", InstructionType.Read);
        instructionSet[0x59] = new Instruction(AddressingMode.AbsoluteY, (byte) 0x59, 3, 4, "EOR", InstructionType.Read);
        instructionSet[0x41] = new Instruction(AddressingMode.IndirectX, (byte) 0x41, 2, 6, "EOR", InstructionType.Read);
        instructionSet[0x51] = new Instruction(AddressingMode.IndirectY, (byte) 0x51, 2, 5, "EOR", InstructionType.Read);

        /* ======================
         * INCREMENT INSTRUCTIONS
         ======================== */

        // INX
        instructionSet[0xE8] = new Instruction(AddressingMode.Implied, (byte) 0xE8, 1, 2, "INX", InstructionType.Read);

        /* =================
         * LOAD INSTRUCTIONS
         =================== */

        // LDA
        instructionSet[0xA9] = new Instruction(AddressingMode.Immediate, (byte) 0xA9, 2, 2, "LDA", InstructionType.Read);
        instructionSet[0xA5] = new Instruction(AddressingMode.ZeroPage, (byte) 0xA5, 2, 3, "LDA", InstructionType.Read);
        instructionSet[0xB5] = new Instruction(AddressingMode.ZeroPageX, (byte) 0xB5, 2, 4, "LDA", InstructionType.Read);
        instructionSet[0xAD] = new Instruction(AddressingMode.Absolute, (byte) 0xAD, 3, 4, "LDA", InstructionType.Read);
        instructionSet[0xBD] = new Instruction(AddressingMode.AbsoluteX, (byte) 0xBD, 3, 4, "LDA", InstructionType.Read);
        instructionSet[0xB9] = new Instruction(AddressingMode.AbsoluteY, (byte) 0xB9, 3, 4, "LDA", InstructionType.Read);
        instructionSet[0xA1] = new Instruction(AddressingMode.IndirectX, (byte) 0xA1, 2, 6, "LDA", InstructionType.Read);
        instructionSet[0xB1] = new Instruction(AddressingMode.IndirectY, (byte) 0xB1, 2, 5, "LDA", InstructionType.Read);

        // LDX
        instructionSet[0xA2] = new Instruction(AddressingMode.Immediate, (byte) 0xA2, 2, 2, "LDX", InstructionType.Read);
        instructionSet[0xA6] = new Instruction(AddressingMode.ZeroPage, (byte) 0xA6, 2, 3, "LDX", InstructionType.Read);
        instructionSet[0xB6] = new Instruction(AddressingMode.ZeroPageY, (byte) 0xB6, 2, 4, "LDX", InstructionType.Read);
        instructionSet[0xAE] = new Instruction(AddressingMode.Absolute, (byte) 0xAE, 3, 4, "LDX", InstructionType.Read);
        instructionSet[0xBE] = new Instruction(AddressingMode.AbsoluteY, (byte) 0xBE, 3, 4, "LDX", InstructionType.Read);

        // LDY
        instructionSet[0xA0] = new Instruction(AddressingMode.Immediate, (byte) 0xA0, 2, 2, "LDY", InstructionType.Read);
        instructionSet[0xA4] = new Instruction(AddressingMode.ZeroPage, (byte) 0xA4, 2, 3, "LDY", InstructionType.Read);
        instructionSet[0xB4] = new Instruction(AddressingMode.ZeroPageX, (byte) 0xB4, 2, 4, "LDY", InstructionType.Read);
        instructionSet[0xAC] = new Instruction(AddressingMode.Absolute, (byte) 0xAC, 3, 4, "LDY", InstructionType.Read);
        instructionSet[0xBC] = new Instruction(AddressingMode.AbsoluteX, (byte) 0xBC, 3, 4, "LDY", InstructionType.Read);

        // NOP
        instructionSet[0xEA] = new Instruction(AddressingMode.Implied, (byte) 0xEA, 1, 2, "NOP", InstructionType.Read);

        // ORA
        instructionSet[0x09] = new Instruction(AddressingMode.Immediate, (byte) 0x09, 2, 2, "ORA", InstructionType.Read);
        instructionSet[0x05] = new Instruction(AddressingMode.ZeroPage, (byte) 0x05, 2, 3, "ORA", InstructionType.Read);
        instructionSet[0x15] = new Instruction(AddressingMode.ZeroPageX, (byte) 0x15, 2, 4, "ORA", InstructionType.Read);
        instructionSet[0x0d] = new Instruction(AddressingMode.Absolute, (byte) 0x0d, 3, 4, "ORA", InstructionType.Read);
        instructionSet[0x1d] = new Instruction(AddressingMode.AbsoluteX, (byte) 0x1d, 3, 4, "ORA", InstructionType.Read);
        instructionSet[0x19] = new Instruction(AddressingMode.AbsoluteY, (byte) 0x19, 3, 4, "ORA", InstructionType.Read);
        instructionSet[0x01] = new Instruction(AddressingMode.IndirectX, (byte) 0x01, 2, 6, "ORA", InstructionType.Read);
        instructionSet[0x11] = new Instruction(AddressingMode.IndirectY, (byte) 0x11, 2, 5, "ORA", InstructionType.Read);

        // SBC
        instructionSet[0xe9] = new Instruction(AddressingMode.Immediate, (byte) 0xe9, 2, 2, "SBC", InstructionType.Read);
        instructionSet[0xe5] = new Instruction(AddressingMode.ZeroPage, (byte) 0xe5, 2, 3, "SBC", InstructionType.Read);
        instructionSet[0xf5] = new Instruction(AddressingMode.ZeroPageX, (byte) 0xf5, 2, 4, "SBC", InstructionType.Read);
        instructionSet[0xed] = new Instruction(AddressingMode.Absolute, (byte) 0xed, 3, 4, "SBC", InstructionType.Read);
        instructionSet[0xfd] = new Instruction(AddressingMode.AbsoluteX, (byte) 0xfd, 3, 4, "SBC", InstructionType.Read);
        instructionSet[0xf9] = new Instruction(AddressingMode.AbsoluteY, (byte) 0xf9, 3, 4, "SBC", InstructionType.Read);
        instructionSet[0xe1] = new Instruction(AddressingMode.IndirectX, (byte) 0xe1, 2, 6, "SBC", InstructionType.Read);
        instructionSet[0xf1] = new Instruction(AddressingMode.IndirectY, (byte) 0xf1, 2, 5, "SBC", InstructionType.Read);

        /* =====================
         * TRANSFER INSTRUCTIONS
         ======================= */

        // TAX
        instructionSet[0xAA] = new Instruction(AddressingMode.Implied, (byte) 0xAA, 1, 2, "TAX", InstructionType.Read);

        return instructionSet;
    }
}
