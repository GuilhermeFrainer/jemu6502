package org.guisado;

/**
 * Represents different memory addressing modes for instructions.
 */
public enum AddressingMode {
    Implied,
    Immediate,
    ZeroPage,
    ZeroPageX,
    Absolute,
    AbsoluteX,
    AbsoluteY,
    Indirect,
    IndirectX,
    IndirectY;
}
