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

    private int mViewMode = Constants.Graph.VIEW_NANOSENSOR;

    private long mDrawStart = System.nanoTime();


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
            NanoSenseActivity.mData.add(new ArrayList<Data>());
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
            NanoSenseActivity.mData.add(new ArrayList<Data>());
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
            NanoSenseActivity.mData.add(new ArrayList<Data>());
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
        mDrawStart = System.nanoTime();
        canvas.drawColor(Color.WHITE);
        drawAxis(canvas);
        drawDebug(canvas);
    }

    private void drawDebug(Canvas canvas) {
        int lastIndex = -1;
        Paint textPaint = new Paint(Color.BLACK);
        textPaint.setTextSize(32);
        for (int i = 0; i < NanoSenseActivity.mData.size(); ++i) {
            lastIndex = NanoSenseActivity.mData.get(i).size() - 1;
            if (lastIndex >= 0) {
                String debugString = "Sensor " + i + " - [";
                switch (i) {
                    case Constants.Thermistor.SENSOR_INDEX:
                        debugString = "Thermistor (C) - [";
                        break;
                    case Constants.Humidity.SENSOR_INDEX:
                        debugString = "RH (%) - [";
                        break;
                    case Constants.Temperature.SENSOR_INDEX:
                        debugString = "Temperature (C) - [";
                        break;
                    default:
                        break;
                }
                debugString += String.valueOf(lastIndex) + "]: ";
                debugString += NanoSenseActivity.mData.get(i).get(lastIndex).toString();
                canvas.drawText(debugString, 50, 50 * (i + 1), textPaint);
            }
        }
        int fps = (int) (1000000000 / (System.nanoTime() - mDrawStart));
        String debugString = "FPS: " + fps;
        canvas.drawText(debugString, 50, 1000, textPaint);
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

}
