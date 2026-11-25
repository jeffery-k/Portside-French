package com.example.portside.util;

import android.app.AlertDialog;
import android.os.Looper;
import android.content.Context;
import java.io.PrintWriter;
import java.io.StringWriter;

public class ExceptionHandler {
    public static void interceptExceptions(Context context) {
        Thread.UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();

        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            throwable.printStackTrace(printWriter);
            String stackTrace = stringWriter.toString();

            new Thread(() -> {
                Looper.prepare();
                new AlertDialog.Builder(context)
                        .setTitle("App Crashed")
                        .setMessage(stackTrace)
                        .setCancelable(false)
                        .setPositiveButton("Close App", (dialog, which) -> {
                            if (defaultHandler != null) {
                                defaultHandler.uncaughtException(thread, throwable);
                            } else {
                                System.exit(1);
                            }
                        })
                        .show();
                Looper.loop();
            }).start();
        });
    }
}
