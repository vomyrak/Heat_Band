package com.example.vomyrak.heatband;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.lang.reflect.Method;
import java.util.ArrayList;
import static com.example.vomyrak.heatband.MainActivity.bluetoothAdapter;

public class ScanActivity extends AppCompatActivity {

    protected static ArrayList<BluetoothDevice> discoveredDevices = new ArrayList<>();
    private RecyclerView mRecyclerView;
    private DeviceAdapter mDeviceAdapter;
    private ProgressDialog mProgressDlg;
    private AlertDialog mAlertDlg;
    private Button mRefresh;
    private int REQUEST_COURSE_PERMISSION = 10;
    private int result = 0;
    protected static final String arduino = "98:D3:32:11:36:49";
    private Handler scanTimeHandle= new Handler();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);
        checkLocationPermission();
        ActionBar bar = getSupportActionBar();
        bar.setBackgroundDrawable(getDrawable(R.drawable.gradient));
        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        mRefresh = findViewById(R.id.refresh);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setHasFixedSize(true);
        mDeviceAdapter = new DeviceAdapter(new DeviceAdapter.RecyclerViewClickListener() {
            @Override
            public void onListItemClick(int clickedItemIndex) {
                try {
                    Method method = discoveredDevices.get(clickedItemIndex).getClass().getMethod("createBond", (Class[]) null);
                    method.invoke(discoveredDevices.get(clickedItemIndex), (Object[]) null);
                    setResult(RESULT_OK);
                    result = RESULT_OK;
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
        mRecyclerView.setAdapter(mDeviceAdapter);
        this.registerReceiver(bReciever, new IntentFilter(BluetoothDevice.ACTION_FOUND));
        mRecyclerView.setVisibility(View.VISIBLE);
        mRefresh.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                if (bluetoothAdapter.isDiscovering()){
                    bluetoothAdapter.cancelDiscovery();
                }
                discoveredDevices.clear();
                scanningRoutine();
                mDeviceAdapter.notifyDataSetChanged();
            }
        });
        scanningRoutine();

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (bluetoothAdapter.isDiscovering()){
            bluetoothAdapter.cancelDiscovery();
        }
        try {
            this.unregisterReceiver(bReciever);

        } catch (IllegalArgumentException e){
            e.printStackTrace();
        } finally {
            finish();
        }
    }

    //Register for broadcast receiver
    private final BroadcastReceiver bReciever = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                Log.d("DEVICELIST", "Bluetooth device found\n");
                final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getAddress().equals(arduino)){
                    bluetoothAdapter.cancelDiscovery();
                    mProgressDlg.dismiss();
                    final AlertDialog.Builder builder = new AlertDialog.Builder(ScanActivity.this);
                    builder.setMessage("Compatible Device Found\nat Address "+device.getAddress()+"\nConnect?")
                            .setCancelable(false)
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    try {
                                        Method method = device.getClass().getMethod("createBond", (Class[]) null);
                                        method.invoke(device, (Object[]) null);
                                    } catch (Exception e){
                                        e.printStackTrace();
                                    }
                                    ScanActivity.this.setResult(RESULT_OK);
                                    finish();
                                }
                            })
                            .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                }
                            });
                    mAlertDlg = builder.create();
                    mAlertDlg.show();
                }

                else {
                    addItemForDisplay(device);
                }
            }
        }
    };

    //Add discovered device fro display
    private void addItemForDisplay(BluetoothDevice device){
        mDeviceAdapter.setDeviceData(device);
    }

    //Check device user permission to use location
    private void checkLocationPermission(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_COURSE_PERMISSION);
        }
    }

    //Routine for scanning discoverable bluetooth devices for pairing
    private void scanningRoutine(){
        try {
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

            bluetoothAdapter.startDiscovery();
            mProgressDlg = new ProgressDialog(this);
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
            scanTimeHandle.postDelayed(new Runnable() {
                @Override
                public void run() {
                    bluetoothAdapter.cancelDiscovery();
                    try {
                        if (mProgressDlg.isShowing()) {
                            mProgressDlg.dismiss();
                        }
                    } catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }, 10000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
