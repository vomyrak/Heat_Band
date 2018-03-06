package com.example.vomyrak.heatband;

import android.app.IntentService;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelUuid;
import android.os.Process;
import android.os.health.ServiceHealthStats;
import android.util.Log;
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

public class MyBtService extends IntentService {
    protected static final String TAG ="MyBtService";
    private final IBinder mIBinder = new MyBinder();
    protected InputStream btIn;
    protected OutputStream btOut;
    protected Thread btThread;
    protected DataThread dataThread;
    protected Handler timerHandler;
    protected WritingThread writingThread;
    protected Thread listeningThread;

    protected static final String mConfig = "Config";

    protected Random random = new Random();
    protected String randString;
    protected class MyBinder extends Binder{
        MyBtService getService(){return MyBtService.this;}
    }

    public class BluetoothThread implements Runnable{
        int serviceId;
        BluetoothThread(int serviceId){
            this.serviceId = serviceId;
        }
        @Override
        public void run() {
            timerHandler = new Handler();
            try{
                if (bluetoothSocket.isConnected()){
                    Toast.makeText(getApplicationContext(), "BT Name: "+DEVICE_NAME+"\nBT Address: "+DEVICE_ADDRESS, Toast.LENGTH_SHORT).show();
                    //bluetoothSocket.getOutputStream().write("j255,0,255 ".getBytes());
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
            } catch (NullPointerException f){
                try{
                    connectedDevice = bluetoothAdapter.getRemoteDevice(DEVICE_ADDRESS);
                    bluetoothSocket = connectedDevice.createInsecureRfcommSocketToServiceRecord(myUUID);
                    bluetoothSocket.connect();
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
            timerHandler.postDelayed(btThread, 1000 * 5);
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
        btThread = new Thread(new BluetoothThread(startId));
        btThread.run();
        listeningThread = new Thread(new ListeningThread());
        writingThread = new WritingThread();
        listeningThread.run();
        writingThread.start();
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
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            enableBtIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getApplicationContext().startActivity(enableBtIntent);
        }

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

    public void sendBtData(byte[] data){
        try {
            btOut.write(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public String receiveBtData(byte[] buffer) throws IOException, NullPointerException{
        if (btIn.available() > 0) {
            while (btIn.read() != (int)'?'){
                Log.d(TAG, String.valueOf(btIn.read()));
                Log.d(TAG, String.valueOf((int)'?'));
            }
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
        try {
            synchronized (this) {
                this.wait(1000);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            enableBtIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getApplicationContext().startActivity(enableBtIntent);
        }

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
        //return true;
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
                // TODO to change mode number
            case 'e':
                // TODO to confirm received
            case 'f':
                batteryLife = (byte)Integer.parseInt(dataArray[2]);
                return true;
            case 'g':
                tempVal = (byte)Integer.parseInt(dataArray[2]);
                return true;
            case 'h':
                // TODO band switched off
            case 'i':
                // TODO band switched on
            case 'j':
                // TODO low battery warning
            default:
                return false;

        }
    }

    public class DataThread extends Thread{
        Handler handler;
        DataThread(){
        }
        @Override
        public void run() {
            Looper.prepare();
            handler = new Handler(){
                @Override
                public void handleMessage(Message msg) {
                    String data = msg.getData().getString(mConfig);
                    boolean decodeSuccess = decodeMessage(data);
                    Log.d(TAG, "handleMessage: " + String.valueOf(decodeSuccess));
                }
            };
            Looper.loop();
        }
    }

    public class ListeningThread implements Runnable{
        Handler handler = new Handler();
        ListeningThread(){
        }

        @Override
        public void run() {
                try {
                    if (!(btIn.available() > 0)){}
                    else{
                        byte[] bluetoothReturn = new byte[1024];
                        String readMessage = receiveBtData(bluetoothReturn);
                        Log.d(TAG, "GotMessage: " + readMessage);

                        if (readMessage.length() != 0){
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
                    String data = msg.getData().getString(mOutgoingData);
                    sendBtData(data.getBytes());
                }
            };
            Looper.loop();
        }
    }
}
