package com.apiarymanager.apiaryassistant.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.apiarymanager.apiaryassistant.Active;
import com.apiarymanager.apiaryassistant.Assistant;
import com.apiarymanager.apiaryassistant.OnClickListener;
import com.apiarymanager.apiaryassistant.R;
import com.apiarymanager.apiaryassistant.RecognitionService;

import android.Manifest;
import android.content.pm.PackageManager;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class AssistantActivity extends AppCompatActivity {

    private Assistant assistant;
    ImageButton record;
    Button toEnd;
    TextView status, hint, result, nameMyApiary, wait;
    String resultText;
    Active active;
    ConstraintLayout noReport;
    Boolean permission = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_assistant);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        record = findViewById(R.id.record);
        status = findViewById(R.id.status);
        toEnd = findViewById(R.id.to_end);
        hint = findViewById(R.id.hint);
        result = findViewById(R.id.result);
        noReport = findViewById(R.id.noReport);
        active = (Active) getApplication();
        nameMyApiary = findViewById(R.id.nameMyApiary);
        wait = findViewById(R.id.wait);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 1);
        }

        assistant = new Assistant(active.getActiveReport());
        assistant.initModel(this, new Assistant.OnModelReadyCallback() {
            @Override
            public void onModelReady() {
                record.setEnabled(true);
            }
        });

        if (!permission) {
            record.setOnClickListener(v -> {
            });
        }

        if (active.getActiveReport() == null) {
            nameMyApiary.setVisibility(View.GONE);
            record.setOnClickListener(v -> {
                noReport();
            });
        } else {
            record.setOnClickListener(v -> {
                toRecord();
            });
            nameMyApiary.setText(active.getActiveReport().nameReport);
        }

        OnClickListener.initMenu(this, R.id.assistant_activity);

        findViewById(R.id.to_end).setOnClickListener(v -> {
            toEnd();
        });
    }

    public void noReport() {
        noReport.setVisibility(View.VISIBLE);
        noReport.postDelayed(() -> noReport.setVisibility(View.GONE), 2000);
    }

    private void toRecord() {
        String currentText = status.getText().toString();
        if (!assistant.isReady()) {
            wait.setVisibility(View.VISIBLE);
            wait.postDelayed(() -> wait.setVisibility(View.GONE), 1000);
        } else {
            if (currentText.equals(getString(R.string.to_start)) || currentText.equals(getString(R.string.pause))) {
                startServiceIfNeeded();
                assistant.toggleRecognition(result);
                updateState("started");
            } else if (currentText.equals(getString(R.string.started))) {
                assistant.stopRecognition();
                updateState("pause");
            }
        }
    }

    private void updateState(String state) {
        switch (state) {
            case "started":
                status.setText(getString(R.string.started));
                record.setImageResource(R.drawable.start);
                toEnd.setVisibility(View.VISIBLE);
                hint.setVisibility(View.VISIBLE);
                hint.setText(getString(R.string.to_pause));
                break;

            case "pause":
                status.setText(getString(R.string.pause));
                record.setImageResource(R.drawable.pause);
                hint.setText(getString(R.string.resume));
                break;

            case "completed":
                status.setText(getString(R.string.completed));
                record.setImageResource(R.drawable.complete);
                toEnd.setVisibility(View.GONE);
                hint.setVisibility(View.GONE);
                record.postDelayed(() -> updateState("to_start"), 2000);
                break;

            case "to_start":
                status.setText(getString(R.string.to_start));
                record.setImageResource(R.drawable.record);
                break;
        }
    }

    private void toEnd() {
        stopServiceIfRunning();
        assistant.stopRecognition();
        updateState("completed");
        resultText = TextUtils.join("\n", Assistant.text);
        Assistant.text.clear();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Разрешение на запись получено", Toast.LENGTH_SHORT).show();
                status.setText(getString(R.string.to_start));
                status.setTextColor(getColor(R.color.brown));
                status.setTextSize(22);
                permission = true;
            } else {
                Toast.makeText(this, "Разрешение на запись не предоставлено", Toast.LENGTH_SHORT).show();
                status.setText(getString(R.string.permission));
                status.setTextColor(getColor(R.color.red));
                status.setTextSize(18);
                permission = false;
            }
        }
    }

    private void startServiceIfNeeded() {
        Intent serviceIntent = new Intent(this, RecognitionService.class);
        ContextCompat.startForegroundService(this, serviceIntent);
    }

    private void stopServiceIfRunning() {
        Intent serviceIntent = new Intent(this, RecognitionService.class);
        stopService(serviceIntent);
    }

}