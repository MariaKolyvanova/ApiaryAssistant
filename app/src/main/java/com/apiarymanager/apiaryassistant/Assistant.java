package com.apiarymanager.apiaryassistant;

import android.content.Context;
import android.media.MediaPlayer;
import android.util.Log;
import android.widget.TextView;

import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.android.RecognitionListener;
import org.vosk.android.SpeechService;
import org.vosk.android.StorageService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Assistant {
    private static final String TAG = "VoskManager";
    private SpeechService speechService;
    private Model model;
    private boolean isListening = false;
    public static List<String> text = new ArrayList<>();
    Report report;
    String value = "", keyword;
    private boolean recordingStarted = false;
    MediaPlayer soundStart, soundEnd, soundFound, soundWrote, soundNewCell;

    public Assistant(Report report) {
        this.report = report;
    }

    public interface OnModelReadyCallback {
        void onModelReady();
    }

    public void initModel(Context context, OnModelReadyCallback callback) {
        if (model != null) {
            callback.onModelReady();
            return;
        }

        StorageService.unpack(context, "model/vosk-model-small-ru-0.22", "model",
                resultModel -> {
                    Log.d(TAG, "Модель успешно загружена");
                    model = resultModel;
                    callback.onModelReady();
                },
                exception -> Log.e(TAG, "Ошибка загрузки модели", exception));

        soundStart = MediaPlayer.create(context, R.raw.start);
        soundEnd = MediaPlayer.create(context, R.raw.end);
        soundNewCell = MediaPlayer.create(context, R.raw.new_cell);
        soundFound = MediaPlayer.create(context, R.raw.found);
        soundWrote = MediaPlayer.create(context, R.raw.wrote);
    }

    public boolean isReady() {
        return model != null;
    }

    public void toggleRecognition(TextView resultView) {
        if (model == null) {
            Log.e(TAG, "Модель ещё не загружена");
            return;
        }

        if (!isListening) {
            startRecognition(resultView);
        } else {
            stopRecognition();
        }
    }

    private void startRecognition(TextView resultView) {
        if (speechService != null && isListening) {
            Log.d(TAG, "Распознавание уже запущено");
            return;
        }

        try {
            soundStart.start();
            Recognizer recognizer = new Recognizer(model, 16000.0f);
            speechService = new SpeechService(recognizer, 16000.0f);
            speechService.startListening(new RecognitionListener() {
                @Override
                public void onPartialResult(String hypothesis) {
                    resultView.setText(cleanUpText(hypothesis.toString()));
                }

                @Override
                public void onResult(String hypothesis) {
                    rec(hypothesis.toString());
                }

                @Override
                public void onFinalResult(String hypothesis) {
                    Log.d(TAG, "Final: " + hypothesis);
                }

                @Override
                public void onError(Exception e) {
                    Log.e(TAG, "Ошибка распознавания", e);
                }

                @Override
                public void onTimeout() {
                    Log.d(TAG, "Распознавание завершено по таймауту");
                }
            });

            isListening = true;
            Log.d(TAG, "Распознавание запущено");
        } catch (Exception e) {
            Log.e(TAG, "Ошибка запуска распознавания", e);
        }
    }

    public void stopRecognition() {
        Log.d(TAG, "stopRecognition вызван");
        if (speechService != null) {
            speechService.stop();
            speechService.shutdown();
            speechService = null;
            isListening = false;
            rec("стоп");
            Log.d(TAG, "Распознавание остановлено");

            soundEnd.start();
        }
    }

    public void rec(String hypothesis) {
        System.out.println(">>> Входящее сообщение: " + hypothesis);

        hypothesis = hypothesis.toLowerCase();

        while (hypothesis.contains(report.newRow)) {
            String[] parts = hypothesis.split(report.newRow, 2);
            String beforeNextRow = parts[0].trim();

            if (!beforeNextRow.isEmpty()) {
                rec(beforeNextRow);
            }

            report.nextRow();
            soundNewCell.start();

            hypothesis = (parts.length > 1) ? parts[1].trim() : "";
        }

        if (hypothesis.isEmpty()) {
            return;
        }

        if (hypothesis.contains(report.stopping)) {
            String[] parts = hypothesis.split(report.stopping, 2);
            String beforeStop = parts[0].trim();

            if (!beforeStop.isEmpty()) {
                rec(beforeStop);
            }

            saveBufferedValue();

            if (parts.length > 1) {
                rec(parts[1].trim());
            }

            return;
        }

        if (!recordingStarted) {
            keyword = report.checkKeyword(hypothesis);
            if (keyword != null) {
                soundFound.start();
                recordingStarted = true;

                String remainingText = hypothesis.substring(hypothesis.indexOf(keyword.toLowerCase()) + keyword.length()).trim();
                remainingText = cleanUpText(remainingText);

                if (!remainingText.isEmpty()) {
                    value = remainingText;
                } else {
                    value = "";
                }
            }
        } else {
            value += " " + cleanUpText(hypothesis);
        }
    }

    private void saveBufferedValue() {
        if (!value.trim().isEmpty()) {
            try {
                soundWrote.start();
                report.writeToReport(keyword, value.trim());
                System.out.println("➡️ Данные записаны: [" + keyword + "] => [" + value.trim() + "]");
            } catch (IOException e) {
            }
        }
        recordingStarted = false;
        value = "";
    }

    private String cleanUpText(String text) {
        return text.replaceAll("\"", "").replaceAll("\\s+", " ").replaceAll("\\s*[{}:]\\s*", "").replaceAll("text", "").replaceAll("partial", "").trim();
    }
}