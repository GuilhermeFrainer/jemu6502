package org.guisado;

/**
 * Represents an instruction.
 */
public class Instruction {
    private final AddressingMode addressingMode;
    private final byte opcode;
    private final int bytes;
    private final int cycles;
    private final String mnemonic;

    /**
     * Represents different memory addressing modes for instructions.
     */
    public enum AddressingMode {
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
        IndirectY;
    }

    /**
     *
     * @param addressingMode
     * @param opcode
     * @param bytes
     * @param cycles
     * @param mnemonic the instruction's mnemonic. Must be string with three letters plus an optional '*' if
     *                 it's an "unofficial" instruction.
     * @throws IllegalArgumentException in case invalid mnemonics (i.e. length > 4), 'bytes' (> 3) or 'cycles' (>7)
     * are passed.
     */
    public Instruction(AddressingMode addressingMode, byte opcode, int bytes, int cycles, String mnemonic)
        throws IllegalArgumentException {

        if (mnemonic.length() > 4) {
            throw new IllegalArgumentException("String length must be at most 4. Received string of length " + mnemonic.length());
        }
        else if (bytes > 3 || bytes < 1) {
            throw new IllegalArgumentException("'bytes' must be in the range [1, 3]. Got " + bytes);
        }
        else if (cycles > 7 || cycles < 1) {
            throw new IllegalArgumentException("´cycles' must be in the range [1, 7]. Got " + cycles);

        }
        this.addressingMode = addressingMode;
        this.opcode = opcode;
        this.bytes = bytes;
        this.cycles = cycles;
        this.mnemonic = mnemonic;
    }

    /**
     *
     * @return this instruction's addressing mode.
     */
    public AddressingMode getAddressingMode() {
        return this.addressingMode;
    }

    /**
     *
     * @return this instruction's opcode.
     */
    public byte getOpcode() {
        return this.opcode;
    }

    /**
     *
     * @return this instruction's number of bytes.
     * This means the instruction byte + the operands.
     */
    public int getBytes() {
        return this.bytes;
    }

    /**
     *
     * @return this instruction's cycle count ignoring page crossing.
     */
    public int getCycles() {
        return this.cycles;
    }

    /**
     *
     * @return this instructions mnemonic.
     */
    public String getMnemonic() {
        return this.mnemonic;
    }

    /**
     * Initializes array with the entire 6502 instruction set.
     * @return
     */
    public static Instruction[] initializeInstructionSet() {
        Instruction[] instructionSet = new Instruction[256];

        instructionSet[0x00] = new Instruction(AddressingMode.Implied, (byte) 0x00, 1, 7, "BRK");

        /* ======================
         * INCREMENT INSTRUCTIONS
         ======================== */

        // INX
        instructionSet[0xE8] = new Instruction(AddressingMode.Implied, (byte) 0xE8, 1, 2, "INX");

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
        instructionSet[0xBE] = new Instruction(AddressingMode.AbsoluteY, (byte) 0xBE, 3 ,4, "LDX");

        // LDY
        instructionSet[0xA0] = new Instruction(AddressingMode.Immediate, (byte) 0xA0, 2, 2, "LDY");
        instructionSet[0xA4] = new Instruction(AddressingMode.ZeroPage, (byte) 0xA4, 2, 3, "LDY");
        instructionSet[0xB4] = new Instruction(AddressingMode.ZeroPageX, (byte) 0xB4, 2, 4, "LDY");
        instructionSet[0xAC] = new Instruction(AddressingMode.Absolute, (byte) 0xAC, 3, 4, "LDY");
        instructionSet[0xBC] = new Instruction(AddressingMode.AbsoluteX, (byte) 0xBC, 3, 4, "LDY");

        /* =====================
         * TRANSFER INSTRUCTIONS
         ======================= */

        // TAX
        instructionSet[0xAA] = new Instruction(AddressingMode.Implied, (byte) 0xAA, 1, 2, "TAX");

        return instructionSet;
    }
}
