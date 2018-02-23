package com.example.vomyrak.heatband;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.sax.StartElementListener;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.text.method.NumberKeyListener;
import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import org.w3c.dom.Text;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import static java.lang.System.out;

// TODO(1) Re-assignment of widget ids
// TODO(2) Add a widget to display temperature

public class MainActivity extends AppCompatActivity {

    //Create (3 preferences + 1 temp) * 3 bytes array for storing temperature info data
    private byte[] stateVal = new byte[12];
    //Create a byte for battery info
    private byte batteryLife = (byte) 255;
    //Create an integer for seekbar progress;
    private int seekBarProgress = 50;
    //Create shared preference
    protected SharedPreferences settings;
    protected SharedPreferences.Editor editor;
    //Create UI elements
    protected ProgressBar progressBar;
    protected TextView tvBatteryLife;
    protected TextView tvTemperature;
    protected TextView tvMode1;
    protected TextView tvMode2;
    protected TextView tvMode3;
    protected ImageView imBluetooth;
    protected Button applyChanges;
    protected Button select1;
    protected Button edit1;
    protected Button select2;
    protected Button edit2;
    protected Button select3;
    protected Button edit3;
    protected ToggleButton toggleButton;
    protected SeekBar seekBar;

    //Create constant strings
    private static final String mSettingStateVals = "stateVals";
    private static final String mBatteryLife = "batteryLife";
    private static final String mPreferenceFile  = "MyPreferenceFile";
    private static final String mSeekBarProgress = "seekbarProgress";
    private static final String mJsonFile = "Settings.json";
    private String DEVICE_ADDRESS;
    private String DEVICE_NAME;


    //Create bluetooth adaptor
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private BluetoothDevice connectedDevice;
    private Set<BluetoothDevice> pairedDevices;
    private static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    //A list of request codes
    protected static final int rRequestBt = 1;

    //Timer Function
    Handler timerHandler = new Handler();
    Runnable timeRunnable = new Runnable() {
        @Override
        public void run() {
            try{
                if (bluetoothSocket.isConnected()){
                    bluetoothSocket.getOutputStream().write("j255,0,255 ".getBytes());
                }
            } catch (IOException e){
                e.printStackTrace();
            } catch (NullPointerException f){
                try{
                    connectedDevice = bluetoothAdapter.getRemoteDevice(DEVICE_ADDRESS);
                    bluetoothSocket = connectedDevice.createInsecureRfcommSocketToServiceRecord(myUUID);
                    bluetoothSocket.connect();
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
            timerHandler.postDelayed(timeRunnable, 1000 * 5);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Initialise app variables and UI elements on creation of application
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null){
            //If saved instance is present, get value from previously set instance state
            if (savedInstanceState.containsKey(mSettingStateVals)){
                stateVal = savedInstanceState.getByteArray(mSettingStateVals);
            }
            if (savedInstanceState.containsKey(mBatteryLife)){
                batteryLife = savedInstanceState.getByte(mBatteryLife);
            }
            if (savedInstanceState.containsKey(mSeekBarProgress)){
                seekBarProgress = savedInstanceState.getInt(mSeekBarProgress);
            }
        }
        else{
            try {
                File file = new File(mJsonFile);
                if (file.exists()) {
                    FileInputStream infile = openFileInput(mJsonFile);
                    readJsonStream(infile);
                }
                else{
                    //encode default values here;
                    for (int i = 0; i < stateVal.length; i++){
                        stateVal[i] = 0;
                    }
                    seekBarProgress = 50;
                    batteryLife = 100;
                }
            } catch (Exception e){
                finish();
            }
        }

        setContentView(R.layout.activity_main);
        seekBar = (SeekBar) findViewById(R.id.temp_setter);
        progressBar = (ProgressBar) findViewById(R.id.battery_life);
        tvBatteryLife = (TextView) findViewById(R.id.battery);
        tvTemperature = (TextView) findViewById(R.id.current_temp);
        tvMode1 = (TextView) findViewById(R.id.mode_1);
        tvMode2 = (TextView) findViewById(R.id.mode_2);
        tvMode3 = (TextView) findViewById(R.id.mode_3);
        toggleButton = (ToggleButton) findViewById(R.id.temp_unit);
        imBluetooth = (ImageView) findViewById(R.id.bluetooth);
        applyChanges = (Button) findViewById(R.id.change);
        //select1 = (Button) findViewById(R.id.select_1);
        //edit1 = (Button) findViewById(R.id.edit_1);
        //select2 = (Button) findViewById(R.id.select_2);
        //edit2 = (Button) findViewById(R.id.mode_2);
        //select3 = (Button) findViewById(R.id.select_3);
        //edit3 = (Button) findViewById(R.id.mode_3);
        //TODO(3) set onClickListeners for all widgets
        tvMode1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //OnClick of the button the floating activity is started
                Intent startFloatingActivity = new Intent(MainActivity.this,  FloatingActivity.class);
                startFloatingActivity.putExtra(mSettingStateVals, stateVal);
                startFloatingActivity.putExtra("Mode", 1);
                MainActivity.this.startActivity(startFloatingActivity);
            }
        });
        tvMode2.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                try {
                    Toast.makeText(getApplicationContext(), "BT Name: "+DEVICE_NAME+"\nBT Address: "+DEVICE_ADDRESS, Toast.LENGTH_SHORT).show();
                    bluetoothSocket.getOutputStream().write("j255,0,255 ".toString().getBytes());
                } catch (Exception e){
                    Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }

        });
        seekBar.setProgress(seekBarProgress);
        progressBar.setProgress(((int) batteryLife));
        //Toast.makeText(this, "Create finished", Toast.LENGTH_SHORT).show();

        //Initialise Bluetooth
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null){
            Toast.makeText(getApplicationContext(), "Bluetooth not supported", Toast.LENGTH_SHORT).show();
            finish();
        }
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            this.startActivityForResult(enableBtIntent, rRequestBt);
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        //this.registerReceiver(mReceiver, filter);
        bluetoothAdapter.startDiscovery();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == rRequestBt){
            if (resultCode == 0){
                Toast.makeText(getApplicationContext(), "The user decided to deny bluetooth access", Toast.LENGTH_SHORT).show();
            }
        }
    }
    private boolean matchedUUID(BluetoothDevice bt){
        for (ParcelUuid uuid : bt.getUuids()){
            if (uuid.getUuid().toString().equals(myUUID.toString())) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onStart() {
        super.onStart();

        //onStart, UI elements are associated with variables
    }
/*
     private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            Toast.makeText(getApplicationContext(), "1", Toast.LENGTH_SHORT).show();
            if (!connectedDevice.ACTION_ACL_CONNECTED.equals(action)) {
                Toast.makeText(getApplicationContext(), "2", Toast.LENGTH_SHORT).show();
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    Toast.makeText(getApplicationContext(), "3", Toast.LENGTH_SHORT).show();
                    connectedDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    String deviceName = connectedDevice.getName();
                    if (deviceName == "HC-05") {
                        DEVICE_ADDRESS = connectedDevice.getAddress();
                        DEVICE_NAME = deviceName;
                        try {
                            Boolean isBonded = connectedDevice.createBond();
                            if (isBonded){
                                Toast.makeText(getApplicationContext(), "BT Name: "+DEVICE_NAME+"\nBT Address: "+DEVICE_ADDRESS, Toast.LENGTH_SHORT).show();
                                bluetoothSocket = connectedDevice.createInsecureRfcommSocketToServiceRecord(myUUID);//create a RFCOMM (SPP) connection
                                bluetoothSocket.connect();
                            }
                        } catch (Exception e) {
                            Toast.makeText(getApplicationContext(), "Failed to pair device", Toast.LENGTH_SHORT).show();
                        }
                    }

                }
            }
        }
    };
*/
    @Override
    protected void onResume(){
        super.onResume();
        if (bluetoothSocket == null) {
            try {
                Toast.makeText(getApplicationContext(), "Resumed", Toast.LENGTH_SHORT).show();
                pairedDevices = bluetoothAdapter.getBondedDevices();
                if (pairedDevices.size() > 0) {
                    for (BluetoothDevice bt : pairedDevices) {
                        if (matchedUUID(bt)) {
                            DEVICE_ADDRESS = bt.getAddress();
                            DEVICE_NAME = bt.getName();
                            break;
                        } else {
                            Toast.makeText(getApplicationContext(), "No matched UUID found", Toast.LENGTH_SHORT).show();
                        }
                    }
                    try {
                        connectedDevice = bluetoothAdapter.getRemoteDevice(DEVICE_ADDRESS);
                        bluetoothSocket = connectedDevice.createInsecureRfcommSocketToServiceRecord(myUUID);
                        bluetoothSocket.connect();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }


            } catch (Exception e) {
                Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
        else{
            if(!bluetoothSocket.isConnected()){
                try {
                    connectedDevice = bluetoothAdapter.getRemoteDevice(DEVICE_ADDRESS);
                    bluetoothSocket = connectedDevice.createInsecureRfcommSocketToServiceRecord(myUUID);
                    bluetoothSocket.connect();
                } catch (IOException e){
                    e.printStackTrace();
                }
            }
        }
        timeRunnable.run();

    }

    @Override
    protected void onPause(){
        super.onPause();
        timeRunnable.run();
    }

    @Override
    protected void onStop(){
        super.onStop();
        if(bluetoothSocket.isConnected()){
            try {
                bluetoothSocket.close();
            } catch (IOException e){
                e.printStackTrace();
            }
        }
        //Add shared preference settings
        saveFile(mJsonFile);
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putByteArray(mSettingStateVals, stateVal);
        outState.putByte(mBatteryLife, batteryLife);
        outState.putInt(mSeekBarProgress, seekBarProgress);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    //Json Handler
    public void saveFile(String fileName){
        try {
            FileOutputStream file = openFileOutput(fileName, Context.MODE_PRIVATE);
            writeJsonStream(file);
            file.close();
        } catch (Exception e){
            finish();
        }
    }
    public void loadFile(String fileName){
        try{
            FileInputStream file = openFileInput(fileName);
            readJsonStream(file);
            file.close();
        } catch (Exception e){
            finish();
        }
    }

    public void writeJsonStream(OutputStream out) {
        try {
            JsonWriter writer= new JsonWriter(new OutputStreamWriter(out, "utf-8"));
            writer.setIndent(" ");
            writer.beginArray();
            writer.beginObject();
            writer.name("Current Profile");
            writer.beginArray();
            writer.value(stateVal[0]);
            writer.value(stateVal[1]);
            writer.value(stateVal[2]);
            writer.endArray();
            writer.name("Mode 1");
            writer.beginArray();
            writer.value(stateVal[3]);
            writer.value(stateVal[4]);
            writer.value(stateVal[5]);
            writer.endArray();
            writer.name("Mode 2");
            writer.beginArray();
            writer.value(stateVal[6]);
            writer.value(stateVal[7]);
            writer.value(stateVal[8]);
            writer.endArray();
            writer.name("Mode 3");
            writer.beginArray();
            writer.value(stateVal[9]);
            writer.value(stateVal[10]);
            writer.value(stateVal[11]);
            writer.endArray();
            writer.name("Battery").value(batteryLife);
            writer.name("Seekbar").value(seekBarProgress);
            writer.endObject();
            writer.endArray();
            writer.close();
        } catch (Exception e){
            finish();
        }
    }
    //Json Parser
    public void readJsonStream(InputStream in){
        try{
            JsonReader reader = new JsonReader(new InputStreamReader(in, "UTF-8"));
            reader.beginArray();
            reader.beginObject();
            while (reader.hasNext()){
                String name = reader.nextName();
                if (name.equals("Current Profile")){
                    reader.beginArray();
                    int index = 0;
                    while (reader.hasNext()){
                        stateVal[index] = (byte) reader.nextInt();
                        index += 1;
                    }
                    reader.endArray();
                }
                else if (name.equals("Mode 1")){
                    reader.beginArray();
                    int index = 3;
                    while (reader.hasNext()){
                        stateVal[index] = (byte) reader.nextInt();
                        index += 1;
                    }
                    reader.endArray();
                }
                else if (name.equals("Mode 2")){
                    reader.beginArray();
                    int index = 6;
                    while (reader.hasNext()){
                        stateVal[index] = (byte) reader.nextInt();
                        index += 1;
                    }
                    reader.endArray();
                }
                else if (name.equals("Current Profile")){
                    reader.beginArray();
                    int index = 9;
                    while (reader.hasNext()){
                        stateVal[index] = (byte) reader.nextInt();
                        index += 1;
                    }
                    reader.endArray();
                }
                else if (name.equals("Battery")){
                    batteryLife = (byte) reader.nextInt();
                }
                else if (name.equals("Seekbar")){
                    seekBarProgress = reader.nextInt();
                }
            }
            reader.endObject();
            reader.endArray();
            reader.close();
        } catch (Exception e) {
            finish();
        }
    }
}
