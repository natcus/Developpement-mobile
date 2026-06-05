package com.enastic.chat.utils;

import android.util.Log;
import com.enastic.chat.BuildConfig;

/**
 * Utilitaire de log centralisé pour la maintenance et le débogage.
 * Filtre automatiquement les logs en production pour plus de sécurité.
 */
public class AppLogger {
    private static final String TAG = "ChatApp_Log";

    public static void d(String message) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, message);
        }
    }

    public static void e(String message, Throwable throwable) {
        Log.e(TAG, message, throwable);
        // Ici, on pourrait envoyer l'erreur vers Firebase Crashlytics
    }

    public static void i(String message) {
        Log.i(TAG, message);
    }
}
