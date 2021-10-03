/*
 Copyright (c) 2010 - 2021 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote.preferences;


import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

// https://stackoverflow.com/a/20368622
public abstract class DrawableClickListener implements View.OnTouchListener {

    public static final int DEFAULT_FUZZ = 10;
    public static final int DRAWABLE_INDEX_RIGHT = 2;
    private Drawable drawable = null;

    public DrawableClickListener(final TextView view) {
        super();
        final Drawable[] drawables = view.getCompoundDrawables();
        if (drawables != null)
            drawable = drawables[DRAWABLE_INDEX_RIGHT];
    }

    @Override
    public boolean onTouch(final View v, final MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN && drawable != null) {
            final int x = (int) event.getX();
            final int y = (int) event.getY();
            final Rect bounds = drawable.getBounds();
            if (isClickOnDrawable(x, y, v, bounds))
                return this.onDrawableClick();
        }
        return false;
    }

    public boolean isClickOnDrawable(final int x, final int y, final View view, final Rect drawableBounds) {
        final int fuzz = DEFAULT_FUZZ;
        if (x >= view.getWidth() - view.getPaddingRight() - drawableBounds.width() - fuzz)
            if (x <= view.getWidth() - view.getPaddingRight() + fuzz)
                if (y >= view.getPaddingTop() - fuzz)
                    if (y <= view.getHeight() - view.getPaddingBottom() + fuzz)
                        return true;
        return false;
    };

    public abstract boolean onDrawableClick();
}
