package edu.ucr.nanosense;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
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
public class GraphView extends SurfaceView implements SurfaceHolder.Callback,
        GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener,
        ScaleGestureDetector.OnScaleGestureListener {

    private static final String TAG = "GraphView";

    /** Constant {@link android.graphics.Paint} for the Axis */
    private static final Paint AXIS_PAINT = new Paint(Color.BLACK);

    /** The current display mode of the graph */
    private int mViewMode = Constants.Graph.VIEW_NANOSENSOR;
    /** Time that the last draw was started. Used to calculate FPS */
    private long mDrawStart = System.nanoTime();

    /** Current window maximum value to display in kOhms */
    private double mWindowMax = Constants.Device.RHEOSTAT_RESISTANCE_MAX;
    /** Current window minimum value to display in kOhms */
    private double mWindowMin = 0.0;

    private GestureDetector mGestureDetector;

/***************************************************************************************************
 *
 * GraphView Constructors
 *
 **************************************************************************************************/

    public GraphView(Context context) {
        super(context);
        SurfaceHolder holder = getHolder();
        holder.addCallback(this);
        mGestureDetector = new GestureDetector(getContext(), this);
        mGraphThread = new GraphThread(holder, context, new Handler());
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
        mGestureDetector = new GestureDetector(getContext(), this);
        mGraphThread = new GraphThread(holder, context, new Handler());
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
        mGestureDetector = new GestureDetector(getContext(), this);
        mGraphThread = new GraphThread(holder, context, new Handler());
        setFocusable(true);
        /** Initialize an array for each sensor **/
        for (int i = 0; i < Constants.Device.NUM_SENSORS; ++i) {
            NanoSenseActivity.mData.add(new ArrayList<Data>());
        }
    }

/***************************************************************************************************
 *
 * GraphView Methods
 *
 **************************************************************************************************/

    public int nextViewMode() {
        mViewMode = (mViewMode + 1) % Constants.Graph.NUM_VIEW_MODES;
        return mViewMode;
    }

/***************************************************************************************************
 *
 * GraphThread Code (Updates what's drawn)
 *
 **************************************************************************************************/

    private GraphThread mGraphThread;

    /**
     * GraphThread is the Thread that makes the update/draw calls for the
     * {@link android.view.SurfaceHolder} and {@link android.view.SurfaceView}.
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

    /**
     * onDraw handles the drawing of items to the graph. It uses {@link android.graphics.Canvas}
     * @param canvas The {@link android.graphics.Canvas} to draw to.
     */
    @Override
    protected void onDraw(Canvas canvas) {
        mDrawStart = System.nanoTime();
        canvas.drawColor(Color.WHITE);
        drawAxis(canvas);
        drawDebug(canvas);
    }

    /**
     * Draws the last read value of each channel and the time as well as the FPS.
     * @param canvas The {@link android.graphics.Canvas} to draw to.
     */
    private void drawDebug(Canvas canvas) {
        int lastIndex = -1;
        Paint textPaint = new Paint(Color.BLACK);
        textPaint.setTextSize(32);
        for (int i = 0; i < NanoSenseActivity.mData.size(); ++i) {
            lastIndex = NanoSenseActivity.mData.get(i).size() - 1;
            if (lastIndex >= 0) {
                String debugString = "Sensor " + i + " (kOhms) - [";
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
            case Constants.Graph.VIEW_NANOSENSOR_DELTA:
                break;
            case Constants.Graph.VIEW_TEMPERATURE:
                canvas.drawLine(15, 5, 15, getHeight() - 5, AXIS_PAINT);
                canvas.drawLine(5, getHeight() - 15, getWidth() - 5, getHeight() - 15, AXIS_PAINT);
                break;
            case Constants.Graph.VIEW_HUMIDITY:
                canvas.drawLine(15, 5, 15, getHeight() - 5, AXIS_PAINT);
                canvas.drawLine(5, getHeight() - 15, getWidth() - 5, getHeight() - 15, AXIS_PAINT);
                break;
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (mGraphThread.getState() == Thread.State.TERMINATED) {
            mGraphThread = new GraphThread(holder, null, null);
        }
        mGraphThread.start();
        mGraphThread.setRunning(true);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        boolean retry = true;
        mGraphThread.setRunning(false);
        while (retry) {
            try {
                mGraphThread.join();
                retry = false;
            } catch (InterruptedException interruptedException) {
                interruptedException.printStackTrace();
            }
        }
    }

/***************************************************************************************************
 *
 * GestureListener Callbacks
 *
 **************************************************************************************************/

    @Override
    public boolean onDown(MotionEvent event) {
        Log.d(TAG, "in onDown");
        return false;
    }

    @Override
    public void onShowPress(MotionEvent event) {
        Log.d(TAG, "in onShowPress");
    }

    @Override
    public boolean onSingleTapUp(MotionEvent event) {
        Log.d(TAG, "in onSingleTapUp");
        /** Switch views */
        mViewMode = (mViewMode + 1) % Constants.Graph.NUM_VIEW_MODES;
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent event1, MotionEvent event2, float distanceX,
                            float distanceY) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {
        Log.d(TAG, "in onLongPress");
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        Log.d(TAG, "in onFling");
        Log.d(TAG, "Flinged [velX, velY]: [" + velocityX + ", " + velocityY + "]");
        return false;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        Log.d(TAG, "in onSingleTapConfirmed");
        return false;
    }

/***************************************************************************************************
 *
 * DoubleTapListener Callbacks
 *
 **************************************************************************************************/

    @Override
    public boolean onDoubleTap(MotionEvent e) {
        Log.d(TAG, "in onDoubleTap");
        return false;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent e) {
        Log.d(TAG, "in onDoubleTapEvent");
        return false;
    }

/***************************************************************************************************
 *
 * ScaleGestureListener Callbacks
 *
 **************************************************************************************************/

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        Log.d(TAG, "in onScale");
        return false;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        Log.d(TAG, "in onScaleBegin");
        return false;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
        Log.d(TAG, "in onScaleEnd");

    }

}
