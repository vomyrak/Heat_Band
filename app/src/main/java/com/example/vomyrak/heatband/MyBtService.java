package com.example.vomyrak.heatband;

import android.app.Activity;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelUuid;
import android.os.Process;
import android.os.ResultReceiver;
import android.os.health.ServiceHealthStats;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import static com.example.vomyrak.heatband.MainActivity.dTempUnit;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import javax.net.ssl.HandshakeCompletedEvent;

import static com.example.vomyrak.heatband.MainActivity.DEVICE_ADDRESS;
import static com.example.vomyrak.heatband.MainActivity.DEVICE_NAME;
import static com.example.vomyrak.heatband.MainActivity.batteryLife;
import static com.example.vomyrak.heatband.MainActivity.bluetoothAdapter;
import static com.example.vomyrak.heatband.MainActivity.bluetoothSocket;
import static com.example.vomyrak.heatband.MainActivity.connectedDevice;
import static com.example.vomyrak.heatband.MainActivity.mBatteryLife;
import static com.example.vomyrak.heatband.MainActivity.myUUID;
import static com.example.vomyrak.heatband.MainActivity.pairedDevices;
import static com.example.vomyrak.heatband.MainActivity.mOutgoingData;
import static com.example.vomyrak.heatband.MainActivity.zoneTemperature;
import static com.example.vomyrak.heatband.MainActivity.rRequestBt;
import static com.example.vomyrak.heatband.MainActivity.stateVal;
import static com.example.vomyrak.heatband.MainActivity.tempVal;
import static com.example.vomyrak.heatband.MainActivity.currentMode;
import static com.example.vomyrak.heatband.MainActivity.lastDeviceAddress;
import static com.example.vomyrak.heatband.MainActivity.individualCellBattery;

public class MyBtService extends IntentService {
    protected static final String TAG ="MyBtService";
    protected InputStream btIn;
    protected OutputStream btOut;
    protected Thread btThread;
    protected Handler timerHandler;
    protected WritingThread writingThread;
    protected Thread listeningThread;
    protected Thread timerThread;
    protected CountDownTimer countDownTimer;
    protected static final String mConfig = "Config";
    protected static int serviceId;
    protected static final String CHANNEL_ID = "Heatband";
    private NotificationManager mNotificationManager;


    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals("SEND_DATA")){
               String data = intent.getStringExtra("data");
               writingThread.handler.sendMessage(getMessage(data));
            }
            else if (action.equals("TURN_OFF")){
                writingThread.handler.sendMessage(getMessage("t0,0,0 "));
            }
            else if (action.equals("END_SERVICE")){
                unregisterReceiver(mReceiver);
                if (bluetoothAdapter.isDiscovering()){
                    bluetoothAdapter.cancelDiscovery();
                }
                stopSelf();
            }
            else if (action.equals("START_TIMER")){
                long millisToFuture = intent.getLongExtra("millisToFuture", 0);
                long interval = intent.getLongExtra("interval", 0);
                timerThread = new Thread(new TimerThread(millisToFuture, interval));
                timerThread.run();
            }
            else if (action.equals("RESET_TIMER")){
                timerThread.interrupt();
                countDownTimer.cancel();
                timerThread = null;
            }
            else if (action.equals("BAT_LOW")){
                NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                        .setContentTitle("Heat Band")
                        .setSmallIcon(R.drawable.logo2_small)
                        .setContentText(String.format("Your device battery is running low. Connect to power as soon as possible!"))
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                mNotificationManager.notify(1, mBuilder.build());
            }
            else if (action.equals("RESET_BT")){
                resetBluetooth(true);
            }
        }
    };

    private Message getMessage(String data){
        Bundle aBundle = new Bundle();
        aBundle.putString(mOutgoingData, data);
        Message newMessage = Message.obtain();
        newMessage.setData(aBundle);
        return newMessage;
    }
    public class BluetoothThread implements Runnable{
        int serviceId;
        BluetoothThread(int serviceId){
            this.serviceId = serviceId;
            timerHandler = new Handler();
        }
        @Override
        public void run() {
            writingThread = new WritingThread();
            writingThread.start();
            listeningThread = new Thread(new ListeningThread());
            listeningThread.run();
            IntentFilter btServiceFilter = new IntentFilter();
            btServiceFilter.addAction("SEND_DATA");
            btServiceFilter.addAction("BAT_LOW");
            btServiceFilter.addAction("START_TIMER");
            btServiceFilter.addAction("RESET_TIMER");
            btServiceFilter.addAction("END_SERVICE");
            btServiceFilter.addAction("TURN_OFF");
            btServiceFilter.addAction("RESET_BT");
            MyBtService.this.registerReceiver(mReceiver, btServiceFilter);
        }

    }

    public MyBtService() {
        super(TAG);
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
    protected void onHandleIntent(Intent intent){
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        Log.v(TAG, "In onstartCommand");
        registerNotification();

        serviceId = startId;

        btThread = new Thread(new BluetoothThread(startId));
        btThread.run();

        return START_STICKY;
    }

    @Override
    public void onCreate(){
        super.onCreate();
        Log.v(TAG, "in Oncreate");
        //Initialise Bluetooth
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null){
            Toast.makeText(getApplicationContext(), "Bluetooth not supported", Toast.LENGTH_SHORT).show();
        }
        resetBluetooth(false);
    }

    public void sendBtData(byte[] data){
        try {
            btOut.write(data);
            Log.d(TAG, data.toString());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NullPointerException f){
            Toast.makeText(getApplicationContext(), "No successful connection" , Toast.LENGTH_SHORT).show();
        }
    }
    public String receiveBtData(byte[] buffer) throws IOException, NullPointerException{
        if (btIn.available() > 0) {
            while (btIn.read() != (int)'?'){}
            int length = btIn.read(buffer);
            return getBtData(new String(buffer, 0, length));
        }
        else{
            return "";
        }
    }

    protected String getBtData(String data){
        if (data.length() < 3){
            return "";
        }
        for (int i = 0; i < data.length(); i++)
            if (data.charAt(i) != "!".charAt(0)) {}
            else{
            return data.substring(0, i);
        }
        return "";
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.unregisterReceiver(mReceiver);
        try {
            bluetoothSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void resetBluetooth(boolean fullReset){
        if (fullReset) {
            bluetoothAdapter = null;
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (!bluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                enableBtIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getApplicationContext().startActivity(enableBtIntent);
            }
        }
        if (bluetoothSocket == null) {

                try {
                    if(lastDeviceAddress == null) {
                        pairedDevices = bluetoothAdapter.getBondedDevices();
                        if (pairedDevices.size() > 0) {
                            for (BluetoothDevice bt : pairedDevices) {
                                if (matchedUUID(bt)) {
                                    DEVICE_ADDRESS = bt.getAddress();
                                    DEVICE_NAME = bt.getName();
                                    break;
                                } else {
                                    //No matched devices
                                }
                            }
                        }
                    }
                    else{
                        DEVICE_ADDRESS = lastDeviceAddress;
                    }
                    try {
                        establishConnection();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                }


        }
        else{
            if(!bluetoothSocket.isConnected()){
                try {
                    establishConnection();
                } catch (IOException e){
                    e.printStackTrace();
                }
            }
        }
    }


    private void establishConnection() throws IOException{
        connectedDevice = bluetoothAdapter.getRemoteDevice(DEVICE_ADDRESS);
        bluetoothSocket = connectedDevice.createRfcommSocketToServiceRecord(myUUID);
        bluetoothSocket.connect();
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        btIn = bluetoothSocket.getInputStream();
        btOut = bluetoothSocket.getOutputStream();
        lastDeviceAddress = DEVICE_ADDRESS;
        Intent updateBtStatusIntent = new Intent();
        updateBtStatusIntent.setAction("UPDATE_BT_STATUS");
        updateBtStatusIntent.putExtra("Connected?", true);
        sendBroadcast(updateBtStatusIntent);
    }
    protected boolean decodeMessage(String data){
        char opcode = data.charAt(0);
        data = data.substring(1);
        String[] dataArray = data.split(",");
        if (dataArray.length != 3){
            return false;
        }
        Log.d(TAG, String.valueOf(opcode));
        Log.d(TAG, data);
        // TODO to implement decoding mechanism

        switch (opcode){
            case 'a':
                stateVal[3] = Integer.parseInt(dataArray[0]);
                stateVal[4] = Integer.parseInt(dataArray[1]);
                stateVal[5] = Integer.parseInt(dataArray[2]);
                return true;
            case 'b':
                stateVal[6] = Integer.parseInt(dataArray[0]);
                stateVal[7] = Integer.parseInt(dataArray[1]);
                stateVal[8] = Integer.parseInt(dataArray[2]);
                return true;
            case 'c':
                stateVal[9] = Integer.parseInt(dataArray[0]);
                stateVal[10] = Integer.parseInt(dataArray[1]);
                stateVal[11] = Integer.parseInt(dataArray[2]);
                return true;
            case 'd':
                currentMode = Integer.parseInt(dataArray[0]);
                return true;
            case 'f':
                batteryLife = Integer.parseInt(dataArray[0]); // range is [0,1000]
                return true;
            case 'g':
                zoneTemperature[0] = Integer.parseInt(dataArray[0]) / 100;
                zoneTemperature[1] = Integer.parseInt(dataArray[1]) / 100;
                zoneTemperature[2] = Integer.parseInt(dataArray[2]) / 100;
                tempVal = (zoneTemperature[0] + zoneTemperature[1] + zoneTemperature[2]) / (float) 3;


                Intent tempIntent = new Intent();
                tempIntent.setAction("UPDATE_TEMPERATURE");
                sendBroadcast(tempIntent);
                Intent graphIntent = new Intent();
                graphIntent.setAction("UPDATE_GRAPH");
                graphIntent.putExtra("data", tempVal);
                sendBroadcast(graphIntent);
                return true;
            case 'h':
                NotificationCompat.Builder notifySwitchOff = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                        .setContentTitle("Heat Band")
                        .setSmallIcon(R.drawable.logo2_small)
                        .setContentText("Your Device is Switching off!")
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                mNotificationManager.notify(1, notifySwitchOff.build());
                return true;
            case 'i':
                NotificationCompat.Builder notifySwitchOn = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                        .setContentTitle("Heat Band")
                        .setSmallIcon(R.drawable.logo2_small)
                        .setContentText("Your Device is Switching on!")
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                mNotificationManager.notify(1, notifySwitchOn.build());
                return true;
            case 'j':
                batteryLife = Integer.parseInt(dataArray[0]);
                NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                        .setContentTitle("Heat Band")
                        .setSmallIcon(R.drawable.logo2_small)
                        .setContentText(String.format("Your device battery is running low. Connect to power as soon as possible!"))
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                mNotificationManager.notify(1, mBuilder.build());
                return true;
            case 'k':
                individualCellBattery[0] = Integer.parseInt(dataArray[0]) / 100;
                individualCellBattery[1] = Integer.parseInt(dataArray[1]) / 100;
                individualCellBattery[2] = Integer.parseInt(dataArray[2]) / 100;
                return true;
            default:
                return false;

        }
    }



    public class ListeningThread implements Runnable{
        Handler handler = new Handler();
        ListeningThread(){
        }

        @Override
        public void run() {
            Log.d(TAG, "ListeningThread.run()");
            if (bluetoothSocket != null) {
                try {
                    if (!(btIn.available() > 0)) {
                    } else {
                        byte[] bluetoothReturn = new byte[1024];
                        String readMessage = receiveBtData(bluetoothReturn);
                        Log.d(TAG, "GotMessage: " + readMessage);

                        if (readMessage.length() != 0) {
                            boolean decodeSuccess = decodeMessage(readMessage);
                            Log.d(TAG, "handleMessage: " + String.valueOf(decodeSuccess));

                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (NullPointerException f) {
                    Log.d(TAG, "run: NullPointered");
                }
                finally {
                    handler.postDelayed(listeningThread, 1000);
                }
            }
            else {
                handler.postDelayed(listeningThread, 1000);
            }

        }
    }
    public class WritingThread extends Thread{
        Handler handler;

        WritingThread(){
        }
        @Override
        public void run() {

            Looper.prepare();
            handler = new Handler(){
                @Override
                public void handleMessage(Message msg) {
                    if (bluetoothSocket != null) {
                        Log.d(TAG, "WritingThread.run()");
                        String data = msg.getData().getString(mOutgoingData);
                        sendBtData(data.getBytes());
                    }
                }
            };
            Looper.loop();
        }
    }

    public class TimerThread implements Runnable{
        long millisInFuture, interval;
        TimerThread(long millisInFuture, long interval){
            this.millisInFuture = millisInFuture;
            this.interval = interval;
        }

        @Override
        public void run() {
            Log.d(TAG, "TimerThread.run()");
            countDownTimer = new CountDownTimer(millisInFuture, interval) {
                @Override
                public void onTick(long l) {
                    Intent countdownIntent = new Intent();
                    countdownIntent.setAction("SET_TIMER_TEXT");
                    long hours = l / (1000 * 3600);
                    long minutes = (l - hours * 1000 * 3600) / (1000 * 60);
                    long seconds = (l - hours * 1000 * 3600 - minutes * 1000 * 60) / 1000;
                    String tempString = hours + " Hours " + minutes + " Minutes " + seconds + " Seconds";
                    countdownIntent.putExtra("data", tempString);
                    sendBroadcast(countdownIntent);
                }

                @Override
                public void onFinish() {
                    Intent countdownIntent = new Intent();
                    countdownIntent.setAction("TIME_UP");
                    sendBroadcast(countdownIntent);

                }
            };
            countDownTimer.start();

        }

    }
    private void registerNotification(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the NotificationChannel, but only on API 26+ because
            // the NotificationChannel class is new and not in the support library
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.enableLights(true);
            channel.enableVibration(true);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            channel.setDescription(description);
            // Register the channel with the system
            if (mNotificationManager == null){
                mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            }
        }
        else{
            mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        }
    }

}
