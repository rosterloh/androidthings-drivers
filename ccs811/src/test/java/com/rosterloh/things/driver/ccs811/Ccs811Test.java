package com.rosterloh.things.driver.ccs811;

import com.google.android.things.pio.I2cDevice;
import com.rosterloh.things.driver.ccs811.Ccs811;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.IOException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.times;

public class Ccs811Test {

    @Mock
    private I2cDevice mI2c;

    @Rule
    public MockitoRule mMokitoRule = MockitoJUnit.rule();

    @Test
    public void close() throws IOException {
        Ccs811 ccs811 = new Ccs811(mI2c);
        ccs811.close();
        Mockito.verify(mI2c).close();
    }

    @Test
    public void close_safeToCallTwice() throws IOException {
        Ccs811 ccs811 = new Ccs811(mI2c);
        ccs811.close();
        ccs811.close(); // should not throw
        Mockito.verify(mI2c, times(1)).close();
    }
}