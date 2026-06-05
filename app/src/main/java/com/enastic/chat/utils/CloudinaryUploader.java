package com.enastic.chat.utils;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Utilitaire pour envoyer des fichiers sur Cloudinary (Alternative gratuite à
 * Firebase Storage).
 * Ne nécessite pas de SDK supplémentaire.
 */
public class CloudinaryUploader {

    // TODO: REMPLACER CES DEUX VALEURS PAR LES VÔTRES !
    public static final String CLOUD_NAME = "dolktftvj";
    public static final String UPLOAD_PRESET = "ml_default"; // Doit être en mode "Unsigned"

    public interface UploadCallback {
        void onSuccess(String secureUrl);

        void onError(String error);
    }

    /**
     * Upload à partir d'un tableau de bytes (ex: image compressée).
     * Envoie directement les données en mémoire via HTTP sans passer par un fichier temporaire.
     */
    public static void uploadBytes(byte[] data, String fileName, String resourceType, UploadCallback callback) {
        new Thread(() -> {
            HttpURLConnection connection = null;
            DataOutputStream outputStream = null;

            String boundary = "*****" + System.currentTimeMillis() + "*****";
            String lineEnd = "\r\n";
            String twoHyphens = "--";

            String finalResourceType = (resourceType == null || resourceType.isEmpty()) ? "auto" : resourceType;
            String uploadUrl = "https://api.cloudinary.com/v1_1/" + CLOUD_NAME + "/" + finalResourceType + "/upload";

            try {
                URL url = new URL(uploadUrl);
                connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.setDoOutput(true);
                connection.setUseCaches(false);
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Connection", "Keep-Alive");
                connection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);

                outputStream = new DataOutputStream(connection.getOutputStream());

                // Champ Upload Preset
                outputStream.writeBytes(twoHyphens + boundary + lineEnd);
                outputStream.writeBytes("Content-Disposition: form-data; name=\"upload_preset\"" + lineEnd + lineEnd);
                outputStream.writeBytes(UPLOAD_PRESET + lineEnd);

                // Champ Fichier
                outputStream.writeBytes(twoHyphens + boundary + lineEnd);
                outputStream.writeBytes(
                        "Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"" + lineEnd);
                outputStream.writeBytes("Content-Type: application/octet-stream" + lineEnd + lineEnd);

                // Écrit les bytes directement en RAM
                outputStream.write(data);

                outputStream.writeBytes(lineEnd);
                outputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                int serverResponseCode = connection.getResponseCode();

                if (serverResponseCode == 200 || serverResponseCode == 201) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();

                    JSONObject jsonObject = new JSONObject(response.toString());
                    String secureUrl = jsonObject.getString("secure_url");

                    new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(secureUrl));
                } else {
                    java.io.InputStream errStream = connection.getErrorStream();
                    String errorMessage = "Erreur d'envoi. Code: " + serverResponseCode;
                    if (errStream != null) {
                        BufferedReader err = new BufferedReader(new InputStreamReader(errStream));
                        StringBuilder errResponse = new StringBuilder();
                        String inputLine;
                        while ((inputLine = err.readLine()) != null) {
                            errResponse.append(inputLine);
                        }
                        err.close();
                        Log.e("Cloudinary", "Erreur Serveur: " + errResponse.toString());
                        try {
                            JSONObject errJson = new JSONObject(errResponse.toString());
                            if (errJson.has("error")) {
                                errorMessage = errJson.getJSONObject("error").getString("message");
                            }
                        } catch (Exception ignored) {}
                    }
                    final String finalErr = errorMessage;
                    new Handler(Looper.getMainLooper()).post(() -> callback.onError(finalErr));
                }

            } catch (Exception e) {
                e.printStackTrace();
                new Handler(Looper.getMainLooper()).post(() -> callback.onError(e.getMessage()));
            } finally {
                try {
                    if (outputStream != null) {
                        outputStream.flush();
                        outputStream.close();
                    }
                    if (connection != null)
                        connection.disconnect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * Upload à partir d'un fichier direct.
     */
    public static void uploadFile(File file, String resourceType, UploadCallback callback) {

        new Thread(() -> {
            HttpURLConnection connection = null;
            DataOutputStream outputStream = null;
            FileInputStream fileInputStream = null;

            String boundary = "*****" + System.currentTimeMillis() + "*****";
            String lineEnd = "\r\n";
            String twoHyphens = "--";

            // "image", "video", "raw", ou "auto"
            String finalResourceType = (resourceType == null || resourceType.isEmpty()) ? "auto" : resourceType;

            String uploadUrl = "https://api.cloudinary.com/v1_1/" + CLOUD_NAME + "/" + finalResourceType + "/upload";

            try {
                URL url = new URL(uploadUrl);
                connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.setDoOutput(true);
                connection.setUseCaches(false);
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Connection", "Keep-Alive");
                connection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);

                outputStream = new DataOutputStream(connection.getOutputStream());

                // Champ Upload Preset
                outputStream.writeBytes(twoHyphens + boundary + lineEnd);
                outputStream.writeBytes("Content-Disposition: form-data; name=\"upload_preset\"" + lineEnd + lineEnd);
                outputStream.writeBytes(UPLOAD_PRESET + lineEnd);

                // Champ Fichier
                outputStream.writeBytes(twoHyphens + boundary + lineEnd);
                outputStream.writeBytes(
                        "Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getName() + "\"" + lineEnd);
                outputStream.writeBytes("Content-Type: application/octet-stream" + lineEnd + lineEnd);

                fileInputStream = new FileInputStream(file);
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }

                outputStream.writeBytes(lineEnd);
                outputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                int serverResponseCode = connection.getResponseCode();

                if (serverResponseCode == 200 || serverResponseCode == 201) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();

                    JSONObject jsonObject = new JSONObject(response.toString());
                    String secureUrl = jsonObject.getString("secure_url");

                    new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(secureUrl));
                } else {
                    java.io.InputStream errStream = connection.getErrorStream();
                    String errorMessage = "Erreur d'envoi. Code: " + serverResponseCode;
                    if (errStream != null) {
                        BufferedReader err = new BufferedReader(new InputStreamReader(errStream));
                        StringBuilder errResponse = new StringBuilder();
                        String inputLine;
                        while ((inputLine = err.readLine()) != null) {
                            errResponse.append(inputLine);
                        }
                        err.close();
                        Log.e("Cloudinary", "Erreur Serveur: " + errResponse.toString());
                        try {
                            JSONObject errJson = new JSONObject(errResponse.toString());
                            if (errJson.has("error")) {
                                errorMessage = errJson.getJSONObject("error").getString("message");
                            }
                        } catch (Exception ignored) {}
                    }
                    final String finalErr = errorMessage;
                    new Handler(Looper.getMainLooper()).post(() -> callback.onError(finalErr));
                }

            } catch (Exception e) {
                e.printStackTrace();
                new Handler(Looper.getMainLooper()).post(() -> callback.onError(e.getMessage()));
            } finally {
                try {
                    if (fileInputStream != null)
                        fileInputStream.close();
                    if (outputStream != null) {
                        outputStream.flush();
                        outputStream.close();
                    }
                    if (connection != null)
                        connection.disconnect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
