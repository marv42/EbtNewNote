/*
 Copyright (c) 2010 - 2020 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;

class SavePreferencesFocusListener implements View.OnFocusChangeListener {

    private final SharedPreferencesHandler sharedPreferencesHandler;
    private final String preferenceKey;

    SavePreferencesFocusListener(SharedPreferencesHandler sharedPreferencesHandler, String preferenceKey) {
        this.sharedPreferencesHandler = sharedPreferencesHandler;
        this.preferenceKey = preferenceKey;
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (!hasFocus)
            sharedPreferencesHandler.set(preferenceKey, ((EditText) v).getText().toString());
    }
}
