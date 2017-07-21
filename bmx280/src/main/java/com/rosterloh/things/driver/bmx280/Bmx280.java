package com.rosterloh.things.driver.bmx280;

import android.support.annotation.IntDef;
import android.support.annotation.VisibleForTesting;

import com.google.android.things.pio.I2cDevice;
import com.google.android.things.pio.PeripheralManagerService;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Driver for the BMP/BME 280 temperature sensor.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class Bmx280 implements AutoCloseable {

    /**
     * Chip ID for the BMP280
     */
    public static final int CHIP_ID_BMP280 = 0x58;
    /**
     * Chip ID for the BME280
     */
    public static final int CHIP_ID_BME280 = 0x60;
    /**
     * Default I2C address for the sensor.
     */
    public static final int DEFAULT_I2C_ADDRESS = 0x77;

    // Sensor constants from the datasheet.
    // https://cdn-shop.adafruit.com/datasheets/BST-BMP280-DS001-11.pdf
    /**
     * Mininum temperature in Celsius the sensor can measure.
     */
    public static final float MIN_TEMP_C = -40f;
    /**
     * Maximum temperature in Celsius the sensor can measure.
     */
    public static final float MAX_TEMP_C = 85f;
    /**
     * Minimum pressure in hPa the sensor can measure.
     */
    public static final float MIN_PRESSURE_HPA = 300f;
    /**
     * Maximum pressure in hPa the sensor can measure.
     */
    public static final float MAX_PRESSURE_HPA = 1100f;
    /**
     * Maximum power consumption in micro-amperes when measuring temperature.
     */
    public static final float MAX_POWER_CONSUMPTION_TEMP_UA = 325f;
    /**
     * Maximum power consumption in micro-amperes when measuring pressure.
     */
    public static final float MAX_POWER_CONSUMPTION_PRESSURE_UA = 720f;
    /**
     * Maximum frequency of the measurements.
     */
    public static final float MAX_FREQ_HZ = 181f;
    /**
     * Minimum frequency of the measurements.
     */
    public static final float MIN_FREQ_HZ = 23.1f;

    /**
     * Power mode.
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({MODE_SLEEP, MODE_FORCED, MODE_NORMAL})
    public @interface Mode {}
    public static final int MODE_SLEEP = 0;
    public static final int MODE_FORCED = 1;
    public static final int MODE_NORMAL = 2;

    /**
     * Oversampling multiplier.
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({OVERSAMPLING_SKIPPED, OVERSAMPLING_1X, OVERSAMPLING_2X, OVERSAMPLING_4X, OVERSAMPLING_8X,
            OVERSAMPLING_16X})
    public @interface Oversampling {}
    public static final int OVERSAMPLING_SKIPPED = 0;
    public static final int OVERSAMPLING_1X = 1;
    public static final int OVERSAMPLING_2X = 2;
    public static final int OVERSAMPLING_4X = 3;
    public static final int OVERSAMPLING_8X = 4;
    public static final int OVERSAMPLING_16X = 5;

    // Registers
    private static final int BMX280_REG_TEMP_CALIB_1 = 0x88;
    private static final int BMX280_REG_TEMP_CALIB_2 = 0x8A;
    private static final int BMX280_REG_TEMP_CALIB_3 = 0x8C;

    private static final int BMX280_REG_PRESS_CALIB_1 = 0x8E;
    private static final int BMX280_REG_PRESS_CALIB_2 = 0x90;
    private static final int BMX280_REG_PRESS_CALIB_3 = 0x92;
    private static final int BMX280_REG_PRESS_CALIB_4 = 0x94;
    private static final int BMX280_REG_PRESS_CALIB_5 = 0x96;
    private static final int BMX280_REG_PRESS_CALIB_6 = 0x98;
    private static final int BMX280_REG_PRESS_CALIB_7 = 0x9A;
    private static final int BMX280_REG_PRESS_CALIB_8 = 0x9C;
    private static final int BMX280_REG_PRESS_CALIB_9 = 0x9E;

    private static final int BMX280_REG_HUM_CALIB_1 = 0xA1;
    private static final int BMX280_REG_HUM_CALIB_2 = 0xE1;
    private static final int BMX280_REG_HUM_CALIB_3 = 0xE3;
    private static final int BMX280_REG_HUM_CALIB_4 = 0xE4;
    private static final int BMX280_REG_HUM_CALIB_6 = 0xE7;

    private static final int BMX280_REG_ID = 0xD0;
    private static final int BMX280_REG_CTRL_HUM = 0xF2;
    private static final int BMX280_REG_CTRL = 0xF4;

    private static final int BMX280_REG_PRESS = 0xF7;
    private static final int BMX280_REG_TEMP = 0xFA;
    private static final int BMX280_REG_HUM = 0xFD;

    private static final int BMX280_POWER_MODE_MASK = 0b00000011;
    private static final int BMX280_POWER_MODE_SLEEP = 0b00000000;
    private static final int BMX280_POWER_MODE_NORMAL = 0b00000011;
    private static final int BMX280_OVERSAMPLING_HUMIDITY_MASK = 0b00000111;
    private static final int BMX280_OVERSAMPLING_PRESSURE_MASK = 0b00011100;
    private static final int BMX280_OVERSAMPLING_PRESSURE_BITSHIFT = 2;
    private static final int BMX280_OVERSAMPLING_TEMPERATURE_MASK = 0b11100000;
    private static final int BMX280_OVERSAMPLING_TEMPERATURE_BITSHIFT = 5;

    private I2cDevice mDevice;
    private final int[] mTempCalibrationData = new int[3];
    private final int[] mPressureCalibrationData = new int[9];
    private final int[] mHumidityCalibrationData = new int[6];
    private final byte[] mBuffer = new byte[3]; // for reading sensor values
    private boolean mEnabled = false;
    private int mChipId;
    private int mMode;
    private int mHumidityOversampling;
    private int mPressureOversampling;
    private int mTemperatureOversampling;

    /**
     * Create a new BMP/BME280 sensor driver connected on the given bus.
     * @param bus I2C bus the sensor is connected to.
     * @throws IOException if creation fails
     */
    public Bmx280(String bus) throws IOException {
        this(bus, DEFAULT_I2C_ADDRESS);
    }

    /**
     * Create a new BMP/BME280 sensor driver connected on the given bus and address.
     * @param bus I2C bus the sensor is connected to.
     * @param address I2C address of the sensor.
     * @throws IOException if creation fails
     */
    public Bmx280(String bus, int address) throws IOException {
        PeripheralManagerService pioService = new PeripheralManagerService();
        I2cDevice device = pioService.openI2cDevice(bus, address);
        try {
            connect(device);
        } catch (IOException e) {
            try {
                close();
            } catch (IOException ignored) {
            }
            throw e;
        }
    }

    /**
     * Create a new BMP/BME280 sensor driver connected to the given I2c device.
     * @param device I2C device of the sensor.
     * @throws IOException if creation fails
     */
    /*package*/  Bmx280(I2cDevice device) throws IOException {
        connect(device);
    }

    private void connect(I2cDevice device) throws IOException {
        mDevice = device;

        mChipId = mDevice.readRegByte(BMX280_REG_ID);

        // Read temperature calibration data (3 words). First value is unsigned.
        mTempCalibrationData[0] = mDevice.readRegWord(BMX280_REG_TEMP_CALIB_1) & 0xffff;
        mTempCalibrationData[1] = (short) mDevice.readRegWord(BMX280_REG_TEMP_CALIB_2);
        mTempCalibrationData[2] = (short) mDevice.readRegWord(BMX280_REG_TEMP_CALIB_3);
        // Read pressure calibration data (9 words). First value is unsigned.
        mPressureCalibrationData[0] = mDevice.readRegWord(BMX280_REG_PRESS_CALIB_1) & 0xffff;
        mPressureCalibrationData[1] = (short) mDevice.readRegWord(BMX280_REG_PRESS_CALIB_2);
        mPressureCalibrationData[2] = (short) mDevice.readRegWord(BMX280_REG_PRESS_CALIB_3);
        mPressureCalibrationData[3] = (short) mDevice.readRegWord(BMX280_REG_PRESS_CALIB_4);
        mPressureCalibrationData[4] = (short) mDevice.readRegWord(BMX280_REG_PRESS_CALIB_5);
        mPressureCalibrationData[5] = (short) mDevice.readRegWord(BMX280_REG_PRESS_CALIB_6);
        mPressureCalibrationData[6] = (short) mDevice.readRegWord(BMX280_REG_PRESS_CALIB_7);
        mPressureCalibrationData[7] = (short) mDevice.readRegWord(BMX280_REG_PRESS_CALIB_8);
        mPressureCalibrationData[8] = (short) mDevice.readRegWord(BMX280_REG_PRESS_CALIB_9);
        if (mChipId == CHIP_ID_BME280) {
            // Read humidity calibration data
            mHumidityCalibrationData[0] = mDevice.readRegByte(BMX280_REG_HUM_CALIB_1) & 0xff;  // unsigned char
            mHumidityCalibrationData[1] = (short) mDevice.readRegWord(BMX280_REG_HUM_CALIB_2); // signed short
            mHumidityCalibrationData[2] = mDevice.readRegByte(BMX280_REG_HUM_CALIB_3) & 0xff;  // unsigned char

            synchronized (mBuffer) {
                mDevice.readRegBuffer(BMX280_REG_HUM_CALIB_4, mBuffer, 3);
                // msb[7:0] lsb[7:0] xlsb[7:4]
                int e4 = mBuffer[0] & 0xff;
                int e5 = mBuffer[1] & 0xff;
                int e6 = mBuffer[2] & 0xff;
                // e4[0:7] e5[3:0]
                mHumidityCalibrationData[3] = (e4 << 4) | (e5 & 0x0F);
                // e5[7:4] e6[7:0]
                mHumidityCalibrationData[4] = (e6 << 4) | (e5 & 0xF0);
            }
            mHumidityCalibrationData[5] = mDevice.readRegByte(BMX280_REG_HUM_CALIB_6);         // signed char
        }
    }

    /**
     * Set the power mode of the sensor.
     * @param mode power mode.
     * @throws IOException on failure
     * @throws IllegalStateException if device is not open
     */
    public void setMode(@Mode int mode) throws IOException, IllegalStateException {
        if (mDevice == null) {
            throw new IllegalStateException("I2C device not open");
        }

        int regCtrl = mDevice.readRegByte(BMX280_REG_CTRL) & 0xff;
        if (mode == MODE_SLEEP) {
            regCtrl &= ~BMX280_POWER_MODE_MASK;
        } else {
            regCtrl |= BMX280_POWER_MODE_NORMAL;
        }
        mDevice.writeRegByte(BMX280_REG_CTRL, (byte) (regCtrl));
        mMode = mode;
    }

    /**
     * Set oversampling multiplier for the humidity measurement.
     * @param oversampling humidity oversampling multiplier.
     * @throws IOException on failure
     */
    public void setHumidityOversampling(@Oversampling int oversampling) throws IOException {
        if (mDevice == null) {
            throw new IllegalStateException("I2C device not open");
        }
        if (mChipId != CHIP_ID_BME280) {
            throw new IllegalStateException("device does not support humidity measurement");
        }

        int regCtrl = mDevice.readRegByte(BMX280_REG_CTRL_HUM) & 0xff;
        if (oversampling == OVERSAMPLING_SKIPPED) {
            regCtrl &= ~BMX280_OVERSAMPLING_HUMIDITY_MASK;
        } else {
            regCtrl |= oversampling;
        }
        mDevice.writeRegByte(BMX280_REG_CTRL_HUM, (byte) (regCtrl));
        mHumidityOversampling = oversampling;
    }

    /**
     * Set oversampling multiplier for the temperature measurement.
     * @param oversampling temperature oversampling multiplier.
     * @throws IOException on failure
     * @throws IllegalStateException if device is not open
     */
    public void setTemperatureOversampling(@Oversampling int oversampling) throws IOException, IllegalStateException {
        if (mDevice == null) {
            throw new IllegalStateException("I2C device not open");
        }

        int regCtrl = mDevice.readRegByte(BMX280_REG_CTRL) & 0xff;
        if (oversampling == OVERSAMPLING_SKIPPED) {
            regCtrl &= ~BMX280_OVERSAMPLING_TEMPERATURE_MASK;
        } else {
            regCtrl |= oversampling << BMX280_OVERSAMPLING_TEMPERATURE_BITSHIFT;
        }
        mDevice.writeRegByte(BMX280_REG_CTRL, (byte) (regCtrl));
        mTemperatureOversampling = oversampling;
    }

    /**
     * Set oversampling multiplier for the pressure measurement.
     * @param oversampling pressure oversampling multiplier.
     * @throws IOException on failure
     * @throws IllegalStateException if device is not open
     */
    public void setPressureOversampling(@Oversampling int oversampling) throws IOException, IllegalStateException {
        if (mDevice == null) {
            throw new IllegalStateException("I2C device not open");
        }

        int regCtrl = mDevice.readRegByte(BMX280_REG_CTRL) & 0xff;
        if (oversampling == OVERSAMPLING_SKIPPED) {
            regCtrl &= ~BMX280_OVERSAMPLING_PRESSURE_MASK;
        } else {
            regCtrl |= oversampling << BMX280_OVERSAMPLING_PRESSURE_BITSHIFT;
        }
        mDevice.writeRegByte(BMX280_REG_CTRL, (byte) (regCtrl));
        mPressureOversampling = oversampling;
    }

    /**
     * Close the driver and the underlying device.
     * @throws IOException on failure
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
     * Returns the sensor chip ID.
     */
    public int getChipId() {
        return mChipId;
    }

    /**
     * Set internal id. Used for testing only.
     * @param id chip id to set
     */
    @VisibleForTesting
    void setChipId(int id) {
        mChipId = id;
    }

    /**
     * Read the current temperature.
     * @return the current temperature in degrees Celsius
     * @throws IOException on failure
     * @throws IllegalStateException on configuration error
     */
    public float readTemperature() throws IOException, IllegalStateException {
        if (mTemperatureOversampling == OVERSAMPLING_SKIPPED) {
            throw new IllegalStateException("temperature oversampling is skipped");
        }
        int rawTemp = readSample(BMX280_REG_TEMP);
        return compensateTemperature(rawTemp, mTempCalibrationData)[0];
    }

    /**
     * Read the current barometric pressure. If you also intend to use temperature readings, prefer
     * {@link #readTemperatureAndPressure()} instead since sampling the current pressure already
     * requires sampling the current temperature.
     * @return the barometric pressure in hPa units
     * @throws IOException on failure
     * @throws IllegalStateException on configuration error
     */
    public float readPressure() throws IOException, IllegalStateException {
        float[] values = readTemperatureAndPressure();
        return values[1];
    }

    /**
     * Read the humidity temperature.
     * @return the current relative humidity in percent
     * @throws IOException on failure
     * @throws IllegalStateException on configuration error
     */
    public float readHumidity() throws IOException, IllegalStateException {
        if (mChipId != CHIP_ID_BME280) {
            throw new IllegalStateException("device does not support humidity measurement");
        }
        float[] values = readTemperatureAndHumidity();
        return values[1];
    }

    /**
     * Read the current temperature and barometric pressure.
     * @return a 2-element array. The first element is temperature in degrees Celsius and the
     * second is barometric pressure in hPa units.
     * @throws IOException on failure
     * @throws IllegalStateException on configuration error
     */
    public float[] readTemperatureAndPressure() throws IOException, IllegalStateException {
        if (mTemperatureOversampling == OVERSAMPLING_SKIPPED) {
            throw new IllegalStateException("temperature oversampling is skipped");
        }
        if (mPressureOversampling == OVERSAMPLING_SKIPPED) {
            throw new IllegalStateException("pressure oversampling is skipped");
        }
        // The pressure compensation formula requires the fine temperature reading, so we always
        // read temperature first.
        int rawTemp = readSample(BMX280_REG_TEMP);
        float[] temperatures = compensateTemperature(rawTemp, mTempCalibrationData);
        int rawPressure = readSample(BMX280_REG_PRESS);
        float pressure = compensatePressure(rawPressure, temperatures[1], mPressureCalibrationData);
        return new float[]{temperatures[0], pressure};
    }

    /**
     * Read the current temperature and humidity.
     * @return a 2-element array. The first element is temperature in degrees Celsius and the
     * second is humidity in %rH.
     * @throws IOException on failure
     */
    public float[] readTemperatureAndHumidity() throws IOException, IllegalStateException {
        if (mChipId != CHIP_ID_BME280) {
            throw new IllegalStateException("device does not support humidity measurement");
        }
        if (mTemperatureOversampling == OVERSAMPLING_SKIPPED) {
            throw new IllegalStateException("temperature oversampling is skipped");
        }
        if (mHumidityOversampling == OVERSAMPLING_SKIPPED) {
            throw new IllegalStateException("humidity oversampling is skipped");
        }
        // The humidity compensation formula requires the fine temperature reading, so we always
        // read temperature first.
        int rawTemp = readSample(BMX280_REG_TEMP);
        float[] temperatures = compensateTemperature(rawTemp, mTempCalibrationData);
        int rawHumidity = readSample(BMX280_REG_HUM);
        float humidity = compensateHumidity(rawHumidity, temperatures[1], mHumidityCalibrationData);
        return new float[]{temperatures[0], humidity};
    }

    /**
     * Read the current temperature, pressure and humidity.
     * @return a 3-element array. The first element is temperature in degrees Celsius, the second
     * is barometric pressure in hPa units and the third is humidity in %rH.
     * @throws IOException on failure
     */
    public float[] readTemperaturePressureAndHumidity() throws IOException, IllegalStateException {
        if (mChipId != CHIP_ID_BME280) {
            throw new IllegalStateException("device does not support humidity measurement");
        }
        if (mTemperatureOversampling == OVERSAMPLING_SKIPPED) {
            throw new IllegalStateException("temperature oversampling is skipped");
        }
        if (mPressureOversampling == OVERSAMPLING_SKIPPED) {
            throw new IllegalStateException("pressure oversampling is skipped");
        }
        if (mHumidityOversampling == OVERSAMPLING_SKIPPED) {
            throw new IllegalStateException("humidity oversampling is skipped");
        }
        // The pressure and humidity compensation formulas require the fine temperature reading, so we always
        // read temperature first.
        int rawTemp = readSample(BMX280_REG_TEMP);
        float[] temperatures = compensateTemperature(rawTemp, mTempCalibrationData);
        int rawPressure = readSample(BMX280_REG_PRESS);
        float pressure = compensatePressure(rawPressure, temperatures[1], mPressureCalibrationData);
        int rawHumidity = readSample(BMX280_REG_HUM);
        float humidity = compensateHumidity(rawHumidity, temperatures[1], mHumidityCalibrationData);
        return new float[]{temperatures[0], pressure, humidity};
    }

    /**
     * Reads 20 bits from the given address.
     * @throws IOException on failure
     */
    private int readSample(int address) throws IOException, IllegalStateException {
        if (mDevice == null) {
            throw new IllegalStateException("I2C device not open");
        }

        synchronized (mBuffer) {
            if (address == BMX280_REG_HUM) {
                mDevice.readRegBuffer(address, mBuffer, 2);
                int msb = mBuffer[0] & 0xff;
                int lsb = mBuffer[1] & 0xff;
                return (msb << 8 | lsb);
            } else {
                mDevice.readRegBuffer(address, mBuffer, 3);
                // msb[7:0] lsb[7:0] xlsb[7:4]
                int msb = mBuffer[0] & 0xff;
                int lsb = mBuffer[1] & 0xff;
                int xlsb = mBuffer[2] & 0xf0;
                // Convert to 20bit integer
                return (msb << 16 | lsb << 8 | xlsb) >> 4;
            }
        }
    }

    // Compensation formula from the BMP280 datasheet.
    // https://cdn-shop.adafruit.com/datasheets/BST-BMP280-DS001-11.pdf
    @VisibleForTesting
    static float[] compensateTemperature(int rawTemp, int[] calibrationData) {
        int digT1 = calibrationData[0];
        int digT2 = calibrationData[1];
        int digT3 = calibrationData[2];

        float adcT = (float) rawTemp;
        float var1 = (adcT / 16384f - ((float) digT1) / 1024f) * ((float) digT2);
        float var2 = ((adcT / 131072f - ((float) digT1) / 8192f) * (adcT / 131072f
                - ((float) digT1) / 8192f)) * ((float) digT3);
        float fineTemp = var1 + var2;
        return new float[]{fineTemp / 5120.0f, fineTemp};
    }

    // Compensation formula from the BMP280 datasheet.
    // https://cdn-shop.adafruit.com/datasheets/BST-BMP280-DS001-11.pdf
    @VisibleForTesting
    static float compensatePressure(int rawPressure, float fineTemperature, int[] calibration) {
        int digP1 = calibration[0];
        int digP2 = calibration[1];
        int digP3 = calibration[2];
        int digP4 = calibration[3];
        int digP5 = calibration[4];
        int digP6 = calibration[5];
        int digP7 = calibration[6];
        int digP8 = calibration[7];
        int digP9 = calibration[8];

        float var1 = (fineTemperature / 2.0f) - 64000.0f;
        float var2 = var1 * var1 * ((float) digP6) / 32768.0f;
        var2 = var2 + var1 * ((float) digP5) * 2.0f;
        var2 = (var2 / 4.0f) + (((float) digP4) * 65536.0f);
        var1 = (((float) digP3) * var1 * var1 / 524288.0f + ((float) digP2) * var1) / 524288.0f;
        var1 = (1.0f + var1 / 32768.0f) * ((float) digP1);
        if (var1 == 0.0) {
            return 0; // avoid exception caused by division by zero
        }
        float p = 1048576.0f - (float) rawPressure;
        p = (p - (var2 / 4096.0f)) * 6250.0f / var1;
        var1 = ((float) digP9) * p * p / 2147483648.0f;
        var2 = p * ((float) digP8) / 32768.0f;
        p = p + (var1 + var2 + ((float) digP7)) / 16.0f;
        // p is in Pa, convert to hPa
        return p / 100.0f;
    }

    // Compensation formula from the BME280 datasheet.
    // https://ae-bst.resource.bosch.com/media/_tech/media/datasheets/BST-BME280_DS001-11.pdf
    @VisibleForTesting
    static float compensateHumidity(int rawHum, float fineTemperature, int[] calibration) {
        int digH1 = calibration[0];
        int digH2 = calibration[1];
        int digH3 = calibration[2];
        int digH4 = calibration[3];
        int digH5 = calibration[4];
        int digH6 = calibration[5];

        float adcH = (float) rawHum;
        float varH = fineTemperature - 76800.0f;
        varH = (adcH - (((float) digH4) * 64.0f + ((float) digH5) / 16384.0f * varH))
                * (((float) digH2) / 65536.0f * (1.0f + ((float) digH6) / 67108864.0f * varH
                * (1.0f + ((float) digH3) / 67108864.0f * varH)));
        varH = varH * (1.0f - ((float) digH1) * varH / 524288.0f);
        if (varH > 100.0f)
            varH = 100.0f;
        else if (varH < 0.0f)
            varH = 0.0f;
        return varH;
    }
}