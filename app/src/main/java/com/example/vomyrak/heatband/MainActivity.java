package com.example.vomyrak.heatband;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.JsonReader;
import android.util.JsonWriter;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

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

    //Create bluetooth adaptor
    private BluetoothAdapter bluetoothAdapter;

    //A list of request codes
    protected static final int rRequestBt = 1;

    //On click listener


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Initialise app variables and UI elements on creation of application
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null){
            //If saved intance is present, get value from previously set instance state
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
                MainActivity.this.startActivity(startFloatingActivity);
            }
        });
        seekBar.setProgress(seekBarProgress);
        progressBar.setProgress(((int) batteryLife));
    }

    @Override
    protected void onStart() {
        super.onStart();
        //onStart, UI elements are associated with variables
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        //if (bluetoothAdapter == null) {
        //bluetooth is not supported

        //}
        try {
            if (!bluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                this.startActivityForResult(enableBtIntent, rRequestBt);
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
            //finish();
        }
    }



    @Override
    protected void onResume(){
        super.onResume();
    }

    @Override
    protected void onPause(){
        super.onPause();
    }

    @Override
    protected void onStop(){
        super.onStop();
        //Add shared preference settings
        settings = getSharedPreferences(mPreferenceFile, 0);
        editor = settings.edit();
        editor.putInt(mBatteryLife, (int) batteryLife);
        editor.putInt(mSeekBarProgress, seekBarProgress);


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
    public void writeJsonStream(OutputStream out, String a) {
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
