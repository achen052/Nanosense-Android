package edu.ucr.nanosense;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

/**
 * Written by Albert Chen
 * Last Updated 12/09/2013
 *
 * SettingsActivity contains a OptionsFragment which handles the UI elements. SettingsActivity is used
 * for handling the extras being passed between the main {@link NanoSenseActivity} and
 * SettingsActivity
 */
public class SettingsActivity extends ActionBarActivity {

    /** Extras for all devices. **/
    public static final String EXTRA_POLLING_RATE = "polling_rate";
    public static final String EXTRA_SERVER_IP = "server_ip";
    public static final String EXTRA_SERVER_PORT = "server_port";

    public static final String EXTRA_DEVICE_TYPE = "device_type";

    /** Extras for pump. **/
    public static final String EXTRA_PUMP_BASELINE = "pump_baseline";

    /** Extras for rover. **/
    public static final String EXTRA_ACCEL_X = "accel_x";
    public static final String EXTRA_ACCEL_Y = "accel_y";
    public static final String EXTRA_ACCEL_Z = "accel_z";

    /** Extras for rover. **/
    public static final String STATE_ACCEL_X = "accel_x";
    public static final String STATE_ACCEL_Y = "accel_y";
    public static final String STATE_ACCEL_Z = "accel_z";

    public static Intent createIntent(Context context,
                                      int pollingRate,
                                      String serverIp,
                                      int serverPort) {
        Intent intent = new Intent(context, SettingsActivity.class);
        intent.putExtra(EXTRA_POLLING_RATE, pollingRate);
        intent.putExtra(EXTRA_SERVER_IP, serverIp);
        intent.putExtra(EXTRA_SERVER_PORT, serverPort);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new OptionsFragment())
                    .commit();
        }
    }

    public void onBackPressed() {
        // TODO: AlertDialog to show whether or not to cancel
        setResult(RESULT_CANCELED);
        Toast.makeText(this, getResources().getString(R.string.options_cancelled),
                Toast.LENGTH_LONG).show();
        finish();
    }

    /**
     * OptionsFragment contains and handles the UI and parsing options selections.
     */
    public static class OptionsFragment extends Fragment implements
            RadioGroup.OnCheckedChangeListener, View.OnClickListener, SensorEventListener {

        private static final String serverIpRegex = "^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$";

        /** Constants for the type of device **/
        private static final int DEVICE_STANDARD = 0;
        private static final int DEVICE_PUMP = 1;
        private static final int DEVICE_ROVER = 2;

        private SensorManager mSensorManager;

        private float mXAccel = 0;
        private float mYAccel = 0;
        private float mZAccel = 9.8f;

        private float mNeutralX;
        private float mNeutralY;
        private float mNeutralZ;

        /** RadioButtons for selecting device type **/
        private RadioGroup mDeviceSelectionRadioGroup;
        private RadioButton mStandardRadioButton;
        private RadioButton mPumpRadioButton;
        private RadioButton mRoverRadioButton;

        /** EditTexts for user input for standard options **/
        private EditText mPollingRateEditText;
        private EditText mServerIpEditText;
        private EditText mServerPortEditText;

        /** EditText for pump baseline duration **/
        private EditText mBaselineDurationEditText;

        /** Buttons for rover options **/
        private Button mCalibrateAccelButton;

        /** Button for finishing options. **/
        private Button mDoneButton;

        /** LinearLayouts that contain the custom options for the pump and rover devices. **/
        private LinearLayout mPumpOptionsLayout;
        private LinearLayout mRoverOptionsLayout;
        private View mExtraListDivider;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_settings, container, false);

            /** Initialize Accelerometer **/
            mSensorManager = (SensorManager) getActivity().getSystemService(SENSOR_SERVICE);
            mSensorManager.registerListener(this,
                        mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                        SensorManager.SENSOR_DELAY_FASTEST);

            /** Initialize RadioButtons. **/
            mDeviceSelectionRadioGroup =
                    (RadioGroup) rootView.findViewById(R.id.device_selection_radio_group);
            mDeviceSelectionRadioGroup.setOnCheckedChangeListener(this);
            mStandardRadioButton = (RadioButton) rootView.findViewById(R.id.standard_radio_button);
            mPumpRadioButton = (RadioButton) rootView.findViewById(R.id.pump_radio_button);
            mRoverRadioButton = (RadioButton) rootView.findViewById(R.id.rover_radio_button);

            /** Initialize layouts for custom device options. **/
            mPumpOptionsLayout = (LinearLayout) rootView.findViewById(R.id.pump_selection_options);
            mRoverOptionsLayout = (LinearLayout) rootView.findViewById(R.id.rover_selection_options);
            mExtraListDivider = rootView.findViewById(R.id.extra_options_list_divider);

            /** Initialize standard EditTexts for getting user input **/
            mPollingRateEditText = (EditText) rootView.findViewById(R.id.edit_text_polling_rate);
            mServerIpEditText = (EditText) rootView.findViewById(R.id.edit_text_server_ip);
            mServerPortEditText = (EditText) rootView.findViewById(R.id.edit_text_server_port);

            /** Initialize EditText for pump baseline duration **/
            mBaselineDurationEditText =
                    (EditText) rootView.findViewById(R.id.edit_text_baseline_duration);

            /** Initialize buttons for rover accelerometer control options **/
            mCalibrateAccelButton =
                    (Button) rootView.findViewById(R.id.button_calibrate_accelerometer);
            mCalibrateAccelButton.setOnClickListener(this);

            /** Initialize done button **/
            mDoneButton = (Button) rootView.findViewById(R.id.button_done);
            mDoneButton.setOnClickListener(this);

            /** Restore zeroed accel if we have it **/
            if (savedInstanceState != null) {
                mNeutralX = savedInstanceState.getFloat(STATE_ACCEL_X);
                mNeutralY = savedInstanceState.getFloat(STATE_ACCEL_Y);
                mNeutralZ = savedInstanceState.getFloat(STATE_ACCEL_Z);
            }

            return rootView;
        }

        @Override
        public void onSaveInstanceState(Bundle savedInstanceState) {
            /**
             * Zeroed accel is saved when orientation changes. Current accel does not need to be
             * saved since it will be constantly updated anyways. Other options are saved in the
             * EditText and parsed when the done button is clicked so they also do not need to be
             * saved.
             **/
            savedInstanceState.putFloat(STATE_ACCEL_X, mNeutralX);
            savedInstanceState.putFloat(STATE_ACCEL_Y, mNeutralY);
            savedInstanceState.putFloat(STATE_ACCEL_Z, mNeutralZ);
        }

        /**
         * updateVisibleOptions sets the visibility of options for the rover based and pump based
         * devices and the extra list divider.
         *
         * @param selectedDevice Constants defined in OptionsFragment representing the selected
         *                       device type. Valid options are:
         *                       {@link OptionsFragment#DEVICE_STANDARD}
         *                       {@link OptionsFragment#DEVICE_PUMP}
         *                       {@link OptionsFragment#DEVICE_ROVER}
         *
         */
        private void updateVisibleOptions(int selectedDevice) {
            mPumpOptionsLayout.
                    setVisibility(selectedDevice == DEVICE_PUMP ? View.VISIBLE : View.GONE);
            mRoverOptionsLayout.
                    setVisibility(selectedDevice == DEVICE_ROVER ? View.VISIBLE : View.GONE);
            mExtraListDivider.
                    setVisibility(selectedDevice != DEVICE_STANDARD ? View.VISIBLE : View.GONE);
        }

        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            /** Check which RadioButton is selected and hide/show the corresponding options. **/
            if (checkedId == mStandardRadioButton.getId()) {
                updateVisibleOptions(DEVICE_STANDARD);
            } else if (checkedId == mPumpRadioButton.getId()) {
                updateVisibleOptions(DEVICE_PUMP);
            } else if (checkedId == mRoverRadioButton.getId()) {
                updateVisibleOptions(DEVICE_ROVER);
            }
        }

        @Override
        public void onClick(View view) {
            if (view.getId() == mDoneButton.getId()) {
                Activity pActivity = getActivity();
                Intent intent = pActivity.getIntent();
                Resources res = getResources();

                int pollingRate = Constants.Options.DEFAULT_POLLING_RATE;
                String serverIp = Constants.Options.DEFAULT_SERVER_IP;
                int serverPort = Constants.Options.DEFAULT_SERVER_PORT;
                int baselineDuration = Constants.Options.DEFAULT_BASELINE_DURATION;

                String statusString = "";

                /** Parse polling rate. **/
                Editable pollingRateText = mPollingRateEditText.getText();
                try {
                    pollingRate = Integer.parseInt(pollingRateText.toString());
                    /** Make sure polling rate is greater than minimum **/
                    if (pollingRate < Constants.Options.DEFAULT_POLLING_RATE) {
                        pollingRate = Constants.Options.DEFAULT_POLLING_RATE;
                    }
                    statusString += "\n" + res.getString(R.string.polling_rate_label) + pollingRate
                            + "\n";
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    statusString += "\n" + res.getString(R.string.error_polling_rate) + pollingRate
                            + "\n";
                }

                /** Parse server IP. **/
                Editable serverIpText = mServerIpEditText.getText();
                if (serverIpText.toString().matches(serverIpRegex)) {
                    serverIp = serverIpText.toString();
                    statusString += "\n" + res.getString(R.string.server_ip_label) + serverIp +
                            "\n";
                } else {
                    statusString += "\n" + res.getString(R.string.error_server_ip) + serverIp +
                            "\n";
                }

                /** Parse server port. **/
                Editable serverPortText = mServerPortEditText.getText();
                try {
                    serverPort = Integer.parseInt(serverPortText.toString());
                    statusString += "\n" + res.getString(R.string.server_port_label) + serverPort +
                            "\n";
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    statusString += "\n" + res.getString(R.string.error_server_port) + serverPort +
                            "\n";
                }

                /** Store values into return intent **/
                intent.putExtra(EXTRA_POLLING_RATE, pollingRate);
                intent.putExtra(EXTRA_SERVER_IP, serverIp);
                intent.putExtra(EXTRA_SERVER_PORT, serverPort);
                intent.putExtra(EXTRA_ACCEL_X, mNeutralX);
                intent.putExtra(EXTRA_ACCEL_Y, mNeutralY);
                intent.putExtra(EXTRA_ACCEL_Z, mNeutralZ);
                if (mStandardRadioButton.isChecked()) {
                    intent.putExtra(EXTRA_DEVICE_TYPE, DEVICE_STANDARD);
                } else if (mPumpRadioButton.isChecked()) {
                    intent.putExtra(EXTRA_DEVICE_TYPE, DEVICE_PUMP);
                    /** Parse pump baseline duration and store value if device selected is pump. **/
                    Editable baselineDurationText = mBaselineDurationEditText.getText();
                    try {
                        baselineDuration = Integer.parseInt(baselineDurationText.toString());
                        intent.putExtra(EXTRA_PUMP_BASELINE, baselineDuration);
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                        statusString += "\n" + res.getString(R.string.error_baseline_duration) +
                                baselineDuration + "\n";
                    }
                } else if (mRoverRadioButton.isChecked()) {
                    /** Pass back the "zeroed" location of the accelerometer and device type **/
                    intent.putExtra(EXTRA_DEVICE_TYPE, DEVICE_ROVER);
                    /** If the user didn't manually zero take the zero when they hit done **/
                    if (mNeutralX == 0 && mNeutralY == 0 && mNeutralZ == 0) {
                        intent.putExtra(EXTRA_ACCEL_X, mXAccel);
                        intent.putExtra(EXTRA_ACCEL_Y, mYAccel);
                        intent.putExtra(EXTRA_ACCEL_Z, mZAccel);
                    } else {
                        intent.putExtra(EXTRA_ACCEL_X, mNeutralX);
                        intent.putExtra(EXTRA_ACCEL_Y, mNeutralY);
                        intent.putExtra(EXTRA_ACCEL_Z, mNeutralZ);
                    }
                }

                /** Show selected values/options **/
                Toast.makeText(pActivity, statusString, Toast.LENGTH_LONG).show();
                mSensorManager.unregisterListener(this);
                getActivity().setResult(RESULT_OK, intent);
                getActivity().finish();
            } else if (view.getId() == R.id.button_calibrate_accelerometer) {
                mNeutralX = mXAccel;
                mNeutralY = mYAccel;
                mNeutralZ = mZAccel;
                Resources res = getResources();
                Toast.makeText(getActivity(),
                        res.getString(R.string.confirm_calibrate_accel),
                        Toast.LENGTH_LONG).show();
            }
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            /** Save the current accelerometer values. If calibrate is selected it uses the
             *  last stored value as the zero position. **/
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                mXAccel = event.values[0];
                mYAccel = event.values[1];
                mZAccel = event.values[2];
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            /** Do nothing **/
        }
    }
}
