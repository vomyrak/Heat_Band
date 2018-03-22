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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.daimajia.numberprogressbar.NumberProgressBar;
import com.daimajia.numberprogressbar.OnProgressBarListener;
import com.gc.materialdesign.views.Switch;
import org.adw.library.widgets.discreteseekbar.DiscreteSeekBar;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.UUID;
import com.gc.materialdesign.views.ButtonRectangle;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;


public class MainActivity extends AppCompatActivity {

    //Declaration of various data variables
    protected static int[] stateVal = new int[12];
    protected static int batteryLife = 100;
    protected static int seekBarProgress = 0;
    protected static final int PLOT_ARRAY_SIZE = 500;
    protected static float tempVal;
    protected static int currentMode;
    protected static int[] zoneTemperature = new int[3];
    protected static int[] individualCellBattery = new int[3];
    protected static DataPoint[] tempHistory  = new DataPoint[PLOT_ARRAY_SIZE];
    protected LineGraphSeries<DataPoint> series;
    protected static int axisCount = -1;
    protected static int dataPointOffset = 0;

    //Declaration of UI elements
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
    protected GraphView graphView;
    protected TextView weatherMax;
    protected TextView weatherMin;
    protected TextView humidityView;
    protected ConstraintLayout temperatureView;
    protected ConstraintLayout lowerLayer;
    protected ImageView weatherIcon;
    protected TextView windView;


    //Create constant strings
    protected static final String mSettingStateVals = "stateVals";
    protected static final String mBatteryLife = "batteryLife";
    protected static final String mSeekBarProgress = "seekbarProgress";
    protected static final String mJsonFile = "Settings.json";
    protected static final String mOutgoingData = "OutgoingData";
    protected static boolean lowBatRegion = false;
    protected static String DEVICE_ADDRESS;
    protected static String DEVICE_NAME;
    private static String BASE_API = "http://api.openweathermap.org/data/2.5/weather?q=London,uk";
    private static final String API_KEY = "3893f595f8660f725fb84e9a8d9a0e5d";

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


    //Temperature Unit
    protected static boolean dTempUnit = true;
    protected static boolean timerSet = false;

    //Weather related variables
    protected static String actualWeather;
    protected static int humidity;
    protected static float wind;
    protected static int tempMin;
    protected static int tempMax;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Initialise app variables and UI elements on creation of application
        super.onCreate(savedInstanceState);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (!bluetoothAdapter.isEnabled()){
                Intent enableBT = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBT, 9);
            }
        IntentFilter newFilter = new IntentFilter();
        newFilter.addAction("START_DISCOVERY");
        newFilter.addAction("CANCEL_DISCOVERY");
        newFilter.addAction("TIME_UP");
        newFilter.addAction("GRAPH_UPDATE");
        newFilter.addAction("UPDATE_BT_STATUS");
        newFilter.addAction("ENABLE_BT");
        newFilter.addAction("UPDATE_GRAPH");
        newFilter.addAction("UPDATE_TEMPERATURE");
        newFilter.addAction("NOTIFY_BT_DISCONNECTION");
        this.registerReceiver(mReceiver, newFilter);
        if (savedInstanceState != null){
            //If saved instance is present, get value from previously set instance state
            if (savedInstanceState.containsKey(mSettingStateVals)){
                stateVal = savedInstanceState.getIntArray(mSettingStateVals);
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
                    seekBarProgress = 0;
                    batteryLife = 78;

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
        ivBatteryLow.setVisibility(View.INVISIBLE);
        mode1Switch = findViewById(R.id.switch1);
        mode2Switch = findViewById(R.id.switch2);
        mode3Switch = findViewById(R.id.switch3);
        graphView = findViewById(R.id.graph);
        weatherMax = findViewById(R.id.weather_max);
        weatherMin = findViewById(R.id.weather_min);
        temperatureView = findViewById(R.id.top_half);
        humidityView = findViewById(R.id.weather_humidity);
        lowerLayer = findViewById(R.id.lower_layer);
        weatherIcon = findViewById(R.id.weather_icon);
        windView = findViewById(R.id.weather_wind);
        for (int i = 0; i < PLOT_ARRAY_SIZE; i++){
            DataPoint tempData = new DataPoint(i, 0);
            tempHistory[i] = tempData;
            axisCount++;
        }
        series = new LineGraphSeries<>(tempHistory);
        graphView.getViewport().setXAxisBoundsManual(true);
        graphView.getViewport().setMinX(0);
        graphView.getViewport().setMaxX(PLOT_ARRAY_SIZE);
        graphView.getViewport().setYAxisBoundsManual(false);
        graphView.getViewport().setMinY(0);
        graphView.getViewport().setMaxY(10);
        graphView.getGridLabelRenderer().setHorizontalLabelsColor(Color.WHITE);
        graphView.getGridLabelRenderer().setVerticalLabelsColor(Color.WHITE);
        graphView.getGridLabelRenderer().setVerticalAxisTitle("Temperature/°C");
        graphView.getGridLabelRenderer().setVerticalAxisTitleColor(Color.WHITE);
        graphView.getGridLabelRenderer().setVerticalAxisTitleTextSize(35);
        graphView.getGridLabelRenderer().setGridStyle(GridLabelRenderer.GridStyle.HORIZONTAL);
        graphView.getGridLabelRenderer().setGridColor(Color.WHITE);
        graphView.getGridLabelRenderer().setHorizontalLabelsVisible(false);
        graphView.addSeries(series);
        series.setThickness(3);
        series.setColor(Color.WHITE);
        series.setDrawDataPoints(false);
        series.setDrawBackground(true);
        series.setBackgroundColor(getResources().getColor(R.color.graph_transparent));
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3);
        paint.setColor(Color.WHITE);
        series.setCustomPaint(paint);

    }

    //Auxilliary function for text display
    protected final class MyTextView{
        final private TextView myTextView;
        private BroadcastReceiver mBrocastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action.equals("SET_TIMER_TEXT")){
                    String data = intent.getStringExtra("data");
                    myTextView.setText(data);
                }
            }
        };
        MyTextView(TextView textView){
            IntentFilter filter = new IntentFilter();
            filter.addAction("SET_TIMER_TEXT");
            myTextView = textView;
            MainActivity.this.registerReceiver(mBrocastReceiver, filter);
        }
        public void unregisterReceiver(){
            try {
                MainActivity.this.unregisterReceiver(mBrocastReceiver);
            } catch (IllegalArgumentException e){
                e.printStackTrace();
            }
        }
    }

    //Write error message and terminate programme
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


    //Specify action in response to specific result code
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
        else if (requestCode == 9){
            if (resultCode != RESULT_OK){
                Toast.makeText(getApplicationContext(), "User denies Bluetooth Access", Toast.LENGTH_SHORT);
                finish();
            }
        }
    }

    //Load remote weather data
    private void loadWeatherData(){
        String currentLocation = BASE_API;
        new FetchWeatherTask().execute(currentLocation);
    }

    //Fetch remote weather data
    public static String getResponseFromHttpUrl(URL url) throws IOException {
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        try {
            InputStream in = urlConnection.getInputStream();

            Scanner scanner = new Scanner(in);
            scanner.useDelimiter("\\A");

            boolean hasInput = scanner.hasNext();
            if (hasInput) {
                return scanner.next();
            } else {
                return null;
            }
        } finally {
            urlConnection.disconnect();
        }
    }
    public class FetchWeatherTask extends AsyncTask<String, Void, String[]>{


        @Override
        protected String[] doInBackground(String... strings) {
            if (strings.length == 0){
                return null;
            }
            String location = strings[0] + "&appid=3893f595f8660f725fb84e9a8d9a0e5d";
            Uri weatherRequestUrl = Uri.parse(location).buildUpon().build();
            URL url = null;
            try{
                url = new URL(weatherRequestUrl.toString());
            } catch (MalformedURLException e){
                e.printStackTrace();
            }
            try{
                String response = getResponseFromHttpUrl(url);
                return new String[]{response};
            } catch (Exception e){
                e.printStackTrace();
            } return null;
        }

        @Override
        protected void onPostExecute(String[] strings) {
            if (strings != null){
                try {
                    JSONObject newObject = new JSONObject(strings[0]);
                    JSONArray weather = newObject.getJSONArray("weather");
                    JSONObject mainWeather = weather.getJSONObject(0);
                    actualWeather = mainWeather.getString("main");
                    final String wIcon = mainWeather.getString("icon");
                    // TODO to implement icon thing
                    JSONObject getMain = newObject.getJSONObject("main");
                    humidity = Integer.parseInt(getMain.getString("humidity"));
                    String humidityString = "Humidity: " + humidity + "%";
                    humidityView.setText(humidityString);
                    tempMin = (int)(Float.parseFloat(getMain.getString("temp_min")) - 273.15);
                    tempMax = (int)(Float.parseFloat(getMain.getString("temp_max")) - 273.15);
                    String minString = "/"+String.valueOf(tempMin) + "°";
                    String maxString = String.valueOf(tempMax) + "°";
                    weatherMax.setText(maxString);
                    weatherMin.setText(minString);
                    JSONObject windObject = newObject.getJSONObject("wind");
                    wind = Float.parseFloat(windObject.getString("speed"));
                    String windString = "Wind: " + wind + "m/s";
                    windView.setText(windString);
                    String imageAddress = "http://openweathermap.org/img/w/" + wIcon + ".png";
                    class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
                        ImageView bmImage;
                        public DownloadImageTask(ImageView bmImage) {
                            this.bmImage = bmImage;
                        }

                        protected Bitmap doInBackground(String... urls) {
                            String urldisplay = urls[0];
                            Bitmap mIcon11 = null;
                            try {
                                InputStream in = new java.net.URL(urldisplay).openStream();
                                mIcon11 = BitmapFactory.decodeStream(in);
                            } catch (Exception e) {
                                Log.e("Error", e.getMessage());
                                e.printStackTrace();
                            }
                            return mIcon11;
                        }

                        protected void onPostExecute(Bitmap result) {
                            bmImage.setImageBitmap(result);
                        }
                    }
                    new DownloadImageTask(weatherIcon).execute(imageAddress);

                    Log.d("weather", "onPostExecute: " + weather);
                } catch (JSONException e) {
                    e.printStackTrace();
                } finally {

                }
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        final Handler handler = new Handler();
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (batteryLife > 0) {
                    batteryLife--;
                }
                progressBar.setProgress(batteryLife);
                if (batteryLife < 20){
                    ivBatteryLow.setVisibility(View.VISIBLE);
                }
                else{
                    ivBatteryLow.setVisibility(View.INVISIBLE);
                }
                handler.postDelayed(this, 180000);
            }
        };
        Thread thread = new Thread(runnable);
        thread.run();
        loadWeatherData();
    }

    //Conversion from celcius to Fahrenheit
    public double toFahrenheit(float celcius){
        return celcius * 1.8 + 32;
    }

    //Declaratoin of broadcast receiver
     private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            if ("START_DISCOVERY".equals(action)) {
                bluetoothAdapter.startDiscovery();
                Intent startupIntent = new Intent(MainActivity.this, ScanActivity.class);
                startActivityForResult(startupIntent, 1);
            }
            else if ("CANCEL_DISCOVERY".equals(action)){
                bluetoothAdapter.cancelDiscovery();
            }
            else if ("TIME_UP".equals(action)){

                timerSet = false;
                ivTimerOn.setVisibility(View.GONE);
                ivTimerOff.setVisibility(View.VISIBLE);
            }


            else if ("UPDATE_BT_STATUS".equals(action)){
                boolean isConnected = intent.getBooleanExtra("Connected?", false);
                if(isConnected){
                    ivBtConnected.setVisibility(View.VISIBLE);
                    ivBtSearching.setVisibility(View.INVISIBLE);
                }
                else{
                    ivBtConnected.setVisibility(View.INVISIBLE);
                    ivBtSearching.setVisibility(View.VISIBLE);
                }
            }
            else if ("ENABLE_BT".equals(action)){
                Intent enableBT = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBT, 9);
            }
            else if ("UPDATE_GRAPH".equals(action)){
                float newData = intent.getFloatExtra("data", 0);
                try {
                    series.appendData(new DataPoint(graphView.getViewport().getMaxX(false) + 1 + dataPointOffset, dTempUnit? newData : toFahrenheit(newData)), false, 2000);
                    graphView.getViewport().setMinX(graphView.getViewport().getMinX(false) + 1);
                    graphView.getViewport().setMaxX(graphView.getViewport().getMaxX(false) + 1);
                } catch (IllegalArgumentException e){
                    e.printStackTrace();
                    dataPointOffset++;
                }
            }
            else if ("UPDATE_TEMPERATURE".equals(action)){
                if (dTempUnit) {
                    tvTemperature.setText(String.valueOf(new DecimalFormat("#.#").format(tempVal)));
                }
                else{
                    tvTemperature.setText(String.valueOf(new DecimalFormat("#,#").format(toFahrenheit(tempVal))));
                }
            }
            else if ("NOTIFY_BT_DISCONNECTION".equals(action)){
                ivBtSearching.setVisibility(View.VISIBLE);
                ivBtConnected.setVisibility(View.INVISIBLE);
            }

        }
    };

    @Override
    protected void onResume(){
        super.onResume();
        loadFile();

        bindListeners();
        Intent UpdateProgress = getIntent();
        int mode = UpdateProgress.getIntExtra("Mode", 0);
        byte NewData[] = UpdateProgress.getByteArrayExtra("New Progress");
        if(mode!=0){
            int offset = mode * 3;
            stateVal[offset] = NewData[0];
            stateVal[offset + 1] = NewData[1];
            stateVal[offset + 2] = NewData[2];
        }
        requestDeviceSync();
    }

    //Request for device data update
    private void requestDeviceSync(){
        Intent requestIntent = new Intent();
        requestIntent.setAction("SEND_DATA");
        requestIntent.putExtra("data", "m0,0,0 ");
    }

    //Bind user interface elements with function to execute
    private void bindListeners(){
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
                    tvTemperature.setText(String.valueOf(toFahrenheit(tempVal)));
                    weatherMax.setText(String.valueOf(toFahrenheit(tempMax)) + "°");
                    weatherMin.setText("/"+String.valueOf(toFahrenheit(tempMin)) + "°");
                    weatherMax.setTextSize(26);
                    weatherMin.setTextSize(17);
                    graphView.getGridLabelRenderer().setVerticalAxisTitle("Temperature/°F");
                }
                else{
                    dTempUnit = true;
                    tempUnit.setText(R.string.Celsius);
                    tvTemperature.setText(String.valueOf(tempVal));
                    weatherMax.setText(String.valueOf(tempMax) + "°");
                    weatherMin.setText("/" + String.valueOf(tempMin) + "°");
                    weatherMax.setTextSize(40);
                    weatherMin.setTextSize(30);
                    graphView.getGridLabelRenderer().setVerticalAxisTitle("Temperature/°C");

                }

            }
        });

        applyChanges.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {

                Intent sendIntent = new Intent("SEND_DATA");
                sendIntent.putExtra("data", "n" + stateVal[3] + "," + stateVal[4] + "," + stateVal[5] + " ");
                sendBroadcast(sendIntent);
/*
                sendIntent = new Intent("SEND_DATA");
                sendIntent.putExtra("data", "o" + stateVal[6] + "," + stateVal[7] + "," + stateVal[8] + " ");
                sendBroadcast(sendIntent);

                sendIntent = new Intent("SEND_DATA");
                sendIntent.putExtra("data", "p" + stateVal[9] + "," + stateVal[10] + "," + stateVal[11] + " ");
                sendBroadcast(sendIntent);*/
            }
        });



        ivTimerOff.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                if (!timerSet) {
                    final AlertDialog.Builder aBuilder = new AlertDialog.Builder(MainActivity.this);
                    LayoutInflater inflater = getLayoutInflater();
                    View dialogView = inflater.inflate(R.layout.number_picker, (RelativeLayout) findViewById(R.id.coordinatorLayout), false);
                    aBuilder.setTitle("Select time");
                    aBuilder.setView(dialogView);
                    final NumberPicker numberPicker1 = dialogView.findViewById(R.id.number_picker);
                    final NumberPicker numberPicker2 = dialogView.findViewById(R.id.number_picker2);
                    final NumberPicker numberPicker3 = dialogView.findViewById(R.id.number_picker3);
                    final TextView tvTimer = dialogView.findViewById(R.id.timer_display);
                    numberPicker1.setMaxValue(11);
                    numberPicker2.setMaxValue(59);
                    numberPicker3.setMaxValue(59);
                    numberPicker1.setMinValue(0);
                    numberPicker2.setMinValue(0);
                    numberPicker3.setMinValue(0);
                    numberPicker1.setWrapSelectorWheel(true);
                    numberPicker2.setWrapSelectorWheel(true);
                    numberPicker3.setWrapSelectorWheel(true);
                    String timerString = String.valueOf(numberPicker1.getValue()) + " hours " + String.valueOf(numberPicker2.getValue()) + " minutes " + String.valueOf(numberPicker3.getValue()) + " seconds ";
                    tvTimer.setText(timerString);
                    numberPicker1.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
                        @Override
                        public void onValueChange(NumberPicker numberPicker, int i, int i1) {
                            String timerString = String.valueOf(i1) + " hours " + String.valueOf(numberPicker2.getValue()) + " minutes " + String.valueOf(numberPicker3.getValue()) + " seconds";
                            tvTimer.setText(timerString);
                        }
                    });
                    numberPicker2.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
                        @Override
                        public void onValueChange(NumberPicker numberPicker, int i, int i1) {
                            String timerString = String.valueOf(numberPicker1.getValue()) + " hours " + String.valueOf(i1) + " minutes " + String.valueOf(numberPicker3.getValue()) + " seconds";
                            tvTimer.setText(timerString);
                        }
                    });
                    numberPicker3.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
                        @Override
                        public void onValueChange(NumberPicker numberPicker, int i, int i1) {
                            String timerString = String.valueOf(numberPicker1.getValue()) + " hours " + String.valueOf(numberPicker2.getValue()) + " minutes " + String.valueOf(i1) + " seconds";
                            tvTimer.setText(timerString);
                        }
                    });
                    aBuilder.setPositiveButton("Done", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Log.d("positive", "onClick: ");
                            Intent setTimerIntent = new Intent();
                            setTimerIntent.setAction("START_TIMER");
                            long temp = (numberPicker1.getValue() * 3600 + numberPicker2.getValue() * 60 + numberPicker3.getValue()) * 1000;
                            setTimerIntent.putExtra("millisToFuture", temp);
                            temp = 1000;
                            setTimerIntent.putExtra("interval", temp);
                            sendBroadcast(setTimerIntent);
                            Intent sendIntent = new Intent("SEND_DATA");
                            sendIntent.putExtra("data", "u" + String.valueOf(numberPicker1.getValue())
                                    + "," + String.valueOf(numberPicker2.getValue()) + ",0, ");
                            sendBroadcast(sendIntent);
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
        ivBtSearching.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    Intent resetBtIntent = new Intent();
                    resetBtIntent.setAction("RESET_BT");
                    sendBroadcast(resetBtIntent);
                }
                return true;
            }
        });
        ivBtConnected.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    Intent resetBtIntent = new Intent();
                    resetBtIntent.setAction("RESET_BT");
                    sendBroadcast(resetBtIntent);
                }
                return true;
            }
        });
        ivTimerOn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                final AlertDialog.Builder aBuilder = new AlertDialog.Builder(MainActivity.this);
                LayoutInflater inflater = getLayoutInflater();
                View dialogView = inflater.inflate(R.layout.timer_set, (RelativeLayout) findViewById(R.id.coordinatorLayout), false);
                final MyTextView tvDownCounter = new MyTextView((TextView)dialogView.findViewById(R.id.count_down));
                aBuilder.setTitle("Time Till Heater Turns off:");
                aBuilder.setView(dialogView);
                aBuilder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        tvDownCounter.unregisterReceiver();
                    }
                });
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
        progressBar.setOnProgressBarListener(new OnProgressBarListener() {
            @Override
            public void onProgressChange(int current, int max) {
                if (current <= 20 && !lowBatRegion){
                    Intent notificationIntent = new Intent();
                    notificationIntent.setAction("BAT_LOW");
                    sendBroadcast(notificationIntent);
                    lowBatRegion = true;
                    ivBatteryLow.setVisibility(View.VISIBLE);
                }
                else if (current > 20){
                    ivBatteryLow.setVisibility(View.INVISIBLE);
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

        temperatureView.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                if (lowerLayer.getVisibility() != View.VISIBLE){

                    lowerLayer.setVisibility(View.VISIBLE);
                    setViewGroupClickable(lowerLayer, true);
                    TranslateAnimation animate = new TranslateAnimation(0, 0, lowerLayer.getHeight(), 0);
                    animate.setDuration(500);
                    animate.setFillAfter(true);
                    lowerLayer.startAnimation(animate);

                }
                else{

                    TranslateAnimation animate = new TranslateAnimation(0, 0, 0,lowerLayer.getHeight());
                    animate.setDuration(500);
                    animate.setFillAfter(true);
                    animate.setAnimationListener(new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {

                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            setViewGroupClickable(lowerLayer, false);
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {

                        }
                    });
                    lowerLayer.startAnimation(animate);
                    lowerLayer.setVisibility(View.INVISIBLE);


                }

            }
        });
    }

    @Override
    protected void onPause(){
        super.onPause();
    }

    //Activate/Deactivate UI elements
    protected void setViewGroupClickable(View view, boolean clickable){
        view.setEnabled(clickable);
        if (view instanceof ViewGroup){
            ViewGroup viewGroup = (ViewGroup) view;
            int childCount = viewGroup.getChildCount();
            for (int i = 0; i < childCount; i++){
                setViewGroupClickable(viewGroup.getChildAt(i), clickable);
            }
        }
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
        if (bluetoothSocket != null) {
            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //Temporarily save current device setting when paused
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putIntArray(mSettingStateVals, stateVal);
        outState.putInt(mBatteryLife, batteryLife);
        outState.putInt(mSeekBarProgress, seekBarProgress);
    }

    //Json Settings Handler
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
            newArray.put(stateVal[0]);
            newArray.put(stateVal[1]);
            newArray.put(stateVal[2]);
            newObject.put("Current Profile", newArray);
            newArray = new JSONArray();
            newArray.put(stateVal[3]);
            newArray.put(stateVal[4]);
            newArray.put(stateVal[5]);
            newObject.put("Mode 1", newArray);
            newArray = new JSONArray();
            newArray.put(stateVal[6]);
            newArray.put(stateVal[7]);
            newArray.put(stateVal[8]);
            newObject.put("Mode 2", newArray);
            newArray = new JSONArray();
            newArray.put(stateVal[9]);
            newArray.put(stateVal[10]);
            newArray.put(stateVal[11]);
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
    public void readJsonStream(DataInputStream in){
        try{
            String inputString = in.readUTF();
            Log.d("Get json", inputString);
            JSONObject newObject = new JSONObject(inputString);
            JSONArray newArray = newObject.getJSONArray("Current Profile");
            stateVal[0] = newArray.getInt(0) ;
            stateVal[1] = newArray.getInt(1) ;
            stateVal[2] = newArray.getInt(2) ;
            newArray = newObject.getJSONArray("Mode 1");
            stateVal[3] = newArray.getInt(0) ;
            stateVal[4] = newArray.getInt(1) ;
            stateVal[5] = newArray.getInt(2) ;
            newArray = newObject.getJSONArray("Mode 2");
            stateVal[6] = newArray.getInt(0) ;
            stateVal[7] = newArray.getInt(1) ;
            stateVal[8] = newArray.getInt(2) ;
            newArray = newObject.getJSONArray("Mode 3");
            stateVal[9] =  newArray.getInt(0) ;
            stateVal[10] = newArray.getInt(1) ;
            stateVal[11] = newArray.getInt(2) ;
            lastDeviceAddress = newObject.getString("Bt Address");
            batteryLife = (byte)newObject.getInt("Battery");
            seekBarProgress = (byte)newObject.getInt("Seekbar");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //Set hardware mode
    public void changeActiveMode(int newMode){
        if (newMode == 0){
            currentMode = 0;
            Intent sendIntent = new Intent("SEND_DATA");
            sendIntent.putExtra("data", "q0,0,0 ");
            sendBroadcast(sendIntent);
        }
        else if (newMode != currentMode){
            currentMode = newMode;
                Intent sendIntent = new Intent("SEND_DATA");
                sendIntent.putExtra("data", "q" + String.valueOf(newMode) + ",0,0 ");
                sendBroadcast(sendIntent);
        }
    }
}
