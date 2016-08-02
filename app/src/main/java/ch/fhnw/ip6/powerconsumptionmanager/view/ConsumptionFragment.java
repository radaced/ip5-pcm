package ch.fhnw.ip6.powerconsumptionmanager.view;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.github.mikephil.charting.charts.LineChart;

import java.util.ArrayList;

import ch.fhnw.ip6.powerconsumptionmanager.R;
import ch.fhnw.ip6.powerconsumptionmanager.adapter.ConsumptionDeviceListAdapter;
import ch.fhnw.ip6.powerconsumptionmanager.network.AsyncTaskCallback;
import ch.fhnw.ip6.powerconsumptionmanager.network.GetConsumptionDataAsyncTask;
import ch.fhnw.ip6.powerconsumptionmanager.util.helper.ConsumptionDataHelper;
import ch.fhnw.ip6.powerconsumptionmanager.util.PowerConsumptionManagerAppContext;

/**
 * This Fragment shows usage data in a chart and connected devices in a list
 */
public class ConsumptionFragment extends Fragment implements AsyncTaskCallback {
    private static final String TAG = "ConsumptionFragment";

    private Handler mUpdateHandler;
    private ConsumptionDataHelper mConsumptionDataHelper;
    private PowerConsumptionManagerAppContext mAppContext;

    private LinearLayout mLoadingLayout;
    private LinearLayout mConsumptionDataLayout;
    private LinearLayout mOnErrorConsumptionDataLayout;
    private ListView mListViewDevices;
    private boolean mHasUpdated = false;

    public static ConsumptionFragment newInstance() {
        return new ConsumptionFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_consumption, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mAppContext = (PowerConsumptionManagerAppContext) getActivity().getApplicationContext();

        mLoadingLayout = (LinearLayout) view.findViewById(R.id.llLoading);
        mConsumptionDataLayout = (LinearLayout) view.findViewById(R.id.llConsumptionData);
        mOnErrorConsumptionDataLayout = (LinearLayout) view.findViewById(R.id.llOnErrorConsumptionData);

        LineChart consumptionChart = (LineChart) view.findViewById(R.id.consumptionDataLineChart);
        mListViewDevices = (ListView) view.findViewById(R.id.lvDevices);
        mConsumptionDataHelper = new ConsumptionDataHelper(getContext(), consumptionChart);

        new GetConsumptionDataAsyncTask(mAppContext, this).execute();
    }

    @Override
    public void onStop() {
        super.onStop();
        // Stop data updater if instantiated
        if(mUpdateHandler != null) {
            mUpdateHandler.removeCallbacks(updateConsumptionData);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Start data updater if instantiated
        if(mUpdateHandler != null) {
            mHasUpdated = true;
            mUpdateHandler.postDelayed(updateConsumptionData, 10000);
        }
    }

    @Override
    public void asyncTaskFinished(boolean result) {
        mLoadingLayout.setVisibility(View.GONE);

        if(result) {
            mOnErrorConsumptionDataLayout.setVisibility(View.GONE);
            mConsumptionDataLayout.setVisibility(View.VISIBLE);

            if(!mHasUpdated) {
                // Set up the whole chart with data sets and so on with the helper class
                mConsumptionDataHelper.setup();
                mConsumptionDataHelper.setLegend(false);
                mConsumptionDataHelper.generateXValues(mAppContext.getConsumptionData().get(0));

                // Generate the data sets to display
                for (int i = 0; i < mAppContext.getConsumptionData().size(); i++) {
                    mConsumptionDataHelper.generateDataSet(mAppContext.getConsumptionData().get(i), i);
                }

                // Display the chart
                mConsumptionDataHelper.initChartData();
                mConsumptionDataHelper.displayAnimated();

                // Instantiate the update handler
                mUpdateHandler = new Handler();

                // Define device list adapter parameters
                int layoutResource = R.layout.list_consumption_device;
                ArrayList<String> listItems = new ArrayList<>(mAppContext.getPCMData().getComponentData().keySet());

                // Set up the device list
                mListViewDevices.setAdapter(
                        new ConsumptionDeviceListAdapter(
                                getActivity(),
                                layoutResource,
                                listItems,
                                mConsumptionDataHelper
                        )
                );
            } else {
                mConsumptionDataHelper.generateXValues(mAppContext.getConsumptionData().get(0));

                // Generate the updated data sets
                for (int z = 0; z < mAppContext.getConsumptionData().size(); z++) {
                    mConsumptionDataHelper.generateDataSet(mAppContext.getConsumptionData().get(z), z);
                }

                // Add the data sets to the chart
                mConsumptionDataHelper.updateChartData();
            }
        } else {
            mConsumptionDataLayout.setVisibility(View.GONE);
            mOnErrorConsumptionDataLayout.setVisibility(View.VISIBLE);
        }

        if(mAppContext.isUpdatingAutomatically() && !mHasUpdated) {
            mUpdateHandler = new Handler();
            onResume();
        }
    }

    /**
     * Runnable for updating the chart (every 10 seconds)
     */
    private final Runnable updateConsumptionData = new Runnable() {
        public void run() {
            new GetConsumptionDataAsyncTask(
                mAppContext,
                getInstance()
            ).execute();
            mUpdateHandler.postDelayed(this, mAppContext.getUpdateInterval() * 1000);
        }
    };

    private ConsumptionFragment getInstance() { return this; }
}