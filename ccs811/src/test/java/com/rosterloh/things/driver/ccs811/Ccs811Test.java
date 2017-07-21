package com.rosterloh.things.driver.ccs811;

import com.google.android.things.pio.I2cDevice;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.IOException;

import static com.rosterloh.things.driver.testutils.BitsMatcher.hasBitsSet;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.hamcrest.MockitoHamcrest.byteThat;

public class Ccs811Test {

    @Mock
    private I2cDevice mI2c;

    @Rule
    public MockitoRule mMokitoRule = MockitoJUnit.rule();

    @Rule
    public ExpectedException mExpectedException = ExpectedException.none();

    private Ccs811 getInstance() throws IOException {
        mExpectedException.expect(IOException.class);
        mExpectedException.expectMessage("not valid");
        return new Ccs811(mI2c);
    }

    @Test
    public void close() throws IOException {
        Ccs811 ccs811 = getInstance();
        ccs811.close();
        Mockito.verify(mI2c).close();
    }

    @Test
    public void close_safeToCallTwice() throws IOException {
        Ccs811 ccs811 = getInstance();
        ccs811.close();
        ccs811.close(); // should not throw
        Mockito.verify(mI2c, times(1)).close();
    }

    @Test
    public void setMode() throws IOException {
        Ccs811 ccs811 = getInstance();
        ccs811.setMode(Ccs811.MODE_250MS);
        Mockito.verify(mI2c).writeRegByte(eq(0x01),
                byteThat(hasBitsSet((byte) (Ccs811.MODE_250MS << 4))));

        Mockito.reset(mI2c);

        ccs811.setMode(Ccs811.MODE_IDLE);
        Mockito.verify(mI2c).writeRegByte(eq(0x01),
                byteThat(hasBitsSet((byte) (Ccs811.MODE_IDLE << 4))));
    }

    @Test
    public void setMode_throwsIfClosed() throws IOException {
        Ccs811 ccs811 = getInstance();
        ccs811.close();
        mExpectedException.expect(IllegalStateException.class);
        mExpectedException.expectMessage("not open");
        ccs811.setMode(Ccs811.MODE_1S);
    }

    @Test
    public void readBootVersion() throws IOException {
        Ccs811 ccs811 = getInstance();
        ccs811.readBootVersion();
        Mockito.verify(mI2c).readRegBuffer(eq(0x23), any(byte[].class), eq(2));
    }

    @Test
    public void readAppVersion() throws IOException {
        Ccs811 ccs811 = getInstance();
        ccs811.readAppVersion();
        Mockito.verify(mI2c).readRegBuffer(eq(0x24), any(byte[].class), eq(2));
    }

    @Test
    public void readAlgorithmResults() throws IOException {
        Ccs811 ccs811 = getInstance();
        ccs811.readAlgorithmResults();
        Mockito.verify(mI2c).readRegBuffer(eq(0x02), any(byte[].class), eq(4));
    }

    @Test
    public void readAlgorithmResults_throwsIfClosed() throws IOException {
        Ccs811 ccs811 = getInstance();
        ccs811.close();
        mExpectedException.expect(IllegalStateException.class);
        mExpectedException.expectMessage("not open");
        ccs811.readAlgorithmResults();
    }
}