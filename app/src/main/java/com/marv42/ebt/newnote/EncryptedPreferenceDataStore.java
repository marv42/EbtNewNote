package com.marv42.ebt.newnote;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceDataStore;
import androidx.security.crypto.EncryptedSharedPreferences;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Set;


public class EncryptedPreferenceDataStore extends PreferenceDataStore {
    private static EncryptedPreferenceDataStore mInstance;
    private SharedPreferences mSharedPreferences;

    private EncryptedPreferenceDataStore(EncryptedSharedPreferencesProvider provider) {
        try {
            mSharedPreferences = provider.getEncryptedSharedPreferences();
        } catch (GeneralSecurityException | IOException e) {
//            mSharedPreferences = getPreferenceScreen().getSharedPreferences();
        }
    }

    PreferenceDataStore getInstance(EncryptedSharedPreferencesProvider provider) {
        if (mInstance == null)
            mInstance = new EncryptedPreferenceDataStore(provider);
        return mInstance;
    }

    @Override
    public void putString(String key, @Nullable String value) {
        mSharedPreferences.edit().putString(key, value).apply();
    }

    @Nullable
    @Override
    public String getString(String key, @Nullable String defValue) {
        return mSharedPreferences.getString(key, defValue);
    }
}
