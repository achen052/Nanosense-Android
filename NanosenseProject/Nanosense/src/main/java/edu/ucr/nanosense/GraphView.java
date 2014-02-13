package edu.ucr.nanosense;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;

/**
 * Written by Albert Chen
 *
 * GraphView uses {@link android.graphics.Canvas} and {@link android.view.SurfaceHolder} to plot
 * the graph data. It shows the nano sensor data, temperature, and humidity data vs time.
 * It also handles the touch events:
 * Vertical Pinch - Vertical Zoom
 * Horizontal Pinch - Horizontal Zoom
 * Swiping - Scrolling Horizontal/Vertically
 * Tapping - Cycles through nano sensor, temperature, and humidity data.
 * Double Tap - Zoom extent/auto-scale.
 *
 * Last updated 02/13/2014
 */
public class GraphView extends SurfaceView implements SurfaceHolder.Callback {

    /**
     * Used for storing the sensor reading values
     * Nano Sensor (0 - 15): kOhms
     * Humidity (16) : RH%
     * Temperature (17) : C
     */
    private static ArrayList<ArrayList<Double>> mValues;

    private int mViewMode = Constants.Graph.VIEW_NANOSENSOR;


    /** Current window maximum value to display in kOhms */
    private double mWindowMax = Constants.Device.MAX_RHEO_RESISTANCE;
    /** Current window minimum value to display in kOhms */
    private double mWindowMin = 0.0;

    private static final Paint AXIS_PAINT = new Paint(Color.BLACK);

    private GraphThread mThread;

    public GraphView(Context context) {
        super(context);
        SurfaceHolder holder = getHolder();
        holder.addCallback(this);
        mThread = new GraphThread(holder, context, new Handler());
        setFocusable(true);
        /** Initialize an array for each sensor **/
        for (int i = 0; i < Constants.Device.NUM_SENSORS; ++i) {
            mValues.add(new ArrayList<Double>());
        }
    }

    public GraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
        SurfaceHolder holder = getHolder();
        holder.addCallback(this);
        mThread = new GraphThread(holder, context, new Handler());
        setFocusable(true);
        /** Initialize an array for each sensor **/
        for (int i = 0; i < Constants.Device.NUM_SENSORS; ++i) {
            mValues.add(new ArrayList<Double>());
        }
    }

    public GraphView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        SurfaceHolder holder = getHolder();
        holder.addCallback(this);
        mThread = new GraphThread(holder, context, new Handler());
        setFocusable(true);
        /** Initialize an array for each sensor **/
        for (int i = 0; i < Constants.Device.NUM_SENSORS; ++i) {
            mValues.add(new ArrayList<Double>());
        }
    }

    /**
     * GraphThread is the Thread that makes the update/draw calls.
     */
    private class GraphThread extends Thread {
        private SurfaceHolder mSurfaceHolder;
        private boolean mRunning = false;

        public GraphThread(SurfaceHolder holder, Context context, Handler handler) {
            mSurfaceHolder = holder;
        }

        public void setRunning(boolean running) {
            mRunning = running;
        }

        public void run() {
            Canvas canvas;
            while (mRunning) {
                canvas = null;
                try {
                    canvas = mSurfaceHolder.lockCanvas(null);
                    if (canvas != null) {
                        synchronized (mSurfaceHolder) {
                            onDraw(canvas);
                        }
                    }
                } finally {
                    if (canvas != null) {
                        mSurfaceHolder.unlockCanvasAndPost(canvas);
                    }
                }
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(Color.WHITE);
        drawAxis(canvas);
    }

    private void drawAxis(Canvas canvas) {
        switch (mViewMode) {
            case Constants.Graph.VIEW_NANOSENSOR:
                canvas.drawLine(15, 5, 15, getHeight() - 5, AXIS_PAINT);
                canvas.drawLine(5, getHeight() - 15, getWidth() - 5, getHeight() - 15, AXIS_PAINT);
                break;
            case Constants.Graph.VIEW_TEMPERATURE:
                break;
            case Constants.Graph.VIEW_HUMIDITY:
                break;
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (mThread.getState() == Thread.State.TERMINATED) {
            mThread = new GraphThread(holder, null, null);
        }
        mThread.start();
        mThread.setRunning(true);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        boolean retry = true;
        mThread.setRunning(false);
        while (retry) {
            try {
                mThread.join();
                retry = false;
            } catch (InterruptedException interruptedException) {
                interruptedException.printStackTrace();
            }
        }
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
