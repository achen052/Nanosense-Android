package edu.ucr.nanosense;

import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import java.io.InputStream;
import java.io.OutputStream;

import ioio.lib.api.AnalogInput;
import ioio.lib.api.DigitalInput;
import ioio.lib.api.DigitalOutput;
import ioio.lib.api.SpiMaster;
import ioio.lib.api.Uart;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;

public class NanoSenseActivity extends IOIOActivity {

    private static final String TAG = "NanoSense";

    private static final int REQUEST_OPTIONS = 1;

    private int mPollingRate;
    private int mServerPort;
    private String mServerIp;

    private boolean mStarted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_graph_view);

        mPollingRate = Constants.Options.DEFAULT_POLLING_RATE;
        mServerPort = Constants.Options.DEFAULT_SERVER_PORT;
        mServerIp = Constants.Options.DEFAULT_SERVER_IP;

        /**
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new GraphViewFragment())
                    .commit();
        }
         **/
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.nano_sense, menu);
        // TODO: Add start/stop button
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, OptionsActivity.class);
            startActivityForResult(intent, REQUEST_OPTIONS);
            return true;
        } else if (id == R.id.action_start) {
            mStarted = !mStarted;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultIntent) {
        if (requestCode == REQUEST_OPTIONS) {
            if (resultCode == RESULT_OK) {
                if (resultIntent != null) {
                    Bundle bundle = resultIntent.getExtras();
                    if (bundle != null) {
                        mPollingRate = bundle.getInt(OptionsActivity.EXTRA_POLLING_RATE);
                        mServerIp = bundle.getString(OptionsActivity.EXTRA_SERVER_IP);
                        mServerPort = bundle.getInt(OptionsActivity.EXTRA_SERVER_PORT);
                        Log.d(TAG, "Polling Rate: " + mPollingRate);
                        Log.d(TAG, "Server IP: " + mServerIp);
                        Log.d(TAG, "Server Port: " + mServerPort);
                    }
                }
            } else if (resultCode == RESULT_CANCELED) {
                /** Cancelled, discard new settings **/
            }
        }
        super.onActivityResult(requestCode, resultCode, resultIntent);
    }

    @Override
    protected IOIOLooper createIOIOLooper() {
        return new Looper();
    }

    class Looper extends BaseIOIOLooper implements SensorEventListener {

        private static final int PIN_LED = 0;

        /** MUX pin numbers **/
        private static final int PIN_MUX_SEL0 = 1;
        private static final int PIN_MUX_SEL1 = 2;
        private static final int PIN_MUX_SEL2 = 3;
        private static final int PIN_MUX_SEL3 = 4;
        /** Rover TX/RX pins **/
        private static final int PIN_ROVER_RX = 5;
        private static final int PIN_ROVER_TX = 6;
        /** SPI pin numbers **/
        private static final int PIN_SPI_MISO = 9;
        private static final int PIN_SPI_SS = 10;
        private static final int PIN_SPI_MOSI = 11;
        private static final int PIN_SPI_CLK = 13;
        /** AnalogInput pin numbers for nano sensor, humidity, temperature respectively **/
        private static final int PIN_ADC0 = 31;
        private static final int PIN_ADC1 = 32;
        private static final int PIN_ADC2 = 33;
        /** Thermistor on sensor chip (wired to a 10kOhm resistor **/
        private static final int PIN_ADC3 = 34;

        /** Number of pins for each to initialize **/
        private static final int NUM_PINS_MUX = 4;
        private static final int NUM_PINS_ANALOG = 3;
        private static final int NUM_PINS_NANOSENSOR = 16;

        private static final int MAX_BIT_VOLTAGE = 1024;
        private static final int MAX_BIT_RESISTANCE = 255;

        /** Reference voltage used for the analog read **/
        private static final double VOLTAGE_REFERENCE = 3.3;

        /** Max potentiometer resistance in kOhms **/
        private static final double MAX_POT_RESISTANCE = 100.0;

        private boolean mRunning = false;

        private byte mSpeed = 0x20;

        /** LED on IOIO. Turns on when connected **/
        private DigitalOutput mLed;
        /** DigitalOutput pins for MUX select **/
        private DigitalOutput[] mMuxPins = new DigitalOutput[NUM_PINS_MUX];
        /** SPI interface for communicating with the digital potentiometer **/
        private AnalogInput[] mAnalogPins = new AnalogInput[NUM_PINS_ANALOG];
        private SpiMaster mSpi;

        private Uart mRoverUart;
        private InputStream mRoverRx;
        private OutputStream mRoverTx;

        private double[] mInitialResistances = new double[NUM_PINS_NANOSENSOR];

        @Override
        protected void setup() throws ConnectionLostException, InterruptedException {
            /** Turn on LED when connected **/
            mLed = ioio_.openDigitalOutput(PIN_LED, true);
            mLed.write(false);
            /** Initialize input/output pins **/
            initializeMux();
            initializeAnalog();
            initializeSpi();
            initializeUart();
        }

        /**
         * Initializes the MUX pins to be DigitalOutput for controlling the MUX.
         * @throws ConnectionLostException
         */
        private void initializeMux() throws ConnectionLostException {
            for (int i = PIN_MUX_SEL0; i <= PIN_MUX_SEL3; ++i) {
                mMuxPins[i - PIN_MUX_SEL0] = ioio_.openDigitalOutput(i);
                mMuxPins[i - PIN_MUX_SEL0].write(false);
            }
        }

        /**
         * Initializes the Analog input pins for reading the sensors.
         * @throws ConnectionLostException
         */
        private void initializeAnalog() throws ConnectionLostException{
            for (int i = PIN_ADC0; i <= PIN_ADC2; ++i) {
                mAnalogPins[i - PIN_ADC0] = ioio_.openAnalogInput(i);
            }
        }

        /**
         * Initializes the SPI connection for communicating with the potentiometer
         *
         * For the AD5271BRMZ-100-ND potentiometer the SPI protocol is the following:
         *  CPOL = 0
         *  CPHA = 1 (Falling Edge Sync)
         *  MSB first
         *  Supports up to 50MHz rate.
         * @throws ConnectionLostException
         */
        private void initializeSpi() throws ConnectionLostException {
            mSpi = ioio_.openSpiMaster(
                    new DigitalInput.Spec(PIN_SPI_MISO, DigitalInput.Spec.Mode.PULL_UP),
                    new DigitalOutput.Spec(PIN_SPI_MOSI),
                    new DigitalOutput.Spec(PIN_SPI_CLK),
                    new DigitalOutput.Spec[] { new DigitalOutput.Spec(PIN_SPI_SS)},
                    new SpiMaster.Config(SpiMaster.Rate.RATE_125K, false, true));
            mSpi = ioio_.openSpiMaster(PIN_SPI_MISO,
                    PIN_SPI_MOSI,
                    PIN_SPI_CLK,
                    PIN_SPI_SS,
                    SpiMaster.Rate.RATE_125K);
        }

        // TODO: Might have to make everything runonuithread and final if getting disconnect issues...

        /**
         * Sets the MUX select pins depending on the desired output.
         * @param pin Pin to select (0-15)
         * @throws ConnectionLostException
         */
        private void setMux(byte pin) throws ConnectionLostException {
            for (byte i = 0; i < NUM_PINS_MUX; ++i) {
                byte bitMask = (byte) (1 << i);
                mMuxPins[i].write((pin & bitMask) > 0);
            }
        }

        /**
         * Sets the resistance of the potentiometer to the specified tap/bit resistance and then reads
         * it back to make sure it was set properly.
         *
         * For the D5271BRMZ-100-ND potentiometer, the range is from 0-100k with 255 taps.
         * The command protocol for the D5271BRMZ-100-ND is as follows. See datasheet for more info.
         * D - data, x - don't care
         * Write resistance: xx00 01DD DDDD DDxx
         * Read resistance: xx00 10xx xxxx xxxx
         *
         * @param bitResistance
         */
        private void digitalPotWrite(byte bitResistance) throws ConnectionLostException,
                InterruptedException {
            /** Write resistance to potentiometer over SPI. **/
            byte writeCommand = (byte) (0x04 | (bitResistance >> 6));
            byte writeData = (byte) (bitResistance << 2);
            byte[] spiSend = new byte[]{writeCommand, writeData};
            Log.d(TAG, "SPI Send: " + bitResistance + ", " + writeCommand + ", " + writeData);
            mSpi.writeRead(spiSend, spiSend.length, spiSend.length, null, 0);
            /** Read back potentiometer resistance over SPI. **/
            byte readCommand = 0x08;
            byte readData = 0x00;
            spiSend = new byte[]{readCommand, readData};
            byte[] spiReceive = new byte[1];
            mSpi.writeRead(spiSend, spiSend.length, spiSend.length + 1, spiReceive, 1);
            Log.d(TAG, "SPI Received: " + spiReceive[0]);
        }

        /**
         * matchResistance attempts to match the resistance of the potentiometer to the nano sensor for
         * the specified pin. This is done by checking the output voltage and attempting to get it as
         * close as possible to 1/2 of the input voltage. Since the voltage value from analogRead gives
         * a value between 0 and 1, it multiplies by 1024 since it's a 10-bit ADC and tries to match it
         * to 512.
         *
         * For the D5271BRMZ-100-ND potentiometer, the range is from 0-100kOhms with 255 taps
         *
         * @param pin Pin to read voltage from/match.
         * @param low Low value of the resistance.
         * @param high High bit value of the resistance.
         */
        private double matchResistance(int pin, int low, int high) throws ConnectionLostException,
                InterruptedException {
            setMux((byte) pin);
            int mid = (low + high) / 2;
            if (low > high) {
                return (double) mid / MAX_BIT_RESISTANCE * MAX_POT_RESISTANCE;
            }

            // TODO: Implement digitalPotWrite
            digitalPotWrite((byte) mid);
            int bitVoltage = (int) (mAnalogPins[PIN_ADC0].read() * MAX_BIT_VOLTAGE);
            if (bitVoltage > 512) {
                return matchResistance(pin, low, mid - 1);
            } else if (bitVoltage > 512) {
                return matchResistance(pin, mid + 1, high);
            } else {
                return (double) mid / MAX_BIT_RESISTANCE * MAX_POT_RESISTANCE;
            }
        }

        /**
         * Calculates the resistance values of the sensor, and attempts to match the resistance.
         * Resistances are then stored in {@link mInitialResistances}
         */
        private void matchResistances() throws ConnectionLostException, InterruptedException {
            for (int i = 0; i < NUM_PINS_NANOSENSOR; ++i) {
                setMux((byte) i);
                mInitialResistances[i] = matchResistance(i, 0, MAX_BIT_RESISTANCE);
            }
        }

        private void initializeUart() throws ConnectionLostException, InterruptedException {
            mRoverUart = ioio_.openUart(PIN_ROVER_RX, PIN_ROVER_TX, 115200, Uart.Parity.NONE, Uart.StopBits.ONE);
            mRoverRx = mRoverUart.getInputStream();
            mRoverTx = mRoverUart.getOutputStream();
        }

        public void stopMovement()
        {
            if(mRoverTx != null)
            {
                byte[] stopBuffer = {(byte) 0xC1, (byte) 0x00, (byte) 0xC5, (byte) 0x00};
                try
                {
                    mRoverTx.write(stopBuffer);
                }
                catch(Exception e)
                {
                    e.printStackTrace();
                }
            }
        }

        public void setWheels(double leftWheel, double rightWheel)
        {
            if(mRoverTx != null)
            {
                byte leftCommand = (byte) 0xC1;
                byte rightCommand = (byte) 0xC5;

                if(leftWheel < 0)
                    leftCommand = (byte) 0xC2;
                if(rightWheel < 0)
                    rightCommand = (byte) 0xC6;

                byte[] moveBuffer = {(byte) leftCommand, (byte) Math.abs(leftWheel), (byte) rightCommand, (byte) Math.abs(rightWheel)};
                try
                {
                    mRoverTx.write(moveBuffer);
                }
                catch(Exception e)
                {
                    e.printStackTrace();
                }
            }
        }

        private void moveRelative(double deltaX, double deltaY)
        {
            double turnRatio = deltaY / 9.8 / 1.25;
            if(turnRatio > 1)
                turnRatio = 1;
            else if(turnRatio < -1)
                turnRatio = -1;

            /** Split into sub if condition */
            if(Math.abs(deltaX) > 3)
            {
                double leftWheel = mSpeed;
                double rightWheel = mSpeed;
                if(Math.abs(deltaY) > 1)
                {
                    if(turnRatio < 0)
                    {
                        leftWheel = mSpeed * (2 * turnRatio + 1);
                    }
                    else if(turnRatio > 0)
                    {
                        rightWheel = mSpeed * (-2 * turnRatio + 1);
                    }
                }
                if(deltaX > 0)
                {
                    leftWheel = -leftWheel;
                    rightWheel = -rightWheel;
                }
                setWheels(leftWheel, rightWheel);
            }
            else
            {
                stopMovement();
            }
        }

        @Override
        public void loop() throws ConnectionLostException, InterruptedException {
            if (mStarted) {
                for (int i = 0; i < NUM_PINS_NANOSENSOR; ++i) {
                    setMux((byte) i);
                    double voltage = mAnalogPins[0].read() * VOLTAGE_REFERENCE;
                    double resistance = voltage * 50 / (VOLTAGE_REFERENCE - voltage);
                }
                double temperature = (mAnalogPins[2].read() * VOLTAGE_REFERENCE - 0.75) * 100 + 25;
                double humidity = (mAnalogPins[1].read() * VOLTAGE_REFERENCE - 0.16) / 0.0062;
                double relativeHumidity = humidity / (1.0546 - 0.00216 * temperature);
            }
            wait(mPollingRate);
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            /**
            if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER && mIsPolling)
            {
                Log.v("Accel", event.values[0] + "," + event.values[1] + "," + event.values[2]);
                for(int i = 0; i < 3; ++i)
                {
                    mLastAccel[i] = event.values[i];
                    mDeltaAccel[i] = mLastAccel[i] - mNeutralAccel[i];
                }
                if(mAccelControl)
                {
                    moveRelative(mDeltaAccel[0], mDeltaAccel[1]);
                }
            }
             **/

        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    }




        /**
         * A placeholder fragment containing a simple view.
         */
    /**
    public static class GraphViewFragment extends Fragment {

        private TextView mPrintout;

        public GraphViewFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_graph_view, container, false);

            if (rootView != null) {
                mPrintout = (TextView) rootView.findViewById(R.id.readings_output);
            }
            return rootView;
        }
    }
        **/
}

