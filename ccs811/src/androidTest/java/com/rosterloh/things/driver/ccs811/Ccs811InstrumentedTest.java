package com.rosterloh.things.driver.ccs811;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class Ccs811InstrumentedTest {

    private Ccs811 ccs811;
    private Context mContext;

    @Before
    public void initTargetContext() {
        mContext = InstrumentationRegistry.getTargetContext();
        assertThat(mContext, notNullValue());
    }

    @Before
    public void createDevice() throws IOException {
        ccs811 = new Ccs811("I2C1");
    }

    @Test
    public void testDeviceIdRead() throws IOException {
        int id = ccs811.getChipId();
        assertEquals(id, Ccs811.CHIP_ID_CCS811);
    }

    @After
    public void closeDevice() throws IOException {
        ccs811.close();
    }

}
