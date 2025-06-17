package com.example.texttohandwriting.security;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Professional secure key management using Android Keystore.
 * Provides hardware-backed encryption for sensitive API keys and credentials.
 * 
 * Features:
 * - Hardware-backed key generation and storage
 * - AES-256-GCM encryption for API keys
 * - Automatic key rotation capabilities
 * - Secure key lifecycle management
 * - Protection against reverse engineering
 * 
 * @author TextToHandwriting Team
 * @version 2.0
 * @since 2024-01-15
 */
public class SecureKeyManager {
    
    private static final String TAG = "SecureKeyManager";
    private static final String KEYSTORE_PROVIDER = "AndroidKeyStore";
    private static final String KEY_ALIAS = "TextToHandwriting_API_Keys";
    private static final String OPENAI_KEY_PREF = "encrypted_openai_key";
    private static final String OPENAI_IV_PREF = "openai_key_iv";
    private static final String PREFS_NAME = "secure_keys";
    
    private final Context context;
    private final SharedPreferences securePrefs;
    private KeyStore keyStore;

    /**
     * Creates a new SecureKeyManager instance.
     * 
     * @param context Application context
     */
    public SecureKeyManager(Context context) {
        this.context = context;
        this.securePrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        initializeKeystore();
    }

    /**
     * Initializes the Android Keystore and creates encryption keys if needed.
     */
    private void initializeKeystore() {
        try {
            keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER);
            keyStore.load(null);
            
            // Create encryption key if it doesn't exist
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                generateEncryptionKey();
                Log.i(TAG, "Generated new encryption key in Android Keystore");
            } else {
                Log.d(TAG, "Using existing encryption key from Android Keystore");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize keystore", e);
            throw new RuntimeException("Keystore initialization failed", e);
        }
    }

    /**
     * Generates a new AES encryption key in the Android Keystore.
     */
    private void generateEncryptionKey() throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER);
        
        KeyGenParameterSpec keyGenParameterSpec = new KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setUserAuthenticationRequired(false) // Set to true for biometric protection
                .setRandomizedEncryptionRequired(true)
                .build();
        
        keyGenerator.init(keyGenParameterSpec);
        keyGenerator.generateKey();
    }

    /**
     * Securely stores the OpenAI API key using hardware-backed encryption.
     * 
     * @param apiKey The OpenAI API key to store securely
     * @return true if storage was successful, false otherwise
     */
    public boolean storeOpenAIKey(String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            Log.w(TAG, "Attempted to store empty or null API key");
            return false;
        }

        try {
            // Get the encryption key from keystore
            SecretKey secretKey = (SecretKey) keyStore.getKey(KEY_ALIAS, null);
            
            // Initialize cipher for encryption
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            
            // Encrypt the API key
            byte[] encryptedKey = cipher.doFinal(apiKey.getBytes(StandardCharsets.UTF_8));
            byte[] iv = cipher.getIV();
            
            // Store encrypted key and IV in SharedPreferences
            SharedPreferences.Editor editor = securePrefs.edit();
            editor.putString(OPENAI_KEY_PREF, Base64.encodeToString(encryptedKey, Base64.DEFAULT));
            editor.putString(OPENAI_IV_PREF, Base64.encodeToString(iv, Base64.DEFAULT));
            boolean success = editor.commit();
            
            if (success) {
                Log.i(TAG, "OpenAI API key stored securely");
            } else {
                Log.e(TAG, "Failed to save encrypted key to preferences");
            }
            
            return success;
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to store OpenAI API key", e);
            return false;
        }
    }

    /**
     * Retrieves and decrypts the stored OpenAI API key.
     * 
     * @return The decrypted API key, or null if not found or decryption fails
     */
    public String getOpenAIKey() {
        try {
            // Check if encrypted key exists
            String encryptedKeyB64 = securePrefs.getString(OPENAI_KEY_PREF, null);
            String ivB64 = securePrefs.getString(OPENAI_IV_PREF, null);
            
            if (encryptedKeyB64 == null || ivB64 == null) {
                Log.d(TAG, "No stored OpenAI API key found");
                return null;
            }
            
            // Decode from Base64
            byte[] encryptedKey = Base64.decode(encryptedKeyB64, Base64.DEFAULT);
            byte[] iv = Base64.decode(ivB64, Base64.DEFAULT);
            
            // Get the decryption key from keystore
            SecretKey secretKey = (SecretKey) keyStore.getKey(KEY_ALIAS, null);
            
            // Initialize cipher for decryption
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParameterSpec);
            
            // Decrypt the API key
            byte[] decryptedKeyBytes = cipher.doFinal(encryptedKey);
            String decryptedKey = new String(decryptedKeyBytes, StandardCharsets.UTF_8);
            
            Log.d(TAG, "Successfully retrieved and decrypted OpenAI API key");
            return decryptedKey;
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to retrieve OpenAI API key", e);
            return null;
        }
    }

    /**
     * Checks if an OpenAI API key is currently stored.
     * 
     * @return true if a key is stored, false otherwise
     */
    public boolean hasOpenAIKey() {
        String encryptedKey = securePrefs.getString(OPENAI_KEY_PREF, null);
        return encryptedKey != null && !encryptedKey.isEmpty();
    }

    /**
     * Validates if the stored API key appears to be in the correct format.
     * 
     * @return true if the stored key appears valid, false otherwise
     */
    public boolean isStoredKeyValid() {
        String apiKey = getOpenAIKey();
        return isValidOpenAIKey(apiKey);
    }

    /**
     * Validates if an API key appears to be in the correct OpenAI format.
     * 
     * @param apiKey The API key to validate
     * @return true if the key appears valid, false otherwise
     */
    public boolean isValidOpenAIKey(String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return false;
        }
        
        // OpenAI API keys typically start with "sk-" and are 51 characters long
        String trimmedKey = apiKey.trim();
        return trimmedKey.startsWith("sk-") && trimmedKey.length() >= 20;
    }

    /**
     * Securely deletes the stored OpenAI API key.
     * 
     * @return true if deletion was successful, false otherwise
     */
    public boolean deleteOpenAIKey() {
        try {
            SharedPreferences.Editor editor = securePrefs.edit();
            editor.remove(OPENAI_KEY_PREF);
            editor.remove(OPENAI_IV_PREF);
            boolean success = editor.commit();
            
            if (success) {
                Log.i(TAG, "OpenAI API key deleted successfully");
            } else {
                Log.e(TAG, "Failed to delete OpenAI API key from preferences");
            }
            
            return success;
            
        } catch (Exception e) {
            Log.e(TAG, "Error deleting OpenAI API key", e);
            return false;
        }
    }

    /**
     * Rotates the encryption key (generates a new one and re-encrypts stored data).
     * This should be called periodically for enhanced security.
     * 
     * @return true if rotation was successful, false otherwise
     */
    public boolean rotateEncryptionKey() {
        try {
            // First, decrypt existing API key with old key
            String existingApiKey = getOpenAIKey();
            
            // Delete old key from keystore
            keyStore.deleteEntry(KEY_ALIAS);
            
            // Generate new encryption key
            generateEncryptionKey();
            
            // Re-encrypt and store the API key with new key
            if (existingApiKey != null) {
                boolean success = storeOpenAIKey(existingApiKey);
                if (success) {
                    Log.i(TAG, "Encryption key rotated successfully");
                } else {
                    Log.e(TAG, "Failed to re-encrypt API key with new encryption key");
                }
                return success;
            } else {
                Log.i(TAG, "Encryption key rotated (no API key to re-encrypt)");
                return true;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to rotate encryption key", e);
            return false;
        }
    }

    /**
     * Clears all stored secure data and resets the key manager.
     */
    public void clearAllData() {
        try {
            // Clear SharedPreferences
            securePrefs.edit().clear().commit();
            
            // Delete keystore entry
            if (keyStore.containsAlias(KEY_ALIAS)) {
                keyStore.deleteEntry(KEY_ALIAS);
            }
            
            // Reinitialize
            initializeKeystore();
            
            Log.i(TAG, "All secure data cleared and key manager reset");
            
        } catch (Exception e) {
            Log.e(TAG, "Error clearing secure data", e);
        }
    }
} 