package org.guisado.jemu6502;

/**
 * Represents an instruction.
 */
public record Instruction(org.guisado.jemu6502.Instruction.AddressingMode addressingMode, byte opcode, int bytes, int cycles,
                          String mnemonic) {
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
        IndirectY,
        Relative
    }

    /**
     * @param addressingMode Enum value representing the instruction's addressing mode.
     * @param opcode Byte representing the instruction's opcode.
     * @param bytes Number of bytes the instruction and its arguments occupy in memory.
     * @param cycles The minimum number of cycles the instruction takes to run.
     *               Might take more in case of page crossing or successfully branching.
     * @param mnemonic       the instruction's mnemonic. Must be string with three letters plus an optional '*' if
     *                       it's an "unofficial" instruction.
     * @throws IllegalArgumentException in case invalid mnemonics (i.e. length > 4), 'bytes' (> 3) or 'cycles' (>7)
     *                                  are passed.
     */
    public Instruction {
        if (mnemonic.length() > 4) {
            throw new IllegalArgumentException("String length must be at most 4. Received string of length " + mnemonic.length());
        } else if (bytes > 3 || bytes < 1) {
            throw new IllegalArgumentException("'bytes' must be in the range [1, 3]. Got " + bytes);
        } else if (cycles > 8 || cycles < 1) {
            throw new IllegalArgumentException("´cycles' must be in the range [1, 8]. Got " + cycles);

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
        instructionSet[0x69] = new Instruction(AddressingMode.Immediate, (byte) 0x69, 2, 2, "ADC");
        instructionSet[0x65] = new Instruction(AddressingMode.ZeroPage, (byte) 0x65, 2, 3, "ADC");
        instructionSet[0x75] = new Instruction(AddressingMode.ZeroPageX, (byte) 0x75, 2, 4, "ADC");
        instructionSet[0x6d] = new Instruction(AddressingMode.Absolute, (byte) 0x6d, 3, 4, "ADC");
        instructionSet[0x7d] = new Instruction(AddressingMode.AbsoluteX, (byte) 0x7d, 3, 4, "ADC");
        instructionSet[0x79] = new Instruction(AddressingMode.AbsoluteY, (byte) 0x79, 3, 4, "ADC");
        instructionSet[0x61] = new Instruction(AddressingMode.IndirectX, (byte) 0x61, 2, 6, "ADC");
        instructionSet[0x71] = new Instruction(AddressingMode.IndirectY, (byte) 0x71, 2, 5, "ADC");

        // AND
        instructionSet[0x29] = new Instruction(AddressingMode.Immediate, (byte) 0x29, 2, 2, "AND");
        instructionSet[0x25] = new Instruction(AddressingMode.ZeroPage, (byte) 0x25, 2, 3, "AND");
        instructionSet[0x35] = new Instruction(AddressingMode.ZeroPageX, (byte) 0x35, 2, 4, "AND");
        instructionSet[0x2d] = new Instruction(AddressingMode.Absolute, (byte) 0x2d, 3, 4, "AND");
        instructionSet[0x3d] = new Instruction(AddressingMode.AbsoluteX, (byte) 0x3d, 3, 4, "AND");
        instructionSet[0x39] = new Instruction(AddressingMode.AbsoluteY, (byte) 0x39, 3, 4, "AND");
        instructionSet[0x21] = new Instruction(AddressingMode.IndirectX, (byte) 0x21, 2, 6, "AND");
        instructionSet[0x31] = new Instruction(AddressingMode.IndirectY, (byte) 0x31, 2, 5, "AND");

        // ASL
        instructionSet[0x0A] = new Instruction(AddressingMode.Accumulator, (byte) 0x0A, 1, 2, "ASL");
        instructionSet[0x06] = new Instruction(AddressingMode.ZeroPage, (byte) 0x06, 2, 5, "ASL");
        instructionSet[0x16] = new Instruction(AddressingMode.ZeroPageX, (byte) 0x16, 2, 6, "ASL");
        instructionSet[0x0E] = new Instruction(AddressingMode.Absolute, (byte) 0x0E, 3, 6, "ASL");
        instructionSet[0x1E] = new Instruction(AddressingMode.AbsoluteX, (byte) 0x1E, 3, 7, "ASL");

        /* =
         * B
         === */

        // BIT
        instructionSet[0x24] = new Instruction(AddressingMode.ZeroPage, (byte) 0x24, 2, 3, "BIT");
        instructionSet[0x2C] = new Instruction(AddressingMode.Absolute, (byte) 0x2C, 3, 4, "BIT");

        // BRK
        instructionSet[0x00] = new Instruction(AddressingMode.Implied, (byte) 0x00, 1, 7, "BRK");

        /* ===================
         * BRANCH INSTRUCTIONS
         ===================== */

        instructionSet[0x90] = new Instruction(AddressingMode.Relative, (byte) 0x90, 1 ,2, "BCC");
        instructionSet[0xB0] = new Instruction(AddressingMode.Relative, (byte) 0xB0, 1 ,2, "BCS");
        instructionSet[0xF0] = new Instruction(AddressingMode.Relative, (byte) 0xF0, 1 ,2, "BEQ");
        instructionSet[0x30] = new Instruction(AddressingMode.Relative, (byte) 0x30, 1 ,2, "BMI");
        instructionSet[0xD0] = new Instruction(AddressingMode.Relative, (byte) 0xD0, 1 ,2, "BNE");
        instructionSet[0x10] = new Instruction(AddressingMode.Relative, (byte) 0x10, 1 ,2, "BPL");
        instructionSet[0x50] = new Instruction(AddressingMode.Relative, (byte) 0x50, 1 ,2, "BVC");
        instructionSet[0x70] = new Instruction(AddressingMode.Relative, (byte) 0x70, 1 ,2, "BVS");

        /* =
         * C
         === */

        /* =======================
         * CLEAR FLAG INSTRUCTIONS
         ========================= */

        // CLC
        instructionSet[0x18] = new Instruction(AddressingMode.Implied, (byte) 0x18, 1, 2, "CLC");

        // CLD
        instructionSet[0xD8] = new Instruction(AddressingMode.Implied, (byte) 0xD8, 1, 2, "CLD");

        // CLI
        instructionSet[0x58] = new Instruction(AddressingMode.Implied, (byte) 0x58, 1, 2, "CLI");

        // CLV
        instructionSet[0xB8] = new Instruction(AddressingMode.Implied, (byte) 0xB8, 1, 2, "CLV");

        /* =======================
         * COMPARISON INSTRUCTIONS
         ========================= */

        // CMP
        instructionSet[0xC9] = new Instruction(AddressingMode.Immediate, (byte) 0xc9, 2, 2, "CMP");
        instructionSet[0xC5] = new Instruction(AddressingMode.ZeroPage, (byte) 0xc5, 2, 3, "CMP");
        instructionSet[0xD5] = new Instruction(AddressingMode.ZeroPageX, (byte) 0xd5, 2, 4, "CMP");
        instructionSet[0xCD] = new Instruction(AddressingMode.Absolute, (byte) 0xcd, 3, 4, "CMP");
        instructionSet[0xDD] = new Instruction(AddressingMode.AbsoluteX, (byte) 0xdd, 3, 4, "CMP");
        instructionSet[0xD9] = new Instruction(AddressingMode.AbsoluteY, (byte) 0xd9, 3, 4, "CMP");
        instructionSet[0xC1] = new Instruction(AddressingMode.IndirectX, (byte) 0xc1, 2, 6, "CMP");
        instructionSet[0xD1] = new Instruction(AddressingMode.IndirectY, (byte) 0xd1, 2, 5, "CMP");

        // CPX
        instructionSet[0xE0] = new Instruction(AddressingMode.Immediate, (byte) 0xE0, 2, 2, "CPX");
        instructionSet[0xE4] = new Instruction(AddressingMode.ZeroPage, (byte) 0xE4, 2, 3, "CPX");
        instructionSet[0xEC] = new Instruction(AddressingMode.Absolute, (byte) 0xEC, 3, 4, "CPX");

        // CPY
        instructionSet[0xC0] = new Instruction(AddressingMode.Immediate, (byte) 0xC0, 2, 2, "CPY");
        instructionSet[0xC4] = new Instruction(AddressingMode.ZeroPage, (byte) 0xC4, 2, 3, "CPY");
        instructionSet[0xCC] = new Instruction(AddressingMode.Absolute, (byte) 0xCC, 3, 4, "CPY");

        /* =
         * D
         === */

        // DEC
        instructionSet[0xc6] = new Instruction(AddressingMode.ZeroPage, (byte) 0xc6, 2, 5, "DEC");
        instructionSet[0xd6] = new Instruction(AddressingMode.ZeroPageX, (byte) 0xd6, 2, 6, "DEC");
        instructionSet[0xce] = new Instruction(AddressingMode.Absolute, (byte) 0xce, 3, 6, "DEC");
        instructionSet[0xde] = new Instruction(AddressingMode.AbsoluteX, (byte) 0xde, 3, 7, "DEC");

        // DEX
        instructionSet[0xCA] = new Instruction(AddressingMode.Implied, (byte) 0xCA, 1, 2, "DEX");

        // DEY
        instructionSet[0x88] = new Instruction(AddressingMode.Implied, (byte) 0x88, 1, 2, "DEY");

        /* =
         * E
         === */

        // EOR
        instructionSet[0x49] = new Instruction(AddressingMode.Immediate, (byte) 0x49, 2, 2, "EOR");
        instructionSet[0x45] = new Instruction(AddressingMode.ZeroPage, (byte) 0x45, 2, 3, "EOR");
        instructionSet[0x55] = new Instruction(AddressingMode.ZeroPageX, (byte) 0x55, 2, 4, "EOR");
        instructionSet[0x4d] = new Instruction(AddressingMode.Absolute, (byte) 0x4d, 3, 4, "EOR");
        instructionSet[0x5d] = new Instruction(AddressingMode.AbsoluteX, (byte) 0x5d, 3, 4, "EOR");
        instructionSet[0x59] = new Instruction(AddressingMode.AbsoluteY, (byte) 0x59, 3, 4, "EOR");
        instructionSet[0x41] = new Instruction(AddressingMode.IndirectX, (byte) 0x41, 2, 6, "EOR");
        instructionSet[0x51] = new Instruction(AddressingMode.IndirectY, (byte) 0x51, 2, 5, "EOR");

        /* =
         * I
         === */

        /* ======================
         * INCREMENT INSTRUCTIONS
         ======================== */

        // INC
        instructionSet[0xe6] = new Instruction(AddressingMode.ZeroPage, (byte) 0xe6, 2, 5, "INC");
        instructionSet[0xf6] = new Instruction(AddressingMode.ZeroPageX, (byte) 0xf6, 2, 6, "INC");
        instructionSet[0xee] = new Instruction(AddressingMode.Absolute, (byte) 0xee, 3, 6, "INC");
        instructionSet[0xfe] = new Instruction(AddressingMode.AbsoluteX, (byte) 0xfe, 3, 7, "INC");

        // INX
        instructionSet[0xE8] = new Instruction(AddressingMode.Implied, (byte) 0xE8, 1, 2, "INX");

        // INY
        instructionSet[0xC8] = new Instruction(AddressingMode.Implied, (byte) 0xC8, 1, 2, "INY");

        /* =
         * J
         === */

        // JMP
        instructionSet[0x4C] = new Instruction(AddressingMode.Absolute, (byte) 0x4C, 3, 3, "JMP");
        instructionSet[0x6C] = new Instruction(AddressingMode.Indirect, (byte) 0x6C, 3, 5, "JMP");

        // JSR
        instructionSet[0x20] = new Instruction(AddressingMode.Absolute, (byte) 0x20, 3, 6, "JSR");

        /* =
         * L
         === */

        /* =================
         * LOAD INSTRUCTIONS
         =================== */

        // LDA
        instructionSet[0xA9] = new Instruction(AddressingMode.Immediate, (byte) 0xA9, 2, 2, "LDA");
        instructionSet[0xA5] = new Instruction(AddressingMode.ZeroPage, (byte) 0xA5, 2, 3, "LDA");
        instructionSet[0xB5] = new Instruction(AddressingMode.ZeroPageX, (byte) 0xB5, 2, 4, "LDA");
        instructionSet[0xAD] = new Instruction(AddressingMode.Absolute, (byte) 0xAD, 3, 4, "LDA");
        instructionSet[0xBD] = new Instruction(AddressingMode.AbsoluteX, (byte) 0xBD, 3, 4, "LDA");
        instructionSet[0xB9] = new Instruction(AddressingMode.AbsoluteY, (byte) 0xB9, 3, 4, "LDA");
        instructionSet[0xA1] = new Instruction(AddressingMode.IndirectX, (byte) 0xA1, 2, 6, "LDA");
        instructionSet[0xB1] = new Instruction(AddressingMode.IndirectY, (byte) 0xB1, 2, 5, "LDA");

        // LDX
        instructionSet[0xA2] = new Instruction(AddressingMode.Immediate, (byte) 0xA2, 2, 2, "LDX");
        instructionSet[0xA6] = new Instruction(AddressingMode.ZeroPage, (byte) 0xA6, 2, 3, "LDX");
        instructionSet[0xB6] = new Instruction(AddressingMode.ZeroPageY, (byte) 0xB6, 2, 4, "LDX");
        instructionSet[0xAE] = new Instruction(AddressingMode.Absolute, (byte) 0xAE, 3, 4, "LDX");
        instructionSet[0xBE] = new Instruction(AddressingMode.AbsoluteY, (byte) 0xBE, 3, 4, "LDX");

        // LDY
        instructionSet[0xA0] = new Instruction(AddressingMode.Immediate, (byte) 0xA0, 2, 2, "LDY");
        instructionSet[0xA4] = new Instruction(AddressingMode.ZeroPage, (byte) 0xA4, 2, 3, "LDY");
        instructionSet[0xB4] = new Instruction(AddressingMode.ZeroPageX, (byte) 0xB4, 2, 4, "LDY");
        instructionSet[0xAC] = new Instruction(AddressingMode.Absolute, (byte) 0xAC, 3, 4, "LDY");
        instructionSet[0xBC] = new Instruction(AddressingMode.AbsoluteX, (byte) 0xBC, 3, 4, "LDY");

        // LSR
        instructionSet[0x4a] = new Instruction(AddressingMode.Accumulator, (byte) 0x4a, 1, 2, "LSR");
        instructionSet[0x46] = new Instruction(AddressingMode.ZeroPage, (byte) 0x46, 2, 5, "LSR");
        instructionSet[0x56] = new Instruction(AddressingMode.ZeroPageX, (byte) 0x56, 2, 6, "LSR");
        instructionSet[0x4e] = new Instruction(AddressingMode.Absolute, (byte) 0x4E, 3, 6, "LSR");
        instructionSet[0x5e] = new Instruction(AddressingMode.AbsoluteX, (byte) 0x5e, 3, 7, "LSR");

        /* =
         * N
         === */

        // NOP
        instructionSet[0xEA] = new Instruction(AddressingMode.Implied, (byte) 0xEA, 1, 2, "NOP");

        /* =
         * O
         === */

        // ORA
        instructionSet[0x09] = new Instruction(AddressingMode.Immediate, (byte) 0x09, 2, 2, "ORA");
        instructionSet[0x05] = new Instruction(AddressingMode.ZeroPage, (byte) 0x05, 2, 3, "ORA");
        instructionSet[0x15] = new Instruction(AddressingMode.ZeroPageX, (byte) 0x15, 2, 4, "ORA");
        instructionSet[0x0d] = new Instruction(AddressingMode.Absolute, (byte) 0x0d, 3, 4, "ORA");
        instructionSet[0x1d] = new Instruction(AddressingMode.AbsoluteX, (byte) 0x1d, 3, 4, "ORA");
        instructionSet[0x19] = new Instruction(AddressingMode.AbsoluteY, (byte) 0x19, 3, 4, "ORA");
        instructionSet[0x01] = new Instruction(AddressingMode.IndirectX, (byte) 0x01, 2, 6, "ORA");
        instructionSet[0x11] = new Instruction(AddressingMode.IndirectY, (byte) 0x11, 2, 5, "ORA");

        /* =
         * P
         === */

        /* =================
         * PUSH INSTRUCTIONS
         =================== */

        // PHA
        instructionSet[0x48] = new Instruction(AddressingMode.Implied, (byte) 0x48, 1, 3, "PHA");

        // PHP
        instructionSet[0x08] = new Instruction(AddressingMode.Implied, (byte) 0x08, 1, 3, "PHP");

        /* =================
         * PULL INSTRUCTIONS
         =================== */

        // PLA
        instructionSet[0x68] = new Instruction(AddressingMode.Implied, (byte) 0x68, 1, 4, "PLA");

        // PLP
        instructionSet[0x28] = new Instruction(AddressingMode.Implied, (byte) 0x28, 1, 4, "PLP");

        /* =
         * R
         === */

        /* =====================
         * ROTATION INSTRUCTIONS
         ======================= */

        // ROL
        instructionSet[0x2A] = new Instruction(AddressingMode.Accumulator, (byte) 0x2A, 1, 2, "ROL");
        instructionSet[0x26] = new Instruction(AddressingMode.ZeroPage, (byte) 0x26, 2, 5, "ROL");
        instructionSet[0x36] = new Instruction(AddressingMode.ZeroPageX, (byte) 0x36, 2, 6, "ROL");
        instructionSet[0x2E] = new Instruction(AddressingMode.Absolute, (byte) 0x2E, 3, 6, "ROL");
        instructionSet[0x3E] = new Instruction(AddressingMode.AbsoluteX, (byte) 0x3E, 3, 7, "ROL");

        // ROR
        instructionSet[0x6a] = new Instruction(AddressingMode.Accumulator, (byte) 0x6a, 1, 2, "ROR");
        instructionSet[0x66] = new Instruction(AddressingMode.ZeroPage, (byte) 0x66, 2, 5, "ROR");
        instructionSet[0x76] = new Instruction(AddressingMode.ZeroPageX, (byte) 0x76, 2, 6, "ROR");
        instructionSet[0x6e] = new Instruction(AddressingMode.Absolute, (byte) 0x6e, 3, 6, "ROR");
        instructionSet[0x7e] = new Instruction(AddressingMode.AbsoluteX, (byte) 0x7e, 3, 7, "ROR");

        /* ===================
         * RETURN INSTRUCTIONS
         ===================== */

        // RTI
        instructionSet[0x40] = new Instruction(AddressingMode.Implied, (byte) 0x40, 1, 6, "RTI");

        // RTS
        instructionSet[0x60] = new Instruction(AddressingMode.Implied, (byte) 0x60, 1, 6, "RTS");

        /* =
         * S
         === */

        // SBC
        instructionSet[0xe9] = new Instruction(AddressingMode.Immediate, (byte) 0xe9, 2, 2, "SBC");
        instructionSet[0xe5] = new Instruction(AddressingMode.ZeroPage, (byte) 0xe5, 2, 3, "SBC");
        instructionSet[0xf5] = new Instruction(AddressingMode.ZeroPageX, (byte) 0xf5, 2, 4, "SBC");
        instructionSet[0xed] = new Instruction(AddressingMode.Absolute, (byte) 0xed, 3, 4, "SBC");
        instructionSet[0xfd] = new Instruction(AddressingMode.AbsoluteX, (byte) 0xfd, 3, 4, "SBC");
        instructionSet[0xf9] = new Instruction(AddressingMode.AbsoluteY, (byte) 0xf9, 3, 4, "SBC");
        instructionSet[0xe1] = new Instruction(AddressingMode.IndirectX, (byte) 0xe1, 2, 6, "SBC");
        instructionSet[0xf1] = new Instruction(AddressingMode.IndirectY, (byte) 0xf1, 2, 5, "SBC");

        /* =====================
         * SET FLAG INSTRUCTIONS
         ======================= */

        // SEC
        instructionSet[0x38] = new Instruction(AddressingMode.Implied, (byte) 0x38, 1 , 2, "SEC");

        // SED
        instructionSet[0xF8] = new Instruction(AddressingMode.Implied, (byte) 0xF8, 1 , 2, "SED");

        // SEI
        instructionSet[0x78] = new Instruction(AddressingMode.Implied, (byte) 0x78, 1 , 2, "SEI");

        /* ==================
         * STORE INSTRUCTIONS
         ==================== */

        // STA
        instructionSet[0x85] = new Instruction(AddressingMode.ZeroPage, (byte) 0x85, 2, 3, "STA");
        instructionSet[0x95] = new Instruction(AddressingMode.ZeroPageX, (byte) 0x95, 2, 4, "STA");
        instructionSet[0x8d] = new Instruction(AddressingMode.Absolute, (byte) 0x8d, 3, 4, "STA");
        instructionSet[0x9d] = new Instruction(AddressingMode.AbsoluteX, (byte) 0x9d, 3, 5, "STA");
        instructionSet[0x99] = new Instruction(AddressingMode.AbsoluteY, (byte) 0x99, 3, 5, "STA");
        instructionSet[0x81] = new Instruction(AddressingMode.IndirectX, (byte) 0x81, 2, 6, "STA");
        instructionSet[0x91] = new Instruction(AddressingMode.IndirectY, (byte) 0x91, 2, 6, "STA");

        // STX
        instructionSet[0x86] = new Instruction(AddressingMode.ZeroPage, (byte) 0x86, 2, 3, "STX");
        instructionSet[0x96] = new Instruction(AddressingMode.ZeroPageY, (byte) 0x96, 2, 4, "STX");
        instructionSet[0x8E] = new Instruction(AddressingMode.Absolute, (byte) 0x8E, 3, 4, "STX");

        // STY
        instructionSet[0x84] = new Instruction(AddressingMode.ZeroPage, (byte) 0x84, 2, 3, "STY");
        instructionSet[0x94] = new Instruction(AddressingMode.ZeroPageX, (byte) 0x94, 2, 4, "STY");
        instructionSet[0x8C] = new Instruction(AddressingMode.Absolute, (byte) 0x8C, 3, 4, "STY");

        /* =
         * T
         === */

        /* =====================
         * TRANSFER INSTRUCTIONS
         ======================= */

        // TAX
        instructionSet[0xAA] = new Instruction(AddressingMode.Implied, (byte) 0xAA, 1, 2, "TAX");

        // TAY
        instructionSet[0xA8] = new Instruction(AddressingMode.Implied, (byte) 0xA8, 1, 2, "TAY");

        // TSX
        instructionSet[0xBA] = new Instruction(AddressingMode.Implied, (byte) 0xBA, 1, 2, "TSX");

        // TXA
        instructionSet[0x8A] = new Instruction(AddressingMode.Implied, (byte) 0x8A, 1, 2, "TXA");

        // TXS
        instructionSet[0x9A] = new Instruction(AddressingMode.Implied, (byte) 0x9A, 1, 2, "TXS");

        // TYA
        instructionSet[0x98] = new Instruction(AddressingMode.Implied, (byte) 0x98, 1, 2, "TYA");

        /* ===============
         * ILLEGAL OPCODES
         ================= */

        /*
         * Source for unofficial opcodes: https://www.masswerk.at/nowgobang/2021/6502-illegal-opcodes
         */

        // ALR/ASR
        instructionSet[0x4B] = new Instruction(AddressingMode.Immediate, (byte) 0x4B, 2, 2, "ALR*");

        // ANC
        instructionSet[0x0B] = new Instruction(AddressingMode.Immediate, (byte) 0x0B, 2, 2, "ANC*");

        // ANC/ANC2
        instructionSet[0x2B] = new Instruction(AddressingMode.Immediate, (byte) 0x2B, 2, 2, "ACN*");

        // ANE/XAA
        instructionSet[0x8B] = new Instruction(AddressingMode.Immediate, (byte) 0x8B, 2, 2, "ANE*");

        // ARR
        instructionSet[0x6B] = new Instruction(AddressingMode.Immediate, (byte) 0x6B, 2, 2, "ARR*");

        // DCP/DCM
        instructionSet[0xC7] = new Instruction(AddressingMode.ZeroPage, (byte) 0xC7, 2 ,5, "DCP*");
        instructionSet[0xD7] = new Instruction(AddressingMode.ZeroPageX, (byte) 0xD7, 2 ,6, "DCP*");
        instructionSet[0xCF] = new Instruction(AddressingMode.Absolute, (byte) 0xCF, 3 ,6, "DCP*");
        instructionSet[0xDF] = new Instruction(AddressingMode.AbsoluteX, (byte) 0xDF, 3 ,7, "DCP*");
        instructionSet[0xDB] = new Instruction(AddressingMode.AbsoluteY, (byte) 0xDB, 3 ,7, "DCP*");
        instructionSet[0xC3] = new Instruction(AddressingMode.IndirectX, (byte) 0xC3, 2 ,8, "DCP*");
        instructionSet[0xD3] = new Instruction(AddressingMode.IndirectY, (byte) 0xD3, 2 ,8, "DCP*");

        // ISC/ISB/INS
        instructionSet[0xE7] = new Instruction(AddressingMode.ZeroPage, (byte) 0xE7, 2, 5, "ISC*");
        instructionSet[0xF7] = new Instruction(AddressingMode.ZeroPageX, (byte) 0xF7, 2, 6, "ISC*");
        instructionSet[0xEF] = new Instruction(AddressingMode.Absolute, (byte) 0xEF, 3, 6, "ISC*");
        instructionSet[0xFF] = new Instruction(AddressingMode.AbsoluteX, (byte) 0xFF, 3, 7, "ISC*");
        instructionSet[0xFB] = new Instruction(AddressingMode.AbsoluteY, (byte) 0xFB, 3, 7, "ISC*");
        instructionSet[0xE3] = new Instruction(AddressingMode.IndirectX, (byte) 0xE3, 2, 8, "ISC*");
        instructionSet[0xF3] = new Instruction(AddressingMode.IndirectY, (byte) 0xF3, 2, 8, "ISC*");

        // LAS/LAR
        instructionSet[0xBB] = new Instruction(AddressingMode.AbsoluteY, (byte) 0xBB, 3, 4, "LAS*");

        // LAX
        instructionSet[0xA7] = new Instruction(AddressingMode.ZeroPage, (byte) 0xA7, 2, 3, "LAX*");
        instructionSet[0xB7] = new Instruction(AddressingMode.ZeroPageY, (byte) 0xB7, 2, 4, "LAX*");
        instructionSet[0xAF] = new Instruction(AddressingMode.Absolute, (byte) 0xAF, 3, 4, "LAX*");
        instructionSet[0xBF] = new Instruction(AddressingMode.AbsoluteY, (byte) 0xBF, 3, 4, "LAX*");
        instructionSet[0xA3] = new Instruction(AddressingMode.IndirectX, (byte) 0xA3, 2, 6, "LAX*");
        instructionSet[0xB3] = new Instruction(AddressingMode.IndirectY, (byte) 0xB3, 2, 5, "LAX*");

        // LXA
        instructionSet[0xAB] = new Instruction(AddressingMode.Immediate, (byte) 0xAB, 2, 2, "LXA*");

        // RLA
        instructionSet[0x27] = new Instruction(AddressingMode.ZeroPage, (byte) 0x27, 2, 5, "RLA*");
        instructionSet[0x37] = new Instruction(AddressingMode.ZeroPageX, (byte) 0x37, 2, 6, "RLA*");
        instructionSet[0x2F] = new Instruction(AddressingMode.Absolute, (byte) 0x2F, 3, 6, "RLA*");
        instructionSet[0x3F] = new Instruction(AddressingMode.AbsoluteX, (byte) 0x3F, 3, 7, "RLA*");
        instructionSet[0x3B] = new Instruction(AddressingMode.AbsoluteY, (byte) 0x3B, 3, 7, "RLA*");
        instructionSet[0x23] = new Instruction(AddressingMode.IndirectX, (byte) 0x23, 2, 8, "RLA*");
        instructionSet[0x33] = new Instruction(AddressingMode.IndirectY, (byte) 0x33, 2, 8, "RLA*");

        // RRA
        instructionSet[0x67] = new Instruction(AddressingMode.ZeroPage, (byte) 0x67, 2, 5, "RRA*");
        instructionSet[0x77] = new Instruction(AddressingMode.ZeroPageX, (byte) 0x77, 2, 6, "RRA*");
        instructionSet[0x6F] = new Instruction(AddressingMode.Absolute, (byte) 0x6F, 3, 6, "RRA*");
        instructionSet[0x7F] = new Instruction(AddressingMode.AbsoluteX, (byte) 0x7F, 3, 7, "RRA*");
        instructionSet[0x7B] = new Instruction(AddressingMode.AbsoluteY, (byte) 0x7B, 3, 7, "RRA*");
        instructionSet[0x63] = new Instruction(AddressingMode.IndirectX, (byte) 0x63, 2, 8, "RRA*");
        instructionSet[0x73] = new Instruction(AddressingMode.IndirectY, (byte) 0x73, 2, 8, "RRA*");

        // SAX/AXS/AAX
        instructionSet[0x87] = new Instruction(AddressingMode.ZeroPage, (byte) 0x87, 2, 3, "SAX*");
        instructionSet[0x97] = new Instruction(AddressingMode.ZeroPageY, (byte) 0x97, 2, 4, "SAX*");
        instructionSet[0x8F] = new Instruction(AddressingMode.Absolute, (byte) 0x8F, 3, 4, "SAX*");
        instructionSet[0x83] = new Instruction(AddressingMode.IndirectX, (byte) 0x83, 2, 6, "SAX*");

        // SBX/AXS/SAX
        instructionSet[0xCB] = new Instruction(AddressingMode.Immediate, (byte) 0xCB, 2, 2, "SBX*");

        // SHA/AHX/AXA
        instructionSet[0x9F] = new Instruction(AddressingMode.AbsoluteY, (byte) 0x9F, 3, 5, "SHA*");
        instructionSet[0x93] = new Instruction(AddressingMode.IndirectY, (byte) 0x93, 2, 6, "SHA*");

        // SHX/A11/SXA/XAS
        instructionSet[0x9E] = new Instruction(AddressingMode.AbsoluteY, (byte) 0x9E, 3, 5, "SHX*");

        // SHY/X11/SYA/SAY
        instructionSet[0x9C] = new Instruction(AddressingMode.AbsoluteX, (byte) 0x9C, 3, 5, "SHY*");

        // SLO/ASO
        instructionSet[0x07] = new Instruction(AddressingMode.ZeroPage, (byte) 0x07, 2, 5, "SLO*");
        instructionSet[0x17] = new Instruction(AddressingMode.ZeroPageX, (byte) 0x17, 2, 6, "SLO*");
        instructionSet[0x0F] = new Instruction(AddressingMode.Absolute, (byte) 0x0F, 3, 6, "SLO*");
        instructionSet[0x1F] = new Instruction(AddressingMode.AbsoluteX, (byte) 0x1F, 3, 7, "SLO*");
        instructionSet[0x1B] = new Instruction(AddressingMode.AbsoluteY, (byte) 0x1B, 3, 7, "SLO*");
        instructionSet[0x03] = new Instruction(AddressingMode.IndirectX, (byte) 0x03, 2, 8, "SLO*");
        instructionSet[0x13] = new Instruction(AddressingMode.IndirectY, (byte) 0x13, 2, 8, "SLO*");

        // SRE/LSE
        instructionSet[0x47] = new Instruction(AddressingMode.ZeroPage, (byte) 0x47, 2, 5, "SRE*");
        instructionSet[0x57] = new Instruction(AddressingMode.ZeroPageX, (byte) 0x57, 2, 6, "SRE*");
        instructionSet[0x4F] = new Instruction(AddressingMode.Absolute, (byte) 0x4F, 3, 6, "SRE*");
        instructionSet[0x5F] = new Instruction(AddressingMode.AbsoluteX, (byte) 0x5F, 3, 7, "SRE*");
        instructionSet[0x5B] = new Instruction(AddressingMode.AbsoluteY, (byte) 0x5B, 3, 7, "SRE*");
        instructionSet[0x43] = new Instruction(AddressingMode.IndirectX, (byte) 0x43, 2, 8, "SRE*");
        instructionSet[0x53] = new Instruction(AddressingMode.IndirectY, (byte) 0x53, 2, 8, "SRE*");

        // TAS/XAS/SHS
        instructionSet[0x9B] = new Instruction(AddressingMode.AbsoluteY, (byte) 0x9B, 3, 5, "TAS*");

        // USBC/SBC
        instructionSet[0xEB] = new Instruction(AddressingMode.Immediate, (byte) 0xEB, 2, 2, "USB*");

        // NOP
        instructionSet[0x1A] = new Instruction(AddressingMode.Implied, (byte) 0x1A, 1, 2, "NOP*");
        instructionSet[0x3A] = new Instruction(AddressingMode.Implied, (byte) 0x3A, 1, 2, "NOP*");
        instructionSet[0x5A] = new Instruction(AddressingMode.Implied, (byte) 0x5A, 1, 2, "NOP*");
        instructionSet[0x7A] = new Instruction(AddressingMode.Implied, (byte) 0x7A, 1, 2, "NOP*");
        instructionSet[0xDA] = new Instruction(AddressingMode.Implied, (byte) 0xDA, 1, 2, "NOP*");
        instructionSet[0xFA] = new Instruction(AddressingMode.Implied, (byte) 0xFA, 1, 2, "NOP*");
        instructionSet[0x80] = new Instruction(AddressingMode.Immediate, (byte) 0x80, 2, 2, "NOP*");
        instructionSet[0x82] = new Instruction(AddressingMode.Immediate, (byte) 0x82, 2, 2, "NOP*");
        instructionSet[0x89] = new Instruction(AddressingMode.Immediate, (byte) 0x89, 2, 2, "NOP*");
        instructionSet[0xC2] = new Instruction(AddressingMode.Immediate, (byte) 0xC2, 2, 2, "NOP*");
        instructionSet[0xE2] = new Instruction(AddressingMode.Immediate, (byte) 0xE2, 2, 2, "NOP*");

        // DOP
        instructionSet[0x04] = new Instruction(AddressingMode.ZeroPage, (byte) 0x04, 2, 3, "DOP*");
        instructionSet[0x44] = new Instruction(AddressingMode.ZeroPage, (byte) 0x44, 2, 3, "DOP*");
        instructionSet[0x64] = new Instruction(AddressingMode.ZeroPage, (byte) 0x64, 2, 3, "DOP*");

        // TOP
        instructionSet[0x14] = new Instruction(AddressingMode.ZeroPageX, (byte) 0x14, 2, 4, "TOP*");
        instructionSet[0x34] = new Instruction(AddressingMode.ZeroPageX, (byte) 0x34, 2, 4, "TOP*");
        instructionSet[0x54] = new Instruction(AddressingMode.ZeroPageX, (byte) 0x54, 2, 4, "TOP*");
        instructionSet[0x74] = new Instruction(AddressingMode.ZeroPageX, (byte) 0x74, 2, 4, "TOP*");
        instructionSet[0xD4] = new Instruction(AddressingMode.ZeroPageX, (byte) 0xD4, 2, 4, "TOP*");
        instructionSet[0xF4] = new Instruction(AddressingMode.ZeroPageX, (byte) 0xF4, 2, 4, "TOP*");
        instructionSet[0x0C] = new Instruction(AddressingMode.Absolute, (byte) 0x0C, 3, 4, "TOP*");
        instructionSet[0x1C] = new Instruction(AddressingMode.AbsoluteX, (byte) 0x1C, 3, 4, "TOP*");
        instructionSet[0x3C] = new Instruction(AddressingMode.AbsoluteX, (byte) 0x3C, 3, 4, "TOP*");
        instructionSet[0x5C] = new Instruction(AddressingMode.AbsoluteX, (byte) 0x5C, 3, 4, "TOP*");
        instructionSet[0x7C] = new Instruction(AddressingMode.AbsoluteX, (byte) 0x7C, 3, 4, "TOP*");
        instructionSet[0xDC] = new Instruction(AddressingMode.AbsoluteX, (byte) 0xDC, 3, 4, "TOP*");
        instructionSet[0xFC] = new Instruction(AddressingMode.AbsoluteX, (byte) 0xFC, 3, 4, "TOP*");

        // JAM/KIL/HLT
        instructionSet[0x02] = new Instruction(AddressingMode.Implied, (byte) 0x02, 2, 2, "JAM*");
        instructionSet[0x12] = new Instruction(AddressingMode.Implied, (byte) 0x12, 2, 2, "JAM*");
        instructionSet[0x22] = new Instruction(AddressingMode.Implied, (byte) 0x22, 2, 2, "JAM*");
        instructionSet[0x32] = new Instruction(AddressingMode.Implied, (byte) 0x32, 2, 2, "JAM*");
        instructionSet[0x42] = new Instruction(AddressingMode.Implied, (byte) 0x42, 2, 2, "JAM*");
        instructionSet[0x52] = new Instruction(AddressingMode.Implied, (byte) 0x52, 2, 2, "JAM*");
        instructionSet[0x62] = new Instruction(AddressingMode.Implied, (byte) 0x62, 2, 2, "JAM*");
        instructionSet[0x72] = new Instruction(AddressingMode.Implied, (byte) 0x72, 2, 2, "JAM*");
        instructionSet[0x92] = new Instruction(AddressingMode.Implied, (byte) 0x92, 2, 2, "JAM*");
        instructionSet[0xB2] = new Instruction(AddressingMode.Implied, (byte) 0xB2, 2, 2, "JAM*");
        instructionSet[0xD2] = new Instruction(AddressingMode.Implied, (byte) 0xD2, 2, 2, "JAM*");
        instructionSet[0xF2] = new Instruction(AddressingMode.Implied, (byte) 0xF2, 2, 2, "JAM*");


        return instructionSet;
    }
}
