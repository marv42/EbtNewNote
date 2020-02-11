package com.marv42.ebt.newnote;

import android.content.SharedPreferences;
import android.text.TextUtils;

import javax.inject.Inject;

import static com.marv42.ebt.newnote.scanning.Keys.OCR_SERVICE;

public class SharedPreferencesHandler {
    private ThisApp mApp;
    private SharedPreferences mSharedPreferences;

    @Inject
    public SharedPreferencesHandler(ThisApp app, SharedPreferences sharedPreferences) {
        mApp = app;
        mSharedPreferences = sharedPreferences;
        set(R.string.pref_settings_ocr_key, OCR_SERVICE);
    }

    <T> T get(int prefResId, T defValue) {
        return get(mApp.getString(prefResId), defValue);
    }

    <T> T get(String prefRes, T defValue) {
        if (defValue instanceof String)
            return (T) mSharedPreferences.getString(prefRes, (String) defValue);
        if (defValue instanceof Boolean)
            return (T) (Boolean) mSharedPreferences.getBoolean(prefRes, (Boolean) defValue);
        throw new IllegalArgumentException("Unknown instance of defValue " + defValue);
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
            throw new IllegalArgumentException("Unknown instance of value " + value);
    }
}
