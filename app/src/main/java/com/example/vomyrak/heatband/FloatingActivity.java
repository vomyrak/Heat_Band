package com.example.vomyrak.heatband;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.SeekBar;
import android.widget.Toast;

public class FloatingActivity extends AppCompatActivity {
    protected SeekBar aSeekbar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_floating);
        aSeekbar = (SeekBar) findViewById(R.id.set_zone1);
        Intent newIntent = getIntent();
        int mode = newIntent.getIntExtra("Mode", 0);
        Toast.makeText(getApplicationContext(), String.valueOf(mode), Toast.LENGTH_SHORT).show();

        if (mode != 0){
            setTitle("Mode " + String.valueOf(mode));
            byte data[] = new byte[3];
            byte states[] = newIntent.getByteArrayExtra("stateVals");
            for (int i = 0; i < states.length; i++) {
                Log.d("States " + String.valueOf(i), "is " + states[i]);
            }
            int offset = mode * 3;
            data[0] = states[offset];
            data[1] = states[offset + 1];
            data[2] = states[offset + 2];
            aSeekbar.setProgress((int) data[0]);
        }


        // TODO pass any changes in value back to the array
        // TODO go back to main activity


        else{finish();}
    }
}
