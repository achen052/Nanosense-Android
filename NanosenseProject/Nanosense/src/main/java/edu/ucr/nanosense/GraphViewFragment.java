package edu.ucr.nanosense;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 *
 * GraphViewFragment contains the UI component {@link edu.ucr.nanosense.GraphView} that is used
 * to display the graphs. It also handles the current state and what the GraphView should display.
 * The data will be stored in {@link edu.ucr.nanosense.NanoSenseActivity} which will handle it's
 * state.
 * When the fragment is restored, it will send all the data to {@link edu.ucr.nanosense.GraphView}.
 * Typically GraphViewFragment will receive one data point per channel and
 *
 */
public class GraphViewFragment extends Fragment implements ViewTreeObserver.OnGlobalLayoutListener,
        View.OnClickListener {

    private GraphView mGraphView;

    private FrameLayout mFrameLayoutContainer;

    private TextView mTextViewGraphTitle;
    private TextView mTextViewGraphXLabel;
    private TextView mTextViewGraphYLabel;

    public GraphViewFragment() {
    }

    public static GraphViewFragment newInstance() {
        return new GraphViewFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_graph_view, null);
        if (rootView != null) {
            mFrameLayoutContainer = (FrameLayout) rootView.findViewById(R.id.container);
            mFrameLayoutContainer.setOnClickListener(this);
            mGraphView = (GraphView) rootView.findViewById(R.id.graph_view);
            mTextViewGraphTitle = (TextView) rootView.findViewById(R.id.graph_view_title);
            mTextViewGraphXLabel = (TextView) rootView.findViewById(R.id.graph_view_x_label);
            mTextViewGraphYLabel = (TextView) rootView.findViewById(R.id.graph_view_y_label);
        }
        /**
         * Global Layout Listener used for adjusting the margin of the Y axis label.
         * This should be done programmatically since the text changes depending on what
         * graph is being viewed.
         */
        ViewTreeObserver viewTreeObserver = mTextViewGraphYLabel.getViewTreeObserver();
        if (viewTreeObserver != null) {
            viewTreeObserver.addOnGlobalLayoutListener(this);
        }

        return rootView;
    }

    /**
     * Global layout listener for when the view has changed. Updates the margin for the vertical
     * Y Axis label.
     */
    @Override
    public void onGlobalLayout() {
        int layoutWidth = mTextViewGraphYLabel.getWidth();
        int layoutHeight = mTextViewGraphYLabel.getHeight();
        FrameLayout.LayoutParams layoutParams =
                (FrameLayout.LayoutParams) mTextViewGraphYLabel.getLayoutParams();
        if (layoutParams != null) {
            /** Since the width is still the non-rotated width, take half that plus the height/2 */
            layoutParams.setMargins(-layoutWidth / 2 + layoutHeight / 2, 0, 0, 0);
        }
        mTextViewGraphYLabel.requestLayout();
    }

    @Override
    public void onClick(View view) {
        // TODO; Update Labels.
        // TODO: Switch to gesture listener? Maybe implement directly in GraphView and add callback interface to onChanged?
    }
}
