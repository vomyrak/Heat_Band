package com.example.vomyrak.heatband;

import android.Manifest;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;

import java.lang.reflect.Method;
import java.util.ArrayList;

import static android.bluetooth.BluetoothDevice.ACTION_PAIRING_REQUEST;
import static com.example.vomyrak.heatband.MainActivity.DEVICE_NAME;
import static com.example.vomyrak.heatband.MainActivity.DEVICE_ADDRESS;
import static com.example.vomyrak.heatband.MainActivity.bluetoothAdapter;
import static com.example.vomyrak.heatband.MainActivity.bluetoothSocket;

public class ScanActivity extends AppCompatActivity {

    private ArrayList<BluetoothDevice> discoveredDevices = new ArrayList<>();
    private RecyclerView mRecyclerView;
    private DeviceAdapter mDeviceAdapter;
    private ProgressDialog mProgressDlg;
    private int REQUEST_COURSE_PERMISSION = 10;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);
        checkLocationPermission();
        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setHasFixedSize(true);
        mDeviceAdapter = new DeviceAdapter(new DeviceAdapter.RecyclerViewClickListener() {
            @Override
            public void onListItemClick(int clickedItemIndex) {
                try {
                    Method method = discoveredDevices.get(clickedItemIndex).getClass().getMethod("createBond", (Class[]) null);
                    method.invoke(discoveredDevices.get(clickedItemIndex), (Object[]) null);
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
        mRecyclerView.setAdapter(mDeviceAdapter);
        getApplicationContext().registerReceiver(bReciever, new IntentFilter(BluetoothDevice.ACTION_FOUND));
        mRecyclerView.setVisibility(View.VISIBLE);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothAdapter.startDiscovery();
        mProgressDlg 		= new ProgressDialog(this);
        mProgressDlg.setMessage("Scanning...");
        mProgressDlg.setCancelable(false);
        mProgressDlg.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();

                bluetoothAdapter.cancelDiscovery();
            }
        });

        mProgressDlg.show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            this.unregisterReceiver(bReciever);
        } catch (IllegalArgumentException e){
            e.printStackTrace();
        }
    }

    private final BroadcastReceiver bReciever = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                Log.d("DEVICELIST", "Bluetooth device found\n");
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                discoveredDevices.add(device);
                addItemForDisplay(device);
            }
        }
    };

    private void addItemForDisplay(BluetoothDevice device){
        mDeviceAdapter.setDeviceData(device);
    }

    private void checkLocationPermission(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_COURSE_PERMISSION);
        }
    }
}
