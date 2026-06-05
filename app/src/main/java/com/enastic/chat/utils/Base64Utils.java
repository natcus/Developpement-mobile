package com.enastic.chat.utils;

import android.content.Context;
import android.net.Uri;
import android.util.Base64;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Utilitaire pour convertir des fichiers (audio, pdf, etc.) en chaînes Base64
 * permettant le stockage gratuit dans Firestore.
 */
public class Base64Utils {

    /**
     * Convertit un fichier local (ex: audio) en chaîne Base64.
     */
    public static String encodeFileToBase64(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) return null;

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] bytes = new byte[(int) file.length()];
            fis.read(bytes);
            return Base64.encodeToString(bytes, Base64.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Convertit un fichier depuis une Uri en chaîne Base64.
     */
    public static String encodeUriToBase64(Context context, Uri uri) {
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri)) {
            if (inputStream == null) return null;
            byte[] bytes = new byte[inputStream.available()];
            inputStream.read(bytes);
            return Base64.encodeToString(bytes, Base64.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
