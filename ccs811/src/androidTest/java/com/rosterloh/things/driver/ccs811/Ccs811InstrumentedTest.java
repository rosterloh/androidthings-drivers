package com.rosterloh.things.driver;

import android.support.test.runner.AndroidJUnit4;

import com.rosterloh.things.driver.ccs811.Ccs811;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.any;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class Ccs811InstrumentedTest {

    private Ccs811 ccs811;

    @Before
    public void createDevice() throws IOException {
        ccs811 = new Ccs811("I2C1");
    }
/*
    @Test
    public void testTemperatureRead() throws IOException {
        float temperature = ccs811.readTemperature();
        assertThat(temperature, any(float.class));
    }
*/
    @After
    public void closeDevice() throws IOException {
        ccs811.close();
    }
}
