package com.rosterloh.things.driver.ccs811;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManagerService;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class Ccs811InstrumentedTest {

    private Ccs811 ccs811;
    private Context context;

    @Before
    public void initTargetContext() {
        context = InstrumentationRegistry.getTargetContext();
        assertNotNull(context);
    }

    @Before
    public void createDevice() throws IOException {
        PeripheralManagerService pioService = new PeripheralManagerService();
        Gpio rst = pioService.openGpio("BCM20");
        Gpio wake = pioService.openGpio("BCM16");
        wake.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
        rst.setDirection(Gpio.DIRECTION_OUT_INITIALLY_HIGH);
        ccs811 = new Ccs811("I2C1", 0x5A);
        assertNotNull(ccs811);
    }

    @Test
    public void testDeviceIdRead() throws IOException {
        int id = ccs811.getChipId();
        assertEquals(id, Ccs811.CHIP_ID_CCS811);
    }

    @Test
    public void testStatusForError() throws IOException {
        int status = ccs811.getStatus();
    }

    @After
    public void closeDevice() throws IOException {
        if (ccs811 != null) {
            ccs811.close();
        }
    }
}
