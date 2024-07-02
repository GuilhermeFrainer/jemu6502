package org.guisado;

/**
 * Interface used to represent the CPU's memory.
 * Needed cause the tests I'm going to run assume all 64kBs exist,
 * while the NES doesn't actually have that much memory.
 */
public interface Memory {
    /**
     *
     * @param address Address to read from.
     *             Will be interpreted as an unsigned 16-bit int.
     * @return Number read at address. Should be interpreted as an unsigned byte.
     */
    public byte read(short address);

    /**
     *
     * @param value Value to write at address.
     * @param address Address to be written at.
     *                Will be interpreted as an unsigned 16-bit int.
     */
    public void write(byte value, short address);
}
