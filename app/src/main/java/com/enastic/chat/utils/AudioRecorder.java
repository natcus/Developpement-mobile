package com.enastic.chat.utils;

import android.content.Context;
import android.media.MediaRecorder;
import java.io.File;
import java.io.IOException;

/**
 * Utilitaire pour enregistrer des messages vocaux.
 */
public class AudioRecorder {

    private MediaRecorder recorder;
    private String lastFilePath;
    private long startTime;

    /**
     * Commence l'enregistrement dans un fichier temporaire.
     */
    public void startRecording(Context context) {
        lastFilePath = context.getExternalCacheDir().getAbsolutePath() + "/vocal_" + System.currentTimeMillis() + ".m4a";
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        recorder.setOutputFile(lastFilePath);

        try {
            recorder.prepare();
            recorder.start();
            startTime = System.currentTimeMillis();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Arrête l'enregistrement et renvoie le chemin du fichier et la durée.
     */
    public RecordingResult stopRecording() {
        if (recorder != null) {
            try {
                recorder.stop();
            } catch (RuntimeException e) {
                // Si l'enregistrement est trop court
                return null;
            }
            recorder.release();
            recorder = null;
            
            int duration = (int) ((System.currentTimeMillis() - startTime) / 1000);
            return new RecordingResult(lastFilePath, duration);
        }
        return null;
    }

    public static class RecordingResult {
        public String filePath;
        public int durationSeconds;

        public RecordingResult(String filePath, int durationSeconds) {
            this.filePath = filePath;
            this.durationSeconds = durationSeconds;
        }
    }
}
