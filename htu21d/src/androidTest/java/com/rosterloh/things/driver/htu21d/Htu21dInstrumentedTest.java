package com.rosterloh.things.driver.htu21d;

import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.any;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class Htu21dInstrumentedTest {
    private Htu21d htu21d;

    @Before
    public void createDevice() throws IOException {
        htu21d = new Htu21d("I2C1");
        assertNotNull(htu21d);
    }

    @Test
    public void testTemperatureRead() throws IOException {
        float temperature = htu21d.readTemperature();
        assertThat(temperature, any(float.class));
    }

    @After
    public void closeDevice() throws IOException {
        if (htu21d != null) {
            htu21d.close();
        }
    }
}
