/*
 Copyright (c) 2010 - 2021 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote.preferences;

import android.text.Editable;
import android.text.TextWatcher;

import com.marv42.ebt.newnote.preferences.SharedPreferencesHandler;

public class SavePreferencesTextWatcher implements TextWatcher {

    private final SharedPreferencesHandler sharedPreferencesHandler;
    private final String preferenceKey;

    public SavePreferencesTextWatcher(SharedPreferencesHandler sharedPreferencesHandler, String preferenceKey) {
        this.sharedPreferencesHandler = sharedPreferencesHandler;
        this.preferenceKey = preferenceKey;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) { }

    @Override
    public void afterTextChanged(Editable s) {
        sharedPreferencesHandler.set(preferenceKey, s.toString());
    }
}
