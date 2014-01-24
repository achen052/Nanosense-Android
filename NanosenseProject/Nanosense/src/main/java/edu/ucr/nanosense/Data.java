package edu.ucr.nanosense;

/**
 * Written by Albert Chen
 * Last updated 12/04/2013
 *
 * Data is used for storing the voltage readings from each pin. The actual desired values such as
 * resistance, relative humidity and temperature can be calculated from the voltage.
 */
public class Data {

    /** Time in minutes. **/
    public double mTime;
    /** Raw voltage reading **/
    public double mVoltage;

    public Data(double time, double voltage) {
        mTime = time;
        mVoltage = voltage;
    }
}
