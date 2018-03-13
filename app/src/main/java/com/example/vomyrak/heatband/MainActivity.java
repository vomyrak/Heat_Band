package com.example.vomyrak.heatband;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import com.daimajia.numberprogressbar.NumberProgressBar;
import com.gc.materialdesign.views.Switch;
import org.adw.library.widgets.discreteseekbar.DiscreteSeekBar;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import com.gc.materialdesign.views.ButtonRectangle;


public class MainActivity extends AppCompatActivity {

    //Create (3 preferences + 1 temp) * 3 bytes array for storing temperature info data
    protected static byte[] stateVal = new byte[12];
    //Create a byte for battery info
    protected static byte batteryLife = (byte) 255;
    //Create an integer for seekbar progress;
    protected static int seekBarProgress = 50;
    protected static float tempVal;
    protected static int currentMode;
    //Create shared preference
    //Create UI elements
    protected NumberProgressBar progressBar;
    protected TextView tvBatteryLife;
    protected TextView tvTemperature;
    protected ButtonRectangle brMode1;
    protected ButtonRectangle brMode2;
    protected ButtonRectangle brMode3;
    protected ImageView ivBtConnected;
    protected ImageView ivBtSearching;
    protected ImageView applyChanges;
    protected ImageView ivTimerOn;
    protected ImageView ivTimerOff;
    protected TextView tempUnit;
    protected DiscreteSeekBar seekBar;
    protected ImageView ivBatteryLow;
    protected ImageView ivBatteryCharging;
    protected Switch mode1Switch;
    protected Switch mode2Switch;
    protected Switch mode3Switch;
    protected AlertDialog generalAlert;
    protected AlertDialog timerAlert;
    protected TextView tvDownCounter;


    //Create constant strings
    protected static final String mSettingStateVals = "stateVals";
    protected static final String mBatteryLife = "batteryLife";
    protected static final String mSeekBarProgress = "seekbarProgress";
    protected static final String mJsonFile = "Settings.json";
    protected static final String mOutgoingData = "OutgoingData";
    protected static String DEVICE_ADDRESS;
    protected static String DEVICE_NAME;


    //Create bluetooth adaptor
    protected static BluetoothAdapter bluetoothAdapter;
    protected static BluetoothSocket bluetoothSocket;
    protected static BluetoothDevice connectedDevice;
    protected static Set<BluetoothDevice> pairedDevices;
    protected static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    protected static String lastDeviceAddress;

    //A list of request codes
    protected static final int rRequestBt = 1;
    protected static final int rRequestZoneSetting = 2;
    protected static final int rRequestBtScan = 3;

    //Service Related
    MyBtService myBtService;

    //Temperature Unit
    protected static boolean dTempUnit = true;
    protected static boolean timerSet = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Initialise app variables and UI elements on creation of application
        super.onCreate(savedInstanceState);
        IntentFilter newFilter = new IntentFilter();
        newFilter.addAction("START_DISCOVERY");
        newFilter.addAction("CANCEL_DISCOVERY");
        newFilter.addAction("SET_TIMER_TEXT");
        this.registerReceiver(mReceiver, newFilter);
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
                if (!file.exists()){
                    // First time startup
                    for (int i = 0; i < stateVal.length; i++){
                        stateVal[i] = 0;
                    }
                    seekBarProgress = 4;
                    batteryLife = 100;
                    Intent startupIntent = new Intent(this, ScanActivity.class);
                    startActivityForResult(startupIntent, 1);
                }
            } catch (Exception e){
                finish();
            }
        }

        setContentView(R.layout.activity_main);
        seekBar = findViewById(R.id.temp_setter);
        progressBar =  findViewById(R.id.battery_life);
        tvBatteryLife =  findViewById(R.id.battery);
        tvTemperature = findViewById(R.id.current_temp);
        brMode1 = findViewById(R.id.mode_1);
        brMode2 = findViewById(R.id.mode_2);
        brMode3 = findViewById(R.id.mode_3);
        tempUnit = findViewById(R.id.temp_unit);
        ivBtConnected = findViewById(R.id.btConnected);
        ivBtSearching = findViewById(R.id.btSearching);
        applyChanges = findViewById(R.id.change);
        ivTimerOff = findViewById(R.id.timerOff);
        ivTimerOn = findViewById(R.id.timerOn);
        seekBar.setProgress(seekBarProgress);
        progressBar.setProgress(((int) batteryLife));
        ivBatteryLow = findViewById(R.id.batteryLow);
        ivBatteryCharging = findViewById(R.id.batteryCharging);
        mode1Switch = findViewById(R.id.switch1);
        mode2Switch = findViewById(R.id.switch2);
        mode3Switch = findViewById(R.id.switch3);
        brMode1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //OnClick of the button the floating activity is started
                Intent startFloatingActivity = new Intent(MainActivity.this,  FloatingActivity.class);
                startFloatingActivity.putExtra("Mode", 1);
                MainActivity.this.startActivityForResult(startFloatingActivity, rRequestZoneSetting);
            }
        });

        brMode2.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                Intent startFloatingActivity = new Intent(MainActivity.this,  FloatingActivity.class);
                startFloatingActivity.putExtra("Mode", 2);
                MainActivity.this.startActivityForResult(startFloatingActivity, rRequestZoneSetting);
            }

        });

        brMode3.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                Intent startFloatingActivity = new Intent(MainActivity.this,  FloatingActivity.class);
                startFloatingActivity.putExtra("Mode", 3);
                MainActivity.this.startActivityForResult(startFloatingActivity, rRequestZoneSetting);
            }
        });

        tempUnit.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                if(dTempUnit == true){
                    dTempUnit = false;
                    tempUnit.setText(R.string.Fahrenheit);
                }
                else{
                    dTempUnit = true;
                    tempUnit.setText(R.string.Celsius);
                }

            }
        });

        applyChanges.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                Toast.makeText(getApplicationContext(), "BT Name: "+DEVICE_NAME+"\nBT Address: "+DEVICE_ADDRESS, Toast.LENGTH_SHORT).show();
                //bluetoothSocket.getOutputStream().write("j255,0,255 ".getBytes());
                Intent sendIntent = new Intent("SEND_DATA");
                sendIntent.putExtra("data", "j255,0,255 ");
                sendBroadcast(sendIntent);
            }
        });



        ivTimerOff.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                if (!timerSet) {
                    final AlertDialog.Builder aBuilder = new AlertDialog.Builder(MainActivity.this);
                    LayoutInflater inflater = getLayoutInflater();
                    View dialogView = inflater.inflate(R.layout.number_picker, (ConstraintLayout) findViewById(R.id.coordinatorLayout), false);
                    aBuilder.setTitle("Select time");
                    aBuilder.setView(dialogView);
                    final NumberPicker numberPicker1 = dialogView.findViewById(R.id.number_picker);
                    final NumberPicker numberPicker2 = dialogView.findViewById(R.id.number_picker2);
                    final TextView tvTimer = dialogView.findViewById(R.id.timer_display);
                    dialogView = inflater.inflate(R.layout.timer_set, (ConstraintLayout) findViewById(R.id.coordinatorLayout), false);
                    numberPicker1.setMaxValue(11);
                    numberPicker2.setMaxValue(59);
                    numberPicker1.setMinValue(0);
                    numberPicker2.setMinValue(0);
                    numberPicker1.setWrapSelectorWheel(true);
                    numberPicker2.setWrapSelectorWheel(true);
                    String timerString = String.valueOf(numberPicker1.getValue()) + " hours " + String.valueOf(numberPicker2.getValue()) + " minutes";
                    tvTimer.setText(timerString);
                    numberPicker1.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
                        @Override
                        public void onValueChange(NumberPicker numberPicker, int i, int i1) {
                            String timerString = String.valueOf(i1) + " hours " + String.valueOf(numberPicker2.getValue()) + " minutes";
                            tvTimer.setText(timerString);
                        }
                    });
                    numberPicker2.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
                        @Override
                        public void onValueChange(NumberPicker numberPicker, int i, int i1) {
                            String timerString = String.valueOf(numberPicker1.getValue()) + " hours " + String.valueOf(i1) + " minutes";
                            tvTimer.setText(timerString);
                        }
                    });
                    aBuilder.setPositiveButton("Done", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Log.d("positive", "onClick: ");
                            Intent setTimerIntent = new Intent();
                            setTimerIntent.setAction("START_TIMER");
                            long temp = (numberPicker1.getValue() * 3600 + numberPicker2.getValue() * 60) * 1000;
                            setTimerIntent.putExtra("millisToFuture", temp);
                            temp = 1000;
                            setTimerIntent.putExtra("interval", temp);
                            sendBroadcast(setTimerIntent);
                            timerSet = true;
                            ivTimerOff.setVisibility(View.GONE);
                            ivTimerOn.setVisibility(View.VISIBLE);


                        }
                    });
                    aBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Log.d("Negative", "Onclick: ");

                        }
                    });
                    timerAlert = aBuilder.create();
                    timerAlert.show();

                }

            }
        });


        ivTimerOn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                final AlertDialog.Builder aBuilder = new AlertDialog.Builder(MainActivity.this);
                LayoutInflater inflater = getLayoutInflater();
                View dialogView = inflater.inflate(R.layout.timer_set, (ConstraintLayout) findViewById(R.id.coordinatorLayout), false);
                aBuilder.setTitle("Time Till Heater Turns off:");
                aBuilder.setView(dialogView);
                aBuilder.setNeutralButton("Reset", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Log.d("Reset Timer", "onClick: ");
                        Intent setTimerIntent = new Intent();
                        setTimerIntent.setAction("RESET_TIMER");
                        sendBroadcast(setTimerIntent);
                        timerSet = false;
                        ivTimerOn.setVisibility(View.GONE);
                        ivTimerOff.setVisibility(View.VISIBLE);

                    }
                });
                timerAlert = aBuilder.create();
                timerAlert.show();
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

        mode1Switch.setOncheckListener(new Switch.OnCheckListener() {
            @Override
            public void onCheck(Switch aSwitch, boolean isChecked) {
                if (isChecked){
                    changeActiveMode(1);
                    mode2Switch.setChecked(false);
                    mode3Switch.setChecked(false);
                }
                else{
                    changeActiveMode(0);
                }
            }
        });

        mode2Switch.setOncheckListener(new Switch.OnCheckListener() {
            @Override
            public void onCheck(Switch aSwitch, boolean isChecked) {
                if (isChecked){
                    changeActiveMode(2);
                    mode1Switch.setChecked(false);
                    mode3Switch.setChecked(false);
                }
                else{
                    changeActiveMode(0);
                }
            }
        });
        mode3Switch.setOncheckListener(new Switch.OnCheckListener() {
            @Override
            public void onCheck(Switch aSwitch, boolean isChecked) {
                if (isChecked){
                    changeActiveMode(3);
                    mode2Switch.setChecked(false);
                    mode1Switch.setChecked(false);
                }
                else{
                    changeActiveMode(0);
                }
            }
        });
    }

    protected void errorMessage(String error){
        final AlertDialog.Builder alertBuilder = new AlertDialog.Builder(MainActivity.this  );
        alertBuilder.setTitle("Notification")
                .setMessage(error)
                .setCancelable(false)
                .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent endServiceIntent = new Intent();
                        endServiceIntent.setAction("END_SERVICE");
                        sendBroadcast(endServiceIntent);
                        finish();
                    }
                });
        generalAlert = alertBuilder.create();
        generalAlert.show();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == rRequestBt){
            if (resultCode == 0){
                //errorMessage("The user decided to deny bluetooth access");
                ivBtConnected.setVisibility(View.INVISIBLE);
                ivBtSearching.setVisibility(View.INVISIBLE);
            }
            else{
                Intent intent = new Intent(this, MyBtService.class);
                startService(intent);
            }
        }
        else if (requestCode == rRequestZoneSetting){
            if (resultCode == RESULT_OK){
                saveFile();
            }
        }
        else if (requestCode == rRequestBtScan){
            if (resultCode == RESULT_OK) {
                Intent intent = new Intent(this, MyBtService.class);
                startService(intent);
            }
        }
    }



    @Override
    protected void onStart() {super.onStart();}

     private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            Toast.makeText(getApplicationContext(), "1", Toast.LENGTH_SHORT).show();
            if ("START_DISCOVERY".equals(action)) {
                bluetoothAdapter.startDiscovery();
                Intent startupIntent = new Intent(MainActivity.this, ScanActivity.class);
                startActivityForResult(startupIntent, 1);
            }
            else if ("CANCEL_DISCOVERY".equals(action)){
                bluetoothAdapter.cancelDiscovery();
            }
            else if ("SET_TIMER_TEXT".equals(action)){
                if (tvDownCounter != null) {
                    String data = intent.getStringExtra("data");
                    tvDownCounter.setText(data);
                }
            }
        }
    };

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
        this.unregisterReceiver(mReceiver);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putByteArray(mSettingStateVals, stateVal);
        outState.putByte(mBatteryLife, batteryLife);
        outState.putInt(mSeekBarProgress, seekBarProgress);
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
            errorMessage("The user decided to deny bluetooth access");
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
            newObject.put("Bt Address", lastDeviceAddress);
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
            lastDeviceAddress = newObject.getString("Bt Address");
            batteryLife = (byte)newObject.getInt("Battery");
            seekBarProgress = (byte)newObject.getInt("Seekbar");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void changeActiveMode(int newMode){
        if (newMode == currentMode){
            if ((!mode1Switch.isCheck()) && (!mode2Switch.isCheck()) && (!mode3Switch.isCheck())) {
                currentMode = 0;
                //TODO send op code to turn off heating
            }
        }
        else{
            currentMode = newMode;
            if (myBtService != null) {
                Intent sendIntent = new Intent("SEND_DATA");
                sendIntent.putExtra("data", "q" + String.valueOf(newMode));
                sendBroadcast(sendIntent);
            }
        }
    }
}
