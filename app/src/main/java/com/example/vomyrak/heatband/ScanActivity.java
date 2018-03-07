package com.example.vomyrak.heatband;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;

import static com.example.vomyrak.heatband.MainActivity.DEVICE_NAME;
import static com.example.vomyrak.heatband.MainActivity.DEVICE_ADDRESS;

public class ScanActivity extends AppCompatActivity {

    private ArrayList<BluetoothDevice> discoveredDevices;
    private RecyclerView mRecyclerView;
    private DeviceAdapter mDeviceAdapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);
        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setHasFixedSize(true);
        mDeviceAdapter = new DeviceAdapter();
        mRecyclerView.setAdapter(mDeviceAdapter);
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(bReciever, filter);
        addItemForDisplay(new String[]{"1", "2"});
        mRecyclerView.setVisibility(View.VISIBLE);

    }

    private final BroadcastReceiver bReciever = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                Log.d("DEVICELIST", "Bluetooth device found\n");
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                discoveredDevices.add(device);
                // Create a new device item
                addItemForDisplay(device);
            }
        }
    };

    private void addItemForDisplay(BluetoothDevice device){
        mDeviceAdapter.setDeviceData(new String[]{device.getAddress().toString()});
    }
    private void addItemForDisplay(String[] strings){
        mDeviceAdapter.setDeviceData(strings);
    }
}
