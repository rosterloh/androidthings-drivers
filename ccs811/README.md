CCS811 driver for Android Things
================================

This driver supports ams [CCS811][product_ccs811] indoor air quality monitor.

NOTE: these drivers are not production-ready. They are offered as sample
implementations of Android Things user space drivers for common peripherals
as part of the Developer Preview release. There is no guarantee
of correctness, completeness or robustness.

How to use the driver
---------------------

### Gradle dependency

To use the `ccs811` driver, simply add the line below to your project's `build.gradle`,
where `<version>` matches the last version of the driver available on [jcenter][jcenter].

```
dependencies {
    compile 'com.rosterloh.things:driver-ccs811:<version>'
}
```

### Sample usage

```java
import com.rosterloh.things.driver.ccs811.Ccs811;

// Access the sensor:

Ccs811 mCcs811;

try {
    mCcs811 = new Ccs811(i2cBusName);
} catch (IOException e) {
    // couldn't configure the device...
}

// Tell the sensor to start sampling
mCcs811.setMode(Ccs811.MODE_60S)

// Read the sensor values:

try {
    int[] values = mCcs811.readAlgorithmResults();
} catch (IOException e) {
    // error sensor values
}

// Close the sensor when finished:

try {
    mCcs811.close();
} catch (IOException e) {
    // error closing sensor
}
```

[product_ccs811]: https://cdn.sparkfun.com/assets/learn_tutorials/1/4/3/CCS811_Datasheet-DS000459.pdf
[jcenter]: https://bintray.com/google/androidthings/androidthings-driver-ccs811/_latestVersion