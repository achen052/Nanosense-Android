package edu.ucr.nanosense;

import android.content.Context;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;

/**
 * Written by Albert Chen
 * Last updated 12/04/2013
 */
public class GraphView extends SurfaceView implements SurfaceHolder.Callback {

    /** Used for storing the sensor reading values
     *  Nano Sensor (0 - 15): kOhms
     *  Humidity (16) : RH%
     *  Temperature (17) : C
     */
    private static ArrayList<ArrayList<Double>> mValues;

    /** Number of sensors (16 nanosensors, 1 humidity, 1 temperature) **/
    private static final int NUM_SENSORS = 18;

    private int mViewMode = Constants.GraphView.VIEW_NANOSENSOR;

    public GraphView(Context context) {
        super(context);
        /** Initialize an array for each sensor **/
        for (int i = 0; i < NUM_SENSORS; ++i) {
            mValues.add(new ArrayList<Double>());
        }
    }

    public GraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
        /** Initialize an array for each sensor **/
        for (int i = 0; i < NUM_SENSORS; ++i) {
            mValues.add(new ArrayList<Double>());
        }
    }

    public GraphView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        /** Initialize an array for each sensor **/
        for (int i = 0; i < NUM_SENSORS; ++i) {
            mValues.add(new ArrayList<Double>());
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    /**
     * Adds and stores the new data points.
     * @param values
     */
    public void addData(ArrayList<Double> values) {
        for (int i = 0; i < mValues.size(); ++i) {
            mValues.get(i).add(values.get(i));
        }
    }

}
