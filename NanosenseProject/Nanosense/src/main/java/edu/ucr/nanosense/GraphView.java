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

import java.text.DecimalFormat;
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

    private static final int AXIS_PADDING_VERTICAL = 30;
    private static final int AXIS_PADDING_HORIZONTAL = 80;

    /** The current display mode of the graph */
    private int mViewMode = Constants.Graph.VIEW_NANOSENSOR;
    /** Time that the last draw was started. Used to calculate FPS */
    private long mDrawStart = System.nanoTime();

    /** Stores the max value for each sensor for auto-zooming */
    private double[] mWindowYMax = new double[Constants.Graph.NUM_VIEW_MODES];
    /** Stores the min value for each sensor for auto-zooming */
    private double[] mWindowYMin = new double[Constants.Graph.NUM_VIEW_MODES];

    /** Window limits for time. */
    private double mWindowXMin = 0;
    private double mWindowXMax = 0;

    private boolean mIsZoomExtent = true;

    private boolean[] mIsPinVisible = new boolean[Constants.Device.NUM_PINS_NANOSENSOR];

    private GestureDetector mGestureDetector;
    private ScaleGestureDetector mScaleGestureDetector;

/***************************************************************************************************
 *
 * OnViewModeChanged Callback Interface
 *
 **************************************************************************************************/

    public interface OnViewModeChangedListener {
        public void onViewModeChanged(int viewMode);
    }

    public void setOnViewModeChangedListener(OnViewModeChangedListener onViewModeChangedListener) {
        mOnViewModeChangedListener = onViewModeChangedListener;
    }

    private OnViewModeChangedListener mOnViewModeChangedListener;

/***************************************************************************************************
 *
 * GraphView Constructors
 *
 **************************************************************************************************/

    public GraphView(Context context) {
        super(context);
        SurfaceHolder holder = getHolder();
        holder.addCallback(this);
        for (int i = 0; i < mIsPinVisible.length; ++i) {
            mIsPinVisible[i] = true;
        }
        mGestureDetector = new GestureDetector(context, this);
        mGestureDetector.setOnDoubleTapListener(this);
        mScaleGestureDetector = new ScaleGestureDetector(context, this);
        mGraphThread = new GraphThread(holder, context, new Handler());
        setFocusable(true);
        setClickable(true);
    }

    public GraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
        SurfaceHolder holder = getHolder();
        holder.addCallback(this);
        for (int i = 0; i < mIsPinVisible.length; ++i) {
            mIsPinVisible[i] = true;
        }
        mGestureDetector = new GestureDetector(context, this);
        mGestureDetector.setOnDoubleTapListener(this);
        mScaleGestureDetector = new ScaleGestureDetector(context, this);
        mGraphThread = new GraphThread(holder, context, new Handler());
        setFocusable(true);
        setClickable(true);
    }

    public GraphView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        /** Initialize SurfaceView */
        SurfaceHolder holder = getHolder();
        holder.addCallback(this);
        /** Initialize Visible Pins */
        for (int i = 0; i < mIsPinVisible.length; ++i) {
            mIsPinVisible[i] = true;
        }
        mGestureDetector = new GestureDetector(context, this);
        mGestureDetector.setOnDoubleTapListener(this);
        mScaleGestureDetector = new ScaleGestureDetector(context, this);
        mGraphThread = new GraphThread(holder, context, new Handler());
        /** Set focusable so that clicks can be detected by the GraphView */
        setFocusable(true);
        setClickable(true);
    }

/***************************************************************************************************
 *
 * GraphView Methods
 *
 **************************************************************************************************/

    public int nextViewMode() {
        mViewMode = (mViewMode + 1) % Constants.Graph.NUM_VIEW_MODES;
        mOnViewModeChangedListener.onViewModeChanged(mViewMode);
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
        drawLabels(canvas);
        drawDebug(canvas);
    }

    /**
     * Draws the axis value labels.
     *
     * @param canvas The {@link android.graphics.Canvas} to draw on.
     */
    private void drawLabels(Canvas canvas) {
        Paint textPaint = new Paint(Color.BLACK);
        textPaint.setTextSize(32);
        double minY = 0;
        double maxY = 0;
        switch (mViewMode) {
            case Constants.Graph.VIEW_NANOSENSOR:
                for (int i = 0; i < mIsPinVisible.length; ++i) {
                    /** Get the min and max resistance for the visible pins. */
                    if (mIsPinVisible[i] && mIsZoomExtent) {
                        double channelMax = NanoSenseActivity.mMaxValues.get(i);
                        double channelMin = NanoSenseActivity.mMinValues.get(i);
                        if (channelMax > mWindowYMax[Constants.Graph.VIEW_NANOSENSOR]) {
                            mWindowYMax[Constants.Graph.VIEW_NANOSENSOR] = channelMax;
                        }
                        if (channelMin < mWindowYMax[Constants.Graph.VIEW_NANOSENSOR]) {
                            mWindowYMin[Constants.Graph.VIEW_NANOSENSOR] = channelMin;
                        }
                    }
                }
                maxY = mWindowYMax[Constants.Graph.VIEW_NANOSENSOR];
                minY = mWindowYMax[Constants.Graph.VIEW_NANOSENSOR];
                break;
            case Constants.Graph.VIEW_NANOSENSOR_DELTA:
                break;
            case Constants.Graph.VIEW_HUMIDITY:
                if (mIsZoomExtent) {
                    mWindowYMax[Constants.Graph.VIEW_HUMIDITY] =
                            NanoSenseActivity.mMaxValues.get(Constants.Humidity.SENSOR_INDEX);
                    mWindowYMin[Constants.Graph.VIEW_HUMIDITY] =
                            NanoSenseActivity.mMinValues.get(Constants.Humidity.SENSOR_INDEX);
                }
                maxY = mWindowYMax[Constants.Graph.VIEW_HUMIDITY];
                minY = mWindowYMin[Constants.Graph.VIEW_HUMIDITY];
                break;
            case Constants.Graph.VIEW_TEMPERATURE:
                if (mIsZoomExtent) {
                    mWindowYMax[Constants.Graph.VIEW_TEMPERATURE] =
                            NanoSenseActivity.mMaxValues.get(Constants.Temperature.SENSOR_INDEX);
                    mWindowYMin[Constants.Graph.VIEW_TEMPERATURE] =
                            NanoSenseActivity.mMinValues.get(Constants.Temperature.SENSOR_INDEX);
                }
                maxY = mWindowYMax[Constants.Graph.VIEW_TEMPERATURE];
                minY = mWindowYMin[Constants.Graph.VIEW_TEMPERATURE];
                break;
            default:
                break;
        }
        DecimalFormat decimalFormat = new DecimalFormat("0.00");
        /** Y-Label */
        canvas.drawText(decimalFormat.format(minY), 0, getHeight() - AXIS_PADDING_VERTICAL,
                textPaint);
        canvas.drawText(decimalFormat.format(maxY), 0, AXIS_PADDING_VERTICAL, textPaint);
        /** X-Label */
        canvas.drawText(decimalFormat.format(mWindowXMin), AXIS_PADDING_HORIZONTAL,
                getHeight(), textPaint);
        if (mIsZoomExtent) {
            int lastIndex = NanoSenseActivity.mData.get(0).size() - 1;
            if (lastIndex >= 0) {
                Data dataItem = NanoSenseActivity.mData.get(0).get(lastIndex);
                mWindowXMax = dataItem.mTime / 60000.0;
            }
        }
        canvas.drawText(decimalFormat.format(mWindowXMax),
                getWidth() - AXIS_PADDING_HORIZONTAL, getHeight(), textPaint);
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
                canvas.drawText(debugString, AXIS_PADDING_HORIZONTAL, 50 * (i + 1), textPaint);
            }
        }
        int fps = (int) (1000000000 / (System.nanoTime() - mDrawStart));
        String debugString = "FPS: " + fps;
        canvas.drawText(debugString, AXIS_PADDING_HORIZONTAL, 1000, textPaint);
    }

    private void drawAxis(Canvas canvas) {
        switch (mViewMode) {
            case Constants.Graph.VIEW_NANOSENSOR:
                canvas.drawLine(AXIS_PADDING_HORIZONTAL, AXIS_PADDING_VERTICAL,
                        AXIS_PADDING_HORIZONTAL, getHeight() - AXIS_PADDING_VERTICAL, AXIS_PAINT);
                canvas.drawLine(AXIS_PADDING_HORIZONTAL, getHeight() - AXIS_PADDING_VERTICAL,
                        getWidth() - AXIS_PADDING_HORIZONTAL, getHeight() - AXIS_PADDING_VERTICAL,
                        AXIS_PAINT);
                break;
            case Constants.Graph.VIEW_NANOSENSOR_DELTA:
                canvas.drawLine(AXIS_PADDING_HORIZONTAL, AXIS_PADDING_VERTICAL,
                        AXIS_PADDING_HORIZONTAL, getHeight() - AXIS_PADDING_VERTICAL, AXIS_PAINT);
                canvas.drawLine(AXIS_PADDING_HORIZONTAL, getHeight() / 2,
                        getWidth() - AXIS_PADDING_HORIZONTAL, getHeight() / 2, AXIS_PAINT);
                break;
            case Constants.Graph.VIEW_TEMPERATURE:
                canvas.drawLine(AXIS_PADDING_HORIZONTAL, AXIS_PADDING_VERTICAL,
                        AXIS_PADDING_HORIZONTAL, getHeight() - AXIS_PADDING_VERTICAL, AXIS_PAINT);
                canvas.drawLine(AXIS_PADDING_HORIZONTAL, getHeight() - AXIS_PADDING_VERTICAL,
                        getWidth() - AXIS_PADDING_HORIZONTAL, getHeight() - AXIS_PADDING_VERTICAL,
                        AXIS_PAINT);
                break;
            case Constants.Graph.VIEW_HUMIDITY:
                canvas.drawLine(AXIS_PADDING_HORIZONTAL, AXIS_PADDING_VERTICAL,
                        AXIS_PADDING_HORIZONTAL, getHeight() - AXIS_PADDING_VERTICAL, AXIS_PAINT);
                canvas.drawLine(AXIS_PADDING_HORIZONTAL, getHeight() - AXIS_PADDING_VERTICAL,
                        getWidth() - AXIS_PADDING_HORIZONTAL, getHeight() - AXIS_PADDING_VERTICAL,
                        AXIS_PAINT);
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

    /**
     * onTouchEvent should be overriden and the MotionEvent passed to the
     * {@link android.view.ScaleGestureDetector} and {@link android.view.GestureDetector}.
     *
     * @param event The {@link android.view.MotionEvent}.
     * @return True if the event was handled.
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        /** Check for scaling then scroll, touch, etc */
        mScaleGestureDetector.onTouchEvent(event);
        if(!mScaleGestureDetector.isInProgress()) {
            mGestureDetector.onTouchEvent(event);
        }
        return super.onTouchEvent(event);
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
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent event1, MotionEvent event2, float distanceX,
                            float distanceY) {
        // TODO: Implement scrolling
        Log.d(TAG, "in onScroll");
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {
        Log.d(TAG, "in onLongPress");
        /** Do nothing on long press. */
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        Log.d(TAG, "in onFling [velX, velY]: [" + velocityX + ", " + velocityY + "]");
        return false;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        Log.d(TAG, "in onSingleTapConfirmed");
        nextViewMode();
        return true;
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
        return detector.getCurrentSpan() > 0;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
        Log.d(TAG, "in onScaleEnd");
    }

}
