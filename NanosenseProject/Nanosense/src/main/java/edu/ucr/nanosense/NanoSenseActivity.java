package edu.ucr.nanosense;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.Reference;
import java.text.DecimalFormat;

import ioio.lib.api.AnalogInput;
import ioio.lib.api.DigitalInput;
import ioio.lib.api.DigitalOutput;
import ioio.lib.api.SpiMaster;
import ioio.lib.api.Uart;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;

/**
 * NanoSenseActivity holds the variables and handles the control of the device.
 */
public class NanoSenseActivity extends IOIOActivity {

    private static final String TAG = "NanoSense";

    private static final String FRAGMENT_TAG_GRAPH_VALUE = "GraphValueFragment";

    private static final int REQUEST_OPTIONS = 1;

    private int mPollingRate;
    private int mServerPort;
    private String mServerIp;

    private byte[] mInitialResistances = new byte[Constants.Device.NUM_PINS_NANOSENSOR];

    private long mPolledTime;

    private static ProgressDialog mProgressDialog;

    // TODO: These need to be saved in on instance state
    private boolean mStarted = false;
    private boolean mInitialized = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_graph_view);

        /** TODO: These need to be saved and only done if there isn't anything saved**/
        /** TODO: Polled time and initialized need to be saved as well **/
        mPollingRate = Constants.Options.DEFAULT_POLLING_RATE;
        mServerPort = Constants.Options.DEFAULT_SERVER_PORT;
        mServerIp = Constants.Options.DEFAULT_SERVER_IP;

        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setTitle("Initializing...");
        mProgressDialog.setMessage("Sensor 0");
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setCancelable(false);

        Fragment graphValueFragment = getFragmentManager().findFragmentByTag(FRAGMENT_TAG_GRAPH_VALUE);
        if (graphValueFragment == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new GraphValueFragment(), FRAGMENT_TAG_GRAPH_VALUE)
                    .commit();
        }
    }

    public void showProgressDialog() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mProgressDialog.show();
            }
        });
    }

    public void setProgressDialog(int sensorNum) {
        final int finalSensorNum = sensorNum;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mProgressDialog.setMessage("Sensor " + finalSensorNum);
                mProgressDialog.setProgress(finalSensorNum * (100 / Constants.Device.NUM_PINS_NANOSENSOR));
            }
        });
    }

    public void dismissProgressDialog() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mProgressDialog.dismiss();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.nano_sense, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem settingsItem = menu.findItem(R.id.action_settings);
        if (settingsItem != null) {
            settingsItem.setEnabled(!mStarted);
        }
        MenuItem startItem = menu.findItem(R.id.action_start);
        if (startItem != null) {
            startItem.setTitle(mStarted ? R.string.action_stop : R.string.action_start);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivityForResult(intent, REQUEST_OPTIONS);
        } else if (id == R.id.action_start) {
            mStarted = !mStarted;
            if (mStarted) {
                mPolledTime = 0;
            }
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
                        mPollingRate = bundle.getInt(SettingsActivity.EXTRA_POLLING_RATE);
                        mServerIp = bundle.getString(SettingsActivity.EXTRA_SERVER_IP);
                        mServerPort = bundle.getInt(SettingsActivity.EXTRA_SERVER_PORT);
                        Log.d(TAG, "Polling Rate: " + mPollingRate);
                        Log.d(TAG, "Server IP: " + mServerIp);
                        Log.d(TAG, "Server Port: " + mServerPort);
                        float x, y, z;

                        x = bundle.getFloat(SettingsActivity.EXTRA_ACCEL_X);
                        y = bundle.getFloat(SettingsActivity.EXTRA_ACCEL_Y);
                        z = bundle.getFloat(SettingsActivity.EXTRA_ACCEL_Z);
                        Log.d(TAG, "Neutral X, Y, Z: " + x + ", " + y + ", " + z);
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

    /**
     * Looper should be strictly for communication with the device since the connection
     * can be broken and the looper can be recreated at any time. All data values should
     * be managed by the Activity.
     */
    class Looper extends BaseIOIOLooper implements SensorEventListener {

        // TODO: Implement variable speed in options.
        private byte mSpeed = 0x20;

        /** LED on IOIO. Turns on when connected **/
        private DigitalOutput mLed;
        /** DigitalOutput pins for MUX select **/
        private DigitalOutput[] mMuxPins = new DigitalOutput[Constants.Device.NUM_PINS_MUX];
        /** SPI interface for communicating with the digital rheostat **/
        private AnalogInput[] mAnalogPins = new AnalogInput[Constants.Device.NUM_PINS_ANALOG];
        private SpiMaster mSpi;

        private Uart mRoverUart;
        private InputStream mRoverRx;
        private OutputStream mRoverTx;

        private byte rheostatVal = 0;

        /**
         * setup is called every time the device is connected or when the Looper is recreated.
         * It opens and initializes the proper digital pins, analog pins, and communication
         * protocols that the IOIO will use with the sensor device.
         *
         * @throws ConnectionLostException
         * @throws InterruptedException
         */
        @Override
        protected void setup() throws ConnectionLostException, InterruptedException {
            /** Turn on LED when connected **/
            mLed = ioio_.openDigitalOutput(Constants.Device.PIN_LED, true);
            mLed.write(false);
            /** Initialize input/output pins **/
            // TODO: For some reason SPI won't initialize properly until unplugging and replugging. It's not properly reading back.
            initializeSpi();
            initializeMux();
            initializeAnalog();
            initializeUart();
            initializeRheostat();
        }

        /**
         * The AD5271BRMZ-100-ND needs to be sent an initial command to allow writing to the RDAC
         * register and setting the wiper resistance.
         *
         * @throws ConnectionLostException
         * @throws InterruptedException
         */
        private void initializeRheostat() throws ConnectionLostException, InterruptedException {
            /** This needs to be run on the UI Thread otherwise the connection is lost. Usually
             * longer operations such as SPI or Uart transmissions have to be run on a separate
             * thread. **/
            // TODO: Try removing, or doing a separate thread from UI since that can potentially lock up.
             runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "Initializing Rheostat...");
                    byte[] bytesToSend = {0x1C, 0x02};
                    try {
                        mSpi.writeRead(bytesToSend, bytesToSend.length, bytesToSend.length, null, 0);
                        Log.d(TAG, "Rheostat Initialized");
                    } catch (ConnectionLostException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        /**
         * Initializes the MUX pins to be DigitalOutput for controlling the MUX and sets the MUX to
         * 0.
         *
         * @throws ConnectionLostException
         */
        private void initializeMux() throws ConnectionLostException {
            for (int i = Constants.Device.PIN_MUX_SEL0; i <= Constants.Device.PIN_MUX_SEL3; ++i) {
                mMuxPins[i - Constants.Device.PIN_MUX_SEL0] = ioio_.openDigitalOutput(i);
                mMuxPins[i - Constants.Device.PIN_MUX_SEL0].write(false);
            }
        }

        /**
         * Initializes the Analog input pins for reading the sensors.
         * @throws ConnectionLostException
         */
        private void initializeAnalog() throws ConnectionLostException {
            for (int i = Constants.Device.PIN_ADC0; i <= Constants.Device.PIN_ADC3; ++i) {
                mAnalogPins[i - Constants.Device.PIN_ADC0] = ioio_.openAnalogInput(i);
            }
        }

        /**
         * Initializes the SPI connection for communicating with the rheostat
         *
         * For the AD5271BRMZ-100-ND rheostat the SPI protocol is the following:
         *  CPOL = 0
         *  CPHA = 1 (Falling Edge Sync)
         *  MSB first
         *  Supports up to 50MHz rate.
         * @throws ConnectionLostException
         */
        private void initializeSpi() throws ConnectionLostException {
            // TODO: Try increasing the rate?
            mSpi = ioio_.openSpiMaster(
                    new DigitalInput.Spec(Constants.Device.PIN_SPI_MISO,
                            DigitalInput.Spec.Mode.PULL_UP),
                    new DigitalOutput.Spec(Constants.Device.PIN_SPI_MOSI),
                    new DigitalOutput.Spec(Constants.Device.PIN_SPI_CLK),
                    new DigitalOutput.Spec[] { new DigitalOutput.Spec(Constants.Device.PIN_SPI_SS)},
                    new SpiMaster.Config(SpiMaster.Rate.RATE_125K, false, true));
        }

        /**
         * Sets the MUX select pins depending on the desired output. Simple bit-wise AND with the
         * pin to know which digital pins to open.
         * @param pin Pin to select (0-15)
         * @throws ConnectionLostException
         */
        private void setMux(byte pin) throws ConnectionLostException {
            for (byte i = 0; i < Constants.Device.NUM_PINS_MUX; ++i) {
                byte bitMask = (byte) (1 << i);
                mMuxPins[i].write((pin & bitMask) > 0);
            }
        }

        /**
         * Sets the resistance of the rheostat to the specified tap/bit resistance and then reads
         * it back to make sure it was set properly.
         *
         * For the AD5271BRMZ-100-ND rheostat, the range is from 0-100k with 255 taps.
         * The command protocol for the D5271BRMZ-100-ND is as follows. See datasheet for more info.
         * D - data, x - don't care
         * Write resistance: xx00 01DD DDDD DDxx
         * Read resistance: xx00 10xx xxxx xxxx
         *
         * @param bitResistance
         */
        public void writeRheostat(byte bitResistance) throws ConnectionLostException,
                InterruptedException {
            // TODO: Move to Constants.Commands
            byte upper = (byte) (Constants.Commands.RHEOSTAT_WRITE | (bitResistance >> 6));
            byte lower = (byte) (bitResistance << 2);
            byte[] bytesToSend = {upper, lower};
            Log.d(TAG, "SPI Send: " + upper + ", " + lower);
            Log.d(TAG, "Writing rheostat: " + bitResistance);
            mSpi.writeRead(bytesToSend, bytesToSend.length, bytesToSend.length, null, 0);
        }

        /**
         * Reads the resistance set from the RDAC register of the divider. This is used to check
         * that the potentiometer is indeed set to the correct resistance before taking the ADC
         * reading.
         *
         * The RDAC register should be read before performing calculations to determine the sensor
         * resistance. This fixes the issue with the delay between sending the SPI command to write
         * and reading the ADC since the SPI read in {@link Looper#writeRheostat(byte)} is
         * asynchronous. This results in reading the ADC before the SPI is actually set using
         * different values for the calculation than what the bridge is actually set at.
         *
         * @return Returns the bit resistance of the rheostat (0-255) corresponding to 0-100kOhms
         */
        private int readRheostat() throws ConnectionLostException, InterruptedException {
            byte[] upperArray = {Constants.Commands.RHEOSTAT_READ_UPPER};
            byte[] lowerArray = {Constants.Commands.RHEOSTAT_READ_LOWER};
            byte[] bytesReceived = new byte[1];
            /** Read/write one byte at a time since we're getting back a 16-bit response **/
            mSpi.writeRead(upperArray, upperArray.length, upperArray.length, bytesReceived, 1);
            int upperInt = bytesReceived[0] & 0xFF;
            mSpi.writeRead(lowerArray, lowerArray.length, lowerArray.length, bytesReceived, 1);
            int lowerInt = bytesReceived[0] & 0xFF;
            /**
             * C = command bits; D = data bits
             * Response from rheostat is 2 bytes. 00[C3:C0][D9:D0].
             * Upper byte is 00[C3:C0] [D9:D6] so drop the command and shift [D9:D6] to the proper
             * position .
             * Lower byte is [D5:D0] but since it's an 8-bit Rheostat the lower 2 bits are garbage
             * and dropped.
             * See data sheet on AD5271 for more details.
             **/
            int readVal = ((upperInt & 0x03) << 6 | (lowerInt >> 2));
            return readVal;
        }

        /**
         * writeReadRheostat is a blocking function. It writes the value to the rheostat and then
         * reads it back to verify that it has been set before returning.
         */
        private void writeReadRheostat(byte bitResistance) throws ConnectionLostException,
                InterruptedException{
            writeRheostat(bitResistance);
            while(readRheostat() != bitResistance);
        }

        /**
         * matchResistance attempts to match the resistance of the rheostat to the nano sensor for
         * the specified pin. This is done by checking the output voltage and attempting to get it as
         * close as possible to 1/2 of the input voltage. Since the voltage value from analogRead gives
         * a value between 0 and 1, it multiplies by 1024 since it's a 10-bit ADC and tries to match it
         * to 512.
         *
         * The matching is done using a recursive binary search algorithm.
         *
         * For the AD5271BRMZ-100-ND rheostat, the range is from 0-100kOhms with 255 taps
         *
         * @param pin Pin to read voltage from/match.
         * @param low Low value of the resistance.
         * @param high High bit value of the resistance.
         *
         * @return byte The bit value of the divider
         */
        private byte matchResistance(int low, int high) throws ConnectionLostException,
                InterruptedException {
            int mid = (low + high) / 2;
            if (low > high) {
                return (byte) mid;
            }

            /** Write the new value **/
            writeReadRheostat((byte) mid);
            /** IMPORTANT: Read the bitVoltage rather than the actual voltage. This assumes that the
             * reference voltage is the same as the bridge input voltage. The bridge voltage should
             * be the same though since otherwise there is risk of frying the pin in the event that
             * the nano sensors connection is broken. **/
            int bitVoltage = (int) (mAnalogPins[Constants.Device.ADC_NANO_SENSOR].read() *
                    Constants.Device.MAX_BIT_VOLTAGE);
            if (bitVoltage > 512) {
                return matchResistance(low, mid - 1);
            } else if (bitVoltage > 512) {
                return matchResistance(mid + 1, high);
            } else {
                return (byte) mid;
            }
        }

        /**
         * Calculates the resistance values of the sensor, and attempts to match the resistance.
         * Resistances are then stored in {@link mInitialResistances}
         */
        private void matchResistances() throws ConnectionLostException, InterruptedException {
            // TODO: Add dialog showing that they are being initialized and which pin it's on.
            showProgressDialog();
            for (int i = 0; i < Constants.Device.NUM_PINS_NANOSENSOR; ++i) {
                setProgressDialog(i);
                setMux((byte) i);
                mInitialResistances[i] = matchResistance(0, Constants.Device.MAX_BIT_RESISTANCE);
            }
            dismissProgressDialog();
        }

        private void initializeUart() throws ConnectionLostException, InterruptedException {
            mRoverUart = ioio_.openUart(Constants.Device.PIN_ROVER_RX,
                    Constants.Device.PIN_ROVER_TX,
                    Constants.Device.UART_RATE,
                    Uart.Parity.NONE,
                    Uart.StopBits.ONE);
            mRoverRx = mRoverUart.getInputStream();
            mRoverTx = mRoverUart.getOutputStream();
        }

        public void stopMovement() {
            if(mRoverTx != null)
            {
                // TODO: Define as constant
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

        public void setWheels(double leftWheel, double rightWheel) {
            if(mRoverTx != null)
            {
                // TODO: Define as constants
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
            // TODO: Define as constants
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

        /**
         * Helper function for converting read voltage to sensor resistance. The voltage bridge has
         * the rheostat as R1 and the sensor as R2.
         *
         * @param readVoltage The read voltage in volts.
         * @param bitResistance The resistance of the nano sensor in kOhms.
         */
        private double voltageToResistance(double readVoltage, byte bitResistance) {
            /**
             * Divider resistance in kOhms.
             */
            double dividerResistance = (double) bitResistance / Constants.Device.MAX_BIT_RESISTANCE
                    * Constants.Device.MAX_RHEO_RESISTANCE;
            double sensorResistance = readVoltage * dividerResistance /
                    (Constants.Device.VOLTAGE_REFERENCE - readVoltage);
            Log.d(TAG, "Sensor Resistance: " + sensorResistance);
            return sensorResistance;
        }

        /**
         * Reads the nano sensors and returns their value in kOhms.
         *
         * @return double[] The nano sensor's resistance in kOhms.
         * @throws ConnectionLostException
         */
        private double[] readNanoSensors() throws ConnectionLostException, InterruptedException {
            double[] sensorResistances = new double[Constants.Device.NUM_PINS_NANOSENSOR];
            for (int i = 0; i < Constants.Device.NUM_PINS_NANOSENSOR; ++i) {
                /**
                 * Set the MUX, set the matching resistance, then read the voltage.
                 */
                setMux((byte) i);
                writeReadRheostat(mInitialResistances[i]);
                /**
                 * ADC0 is connected to the nano sensor and MUX.
                 */
                double readVoltage = (double) mAnalogPins[Constants.Device.ADC_NANO_SENSOR]
                        .getVoltage();
                double sensorResistance = voltageToResistance(readVoltage, mInitialResistances[i]);
                Log.d(TAG, "Sensor Resistance: " + sensorResistance);
                sensorResistances[i] = sensorResistance;
            }
            return sensorResistances;
        }

        /**
         * readTemperature reads the TMP36 temperature sensor's voltage and calculates the
         * corresponding temperature in Celcius.
         * @return The read temperature in Celcius.
         * @throws ConnectionLostException
         * @throws InterruptedException
         */
        private double readTemperature() throws ConnectionLostException, InterruptedException {
            double readVoltage = (double) mAnalogPins[Constants.Device.ADC_TEMPERATURE]
                    .getVoltage();
            double tempCelcius = (readVoltage + Constants.Temperature.VOLTAGE_OFFSET)
                    * Constants.Temperature.TEMPERATURE_SCALE +
                    Constants.Temperature.TEMPERATURE_OFFSET;
            Log.d(TAG, "Temperature (C): " + tempCelcius);
            return tempCelcius;
        }

        /**
         * Read humidity reads the HIH-4030 humidity sensor's voltage and calculates the
         * corresponding relative humidity based on the voltage and the temperature.
         * @param tempCelcius The temperature in Celcius.
         * @return double The relative humidity percentage.
         */

        private double readHumidity(double tempCelcius) throws ConnectionLostException,
                InterruptedException {
            double readVoltage = (double) mAnalogPins[Constants.Device.ADC_HUMIDITY].getVoltage();
            double humidityPercentage = (readVoltage / Constants.Device.VOLTAGE_REFERENCE -
                    Constants.Humidity.VOLTAGE_OFFSET) / Constants.Humidity.VOLTAGE_SCALE;
            double relativeHumidity = humidityPercentage / (Constants.Humidity.TEMPERATURE_OFFSET +
                    Constants.Humidity.TEMPERATURE_SCALE * tempCelcius);
            if (relativeHumidity > 100) {
                relativeHumidity = 100;
            } else if (relativeHumidity < 0) {
                relativeHumidity = 0;
            }
            return relativeHumidity;
        }

        private double readThermistor() throws ConnectionLostException, InterruptedException {
            double readVoltage = (double) mAnalogPins[Constants.Device.ADC_THERMISTOR].getVoltage();
            /** Calculate resistance of thermistor. Simple voltage bridge with R1 as thermistor and
             * R2 as 10kOhms
             */
            double thermistorResistance = Constants.Thermistor.DIVIDER_RESISTANCE *
                    Constants.Device.VOLTAGE_REFERENCE / readVoltage -
                    Constants.Thermistor.DIVIDER_RESISTANCE;
            double tempCelcius = Constants.Thermistor.TEMPERATURE_SCALE * thermistorResistance +
                    Constants.Thermistor.TEMPERATURE_OFFSET;
            return tempCelcius;
        }

        @Override
        public void loop() throws ConnectionLostException, InterruptedException {
            if (mStarted) {
                if (!mInitialized) {
                    matchResistances();
                    mInitialized = true;
                } else {
                    long elapsedTime = System.currentTimeMillis() - mPolledTime;
                    if (mInitialized && elapsedTime >= mPollingRate) {
                        double[] sensorResistances = readNanoSensors();
                        double tempCelcius = readTemperature();
                        double relativeHumidity = readHumidity(tempCelcius);
                        double thermistorCelcius = readThermistor();
                        StringBuilder sb = new StringBuilder();
                        DecimalFormat df = new DecimalFormat("#.##");
                        final String tempCelciusString = df.format(tempCelcius);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                GraphValueFragment graphValueFragment = (GraphValueFragment)
                                        getFragmentManager().findFragmentByTag(FRAGMENT_TAG_GRAPH_VALUE);
                                if (graphValueFragment != null) {
                                    graphValueFragment.setDataLabel("Temp C:" );
                                    graphValueFragment.setDataValue(tempCelciusString);
                                }
                            }
                        });
                        for (double sensorResistance : sensorResistances) {
                            sb.append(df.format(sensorResistance));
                            sb.append(",");
                        }
                        sb.append("\n\nTemp:");
                        sb.append(df.format(tempCelcius));
                        sb.append(",");
                        sb.append(df.format(thermistorCelcius));
                        sb.append("\n\nRH:");
                        sb.append(df.format(relativeHumidity));
                        final String dataString = sb.toString();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getApplicationContext(), dataString, Toast.LENGTH_LONG).show();
                            }
                        });
                        mPolledTime = System.currentTimeMillis();
                    }
                }
            }
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            // TODO: Init acceleromter control if rover is selected.
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

}

