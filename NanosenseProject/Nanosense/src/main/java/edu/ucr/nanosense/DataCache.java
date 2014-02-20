package edu.ucr.nanosense;

import java.util.ArrayList;

/**
 * This class is used to store the polled data. It is a memory cache so that only 1 copy of the
 * data needs to exist. The class can be declared in both {@link edu.ucr.nanosense.NanoSenseActivity}
 * and {@link edu.ucr.nanosense.GraphView} with {@link edu.ucr.nanosense.NanoSenseActivity} writing
 * to it and {@link edu.ucr.nanosense.GraphView} reading from it while maintaining only 1 copy.
 *
 * The size of the data if polling at 100 ms with a 1 hour run.
 * 1000 ms/s / 100ms = 10 loops/s
 * 17 sensor data points stored as doubles every polling loop
 * 17 * 8 bytes = 136 bytes/loop
 * 136 bytes/loop * 10 loops/s = 1360 bytes/s
 * Running for 60 min would give 3600s
 * 3600s * 1360 bytes/s = 4896000 bytes = 4.66MB after 1 hour.
 *
 * Additionally, since this may be too large to restore after a certain point, it might be necessary
 * to either save it to a file and load it in save instance state after the size exceeds a certain
 * amount. // TODO: Add writing to file when cache exceeds certain size.
 */
public class DataCache {

    /**
     * Used for storing the sensor reading values
     * Nano Sensor (0 - 15): kOhms
     * Humidity (16) : RH%
     * Temperature (17) : C
     *
     * This is public rather than using getter/setter methods so it will not need to be passed
     * by reference but can be directly accessed. Also, this will be a static class.
     */
    public static ArrayList<ArrayList<Double>> SENSOR_DATA = new ArrayList<ArrayList<Double>>();
}
