/*
 Copyright (c) 2010 - 2021 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;

import androidx.preference.EditTextPreference;

import static com.marv42.ebt.newnote.AllResults.MAX_LOAD_NUM;

public class IntEditTextPreference extends EditTextPreference {

    private static final String TAG = IntEditTextPreference.class.getSimpleName();

    public IntEditTextPreference(Context context) {
        super(context);
    }

    public IntEditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public IntEditTextPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public IntEditTextPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected boolean persistString(String value) {
        try {
            Integer.parseInt(value);
            return super.persistString(value);
        } catch (NumberFormatException e) {
            Log.w(TAG, e.getMessage());
            e.printStackTrace();
            super.setText(String.valueOf(MAX_LOAD_NUM));
            return false;
        }
    }
}
