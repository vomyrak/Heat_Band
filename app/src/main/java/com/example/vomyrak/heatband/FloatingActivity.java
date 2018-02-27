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
public class FloatingActivity extends AppCompatActivity {
    protected SeekBar SeekBar1;
    protected SeekBar SeekBar2;
    protected SeekBar SeekBar3;
    protected Button Save;
    protected byte data[] = new byte[3];
    protected int mode;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_floating);
        SeekBar1 = (SeekBar) findViewById(R.id.set_zone1);
        SeekBar2 = (SeekBar) findViewById(R.id.set_zone2);
        SeekBar3 = (SeekBar) findViewById(R.id.set_zone3);
        Save = (Button) findViewById(R.id.save);
        Intent newIntent = getIntent();
        mode = newIntent.getIntExtra("Mode", 0);
        Toast.makeText(getApplicationContext(), String.valueOf(mode), Toast.LENGTH_SHORT).show();
        Save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent NewZoneTemp = new Intent(FloatingActivity.this, MainActivity.class);
                NewZoneTemp.putExtra("New Progress", data);
                NewZoneTemp.putExtra("Mode", mode);
                startActivity(NewZoneTemp);
                try {
                    bluetoothSocket.getOutputStream().write("j".getBytes());
                    bluetoothSocket.getOutputStream().write(data[0]);
                    bluetoothSocket.getOutputStream().write(data[1]);
                    bluetoothSocket.getOutputStream().write(data[2]);
                    bluetoothSocket.getOutputStream().write(" ".getBytes());
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
        if (mode != 0){
            setTitle("Mode " + String.valueOf(mode));

            byte states[] = newIntent.getByteArrayExtra("stateVals");
            for (int i = 0; i < states.length; i++) {
                Log.d("States " + String.valueOf(i), "is " + states[i]);
            }
            int offset = mode * 3;
            data[0] = states[offset];
            data[1] = states[offset + 1];
            data[2] = states[offset + 2];
            SeekBar1.setProgress((int) data[0]);
            SeekBar2.setProgress((int) data[1]);
            SeekBar3.setProgress((int) data[2]);


            SeekBar1.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
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
            SeekBar2.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
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
            SeekBar3.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
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
