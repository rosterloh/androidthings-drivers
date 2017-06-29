package com.rosterloh.things.driver.ccs811;

import android.util.Log;

import com.google.android.things.pio.I2cDevice;
import com.google.android.things.pio.PeripheralManagerService;

import java.io.IOException;

/**
 * Driver for the CCS811 indoor air quality sensor.
 */
public class Ccs811 implements AutoCloseable {

    /**
     * Default I2C address for the sensor.
     */
    public static final int DEFAULT_I2C_ADDRESS = 0x5B;

    private static final String TAG = Ccs811.class.getSimpleName();

    private I2cDevice mDevice;

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
            } catch (IOException ex) {
                Log.e(TAG, "Failed to close device");
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
}
