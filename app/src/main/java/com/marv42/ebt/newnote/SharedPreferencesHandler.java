/*
 Copyright (c) 2010 - 2020 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote;

import android.content.SharedPreferences;

import javax.inject.Inject;

public class SharedPreferencesHandler {

    private final ThisApp app;
    private final SharedPreferences sharedPreferences;

    @Inject
    public SharedPreferencesHandler(ThisApp app, SharedPreferences sharedPreferences) {
        this.app = app;
        this.sharedPreferences = sharedPreferences;
    }

    <T> T get(int keyId, T defValue) {
        return get(app.getString(keyId), defValue);
    }

    private <T> T get(String key, T defValue) {
        if (defValue instanceof String)
            return (T) sharedPreferences.getString(key, (String) defValue);
        if (defValue instanceof Boolean)
            return (T) (Boolean) sharedPreferences.getBoolean(key, (Boolean) defValue);
        throw new IllegalArgumentException("Defvalue " + defValue + " is instance of unhandled class");
    }

    public <T> void set(int keyId, T value) {
        set(app.getString(keyId), value);
    }

    <T> void set(String key, T value) {
        if (value instanceof String)
            sharedPreferences.edit().putString(key, (String) value).apply();
        else if (value instanceof Boolean)
            sharedPreferences.edit().putBoolean(key, (Boolean) value).apply();
        else
            throw new IllegalArgumentException("Value " + value + " is instance of unhandled class");
    }
}
