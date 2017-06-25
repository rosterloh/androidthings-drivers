package com.rosterloh.things.driver.htu21d;

import android.support.annotation.IntDef;
import android.support.annotation.VisibleForTesting;

import com.google.android.things.pio.I2cDevice;
import com.google.android.things.pio.PeripheralManagerService;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Driver for the HTU21D environmental sensor.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class Htu21d implements AutoCloseable {

    private static final String TAG = Htu21d.class.getSimpleName();

    /**
     * Default I2C address for the sensor.
     */
    public static final int DEFAULT_I2C_ADDRESS = 0x40;

    // Sensor constants from the datasheet.
    // https://cdn-shop.adafruit.com/datasheets/1899_HTU21D.pdf
    /**
     * Minimum temperature in Celsius the sensor can measure.
     */
    public static final float MIN_TEMP_C = -40f;
    /**
     * Maximum temperature in Celsius the sensor can measure.
     */
    public static final float MAX_TEMP_C = 125f;
    /**
     * Minimum relative humidity in % the sensor can measure.
     */
    public static final float MIN_RH = 0f;
    /**
     * Maximum relative humidity in % the sensor can measure.
     */
    public static final float MAX_RH = 100f;
    /**
     * Maximum power consumption in micro-amperes.
     */
    public static final float MAX_POWER_CONSUMPTION_UA = 500f;

    /**
     * Sampling mode in bits for temperature and humidity.
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({MODE_12_14, MODE_8_12, MODE_10_13, MODE_11_11})
    public @interface Mode {}
    public static final int MODE_12_14 = 0;
    public static final int MODE_8_12  = 1;
    public static final int MODE_10_13 = 2;
    public static final int MODE_11_11 = 3;

    /**
     * Registers
     */
    private static final int HTU21D_REG_TEMP_HOLD    = 0xE3;
    private static final int HTU21D_REG_HUM_HOLD     = 0xE5;
    private static final int HTU21D_REG_TEMP_NO_HOLD = 0xF3;
    private static final int HTU21D_REG_HUM_NO_HOLD  = 0xF5;
    private static final int HTU21D_REG_USER_WRITE   = 0xE6;
    private static final int HTU21D_REG_USER_READ    = 0xE7;
    private static final int HTU21D_REG_RESET        = 0xFE;

    private static final int HTU21D_RESOLUTION_MASK = 0b10000001;

    private I2cDevice mDevice;
    private final int[] mTempCalibrationData = new int[3];
    private final int[] mPressureCalibrationData = new int[9];
    private final byte[] mBuffer = new byte[3]; // for reading sensor values
    private boolean mEnabled = false;
    private int mSensorResolution;

    /**
     * Create a new HTU21D sensor driver connected on the given bus.
     * @param bus I2C bus the sensor is connected to.
     * @throws IOException if device cannot be opened
     */
    public Htu21d(String bus) throws IOException {
        this(bus, DEFAULT_I2C_ADDRESS);
    }

    /**
     * Create a new HTU21D sensor driver connected on the given bus and address.
     * @param bus I2C bus the sensor is connected to.
     * @param address I2C address of the sensor.
     * @throws IOException if device cannot be opened
     */
    public Htu21d(String bus, int address) throws IOException {
        PeripheralManagerService pioService = new PeripheralManagerService();
        I2cDevice device = pioService.openI2cDevice(bus, address);
        try {
            connect(device);
        } catch (IOException|RuntimeException e) {
            try {
                close();
            } catch (IOException|RuntimeException ignored) {
            }
            throw e;
        }
    }

    /**
     * Create a new HTU21D sensor driver connected to the given I2c device.
     * @param device I2C device of the sensor.
     * @throws IOException if device cannot be opened
     */
    /*package*/ Htu21d(I2cDevice device) throws IOException {
        connect(device);
    }

    private void connect(I2cDevice device) throws IOException {
        mDevice = device;

        // Read current resolution of the sensors.
        mSensorResolution = mDevice.readRegByte(HTU21D_REG_USER_READ) & HTU21D_RESOLUTION_MASK;

        // Issue a soft reset
        mDevice.writeRegByte(HTU21D_REG_RESET, (byte) 1);
    }

    /**
     * Close the driver and the underlying device.
     * @throws IOException if device cannot be closed
     */
    @Override
    public void close() throws IOException {
        if (mDevice != null) {
            try {
                mDevice.close();
            } finally {
                mDevice = null;
            }
        }
    }

    /**
     * Read the current resolution of the sensors
     * @return resolution enum
     */
    public @Mode int getSensorResolution() {
        return mSensorResolution;
    }

    /**
     * Read the current temperature.
     * @return the current temperature in degrees Celsius
     */
    public float readTemperature() throws IOException, IllegalStateException {
        int rawTemp = readSample(HTU21D_REG_TEMP_NO_HOLD);
        return compensateTemperature(rawTemp);
    }

    /**
     * Read the current relative humidity.
     * @return the relative humidity in % units
     * @throws IOException if read fails
     */
    public float readHumidity() throws IOException, IllegalStateException {
        int rawHum = readSample(HTU21D_REG_HUM_NO_HOLD);
        return compensateHumidity(rawHum);
    }

    /**
     * Read the current temperature and humidity.
     * @return a 2-element array. The first element is temperature in degrees Celsius, and the
     * second is relative humidity in %.
     * @throws IOException if read fails
     */
    public float[] readTemperatureAndHumidity() throws IOException, IllegalStateException {
        int rawTemp = readSample(HTU21D_REG_TEMP_NO_HOLD);
        float temperature = compensateTemperature(rawTemp);
        int rawHumidity = readSample(HTU21D_REG_HUM_NO_HOLD);
        float humidity = compensateHumidity(rawHumidity);
        return new float[]{temperature, humidity};
    }

    /**
     * Reads 16 bits from the given address.
     * @throws IOException if read fails
     */
    private int readSample(int address) throws IOException, IllegalStateException {
        if (mDevice == null) {
            throw new IllegalStateException("I2C device not open");
        }

        synchronized (mBuffer) {
            mDevice.readRegBuffer(address, mBuffer, 2);
            // msb[7:0] lsb[7:0]
            int msb = mBuffer[0] & 0xff;
            int lsb = mBuffer[1] & 0xff;
            return (msb << 8 | lsb);
        }
    }

    /**
     * Formula T = -46.85 + 175.72 * ST / 2^16 from datasheet p14
     * @param rawTemp raw temperature value read from device
     * @return temperature in °C range from -40°C to +125°C
     */
    @VisibleForTesting
    static float compensateTemperature(int rawTemp) {
        int temp = ((21965 * (rawTemp & 0xFFFC)) >> 13) - 46850;
        return (float) temp / 1000;
    }

    /**
     * Formula RH = -6 + 125 * SRH / 2^16 from datasheet p14
     * @param rawHumidity raw humidity value read from device
     * @return relative humidity RH% range from 0-100
     */
    @VisibleForTesting
    static float compensateHumidity(int rawHumidity) {
        int hum = ((15625 * (rawHumidity & 0xFFFC)) >> 13) - 6000;
        return (float) hum / 1000;
    }
}