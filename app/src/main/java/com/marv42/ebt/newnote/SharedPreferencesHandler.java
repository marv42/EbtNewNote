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
        if (! OCR_SERVICE.isEmpty())
            set(R.string.pref_settings_ocr_key, OCR_SERVICE);
    }

    <T> T get(int prefResId, T defValue) {
        return get(mApp.getString(prefResId), defValue);
    }

    private <T> T get(String prefRes, T defValue) {
        if (defValue instanceof String)
            return (T) mSharedPreferences.getString(prefRes, (String) defValue);
        if (defValue instanceof Boolean)
            return (T) (Boolean) mSharedPreferences.getBoolean(prefRes, (Boolean) defValue);
        throw new IllegalArgumentException("Defvalue " + defValue + " is instance of unhandled class");
    }

    <T> void set(int prefResId, T value) {
        set(mApp.getString(prefResId), value);
    }

    <T> void set(String prefRes, T value) {
        if (value instanceof String)
            mSharedPreferences.edit().putString(prefRes, (String) value).apply();
        else if (value instanceof Boolean)
            mSharedPreferences.edit().putBoolean(prefRes, (Boolean) value).apply();
        else
            throw new IllegalArgumentException("Value " + value + " is instance of unhandled class");
    }
}
