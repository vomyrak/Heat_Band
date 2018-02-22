package com.example.vomyrak.heatband;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class FloatingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_floating);
        Intent newIntent = getIntent();
        int mode = newIntent.getIntExtra("Mode", 0);
        if (mode != 0){
            setTitle("Mode " + String.valueOf(mode));
            byte data[] = new byte[3];
            byte states[] = newIntent.getByteArrayExtra("stateVals");
            int offset = mode * 3;
            data[0] = states[offset];
            data[1] = states[offset + 1];
            data[2] = states[offset + 2];
        }
        else{finish();}
    }
}
