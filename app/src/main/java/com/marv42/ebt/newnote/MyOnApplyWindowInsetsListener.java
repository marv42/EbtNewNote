/*
 Copyright (c) 2010 - 2026 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote;

import static java.lang.Integer.max;

import android.view.ViewGroup;

import androidx.core.graphics.Insets;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.WindowInsetsCompat;

public class MyOnApplyWindowInsetsListener {
    /// Cf. https://developer.android.com/develop/ui/views/layout/edge-to-edge#handle-overlaps
    public static OnApplyWindowInsetsListener getOnApplyWindowInsetsListener() {
        return (v, windowInsets) -> {
            Insets systemBarsInsets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            mlp.bottomMargin = systemBarsInsets.bottom;
            mlp.leftMargin = systemBarsInsets.left;
            mlp.rightMargin = systemBarsInsets.right;
            v.setLayoutParams(mlp);
            Insets cutoutInsets = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.displayCutout());
            final int imeType = WindowInsetsCompat.Type.ime();
            int insetImeBottom = windowInsets.getInsets(imeType).bottom;
            v.setPadding(cutoutInsets.left, cutoutInsets.top, cutoutInsets.right,
                    max(cutoutInsets.bottom, insetImeBottom));
            return WindowInsetsCompat.CONSUMED;
        };
    }
}
