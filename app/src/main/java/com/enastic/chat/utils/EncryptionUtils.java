package com.enastic.chat.utils;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.security.KeyStore;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Utilitaire pour chiffrer et déchiffrer les messages.
 * Utilise désormais Android Keystore pour la sécurité, avec une rotation de clé pour rétrocompatibilité.
 */
public class EncryptionUtils {
    
    private static final String OLD_ALGORITHM = "AES";
    private static final byte[] OLD_KEY = "ChatExpertKey123".getBytes();
    
    private static final String KEYSTORE_PROVIDER = "AndroidKeyStore";
    private static final String KEY_ALIAS = "ChatAppSecureKeyAlias";
    private static final String ALGORITHM_GCM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12; // GCM recommande 12 octets
    private static final int GCM_TAG_LENGTH = 128; // 128 bits
    private static final String PREFIX_V2 = "v2:";

    /**
     * Récupère ou génère la clé sécurisée dans l'Android Keystore.
     */
    private static SecretKey getSecretKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER);
        keyStore.load(null);
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER);
            KeyGenParameterSpec keyGenParameterSpec = new KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build();
            keyGenerator.init(keyGenParameterSpec);
            keyGenerator.generateKey();
        }
        return ((KeyStore.SecretKeyEntry) keyStore.getEntry(KEY_ALIAS, null)).getSecretKey();
    }

    /**
     * Chiffre un texte (V1 statique pour que les autres utilisateurs puissent déchiffrer).
     */
    public static String encrypt(String text) {
        if (text == null || text.isEmpty()) return text;
        try {
            SecretKeySpec secretKey = new SecretKeySpec(OLD_KEY, OLD_ALGORITHM);
            Cipher cipher = Cipher.getInstance(OLD_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encryptedBytes = cipher.doFinal(text.getBytes());
            return Base64.encodeToString(encryptedBytes, Base64.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
            return text;
        }
    }

    /**
     * Déchiffre un texte (supporte V1 statique et V2 Keystore).
     */
    public static String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isEmpty()) return encryptedText;
        
        try {
            if (encryptedText.startsWith(PREFIX_V2)) {
                // Déchiffrement V2 (Keystore)
                String base64Text = encryptedText.substring(PREFIX_V2.length());
                byte[] combined = Base64.decode(base64Text, Base64.DEFAULT);
                
                byte[] iv = Arrays.copyOfRange(combined, 0, GCM_IV_LENGTH);
                byte[] encryptedBytes = Arrays.copyOfRange(combined, GCM_IV_LENGTH, combined.length);
                
                SecretKey secretKey = getSecretKey();
                Cipher cipher = Cipher.getInstance(ALGORITHM_GCM);
                GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
                cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);
                
                byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
                return new String(decryptedBytes);
            } else {
                // Déchiffrement V1 (Rétrocompatibilité avec l'ancienne clé en dur)
                SecretKeySpec secretKey = new SecretKeySpec(OLD_KEY, OLD_ALGORITHM);
                Cipher cipher = Cipher.getInstance(OLD_ALGORITHM);
                cipher.init(Cipher.DECRYPT_MODE, secretKey);
                byte[] decodedBytes = Base64.decode(encryptedText, Base64.DEFAULT);
                byte[] decryptedBytes = cipher.doFinal(decodedBytes);
                return new String(decryptedBytes);
            }
        } catch (Exception e) {
            // Si le déchiffrement échoue (ex: message non chiffré), on renvoie le texte tel quel
            return encryptedText;
        }
    }
}
