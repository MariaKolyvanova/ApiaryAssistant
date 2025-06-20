package com.apiarymanager.apiaryassistant;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class FileManager {

    public static void importExcelFile(Context context, Uri sourceUri, String fileName) throws IOException {
        File dir = new File(context.getFilesDir(), "/patterns");
        if (!dir.exists() && !dir.mkdirs()) {
            if (!dir.mkdir()) {
                Log.e("FileManager", "Не удалось создать директорию: " + dir.getAbsolutePath());
            }
        }

        File destFile = new File(dir, fileName);

        InputStream inputStream = context.getContentResolver().openInputStream(sourceUri);
        if (inputStream == null) {
            throw new IOException("Не удалось открыть выбранный файл");
        }

        Files.copy(inputStream, destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    public static String getFileNameFromUri(Context context, Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex >= 0) {
                        result = cursor.getString(nameIndex);
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getLastPathSegment();
            if (result != null && result.contains("/")) {
                result = result.substring(result.lastIndexOf("/") + 1);
            }
        }
        return result;

    }
}