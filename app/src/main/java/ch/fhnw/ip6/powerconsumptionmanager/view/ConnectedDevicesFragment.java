package ch.fhnw.ip6.powerconsumptionmanager.view;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import java.util.ArrayList;

import ch.fhnw.ip6.powerconsumptionmanager.R;
import ch.fhnw.ip6.powerconsumptionmanager.activity.ComponentSettingsActivity;
import ch.fhnw.ip6.powerconsumptionmanager.adapter.ConnectedDeviceListAdapter;
import ch.fhnw.ip6.powerconsumptionmanager.util.PowerConsumptionManagerAppContext;

public class ConnectedDevicesFragment extends ListFragment {
    private PowerConsumptionManagerAppContext mAppContext;
    private ArrayList<String> mComponentNamesList;

    public static ConnectedDevicesFragment newInstance() {
        return new ConnectedDevicesFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_connected_devices, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAppContext = (PowerConsumptionManagerAppContext) getContext().getApplicationContext();
        ListView lvConnectedDevices = (ListView) view.findViewById(android.R.id.list);
        int layoutResource;


        if(mAppContext.isOnline()) {
            layoutResource = R.layout.list_connected_device;
            mComponentNamesList = new ArrayList<>(mAppContext.getPCMData().getComponentData().keySet());
        } else {
            layoutResource = R.layout.list_no_device;
            mComponentNamesList = new ArrayList<>();
            mComponentNamesList.add(getString(R.string.list_device_error));
        }

        lvConnectedDevices.setAdapter(
                new ConnectedDeviceListAdapter(
                        getContext(),
                        layoutResource,
                        mComponentNamesList,
                        mAppContext.isOnline()
                )
        );
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        Intent intent = new Intent(getActivity(), ComponentSettingsActivity.class);
        intent.putExtra("componentName", mComponentNamesList.get(position));
        getActivity().startActivity(intent);
    }
}
