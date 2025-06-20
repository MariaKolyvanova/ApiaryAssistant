package com.apiarymanager.apiaryassistant;

import android.app.Application;

import java.io.IOException;

public class Active extends Application {
    private Report activeReport;

    public Report getActiveReport() {
        return activeReport;
    }

    public void setActiveReport(Report report) {
        if (getActiveReport() != null){
            try {
                getActiveReport().closeBook();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        if (getActiveReport() != (report)){
            this.activeReport = report;
        }
    }
}
