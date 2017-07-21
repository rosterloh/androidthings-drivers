package com.rosterloh.things.driver.ccs811;

import android.support.annotation.IntDef;

import com.google.android.things.pio.I2cDevice;
import com.google.android.things.pio.PeripheralManagerService;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Driver for the CCS811 indoor air quality sensor.
 */
public class Ccs811 implements AutoCloseable {

    /**
     * Chip ID for the BMP280
     */
    public static final int CHIP_ID_CCS811 = 0x81;

    /**
     * Default I2C address for the sensor.
     */
    public static final int DEFAULT_I2C_ADDRESS = 0x5B;

    /**
     * Measurement mode.
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({MODE_IDLE, MODE_1S, MODE_10S, MODE_60S, MODE_250MS})
    public @interface Mode {}
    public static final int MODE_IDLE   = 0; // Idle, low current mode
    public static final int MODE_1S     = 1; // Constant power mode, IAQ measurement every second
    public static final int MODE_10S    = 2; // Pulse heating mode IAQ measurement every 10 seconds
    public static final int MODE_60S    = 3; // Low power pulse heating mode IAQ measurement every 60 seconds
    public static final int MODE_250MS  = 4; // Constant power mode, sensor measurement every 250ms

    /**
     * Registers
     */
    private static final int CCS811_STATUS = 0x00;
    private static final int CCS811_MODE = 0x01;
    private static final int CCS811_ALG_RESULT_DATA = 0x02;
    private static final int CCS811_HW_ID = 0x20;
    private static final int CCS811_FW_BOOT_VERSION = 0x23;
    private static final int CCS811_FW_APP_VERSION = 0x24;
    private static final int CCS811_ERROR_ID = 0xE0;
    private static final int CCS811_START_APP = 0xF4;
    private static final int CCS811_SW_RESET = 0xFF;

    private static final int CCS811_DRIVE_MODE_MASK = 0b00000111;
    private static final int CCS811_STATUS_DATA_READY_BITSHIFT = 3;
    private static final int CCS811_STATUS_APP_VALID_BITSHIFT = 4;
    private static final int CCS811_STATUS_FW_MODE_BITSHIFT = 7;

    private I2cDevice mDevice;
    private final byte[] mBuffer = new byte[4];
    private int mChipId;
    private int mMode;

    /**
     * Create a new CCS811 sensor driver connected on the given bus.
     * @param bus I2C bus the sensor is connected to.
     * @throws IOException if device cannot be opened
     */
    public Ccs811(final String bus) throws IOException {
        this(bus, DEFAULT_I2C_ADDRESS);
    }

    /**
     * Create a new CCS811 sensor driver connected on the given bus and address.
     * @param bus I2C bus the sensor is connected to.
     * @param address I2C address of the sensor.
     * @throws IOException if device cannot be opened
     */
    public Ccs811(final String bus, final int address) throws IOException {
        final PeripheralManagerService pioService = new PeripheralManagerService();
        final I2cDevice device = pioService.openI2cDevice(bus, address);
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
     * Create a new CCS811 sensor driver connected to the given I2c device.
     * @param device I2C device of the sensor.
     * @throws IOException if device cannot be opened
     */
    /*package*/ Ccs811(final I2cDevice device) throws IOException {
        connect(device);
    }

    private void connect(final I2cDevice device) throws IOException {
        mDevice = device;

        mChipId = mDevice.readRegByte(CCS811_HW_ID);

        int status = getStatus();
        if ((status & 1) != 0) {
            throw new IOException(getError());
        }
        if ((status & (1 << CCS811_STATUS_APP_VALID_BITSHIFT)) != 0) {
            // Application start. Used to transition the CCS811 state from boot to application mode,
            // a write with no data is required.
            mDevice.write(new byte[]{(byte) CCS811_START_APP}, 1);
        } else {
            throw new IOException("CCS811 app not valid");
        }
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
     * Returns the sensor chip ID.
     */
    public int getChipId() {
        return mChipId;
    }

    /**
     * Issue a software reset of the sensor
     * @throws IOException if soft reset fails
     * @throws IllegalStateException if I2C device is not open
     */
    private void softReset() throws IOException, IllegalStateException {
        if (mDevice == null) {
            throw new IllegalStateException("I2C device not open");
        }

        final byte[] resetSequence = new byte[]{0x11, (byte) 0xE5, 0x72, (byte) 0x8A};

        mDevice.writeRegBuffer(CCS811_SW_RESET, resetSequence, resetSequence.length);
    }

    /**
     * Set the measurement mode of the sensor.
     * @param mode measurement mode.
     * @throws IOException if mode set fails
     * @throws IllegalStateException if I2C device is not open
     */
    public void setMode(@Mode int mode) throws IOException, IllegalStateException {
        if (mDevice == null) {
            throw new IllegalStateException("I2C device not open");
        }

        int regCtrl = mDevice.readRegByte(CCS811_MODE) & 0xff;
        regCtrl &= ~(CCS811_DRIVE_MODE_MASK << 4); // Clear DRIVE_MODE bits
        regCtrl |= (mode << 4); // Mask in mode
        mDevice.writeRegByte(CCS811_MODE, (byte) (regCtrl));
        mMode = mode;
    }

    /**
     * Read the current mode of the sensor
     * @return mode enum
     */
    public @Mode int getMode() {
        return mMode;
    }

    private int getStatus() throws IOException, IllegalStateException {
        if (mDevice == null) {
            throw new IllegalStateException("I2C device not open");
        }

        return mDevice.readRegByte(CCS811_STATUS) & 0xff;
    }

    private String getError() throws IOException {
        if (mDevice == null) {
            throw new IllegalStateException("I2C device not open");
        }

        int error = mDevice.readRegByte(CCS811_ERROR_ID) & 0xff;
        String msg = "Error: ";

        if ((error & (1 << 5)) != 0) msg += "HeaterSupply ";
        if ((error & (1 << 4)) != 0) msg += "HeaterFault ";
        if ((error & (1 << 3)) != 0) msg += "MaxResistance ";
        if ((error & (1 << 2)) != 0) msg += "MeasModeInvalid ";
        if ((error & (1 << 1)) != 0) msg += "ReadRegInvalid ";
        if ((error & 1) != 0) msg += "MsgInvalid ";

        return msg;
    }

    /**
     * Reads the bootloader version from the sensor
     * @return String version in semantic format or null if read fails
     */
    public String readBootVersion() {
        try {
            synchronized (mBuffer) {
                mDevice.readRegBuffer(CCS811_FW_BOOT_VERSION, mBuffer, 2);
                final int major = (mBuffer[0] & 0xf0);
                final int minor = (mBuffer[0] & 0x0f);
                final int trivial = (mBuffer[1] & 0xff);
                return major + "." + minor + "." + trivial;
            }
        } catch (IOException e) {
            return null;
        }
    }
    /**
     * Reads the application version from the sensor
     * @return String version in semantic format or null if read fails
     */
    public String readAppVersion() {
        try {
            synchronized (mBuffer) {
                mDevice.readRegBuffer(CCS811_FW_APP_VERSION, mBuffer, 2);
                final int major = (mBuffer[0] & 0xf0);
                final int minor = (mBuffer[0] & 0x0f);
                final int trivial = (mBuffer[1] & 0xff);
                return major + "." + minor + "." + trivial;
            }
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Read the current value of the algorithm result.
     * @return 2-element array. The first element is eCO2 in ppm, and the second is TVOC in ppb.
     * @throws IOException if read fails
     * @throws IllegalStateException if device is not open
     */
    public int[] readAlgorithmResults() throws IOException, IllegalStateException {
        if (mDevice == null) {
            throw new IllegalStateException("I2C device not open");
        }

        synchronized (mBuffer) {
            mDevice.readRegBuffer(CCS811_ALG_RESULT_DATA, mBuffer, 4);
            final int eCO2 = ((mBuffer[0] & 0xff) << 8) | (mBuffer[1] & 0xff);
            final int tVOC = ((mBuffer[2] & 0xff) << 8) | (mBuffer[3] & 0xff);
            return new int[]{eCO2, tVOC};
        }
    }
}
