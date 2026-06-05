package com.enastic.chat.utils;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.util.Base64;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class FileUtils {

    public static final int MAX_FILE_SIZE_BYTES = 500 * 1024; // 500 Ko

    /**
     * Récupère le nom du fichier à partir de son Uri.
     */
    public static String getFileName(Context context, Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index != -1) {
                        result = cursor.getString(index);
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    /**
     * Lit un fichier, vérifie sa taille, et le convertit en Base64.
     */
    public static String encodeFileToBase64(Context context, Uri uri) throws Exception {
        InputStream inputStream = context.getContentResolver().openInputStream(uri);
        if (inputStream == null) return null;

        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        int len;
        int totalBytes = 0;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
            totalBytes += len;
            if (totalBytes > MAX_FILE_SIZE_BYTES) {
                inputStream.close();
                throw new Exception("File too large"); // Fichier trop grand
            }
        }
        inputStream.close();
        byte[] fileBytes = byteBuffer.toByteArray();
        return Base64.encodeToString(fileBytes, Base64.DEFAULT);
    }

    /**
     * Sauvegarde un fichier Base64 sur le stockage local (Téléchargements) et l'ouvre.
     */
    public static void saveBase64ToFile(Context context, String base64, String fileName, String mimeType) {
        try {
            byte[] fileBytes = Base64.decode(base64, Base64.DEFAULT);
            File downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
            if (downloadsDir == null) {
                downloadsDir = context.getCacheDir();
            }
            File file = new File(downloadsDir, fileName);
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(fileBytes);
            fos.close();
            
            // Ouvrir le fichier avec un Intent ACTION_VIEW
            Uri fileUri = androidx.core.content.FileProvider.getUriForFile(
                context, 
                context.getApplicationContext().getPackageName() + ".fileprovider", 
                file
            );
            
            android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW);
            intent.setDataAndType(fileUri, mimeType != null && !mimeType.isEmpty() ? mimeType : "*/*");
            intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            // On lance l'application tierce pour ouvrir le fichier
            context.startActivity(intent);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "Erreur ou impossible d'ouvrir le fichier : " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Télécharge un fichier depuis une URL HTTP en arrière-plan.
     * Utilise MediaStore sur Android 10+ pour enregistrer le fichier dans le dossier public "Downloads" sans avoir besoin de permissions de stockage.
     * Peut ouvrir le fichier immédiatement après le téléchargement si openAfterDownload est vrai.
     */
    public static void downloadFile(Context context, String url, String fileName, String mimeType, boolean openAfterDownload) {
        Toast.makeText(context, openAfterDownload ? "Ouverture du fichier en cours..." : "Téléchargement en cours...", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                java.net.URL urlObj = new java.net.URL(url);
                java.net.HttpURLConnection connection = (java.net.HttpURLConnection) urlObj.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Mobile Safari/537.36");
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);
                connection.setInstanceFollowRedirects(true);
                connection.connect();

                if (connection.getResponseCode() == java.net.HttpURLConnection.HTTP_OK) {
                    java.io.InputStream input = connection.getInputStream();
                    java.io.OutputStream output = null;
                    Uri fileUri = null;
                    File legacyFile = null;

                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        android.content.ContentValues values = new android.content.ContentValues();
                        values.put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                        values.put(android.provider.MediaStore.MediaColumns.MIME_TYPE, mimeType != null && !mimeType.isEmpty() ? mimeType : "*/*");
                        values.put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                        fileUri = context.getContentResolver().insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                        if (fileUri != null) {
                            output = context.getContentResolver().openOutputStream(fileUri);
                        }
                    } else {
                        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                        if (!downloadsDir.exists()) {
                            downloadsDir.mkdirs();
                        }
                        legacyFile = new File(downloadsDir, fileName);
                        output = new java.io.FileOutputStream(legacyFile);
                    }

                    if (output == null) {
                        throw new Exception("Impossible de créer le fichier sur le stockage.");
                    }

                    byte[] data = new byte[8192];
                    int count;
                    while ((count = input.read(data)) != -1) {
                        output.write(data, 0, count);
                    }

                    output.flush();
                    output.close();
                    input.close();

                    final Uri finalUri = fileUri;
                    final File finalLegacyFile = legacyFile;

                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        if (openAfterDownload) {
                            try {
                                Uri uriToOpen;
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                                    uriToOpen = finalUri;
                                } else {
                                    uriToOpen = androidx.core.content.FileProvider.getUriForFile(
                                        context, 
                                        context.getApplicationContext().getPackageName() + ".fileprovider", 
                                        finalLegacyFile
                                    );
                                }

                                android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW);
                                intent.setDataAndType(uriToOpen, mimeType != null && !mimeType.isEmpty() ? mimeType : "*/*");
                                intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
                                context.startActivity(intent);
                            } catch (Exception e) {
                                e.printStackTrace();
                                Toast.makeText(context, "Aucune application trouvée pour ouvrir ce fichier.", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(context, "Téléchargement terminé : " + fileName, Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    final String responseMsg = connection.getResponseMessage();
                    final String cldErrorMsg = connection.getHeaderField("X-Cld-Error");
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        String displayError = "Erreur de chargement (" + responseMsg + ")";
                        if (cldErrorMsg != null && !cldErrorMsg.isEmpty()) {
                            displayError += " : " + cldErrorMsg;
                        }
                        Toast.makeText(context, displayError, Toast.LENGTH_LONG).show();
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> 
                    Toast.makeText(context, "Erreur lors du téléchargement : " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }
}
