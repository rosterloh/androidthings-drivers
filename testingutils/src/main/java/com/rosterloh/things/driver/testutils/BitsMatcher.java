package com.rosterloh.things.driver.testutils;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

import java.util.Locale;

/**
 * Matcher that checks if necessary bits are set in a byte argument.
 */
public final class BitsMatcher extends TypeSafeMatcher<Byte> {

    private final byte mBits;
    private final boolean mNotSet;

    private BitsMatcher(byte bits, boolean notSet) {
        mBits = bits;
        mNotSet = notSet;
    }

    /**
     * Create a BitsMatcher that checks arguments contain matching 1 bits when compared to the
     * provided mask, e.g.
     * <pre>
     *     BitsMatcher m = BitsMatcher.hasBitsSet((byte) 0b11111110);
     *     m.matches((byte) 0b11111111); // true
     *     m.matches((byte) 0b11111110); // true
     *     m.matches(OTHER_BYTE_VALUES); // false
     * </pre>
     *
     * @param mask mask containing 1s at each bit position that must be set. If the mask contains
     * all zeroes, only zero will match.
     */
    public static BitsMatcher hasBitsSet(byte mask) {
        return new BitsMatcher(mask, false);
    }

    /**
     * Create a BitsMatcher that checks arguments contain matching 0 bits when compared to the
     * provided mask, e.g.
     * <pre>
     *     BitsMatcher m = BitsMatcher.hasBitsNotSet((byte) 0b00000001);
     *     m.matches((byte) 0b00000000); // true
     *     m.matches((byte) 0b00000001); // true
     *     m.matches(OTHER_BYTE_VALUES); // false
     * </pre>
     *
     * @param mask mask containing 0s at each bit position that must be unset. If the mask contains
     * all ones, only -1 will match.
     */
    public static BitsMatcher hasBitsNotSet(byte mask) {
        return new BitsMatcher(mask, true);
    }

    @Override
    public void describeTo(Description description) {
        int bits = mNotSet ? 0 : 1;
        description.appendText(String.format(Locale.ENGLISH, "Value must be a byte with matching %d bits: %s",
                bits, Integer.toBinaryString(mBits & 0xFF))); // suppress sign extension
    }

    @Override
    protected void describeMismatchSafely(Byte item, Description mismatchDescription) {
        int bits = mNotSet ? 0 : 1;
        mismatchDescription.appendText(String.format(Locale.ENGLISH, "Value does not have matching %d bits: %s",
                bits, Integer.toBinaryString(mBits & 0xFF))); // suppress sign extension
    }

    @Override
    protected boolean matchesSafely(Byte item) {
        if (mNotSet) {
            return mBits == -1 ? item == -1 : (item | mBits) == mBits;
        }
        return mBits == 0 ? item == 0 : (item & mBits) == mBits;
    }
}
