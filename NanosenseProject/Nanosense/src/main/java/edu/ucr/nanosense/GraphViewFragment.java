package edu.ucr.nanosense;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by Albert Chen
 * Last updated 12/04/2013
 *
 */
public class GraphViewFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_graph_view, null);
        return rootView;
    }

    private static GraphViewFragment newInstance() {
        return new GraphViewFragment();
    }
}
