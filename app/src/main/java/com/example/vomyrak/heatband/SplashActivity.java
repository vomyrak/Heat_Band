package com.example.vomyrak.heatband;

import android.support.v7.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;


public class SplashActivity extends AppCompatActivity{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        synchronized (this) {
            try {
                wait(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
