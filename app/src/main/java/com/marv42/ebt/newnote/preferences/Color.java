/*
 Copyright (c) 2010 - 2026 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote.preferences;

import android.app.Activity;

import androidx.annotation.ColorRes;
import androidx.annotation.StyleRes;

import com.marv42.ebt.newnote.R;

public enum Color {

    LIGHT_BLUE("light_blue", R.color.blue_light, R.style.ThemeOverlay_AppTheme_BlueLight),
    DARK_BLUE("dark_blue", R.color.blue_dark, R.style.ThemeOverlay_AppTheme_BlueDark),
    DARK_GREEN("dark_green", R.color.green_dark, R.style.ThemeOverlay_AppTheme_GreenDark),
    LIGHT_GREEN("light_green", R.color.green_light, R.style.ThemeOverlay_AppTheme_GreenLight),
    ORANGE("orange", R.color.orange, R.style.ThemeOverlay_AppTheme_Orange),
    LIGHT_RED("light_red", R.color.red_light, R.style.ThemeOverlay_AppTheme_RedLight),
    DARK_RED("dark_red", R.color.red_dark, R.style.ThemeOverlay_AppTheme_RedDark),
    PURPLE("purple", R.color.purple, R.style.ThemeOverlay_AppTheme_Purple);

    public static final Color DEFAULT = ORANGE;

    private final String key;
    @ColorRes
    private final int colorRes;
    @StyleRes
    private final int overlayRes;

    Color(String key, @ColorRes int colorRes, @StyleRes int overlayRes) {
        this.key = key;
        this.colorRes = colorRes;
        this.overlayRes = overlayRes;
    }

    public String getKey() {
        return key;
    }

    @ColorRes
    public int getColorRes() {
        return colorRes;
    }

    public void applyTo(Activity activity) {
        activity.getTheme().applyStyle(overlayRes, true);
    }

    public static Color fromKey(String key) {
        for (Color color : values())
            if (color.key.equals(key))
                return color;
        return DEFAULT;
    }

    public static int indexOf(String key) {
        Color[] colors = values();
        for (int i = 0; i < colors.length; i++)
            if (colors[i].key.equals(key))
                return i;
        return DEFAULT.ordinal();
    }
}
