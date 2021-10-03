/*
 Copyright (c) 2010 - 2021 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote.preferences;

import static android.text.InputType.TYPE_CLASS_TEXT;
import static android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.text.method.PasswordTransformationMethod;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.preference.EditTextPreference;

import com.marv42.ebt.newnote.R;

public class PasswordToggleEditTextPreference extends EditTextPreference {

    public static final int TRANSITION_DURATION_MS = 500;
    public static final int PASSWORD_INPUT_TYPE = TYPE_CLASS_TEXT | TYPE_TEXT_VARIATION_PASSWORD;
    private static final String TAG = PasswordToggleEditTextPreference.class.getSimpleName();
    private Context context;

    public PasswordToggleEditTextPreference(Context context) {
        super(context);
    }

    public PasswordToggleEditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
    }

    public PasswordToggleEditTextPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.context = context;
    }

    public PasswordToggleEditTextPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.context = context;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void setOnBindEditTextListener(@Nullable OnBindEditTextListener onBindEditTextListener) {
        super.setOnBindEditTextListener(onBindEditTextListener);
        super.setOnBindEditTextListener(editText -> {
            Drawable drawable = getDrawable();
            editText.setCompoundDrawables(null, null, drawable, null);
            editText.setInputType(PASSWORD_INPUT_TYPE);
            editText.setOnTouchListener(new DrawableClickListener(editText)
            {
                @Override
                public boolean onDrawableClick()
                {
                    setInputType();
                    toggleIcon();
                    setTransformationMethod();
                    return true;
                }

                private void setInputType() {
                    if (passwordIsHidden())
                        editText.setInputType(TYPE_CLASS_TEXT);
                    else
                        editText.setInputType(PASSWORD_INPUT_TYPE);
                }

                private void toggleIcon() {
                    if (drawable != null) {
                        TransitionDrawable transitionDrawable = (TransitionDrawable) drawable;
                        transitionDrawable.setCrossFadeEnabled(true);
                        if (passwordIsHidden())
                            transitionDrawable.reverseTransition(TRANSITION_DURATION_MS);
                        else
                            transitionDrawable.startTransition(TRANSITION_DURATION_MS);
                    }
                }

                private void setTransformationMethod() {
                    if (passwordIsHidden())
                        editText.setTransformationMethod(new PasswordTransformationMethod());
                    else
                        editText.setTransformationMethod(null);
                }

                private boolean passwordIsHidden() {
                    return editText.getTransformationMethod() == null;
                }
            } );
        });
    }

    private Drawable getDrawable() {
        Drawable drawable = ContextCompat.getDrawable(context, R.drawable.ic_password_eye);
        if (drawable == null)
            return null;
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        return drawable;
    }
}
