package com.rosterloh.things.driver.testutils;

import org.junit.Test;

import static org.junit.Assert.*;

public class BitsMatcherTest {

    @Test
    public void bitsSet() {
        BitsMatcher matcher = BitsMatcher.hasBitsSet((byte) 0b00001010); // 10

        // bits set
        assertTrue(matcher.matches((byte) 10));
        assertTrue(matcher.matches((byte) 0b00001011));
        assertTrue(matcher.matches((byte) 0b00011010));

        // bits not set
        assertFalse(matcher.matches((byte) 0));
        assertFalse(matcher.matches((byte) 0b00011001));
        assertFalse(matcher.matches((byte) 0b01000000));
    }

    @Test
    public void bitsSet_allUnset() {
        BitsMatcher matcher = BitsMatcher.hasBitsSet((byte) 0); // all 0s
        assertTrue(matcher.matches((byte) 0));
        assertFalse(matcher.matches((byte) 1));
    }

    @Test
    public void bitsNotSet() {
        BitsMatcher matcher = BitsMatcher.hasBitsNotSet((byte) 0b11111100); // -4

        // bits not set
        assertTrue(matcher.matches((byte) -4));
        assertTrue(matcher.matches((byte) 0b00001000));
        assertTrue(matcher.matches((byte) 0b11011000));
        assertTrue(matcher.matches((byte) 0));

        // bits set
        assertFalse(matcher.matches((byte) 1));
        assertFalse(matcher.matches((byte) 0b01000010));
        assertFalse(matcher.matches((byte) 0b11000011));
    }

    @Test
    public void bitsNotSet_allSet() {
        BitsMatcher matcher = BitsMatcher.hasBitsNotSet((byte) -1); // all 1s
        assertTrue(matcher.matches((byte) -1));
        assertFalse(matcher.matches(Byte.MAX_VALUE)); // all 1s except MSB
    }
}