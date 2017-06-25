package com.rosterloh.things.driver.htu21d;

import com.google.android.things.pio.I2cDevice;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;

public class Htu21dTest {

    // Sensor readings and expected results for testing compensation functions
    private static final int RAW_TEMPERATURE = 28671;
    private static final int RAW_HUMIDITY = 26662;

    private static final float EXPECTED_TEMPERATURE = 30.0248f;
    private static final float EXPECTED_HUMIDITY = 44.8537f;
    private static final float TOLERANCE = .001f;

    @Mock
    private I2cDevice mI2c;

    @Rule
    public MockitoRule mMokitoRule = MockitoJUnit.rule();

    @Rule
    public ExpectedException mExpectedException = ExpectedException.none();

    @Test
    public void testCompensateTemperature() {
        final float tempResult = Htu21d.compensateTemperature(RAW_TEMPERATURE);
        Assert.assertEquals(tempResult, EXPECTED_TEMPERATURE, EXPECTED_TEMPERATURE * TOLERANCE);
    }

    @Test
    public void testCompensateHumidity() {
        final float humResult = Htu21d.compensateHumidity(RAW_HUMIDITY);
        Assert.assertEquals(humResult, EXPECTED_HUMIDITY, EXPECTED_HUMIDITY * TOLERANCE);
    }

    @Test
    public void close() throws IOException {
        Htu21d htu21d = new Htu21d(mI2c);
        htu21d.close();
        Mockito.verify(mI2c).close();
    }

    @Test
    public void close_safeToCallTwice() throws IOException {
        Htu21d htu21d = new Htu21d(mI2c);
        htu21d.close();
        htu21d.close(); // should not throw
        Mockito.verify(mI2c, times(1)).close();
    }

    @Test
    public void readTemperature() throws IOException {
        Htu21d htu21d = new Htu21d(mI2c);
        htu21d.readTemperature();
        Mockito.verify(mI2c).readRegBuffer(eq(0xF3), any(byte[].class), eq(2));
    }

    @Test
    public void readTemperature_throwsIfClosed() throws IOException {
        Htu21d htu21d = new Htu21d(mI2c);
        htu21d.close();
        mExpectedException.expect(IllegalStateException.class);
        mExpectedException.expectMessage("not open");
        htu21d.readTemperature();
    }

    @Test
    public void readHumidity() throws IOException {
        Htu21d htu21d = new Htu21d(mI2c);
        htu21d.readHumidity();
        Mockito.verify(mI2c).readRegBuffer(eq(0xF5), any(byte[].class), eq(2));
    }

    @Test
    public void readHumidity_throwsIfClosed() throws IOException {
        Htu21d htu21d = new Htu21d(mI2c);
        htu21d.close();
        mExpectedException.expect(IllegalStateException.class);
        mExpectedException.expectMessage("not open");
        htu21d.readHumidity();
    }

    @Test
    public void readTemperatureAndHumidity() throws IOException {
        Htu21d htu21d = new Htu21d(mI2c);
        htu21d.readTemperatureAndHumidity();
        Mockito.verify(mI2c).readRegBuffer(eq(0xF3), any(byte[].class), eq(2));
        Mockito.verify(mI2c).readRegBuffer(eq(0xF5), any(byte[].class), eq(2));
    }

    @Test
    public void readTemperatureAndHumidity_throwsIfClosed() throws IOException {
        Htu21d htu21d = new Htu21d(mI2c);
        htu21d.close();
        mExpectedException.expect(IllegalStateException.class);
        mExpectedException.expectMessage("not open");
        htu21d.readTemperatureAndHumidity();
    }
}