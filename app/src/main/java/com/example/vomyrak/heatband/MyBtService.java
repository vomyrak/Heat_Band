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

import static com.example.vomyrak.heatband.MainActivity.rRequestBt;
import static com.example.vomyrak.heatband.MainActivity.stateVal;
import static com.example.vomyrak.heatband.MainActivity.tempVal;
import static com.example.vomyrak.heatband.MainActivity.btDiscoveryDone;
import static com.example.vomyrak.heatband.MainActivity.currentMode;

public class MyBtService extends IntentService {
    protected static final String TAG ="MyBtService";
    private final IBinder mIBinder = new MyBinder();
    protected InputStream btIn;
    protected OutputStream btOut;
    protected Thread btThread;
    protected Handler timerHandler;
    protected WritingThread writingThread;
    protected Thread listeningThread;

    protected static final String mConfig = "Config";
    protected static int serviceId;
    protected Random random = new Random();
    protected String randString;
    protected static final String CHANNEL_ID = "Heatband";
    private NotificationManager mNotificationManager;
    protected boolean lowBatteryNotified = false;
    protected class MyBinder extends Binder{
        MyBtService getService(){return MyBtService.this;}
    }

    public class BluetoothThread implements Runnable{
        int serviceId;
        BluetoothThread(int serviceId){
            this.serviceId = serviceId;
            timerHandler = new Handler();
        }
        @Override
        public void run() {
            if (bluetoothSocket != null) {
                Log.d(TAG, "BluetoothThread.run()");
                try {
                    if (bluetoothSocket.isConnected()) {
                        Log.d(TAG, "BT Name: " + DEVICE_NAME + "\nBT Address: " + DEVICE_ADDRESS);
                        randString = "a";
                        randString += String.valueOf(random.nextInt(255));
                        randString += ",";
                        randString += String.valueOf(random.nextInt(255));
                        randString += ",";
                        randString += String.valueOf(random.nextInt(255));
                        randString += " ";
                        Log.d("Outgoing data = ", randString);
                        Bundle outgoingData = new Bundle();
                        outgoingData.putString(mOutgoingData, randString);
                        Message messageOut = Message.obtain();
                        messageOut.setData(outgoingData);
                        writingThread.handler.sendMessage(messageOut);
                    }
                    else{
                        resetBluetooth();
                    }
                } catch (NullPointerException f) {
                    try {
                        connectedDevice = bluetoothAdapter.getRemoteDevice(DEVICE_ADDRESS);
                        bluetoothSocket = connectedDevice.createInsecureRfcommSocketToServiceRecord(myUUID);
                        bluetoothSocket.connect();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } finally {
                    timerHandler.postDelayed(btThread, 1000 * 5);
                }

            }
            else {
                resetBluetooth();
                timerHandler.postDelayed(btThread, 1000 * 5);
            }
        }
    }

    public MyBtService() {
        super(TAG);
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.v(TAG, "in onBind");
        return mIBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.v(TAG, "in OnRebind");
        return super.onUnbind(intent);
    }

    @Override
    public void onRebind(Intent intent) {
        Log.v(TAG, "in OnUnbind");
        super.onRebind(intent);
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
        writingThread = new WritingThread();
        writingThread.start();
        btThread = new Thread(new BluetoothThread(startId));
        listeningThread = new Thread(new ListeningThread());

        btThread.run();

        listeningThread.run();

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
        resetBluetooth();
    }

    public void sendBtData(byte[] data){
        try {
            btOut.write(data);
        } catch (IOException e) {
            e.printStackTrace();
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

    protected void resetBluetooth(){
        bluetoothAdapter = null;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            enableBtIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getApplicationContext().startActivity(enableBtIntent);
        }
        if (!btDiscoveryDone){
            Intent broadcastIntent = new Intent();
            broadcastIntent.setAction("com.example.vomyrak.heatband.MY_NOTIFICATION");
            broadcastIntent.putExtra("data", "scanForDevices");
            sendBroadcast(broadcastIntent);
            return;
        }
        if (bluetoothSocket == null) {
            try {
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
                    try {
                        connectedDevice = bluetoothAdapter.getRemoteDevice(DEVICE_ADDRESS);
                        bluetoothSocket = connectedDevice.createInsecureRfcommSocketToServiceRecord(myUUID);
                        bluetoothSocket.connect();
                        bluetoothAdapter.cancelDiscovery();
                        btIn = bluetoothSocket.getInputStream();
                        btOut = bluetoothSocket.getOutputStream();
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
    }

    protected boolean decodeMessage(String data){
        char opcode = data.charAt(0);
        data = data.substring(1);
        String[] dataArray = data.split(",");
        if (dataArray.length != 3){
            return false;
        }

        Log.d(TAG, data);
        // TODO to implement decoding mechanism

        switch (opcode){
            case 'a':
                stateVal[3] = (byte)Integer.parseInt(dataArray[0]);
                stateVal[4] = (byte)Integer.parseInt(dataArray[1]);
                stateVal[5] = (byte)Integer.parseInt(dataArray[2]);
                return true;
            case 'b':
                stateVal[6] = (byte)Integer.parseInt(dataArray[0]);
                stateVal[7] = (byte)Integer.parseInt(dataArray[1]);
                stateVal[8] = (byte)Integer.parseInt(dataArray[2]);
                return true;
            case 'c':
                stateVal[9] = (byte)Integer.parseInt(dataArray[0]);
                stateVal[10] = (byte)Integer.parseInt(dataArray[1]);
                stateVal[11] = (byte)Integer.parseInt(dataArray[2]);
                return true;
            case 'd':
                currentMode = Integer.parseInt(dataArray[2]);
                return true;
            case 'e':
                // not implemented
            case 'f':
                batteryLife = (byte)Integer.parseInt(dataArray[2]);
                return true;
            case 'g':
                tempVal = -10 + (Integer.parseInt(dataArray[1]) * 256 + Integer.parseInt(dataArray[2])) * 70;
                return true;
            case 'h':
                Intent stateIntent = new Intent();
                stateIntent.setAction("NOTIFY_POWER_OFF");
                sendBroadcast(stateIntent);
                return true;
                // TODO band switched off
            case 'i':
                Intent onIntent = new Intent();
                onIntent.setAction("NOTIFY_POWER_ON");
                sendBroadcast(onIntent);
                return true;
                // TODO band switched on
            case 'j':
                NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                        .setContentTitle("Heat Band")
                        .setContentText("Low Battery")
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                mNotificationManager.notify(1, mBuilder.build());
                return true;
                // TODO low battery warning
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
                } finally {
                    handler.postDelayed(listeningThread, 500);
                }
            }
            else {
                handler.postDelayed(listeningThread, 500);
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
    }

}
