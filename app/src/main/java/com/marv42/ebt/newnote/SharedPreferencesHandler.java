package com.marv42.ebt.newnote;

import android.content.SharedPreferences;

import javax.inject.Inject;

import static com.marv42.ebt.newnote.scanning.Keys.OCR_SERVICE;

public class SharedPreferencesHandler {
    private ThisApp mApp;
    private SharedPreferences mSharedPreferences;

    @Inject
    public SharedPreferencesHandler(ThisApp app, SharedPreferences sharedPreferences) {
        mApp = app;
        mSharedPreferences = sharedPreferences;
    }

    <T> T get(int keyId, T defValue) {
        return get(mApp.getString(keyId), defValue);
    }

    private <T> T get(String key, T defValue) {
        if (defValue instanceof String)
            return (T) mSharedPreferences.getString(key, (String) defValue);
        if (defValue instanceof Boolean)
            return (T) (Boolean) mSharedPreferences.getBoolean(key, (Boolean) defValue);
        throw new IllegalArgumentException("Defvalue " + defValue + " is instance of unhandled class");
    }

    <T> void set(int keyId, T value) {
        set(mApp.getString(keyId), value);
    }

    <T> void set(String key, T value) {
        if (value instanceof String)
            mSharedPreferences.edit().putString(key, (String) value).apply();
        else if (value instanceof Boolean)
            mSharedPreferences.edit().putBoolean(key, (Boolean) value).apply();
        else
            throw new IllegalArgumentException("Value " + value + " is instance of unhandled class");
    }
}
