package edu.ucr.nanosense;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Written by Albert Chen
 * Last edited 12/04/2013
 *
 * This fragment is to display the numerical value/temp values. This is separated
 * from the main GraphViewFragment so that if the orientation changes or if it is on a
 * tablet it can be rearranged.
 */
public class GraphValueFragment extends Fragment {
    // TODO: Implement with add function and GraphValueView

    private TextView mDataLabel;
    private TextView mDataValue;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle saveInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_graph_data, container, false);
        mDataLabel = (TextView) rootView.findViewById(R.id.data_label);
        mDataValue = (TextView) rootView.findViewById(R.id.data_value);

        return rootView;
    }

    public void setDataLabel(String dataLabel) {
        mDataLabel.setText(dataLabel);
    }

    public void setDataValue(String dataValue) {
        mDataValue.setText(dataValue);
    }
}
