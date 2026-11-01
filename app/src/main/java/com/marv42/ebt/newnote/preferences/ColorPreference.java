/*
 Copyright (c) 2010 - 2026 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote.preferences;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.preference.Preference;

import com.marv42.ebt.newnote.R;

public class ColorPreference extends Preference {

    private static final int SWATCH_SIZE_DP = 24;
    private static final int SWATCH_PADDING_DP = 12;

    public ColorPreference(Context context) {
        super(context);
    }

    public ColorPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ColorPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public ColorPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getString(index);
    }

    @Override
    protected void onSetInitialValue(@Nullable Object defaultValue) {
        String key = defaultValue instanceof String ? (String) defaultValue : Color.DEFAULT.getKey();
        setColor(getPersistedString(key));
    }

    @Override
    protected void onClick() {
        Context context = getContext();
        Color[] colors = Color.values();
        int checked = Color.indexOf(getPersistedString(Color.DEFAULT.getKey()));
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setSingleChoiceItems(new ColorAdapter(context, colors), checked,
                        (dialog, which) -> {
                            Color color = colors[which];
                            persistString(color.getKey());
                            setColor(color.getKey());
                            dialog.dismiss();
                        })
                .setNegativeButton(android.R.string.cancel, null);
        CharSequence title = getTitle();
        if (title != null)
            builder.setTitle(title);
        builder.show();
    }

    private void setColor(String key) {
        Color color = Color.fromKey(key);
        String summary = getContext().getString(R.string.settings_color_summary);
        setSummary(summary);
        setIcon(createSwatch(getContext(), color.getColorRes()));
    }

    private static Drawable createSwatch(Context context, int colorRes) {
        float density = context.getResources().getDisplayMetrics().density;
        int size = Math.round(SWATCH_SIZE_DP * density);
        ShapeDrawable swatch = new ShapeDrawable(new OvalShape());
        swatch.getPaint().setColor(ContextCompat.getColor(context, colorRes));
        swatch.setIntrinsicWidth(size);
        swatch.setIntrinsicHeight(size);
        return swatch;
    }

    private static final class ColorAdapter extends ArrayAdapter<Color> {

        private final Context context;

        ColorAdapter(Context context, Color[] colors) {
            super(context, android.R.layout.simple_list_item_single_choice, colors);
            this.context = context;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            View view = super.getView(position, convertView, parent);
            if (view instanceof CheckedTextView checkedTextView) {
                Color color = getItem(position);
                if (color != null) {
                    checkedTextView.setCompoundDrawablesRelativeWithIntrinsicBounds(
                            createSwatch(context, color.getColorRes()), null, null, null);
                    int padding = Math.round(SWATCH_PADDING_DP
                            * context.getResources().getDisplayMetrics().density);
                    checkedTextView.setCompoundDrawablePadding(padding);
                }
            }
            return view;
        }
    }
}
