package com.example.vomyrak.heatband;

import android.app.IntentService;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.ParcelUuid;
import android.os.Process;
import android.os.health.ServiceHealthStats;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import static com.example.vomyrak.heatband.MainActivity.DEVICE_ADDRESS;
import static com.example.vomyrak.heatband.MainActivity.DEVICE_NAME;
import static com.example.vomyrak.heatband.MainActivity.bluetoothAdapter;
import static com.example.vomyrak.heatband.MainActivity.bluetoothSocket;
import static com.example.vomyrak.heatband.MainActivity.connectedDevice;
import static com.example.vomyrak.heatband.MainActivity.myUUID;
import static com.example.vomyrak.heatband.MainActivity.pairedDevices;
import static com.example.vomyrak.heatband.MainActivity.rRequestBt;

public class MyBtService extends IntentService {
    private static final String TAG ="MyBtService";
    Looper mServiceLooper;
    Handler mServiceHandler = new Handler();
    private Timer mTimer = null;
    Queue<String> rxQueue;
    Queue<byte[]> txQueue;
    InputStream btIn;
    OutputStream btOut;


    public MyBtService() {
        super("MyBtService");
    }
    //Timer Function
    class MyTimerTask extends TimerTask{
        @Override
        public void run(){
            //timerHandler.post(timeRunnable);
        }

    }
    Handler timerHandler = new Handler();
    Runnable timeRunnable = new Runnable() {
        @Override
        public void run() {
            try{
                byte[] bluetoothReturn = new byte[1024];
                if (bluetoothSocket.isConnected()){
                    Toast.makeText(getApplicationContext(), "BT Name: "+DEVICE_NAME+"\nBT Address: "+DEVICE_ADDRESS, Toast.LENGTH_SHORT).show();
                    //bluetoothSocket.getOutputStream().write("j255,0,255 ".getBytes());
                    //sendBtData("j\n".getBytes());
                    String readMessage = receiveBtData(bluetoothReturn);
                    Log.d("Incoming data = ", readMessage);
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
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
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

        return START_STICKY;
    }

    @Override
    public void onCreate(){
        if (mTimer == null){
            Log.d("Scheduled task", "created");
            mTimer = new Timer();
            mTimer.scheduleAtFixedRate(new MyTimerTask(), 0, 5*1000);
        }
        HandlerThread thread = new HandlerThread("ServicesStartArguments", Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        mServiceLooper = thread.getLooper();
        mServiceHandler = new Handler(mServiceLooper);

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
            } catch (IOException e){
                e.printStackTrace();
            }

    }
    public String receiveBtData(byte[] buffer) throws IOException, NullPointerException{

            int length = btIn.read(buffer);
            return new String(buffer, 0, length);

    }

}
