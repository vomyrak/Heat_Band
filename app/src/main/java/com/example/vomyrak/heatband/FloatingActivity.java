package com.example.vomyrak.heatband;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.Button;
import android.widget.Toast;
import static com.example.vomyrak.heatband.MainActivity.bluetoothSocket;
import static com.example.vomyrak.heatband.MainActivity.stateVal;
import static com.example.vomyrak.heatband.MainActivity.mJsonFile;

public class FloatingActivity extends AppCompatActivity {
    
    //Build UI for the floating activity
    protected SeekBar zone1;
    protected SeekBar zone2;
    protected SeekBar zone3;
    protected Button save;
    protected byte[] data = new byte[3];
    protected int mode;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_floating);
        zone1 = (SeekBar) findViewById(R.id.set_zone1);
        zone2 = (SeekBar) findViewById(R.id.set_zone2);
        zone3 = (SeekBar) findViewById(R.id.set_zone3);
        save = (Button) findViewById(R.id.save);
        Intent newIntent = getIntent();
        mode = newIntent.getIntExtra("Mode", 0);
        final int offset = mode * 3;
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stateVal[offset] = (byte)zone1.getProgress();
                stateVal[offset + 1] = (byte)zone2.getProgress();
                stateVal[offset + 2] = (byte)zone3.getProgress();
                try {
                    bluetoothSocket.getOutputStream().write("j".getBytes());
                    bluetoothSocket.getOutputStream().write(data[0]);
                    bluetoothSocket.getOutputStream().write(data[1]);
                    bluetoothSocket.getOutputStream().write(data[2]);
                    bluetoothSocket.getOutputStream().write(" ".getBytes());
                } catch (Exception e){
                    e.printStackTrace();
                } finally {
                    Intent intent = new Intent();
                    setResult(RESULT_OK, intent);
                    finish();
                }
            }
        });
        if (mode != 0){
            setTitle("Mode " + String.valueOf(mode));
            data[0] = stateVal[offset];
            data[1] = stateVal[offset + 1];
            data[2] = stateVal[offset + 2];
            zone1.setProgress((int) data[0]);
            zone2.setProgress((int) data[1]);
            zone3.setProgress((int) data[2]);
            
            
            zone1.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                    data[0]=(byte)i;
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });
            zone2.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

                @Override
                public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                    data[1]=(byte)i;
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });
            zone3.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                    data[2]=(byte)i;
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });

        }
        else{finish();}
    }
}
