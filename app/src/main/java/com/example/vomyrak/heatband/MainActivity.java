package com.example.vomyrak.heatband;

import android.bluetooth.BluetoothAdapter;
import android.content.ClipData;
import android.content.Intent;
import android.content.SharedPreferences;
import android.icu.util.Output;
import android.os.Bundle;
import android.sax.StartElementListener;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.JsonWriter;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import org.w3c.dom.Text;

import java.io.File;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import static java.lang.System.out;

// TODO(1) Re-assignment of widget ids
// TODO(2) Add a widget to display temperature

public class MainActivity extends AppCompatActivity {

    //Create (3 preferences + 1 temp) * 3 bytes array for storing temperature info data
    private byte[] stateVal = new byte[12];
    //Create a byte for battery info
    private byte batteryLife;
    //Create an integer for seekbar progress;
    private int seekBarProgress;
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
        select1 = (Button) findViewById(R.id.select_1);
        edit1 = (Button) findViewById(R.id.edit_1);
        select2 = (Button) findViewById(R.id.select_2);
        edit2 = (Button) findViewById(R.id.edit_2);
        select3 = (Button) findViewById(R.id.select_3);
        edit3 = (Button) findViewById(R.id.edit_3);
        //TODO(3) set onClickListeners for all widgets
        select1.setOnClickListener(new View.OnClickListener() {
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
}
