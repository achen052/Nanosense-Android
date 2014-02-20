package edu.ucr.nanosense;

/**
 * Written by Albert Chen
 * Last updated 12/04/2013
 *
 * This Data class (struct really) is used to encapsulate a sensor reading. It allows the value to
 * be tied into the time value of when it was taken.
 */
public class Data {

    /** Time in milliseconds. **/
    public long mTime;
    /** Sensor reading (kOhms, C, RH%) */
    public double mValue;

    public Data(long time, double value) {
        mTime = time;
        mValue = value;
    }

    @Override
    public String toString() {
        return String.valueOf(mTime) + "," + String.valueOf(mValue);
    }
}
