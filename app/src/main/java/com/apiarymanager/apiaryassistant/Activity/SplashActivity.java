package com.apiarymanager.apiaryassistant.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.apiarymanager.apiaryassistant.R;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        TextView textView = findViewById(R.id.nameCompany);
        textView.setAlpha(0f);
        textView.animate().alpha(1f).setDuration(1000);

        new Handler().postDelayed(() -> {
            startActivity(new Intent(SplashActivity.this, AssistantActivity.class));
            finish();
        }, 2000);
    }
}