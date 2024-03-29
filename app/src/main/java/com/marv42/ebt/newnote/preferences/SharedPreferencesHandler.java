/*
 Copyright (c) 2010 - 2022 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote.preferences;

import android.content.SharedPreferences;

import com.marv42.ebt.newnote.ThisApp;

import javax.inject.Inject;

public class SharedPreferencesHandler {

    private final ThisApp app;
    private final SharedPreferences sharedPreferences;

    @Inject
    public SharedPreferencesHandler(ThisApp app, SharedPreferences sharedPreferences) {
        this.app = app;
        this.sharedPreferences = sharedPreferences;
    }

    SharedPreferences getSharedPreferences() {
        return sharedPreferences;
    }

    public <T> T get(int keyId, T defValue) {
        return get(app.getString(keyId), defValue);
    }

    private <T> T get(String key, T defValue) {
        if (defValue instanceof String)
            return (T) sharedPreferences.getString(key, (String) defValue);
        if (defValue instanceof Boolean)
            return (T) (Boolean) sharedPreferences.getBoolean(key, (Boolean) defValue);
//        if (defValue instanceof Integer)
//            return (T) (Integer) sharedPreferences.getInt(key, (Integer) defValue);
        throw new IllegalArgumentException("defValue " + defValue + " is instance of unhandled class");
    }

    public <T> void set(int keyId, T value) {
        set(app.getString(keyId), value);
    }

    <T> void set(String key, T value) {
        if (value instanceof String)
            sharedPreferences.edit().putString(key, (String) value).apply();
        else if (value instanceof Boolean)
            sharedPreferences.edit().putBoolean(key, (Boolean) value).apply();
//        else if (value instanceof Integer)
//            sharedPreferences.edit().putInt(key, (Integer) value).apply();
        else
            throw new IllegalArgumentException("Value " + value + " is instance of unhandled class");
    }
}
