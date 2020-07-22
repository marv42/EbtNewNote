package com.marv42.ebt.newnote;

import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceDataStore;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;

import javax.inject.Inject;

import static android.security.keystore.KeyProperties.BLOCK_MODE_GCM;
import static android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE;
import static android.security.keystore.KeyProperties.PURPOSE_DECRYPT;
import static android.security.keystore.KeyProperties.PURPOSE_ENCRYPT;
import static androidx.preference.PreferenceManager.getDefaultSharedPreferences;
import static androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV;
import static androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM;
import static androidx.security.crypto.MasterKey.DEFAULT_MASTER_KEY_ALIAS;

public class EncryptedPreferenceDataStore extends PreferenceDataStore {
    private static final String ENCRYPTED_SHARED_PREFERENCES_FILE_NAME = "ebt_new_note_encrypted_shared_prefs";
//    private static final String MASTER_KEY_ALIAS = "ebt_new_note_master_key";
    private static final int KEY_SIZE = 256;

    private ThisApp mApp;
    private SharedPreferences mSharedPreferences;

    @Inject
    public EncryptedPreferenceDataStore(ThisApp app) {
        mApp = app;
        try {
            mSharedPreferences = EncryptedSharedPreferences.create(app,
                    ENCRYPTED_SHARED_PREFERENCES_FILE_NAME,
                    getMasterKey(), AES256_SIV, AES256_GCM);
        } catch (GeneralSecurityException | IOException e) {
            mSharedPreferences = getDefaultSharedPreferences(app);
        }
    }

    private MasterKey getMasterKey() throws GeneralSecurityException, IOException {
        KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(
                DEFAULT_MASTER_KEY_ALIAS, PURPOSE_ENCRYPT | PURPOSE_DECRYPT)
                .setBlockModes(BLOCK_MODE_GCM)
                .setEncryptionPaddings(ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_SIZE)
                .build();
        return new MasterKey.Builder(mApp)
//                .setKeyScheme(AES256_GCM)
                .setKeyGenParameterSpec(spec)
                .build();
    }

    SharedPreferences getSharedPreferences() {
        return mSharedPreferences;
    }

    @NonNull
    <T> T get(int keyId, T defValue) {
        return get(mApp.getString(keyId), defValue);
    }

    @NonNull
    private <T> T get(String key, T defValue) {
        T value;
        if (defValue instanceof String)
            value = (T) getString(key, (String) defValue);
        else if (defValue instanceof Boolean)
            value = (T) (Boolean) getBoolean(key, (Boolean) defValue);
        else
            throw new IllegalArgumentException("Defvalue " + defValue + " is instance of unhandled class");
        if (value == null)
            return defValue;
        return value;
    }

    <T> void set(int keyId, T value) {
        set(mApp.getString(keyId), value);
    }

    <T> void set(String key, T value) {
        if (value instanceof String)
            putString(key, (String) value);
        else if (value instanceof Boolean)
            putBoolean(key, (Boolean) value);
        else
            throw new IllegalArgumentException("Value " + value + " is instance of unhandled class");
    }

    @Nullable
    @Override
    public String getString(String key, @Nullable String defValue) {
        return mSharedPreferences.getString(key, defValue);
    }

    @Override
    public void putString(String key, @Nullable String value) {
        mSharedPreferences.edit().putString(key, value).apply();
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
        return mSharedPreferences.getBoolean(key, defValue);
    }

    @Override
    public void putBoolean(String key, boolean value) {
        mSharedPreferences.edit().putBoolean(key, value).apply();
    }
}
