package edu.ucr.nanosense;

import java.text.DecimalFormat;

/**
 * Written by Albert Chen
 * Last updated 12/04/2013
 *
 * This Data class (struct really) is used to encapsulate a sensor reading. It allows the value to
 * be tied into the time value of when it was taken. Also makes for easy printing out for debugging.
 */
public class Data {

    /** Time in milliseconds. **/
    public long mTime;
    /** Sensor reading (kOhms, C, RH%) */
    public double mValue;

    /** 3 decimal places */
    public static final String DECIMAL_FORMAT_REGEX = "0.000";
    /** Decimal formater */
    public static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat(DECIMAL_FORMAT_REGEX);

    public Data(long time, double value) {
        mTime = time;
        mValue = value;
    }

    /**
     * Comma delimited "<time>,<value>"
     *
     * @return A {@link String} with the above format.
     */
    @Override
    public String toString() {
        return String.valueOf(mTime) + "," + DECIMAL_FORMAT.format(mValue);
    }
}
