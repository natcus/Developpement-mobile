package com.enastic.chat.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class ImageCompressor {

    /**
     * Lit une image depuis une Uri, la redimensionne (max 600x600),
     * la compresse en JPEG 60% et retourne une chaîne Base64.
     */
    public static String compressUriToBase64(Context context, Uri uri) {
        try {
            // Étape 1 : Lire les dimensions de l'image sans charger tout en mémoire
            InputStream input = context.getContentResolver().openInputStream(uri);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(input, null, options);
            input.close();

            // Étape 2 : Calculer le facteur de réduction (inSampleSize)
            int reqWidth = 600;
            int reqHeight = 600;
            int inSampleSize = 1;

            if (options.outHeight > reqHeight || options.outWidth > reqWidth) {
                final int halfHeight = options.outHeight / 2;
                final int halfWidth = options.outWidth / 2;
                while ((halfHeight / inSampleSize) >= reqHeight
                        && (halfWidth / inSampleSize) >= reqWidth) {
                    inSampleSize *= 2;
                }
            }

            // Étape 3 : Charger l'image redimensionnée en mémoire
            options.inJustDecodeBounds = false;
            options.inSampleSize = inSampleSize;
            
            input = context.getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(input, null, options);
            input.close();

            if (bitmap != null) {
                // Étape 4 : Compresser en JPEG et convertir en Base64
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 60, baos);
                byte[] bytes = baos.toByteArray();
                return Base64.encodeToString(bytes, Base64.DEFAULT);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**

     * Compresse une image et retourne les octets (byte[]) pour upload direct.
     */
    public static byte[] compressUriToBytes(Context context, Uri uri) {
        try {
            InputStream input = context.getContentResolver().openInputStream(uri);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(input, null, options);
            input.close();

            int reqWidth = 1024; // Un peu plus large pour les uploads Storage
            int reqHeight = 1024;
            int inSampleSize = 1;

            if (options.outHeight > reqHeight || options.outWidth > reqWidth) {
                final int halfHeight = options.outHeight / 2;
                final int halfWidth = options.outWidth / 2;
                while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                    inSampleSize *= 2;
                }
            }

            options.inJustDecodeBounds = false;
            options.inSampleSize = inSampleSize;
            input = context.getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(input, null, options);
            input.close();

            if (bitmap != null) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos); // Qualité 70% pour Storage
                return baos.toByteArray();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}

