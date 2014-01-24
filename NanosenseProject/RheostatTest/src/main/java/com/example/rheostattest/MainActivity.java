package com.example.rheostattest;

import ioio.lib.api.AnalogInput;
import ioio.lib.api.DigitalInput;
import ioio.lib.api.DigitalOutput;
import ioio.lib.api.SpiMaster;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.api.exception.IncompatibilityException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;

import java.util.ArrayList;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;

/**
 * This is the main activity of the HelloIOIO example application.
 * 
 * It displays a toggle button on the screen, which enables control of the on-board LED. This example shows a very simple usage of the IOIO, by using the {@link IOIOActivity} class. For a more
 * advanced use case, see the HelloIOIOPower example.
 */
public class MainActivity extends IOIOActivity implements View.OnClickListener
{
	private ToggleButton button_;

    private TextView mReadVal;
    private TextView mAdc0Val;
    private TextView mAdc1Val;
    private TextView mAdc2Val;

    private EditText mMuxVal;
    private EditText mRheostatVal;

    private Button mSetMux;
    private Button mSetRheostat;

    private int mMux;
    private int mRheostat;

	/**
	 * Called when the activity is first created. Here we normally initialize our GUI.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

        mReadVal = (TextView) findViewById(R.id.readResistance);
        mAdc0Val = (TextView) findViewById(R.id.readAdc0);
        mAdc1Val = (TextView) findViewById(R.id.readAdc1);
        mAdc2Val = (TextView) findViewById(R.id.readAdc2);

        mMuxVal = (EditText) findViewById(R.id.muxSelect);
        mRheostatVal = (EditText) findViewById(R.id.rheostatSelect);

        mSetMux = (Button) findViewById(R.id.setMuxButton);
        mSetRheostat = (Button) findViewById(R.id.setRheostatButton);

        mSetMux.setOnClickListener(this);
        mSetRheostat.setOnClickListener(this);
	}

    @Override
    public void onClick(View v) {
        if (v.getId() == mSetMux.getId()) {
            mMux = Integer.parseInt(mMuxVal.getText().toString());
            if (mMux > 15) {
                mMux = 15;
            }
        } else if (v.getId() == mSetRheostat.getId()) {
            mRheostat = Integer.parseInt(mRheostatVal.getText().toString());
            if (mRheostat > 255) {
                mRheostat = 255;
            }
        }

    }

    /**
	 * This is the thread on which all the IOIO activity happens. It will be run every time the application is resumed and aborted when it is paused. The method setup() will be called right after a
	 * connection with the IOIO has been established (which might happen several times!). Then, loop() will be called repetitively until the IOIO gets disconnected.
	 */
	class Looper extends BaseIOIOLooper
	{
		private DigitalOutput led_;
        private SpiMaster mSpiMaster;

        private DigitalOutput mMuxSelect0;
        private DigitalOutput mMuxSelect1;
        private DigitalOutput mMuxSelect2;
        private DigitalOutput mMuxSelect3;

        private AnalogInput mAdc0;
        private AnalogInput mAdc1;
        private AnalogInput mAdc2;

        private long mElapsedTime;

        private static final int PIN_SPI_MISO = 9;
        private static final int PIN_SPI_SS = 10;
        private static final int PIN_SPI_MOSI = 11;
        private static final int PIN_SPI_CLK = 13;

        private static final int PIN_MUX_S0 = 1;
        private static final int PIN_MUX_S1 = 2;
        private static final int PIN_MUX_S2 = 3;
        private static final int PIN_MUX_S3 = 4;

        private static final int PIN_ADC0 = 31;
        private static final int PIN_ADC1 = 32;
        private static final int PIN_ADC2 = 33;

		/**
		 * Called every time a connection with IOIO has been established. Typically used to open pins.
		 * 
		 * @throws ConnectionLostException
		 *             When IOIO connection is lost.
		 * 
		 * @see ioio.lib.util.AbstractIOIOActivity.IOIOThread#setup()
		 */
		@Override
		protected void setup() throws ConnectionLostException, InterruptedException
		{
			led_ = ioio_.openDigitalOutput(0, true);
            led_.write(false);

            mMuxSelect0 = ioio_.openDigitalOutput(PIN_MUX_S0, false);
            mMuxSelect1 = ioio_.openDigitalOutput(PIN_MUX_S1, false);
            mMuxSelect2 = ioio_.openDigitalOutput(PIN_MUX_S2, false);
            mMuxSelect3 = ioio_.openDigitalOutput(PIN_MUX_S3, false);

            mAdc0 = ioio_.openAnalogInput(PIN_ADC0);
            mAdc1 = ioio_.openAnalogInput(PIN_ADC1);
            mAdc2 = ioio_.openAnalogInput(PIN_ADC2);

            mSpiMaster = ioio_.openSpiMaster(
                    new DigitalInput.Spec(PIN_SPI_MISO, DigitalInput.Spec.Mode.PULL_UP),
                    new DigitalOutput.Spec(PIN_SPI_MOSI),
                    new DigitalOutput.Spec(PIN_SPI_CLK),
                    new DigitalOutput.Spec[] { new DigitalOutput.Spec(PIN_SPI_SS)},
                    new SpiMaster.Config(SpiMaster.Rate.RATE_125K, false, true));

            // Initialize Rheostat
            byte[] bytesToSend = {0x1C, 0x02};
            try {
                mSpiMaster.writeRead(bytesToSend, bytesToSend.length, bytesToSend.length, null, 0);
            } catch (ConnectionLostException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
		}

		/**
		 * Called repetitively while the IOIO is connected.
		 * 
		 * @throws ConnectionLostException
		 *             When IOIO connection is lost.
		 * 
		 * @see ioio.lib.util.AbstractIOIOActivity.IOIOThread#loop()
		 */
		@Override
		public void loop() throws ConnectionLostException, InterruptedException {
            // Write Rheostat
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (System.currentTimeMillis() - mElapsedTime > 1000) {
                        byte command = 0x01;
                        byte upper = (byte) ((command << 2) | (mRheostat >> 6));
                        byte lower = (byte) (mRheostat << 2);
                        byte[] bytesToSend = {upper, lower};
                        try {
                            mMuxSelect0.write((0x01 & mMux) > 0);
                            mMuxSelect1.write((0x02 & mMux) > 0);
                            mMuxSelect2.write((0x04 & mMux) > 0);
                            mMuxSelect3.write((0x08 & mMux) > 0);
                            mSpiMaster.writeRead(bytesToSend, bytesToSend.length, bytesToSend.length, null, 0);
                            byte[] upperArray = {(byte) 0x80};
                            byte[] lowerArray = {0x00};
                            byte[] bytesReceived = new byte[1];
                            mSpiMaster.writeRead(upperArray, upperArray.length, upperArray.length, bytesReceived, 1);
                            int upperInt = bytesReceived[0] & 0xFF;
                            mSpiMaster.writeRead(lowerArray, lowerArray.length, lowerArray.length, bytesReceived, 1);
                            int lowerInt = bytesReceived[0] & 0xFF;
                            int readVal = ((upperInt & 0x03) << 6 | (lowerInt >> 2));
                            mReadVal.setText("RDAC Resistance: " + readVal);
                            mAdc0Val.setText("ADC0: " + mAdc0.getVoltage());
                            mAdc1Val.setText("ADC1: " + mAdc1.getVoltage());
                            mAdc2Val.setText("ADC2: " + mAdc2.getVoltage());
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } catch (ConnectionLostException e) {
                            e.printStackTrace();
                        }
                        mElapsedTime = System.currentTimeMillis();
                    }
                }
            });
            /**
            bytesToSend[0] = (byte) 0x80;
            bytesToSend[1] = 0x00;
            byte[] bytesToRead = new byte[2];
            mSpiMaster.writeRead(bytesToSend, 0, bytesToSend.length, bytesToRead, 2);
             **/
		}
	}

	/**
	 * A method to create our IOIO thread.
	 * 
	 * @see ioio.lib.util.AbstractIOIOActivity#createIOIOThread()
	 */
	@Override
	protected IOIOLooper createIOIOLooper()
	{
		return new Looper();
	}
}
