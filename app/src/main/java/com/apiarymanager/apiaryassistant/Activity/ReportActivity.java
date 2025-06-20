package com.apiarymanager.apiaryassistant.Activity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.apiarymanager.apiaryassistant.Active;
import com.apiarymanager.apiaryassistant.FileManager;
import com.apiarymanager.apiaryassistant.OnClickListener;
import com.apiarymanager.apiaryassistant.R;
import com.apiarymanager.apiaryassistant.Report;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ReportActivity extends AppCompatActivity {
    EditText report_name;
    Button toCreateReport, loadReport, newReport, buttonSave;
    ImageButton toBack;
    String namePattern;
    File pattern;
    ImageView plusOne, leftArrow, rightArrow, imgPattern;
    private ActivityResultLauncher<Intent> importFileLauncher;
    ConstraintLayout createReport, thisReport, newOrLoad, hasBeenCreated, toPattern;
    TextView nameMyApiary, textNumberRow, toNamePattern;
    Active active;
    List<EditText> currentEditTexts = new ArrayList<>();
    List<String> cells = new ArrayList<>();
    Report report;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_report);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        OnClickListener.initMenu(this, R.id.report_activity);

        report_name = findViewById(R.id.to_report_name);
        toCreateReport = findViewById(R.id.toCreateReport);
        plusOne = findViewById(R.id.plusOne);
        createReport = findViewById(R.id.createReport);
        thisReport = findViewById(R.id.thisReport);
        loadReport = findViewById(R.id.loadReport);
        newReport = findViewById(R.id.newReport);
        newOrLoad = findViewById(R.id.newOrLoad);
        toBack = findViewById(R.id.toBack);
        nameMyApiary = findViewById(R.id.nameMyApiary);
        hasBeenCreated = findViewById(R.id.hasBeenCreated);
        leftArrow = findViewById(R.id.leftArrow);
        rightArrow = findViewById(R.id.rightArrow);
        textNumberRow = findViewById(R.id.textNumberRow);
        buttonSave = findViewById(R.id.buttonSave);
        toPattern = findViewById(R.id.toPattern);
        toNamePattern = findViewById(R.id.toNamePattern);
        imgPattern = findViewById(R.id.imgPattern);

        toCreateReport.setOnClickListener(v -> {
            try {
                createReport();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        loadReport.setOnClickListener(v -> {
            Intent intent = new Intent(this, ArchiveActivity.class);
            startActivity(intent);
        });

        newReport.setOnClickListener(v -> {
            newOrLoad.setVisibility(View.GONE);
            createReport.setVisibility(View.VISIBLE);
            toBack.setVisibility(View.VISIBLE);
        });

        active = (Active) getApplication();
        if (active.getActiveReport() != null) {
            try {
                activePattern();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        toBack.setOnClickListener(v -> {
            NewOrLoad();
            toBack.setVisibility(View.GONE);
        });

        leftArrow.setOnClickListener(v -> {
            generateCell(active.getActiveReport().previousRow());
        });

        rightArrow.setOnClickListener(v -> {
            if (isCurrentRowEmpty()) {
                Toast.makeText(this, "Введите данные перед добавлением новой строки", Toast.LENGTH_SHORT).show();
            } else {
                generateCell(active.getActiveReport().nextRow());
            }
        });

        buttonSave.setOnClickListener(v -> {
            saveCurrentRowData();
        });

        importFileLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        namePattern = FileManager.getFileNameFromUri(this, uri);
                        if (uri != null) {
                            try {
                                FileManager.importExcelFile(this, uri, namePattern);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            patternIsAdded(namePattern);
                        }
                    }
                }
        );

        toPattern.setOnClickListener(v -> {
            addPattern();
        });
    }

    public void addPattern() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        importFileLauncher.launch(Intent.createChooser(intent, "Выберите .xlsx файл"));
    }

    public void patternIsAdded(String namePattern) {
        toNamePattern.setText(namePattern);
        imgPattern.setImageResource(R.drawable.pattern);
        toPattern.setBackground(getDrawable(R.drawable.border_to_pattern_added));
        pattern = new File(getFilesDir(), "patterns/" + namePattern);
    }

    public void createReport() throws IOException {
        boolean reportIsReady = true;
        if (report_name.getText().toString().trim().isEmpty()) {
            report_name.setBackground(getDrawable(R.drawable.border_round_edit_error));
            report_name.postDelayed(() -> report_name.setBackground(getDrawable(R.drawable.border_round_edit)), 1000);
            reportIsReady = false;
        }

        File dir = new File(getFilesDir(), "reports");
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().equals(report_name.getText().toString() + ".xlsx")) {
                    hasBeenCreated.setVisibility(View.VISIBLE);
                    hasBeenCreated.postDelayed(() -> hasBeenCreated.setVisibility(View.GONE), 3000);
                    reportIsReady = false;
                }
            }
        }

        if (reportIsReady) {
            if (pattern == null) {
                report = new Report(this, report_name.getText().toString(), null);
            } else {
                report = new Report(this, report_name.getText().toString(), pattern);
            }
            active.setActiveReport(report);

            Toast.makeText(this, "Новый отчёт создан", Toast.LENGTH_SHORT).show();
            plusOne.setVisibility(View.VISIBLE);
            plusOne.postDelayed(() -> plusOne.setVisibility(View.GONE), 5000);

            activePattern();
        }
    }

    private void generateCell(int index) {
        textNumberRow.setText(getString(R.string.number_row) + " " + active.getActiveReport().getRow());

        LinearLayout scrollLayout = findViewById(R.id.scroll);
        scrollLayout.removeAllViews();

        cells = active.getActiveReport().keys;
        List<String> values;

        int size = active.getActiveReport().sizeRow();
        if (size > 1) {
            values = active.getActiveReport().readReport(index);
        } else {
            values = new ArrayList<>(active.getActiveReport().keys.size());
            for (int i = 0; i < active.getActiveReport().keys.size(); i++) {
                values.add("");
            }
        }

        int i = 0;
        currentEditTexts.clear();
        for (String name : cells) {
            ConstraintLayout itemLayout = (ConstraintLayout) LayoutInflater.from(this).inflate(R.layout.cell, scrollLayout, false);

            TextView columnName = itemLayout.findViewById(R.id.column_name);
            EditText toColumnName = itemLayout.findViewById(R.id.to_column_name);

            columnName.setText(name);
            toColumnName.setText(values.get(i));

            currentEditTexts.add(toColumnName);

            scrollLayout.addView(itemLayout);
            i++;
        }
    }

    private void saveCurrentRowData() {
        if (active.getActiveReport() == null || currentEditTexts.isEmpty()) return;

        for (int j = 0; j < currentEditTexts.size(); j++) {
            String key = cells.get(j);
            String value = currentEditTexts.get(j).getText().toString();
            active.getActiveReport().writeToCellInRow(active.getActiveReport().getRow(), key, value);
        }

        try {
            active.getActiveReport().saveFile();
            Toast.makeText(this, "Данные сохранены", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Ошибка сохранения", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isCurrentRowEmpty() {
        for (EditText editText : currentEditTexts) {
            if (!editText.getText().toString().trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public void NewOrLoad() {
        newOrLoad.setVisibility(View.VISIBLE);
        createReport.setVisibility(View.GONE);
        thisReport.setVisibility(View.GONE);
        active.setActiveReport(null);
        toBack.setVisibility(View.GONE);
    }

    public void activePattern() throws IOException {
        generateCell(active.getActiveReport().getRow());
        newOrLoad.setVisibility(View.GONE);
        createReport.setVisibility(View.GONE);
        thisReport.setVisibility(View.VISIBLE);
        nameMyApiary.setText(active.getActiveReport().nameReport);
        toBack.setVisibility(View.VISIBLE);
    }
}