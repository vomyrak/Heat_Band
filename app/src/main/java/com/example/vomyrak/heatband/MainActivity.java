package com.example.vomyrak.heatband;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.provider.ContactsContract;
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

import com.daimajia.numberprogressbar.NumberProgressBar;

import org.adw.library.widgets.discreteseekbar.DiscreteSeekBar;
import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.DataInputStream;
import java.io.DataOutputStream;
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

import static java.lang.System.err;
import static java.lang.System.out;

// TODO(1) Re-assignment of widget ids
// TODO(2) Add a widget to display temperature

public class MainActivity extends AppCompatActivity {

    //Create (3 preferences + 1 temp) * 3 bytes array for storing temperature info data
    protected static byte[] stateVal = new byte[12];
    //Create a byte for battery info
    protected static byte batteryLife = (byte) 255;
    //Create an integer for seekbar progress;
    protected static int seekBarProgress = 50;
    //Create shared preference
    protected SharedPreferences settings;
    protected SharedPreferences.Editor editor;
    //Create UI elements
    protected NumberProgressBar progressBar;
    protected TextView tvBatteryLife;
    protected TextView tvTemperature;
    protected TextView tvMode1;
    protected TextView tvMode2;
    protected TextView tvMode3;
    protected ImageView imBluetooth;
    protected Button applyChanges;
    protected ToggleButton toggleButton;
    protected DiscreteSeekBar seekBar;

    //Create constant strings
    protected static final String mSettingStateVals = "stateVals";
    protected static final String mBatteryLife = "batteryLife";
    protected static final String mPreferenceFile  = "MyPreferenceFile";
    protected static final String mSeekBarProgress = "seekbarProgress";
    protected static final String mJsonFile = "Settings.json";
    protected static String DEVICE_ADDRESS;
    protected static String DEVICE_NAME;


    //Create bluetooth adaptor
    protected static BluetoothAdapter bluetoothAdapter;
    protected static BluetoothSocket bluetoothSocket;
    protected static BluetoothDevice connectedDevice;
    protected static Set<BluetoothDevice> pairedDevices;
    protected static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    //A list of request codes
    protected static final int rRequestBt = 1;
    protected static final int rRequestZoneSetting = 2;



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
                Log.d("Json Checking", "onCreate: Json FIle READ");
                Log.d("File InName", file.getName());
                if (file.exists()) {
                    //FileInputStream infile = openFileInput(mJsonFile);
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
        seekBar = (DiscreteSeekBar) findViewById(R.id.temp_setter);
        progressBar = (NumberProgressBar) findViewById(R.id.battery_life);
        tvBatteryLife = (TextView) findViewById(R.id.battery);
        tvTemperature = (TextView) findViewById(R.id.current_temp);
        tvMode1 = (TextView) findViewById(R.id.mode_1);
        tvMode2 = (TextView) findViewById(R.id.mode_2);
        tvMode3 = (TextView) findViewById(R.id.mode_3);
        toggleButton = (ToggleButton) findViewById(R.id.temp_unit);
        imBluetooth = (ImageView) findViewById(R.id.bluetooth);
        applyChanges = (Button) findViewById(R.id.change);
        seekBar.setProgress(seekBarProgress);
        progressBar.setProgress(((int) batteryLife));
        tvMode1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //OnClick of the button the floating activity is started
                Intent startFloatingActivity = new Intent(MainActivity.this,  FloatingActivity.class);
                startFloatingActivity.putExtra("Mode", 1);
                MainActivity.this.startActivityForResult(startFloatingActivity, rRequestZoneSetting);
            }
        });
        tvMode2.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                try {
                    Toast.makeText(getApplicationContext(), "BT Name: "+DEVICE_NAME+"\nBT Address: "+DEVICE_ADDRESS, Toast.LENGTH_SHORT).show();
                    //bluetoothSocket.getOutputStream().write("j255,0,255 ".getBytes());
                    bluetoothSocket.getOutputStream().write("j".getBytes());
                    bluetoothSocket.getOutputStream().write(stateVal[0]);
                    bluetoothSocket.getOutputStream().write(stateVal[1]);
                    bluetoothSocket.getOutputStream().write(stateVal[2]);
                    bluetoothSocket.getOutputStream().write(" ".getBytes());
                } catch (Exception e){
                    e.printStackTrace();
                }
            }

        });
        tvMode3.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                try {
                        bluetoothSocket.getOutputStream().write("j\n".getBytes());


                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
        applyChanges.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                Toast.makeText(getApplicationContext(), "BT Name: "+DEVICE_NAME+"\nBT Address: "+DEVICE_ADDRESS, Toast.LENGTH_SHORT).show();
                //bluetoothSocket.getOutputStream().write("j255,0,255 ".getBytes());
                try {
                    bluetoothSocket.getOutputStream().write("j".getBytes());
                    bluetoothSocket.getOutputStream().write(stateVal[0]);
                    bluetoothSocket.getOutputStream().write(stateVal[1]);
                    bluetoothSocket.getOutputStream().write(stateVal[2]);
                    bluetoothSocket.getOutputStream().write(" ".getBytes());
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        });


        seekBar.setOnProgressChangeListener(new DiscreteSeekBar.OnProgressChangeListener() {
            @Override
            public void onProgressChanged(DiscreteSeekBar seekBar, int value, boolean fromUser) {
                seekBarProgress = seekBar.getProgress();

            }

            @Override
            public void onStartTrackingTouch(DiscreteSeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(DiscreteSeekBar seekBar) {

            }
        });


       // startService(new Intent(MainActivity.this, MyBtService.class));
        //startService(serviceIntent);


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == rRequestBt){
            if (resultCode == 0){
                Toast.makeText(getApplicationContext(), "The user decided to deny bluetooth access", Toast.LENGTH_SHORT).show();
            }
        }
        if (requestCode == rRequestZoneSetting){
            if (resultCode == RESULT_OK){
                saveFile();
            }
        }
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
        loadFile();
        Intent UpdateProgress = getIntent();
        int mode = UpdateProgress.getIntExtra("Mode", 0);
        byte NewData[] = UpdateProgress.getByteArrayExtra("New Progress");

        if(mode!=0){
            int offset = mode * 3;
            stateVal[offset] = NewData[0];
            stateVal[offset + 1] = NewData[1];
            stateVal[offset + 2] = NewData[2];
        }
        ;
    }

    @Override
    protected void onPause(){
        super.onPause();
    }

    @Override
    protected void onStop(){
        super.onStop();
        Log.d("Stop checking", "onStop: Application stoped");
        //Add shared preference settings
        saveFile();

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
    public void saveFile(){
        try {
            String path = getFilesDir().toString() + mJsonFile;
            Log.d("Path is", path);
            FileOutputStream fileOutputStream = new FileOutputStream(path);
            Log.d("File Exists", "File Exists");
            DataOutputStream file = new DataOutputStream(fileOutputStream);
            writeJsonStream(file);
            file.close();
        } catch (Exception e){
            Log.d("Cannot write file", "saveFile: ");
            e.printStackTrace();
        }
    }
    public void loadFile(){
        try{
            String path = getFilesDir().toString() + mJsonFile;
            Log.d("Path is", path);
            FileInputStream fileInputStream = new FileInputStream(path);
            Log.d("File Exists", "File Exists");
            DataInputStream dataFile = new DataInputStream(fileInputStream);
            readJsonStream(dataFile);
            dataFile.close();
        } catch (IOException e){
            Log.d("Writing json", "File not Found ");
            e.printStackTrace();
        }
    }

    public void writeJsonStream(DataOutputStream out) {
        try {
            Log.d("Writing json", "writeJsonStream: ");
            JSONObject newObject = new JSONObject();
            JSONArray newArray = new JSONArray();
            newArray.put((int)stateVal[0]);
            newArray.put((int)stateVal[1]);
            newArray.put((int)stateVal[2]);
            newObject.put("Current Profile", newArray);
            newArray = new JSONArray();
            newArray.put((int)stateVal[3]);
            newArray.put((int)stateVal[4]);
            newArray.put((int)stateVal[5]);
            newObject.put("Mode 1", newArray);
            newArray = new JSONArray();
            newArray.put((int)stateVal[6]);
            newArray.put((int)stateVal[7]);
            newArray.put((int)stateVal[8]);
            newObject.put("Mode 2", newArray);
            newArray = new JSONArray();
            newArray.put((int)stateVal[9]);
            newArray.put((int)stateVal[10]);
            newArray.put((int)stateVal[11]);
            newObject.put("Mode 3", newArray);
            newObject.put("Battery", batteryLife);
            newObject.put("Seekbar", seekBarProgress);
            String jsonString = newObject.toString();
            out.writeUTF(jsonString);
        } catch (Exception e){
            e.printStackTrace();
        }
    }
    //Json Parser
    public void readJsonStream(DataInputStream in){
        try{
            String inputString = in.readUTF();
            Log.d("Get json", inputString);
            JSONObject newObject = new JSONObject(inputString);
            JSONArray newArray = newObject.getJSONArray("Current Profile");
            stateVal[0] = (byte)newArray.getInt(0) ;
            stateVal[1] = (byte)newArray.getInt(1) ;
            stateVal[2] = (byte)newArray.getInt(2) ;
            newArray = newObject.getJSONArray("Mode 1");
            stateVal[3] = (byte)newArray.getInt(0) ;
            stateVal[4] = (byte)newArray.getInt(1) ;
            stateVal[5] = (byte)newArray.getInt(2) ;
            newArray = newObject.getJSONArray("Mode 2");
            stateVal[6] = (byte)newArray.getInt(0) ;
            stateVal[7] = (byte)newArray.getInt(1) ;
            stateVal[8] = (byte)newArray.getInt(2) ;
            newArray = newObject.getJSONArray("Mode 3");
            stateVal[9] = (byte)newArray.getInt(0) ;
            stateVal[10] = (byte)newArray.getInt(1) ;
            stateVal[11] = (byte)newArray.getInt(2) ;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
