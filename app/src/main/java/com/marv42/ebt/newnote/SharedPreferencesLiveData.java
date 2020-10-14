/*
 Copyright (c) 2010 - 2020 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote;

import android.content.SharedPreferences;

import androidx.lifecycle.LiveData;

// cf. https://gist.github.com/idish/f46a8327da7f293f943a5bda31078c95
abstract class SharedPreferencesLiveData<T> extends LiveData<T> {

    SharedPreferences sharedPrefs;
    String key;
    T defValue;

    private final SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener
            = (sharedPreferences, key) -> {
                if (SharedPreferencesLiveData.this.key.equals(key))
                    setToValueFromPreferences(key);
            };

    private void setToValueFromPreferences(String key) {
        setValue(getValueFromPreferences(key, defValue));
    }

    public SharedPreferencesLiveData(SharedPreferences prefs, String key, T defValue) {
        this.sharedPrefs = prefs;
        this.key = key;
        this.defValue = defValue;
    }

    abstract T getValueFromPreferences(String key, T defValue);

    @Override
    protected void onActive() {
        super.onActive();
        setToValueFromPreferences(key);
        sharedPrefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener);
    }

    @Override
    protected void onInactive() {
        sharedPrefs.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener);
        super.onInactive();
    }
}
