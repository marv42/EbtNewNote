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

    public SharedPreferences getSharedPreferences() {
        return mSharedPreferences;
    }

    @NonNull
    String get(int keyResId, String defValue) {
        return get(mApp.getString(keyResId), defValue);
    }

    @NonNull
    private String get(String key, String defValue) {
        String value = getString(key, defValue);
        if (value == null)
            return defValue;
        return value;
    }

    @Nullable
    @Override
    public String getString(String key, @Nullable String defValue) {
        return mSharedPreferences.getString(key, defValue);
    }

    void putStringById(int keyResId, String value) {
        putString(mApp.getString(keyResId), value);
    }

    @Override
    public void putString(String key, @Nullable String value) {
        mSharedPreferences.edit().putString(key, value).apply();
    }
}
