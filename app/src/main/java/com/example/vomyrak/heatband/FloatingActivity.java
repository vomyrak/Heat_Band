package com.example.vomyrak.heatband;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import com.gc.materialdesign.views.ButtonRectangle;

import org.adw.library.widgets.discreteseekbar.DiscreteSeekBar;

import static com.example.vomyrak.heatband.MainActivity.bluetoothSocket;
import static com.example.vomyrak.heatband.MainActivity.stateVal;

public class FloatingActivity extends AppCompatActivity {
    
    //Build UI for the floating activity
    protected DiscreteSeekBar zone1;
    protected DiscreteSeekBar zone2;
    protected DiscreteSeekBar zone3;
    protected ButtonRectangle save;
    protected TextView header;
    protected int[] data = new int[3];
    protected int mode;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_floating);
        getWindow().setFeatureInt(Window.FEATURE_NO_TITLE, R.layout.activity_floating);
        zone1 = findViewById(R.id.set_zone1);
        zone2 = findViewById(R.id.set_zone2);
        zone3 = findViewById(R.id.set_zone3);
        save = findViewById(R.id.save);
        header = findViewById(R.id.modeDisplay);
        Intent newIntent = getIntent();
        mode = newIntent.getIntExtra("Mode", 0);
        final int offset = mode * 3;
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stateVal[offset] = zone1.getProgress();
                stateVal[offset + 1] = zone2.getProgress();
                stateVal[offset + 2] = zone3.getProgress();

                Intent intent = new Intent();
                setResult(RESULT_OK, intent);
                Intent sendDataIntent = new Intent();
                sendDataIntent.setAction("SEND_DATA");
                sendDataIntent.putExtra("data", ((mode == 1)? "n" : ((mode == 2)? "o" : "p"))
                        + String.valueOf(stateVal[offset]) + "," + String.valueOf(stateVal[offset+1]) + "," + String.valueOf(stateVal[offset+2]) + " ");
                sendBroadcast(sendDataIntent);
                finish();
            }
        });
        if (mode != 0){
            setTitle("Mode " + String.valueOf(mode));
            data[0] = stateVal[offset];
            data[1] = stateVal[offset + 1];
            data[2] = stateVal[offset + 2];
            zone1.setProgress(data[0]);
            zone2.setProgress(data[1]);
            zone3.setProgress(data[2]);

            header.setText(R.string.ModeHeader + mode);

            zone1.setOnProgressChangeListener(new DiscreteSeekBar.OnProgressChangeListener() {
                @Override
                public void onProgressChanged(DiscreteSeekBar seekBar, int value, boolean fromUser) {
                    data[0]=value;
                }

                @Override
                public void onStartTrackingTouch(DiscreteSeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(DiscreteSeekBar seekBar) {

                }
            });

            zone2.setOnProgressChangeListener(new DiscreteSeekBar.OnProgressChangeListener() {
                @Override
                public void onProgressChanged(DiscreteSeekBar seekBar, int value, boolean fromUser) {
                    data[1]=value;

                }

                @Override
                public void onStartTrackingTouch(DiscreteSeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(DiscreteSeekBar seekBar) {

                }
            });

            zone3.setOnProgressChangeListener(new DiscreteSeekBar.OnProgressChangeListener() {
                @Override
                public void onProgressChanged(DiscreteSeekBar seekBar, int value, boolean fromUser) {
                    data[2]=value;

                }

                @Override
                public void onStartTrackingTouch(DiscreteSeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(DiscreteSeekBar seekBar) {

                }
            });


        }
        else{finish();}
    }
}
