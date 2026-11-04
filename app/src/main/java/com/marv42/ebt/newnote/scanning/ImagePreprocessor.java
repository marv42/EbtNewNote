/*
 Copyright (c) 2010 - 2026 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote.scanning;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;

class ImagePreprocessor {

    private static final int LOW_PERCENTILE = 2;
    private static final int HIGH_PERCENTILE = 98;
    private static final int MIN_SPAN = 32;

    private ImagePreprocessor() {
    }

    @NonNull
    static Bitmap grayscaleWithLevelStretch(@NonNull Bitmap source) {
        final int width = source.getWidth();
        final int height = source.getHeight();
        final int[] pixels = new int[width * height];
        source.getPixels(pixels, 0, width, 0, 0, width, height);

        final int[] histogram = new int[256];
        for (int i = 0; i < pixels.length; i++) {
            final int color = pixels[i];
            final int luma = (77 * ((color >> 16) & 0xff)
                    + 150 * ((color >> 8) & 0xff)
                    + 29 * (color & 0xff)) >> 8;
            pixels[i] = luma;
            histogram[luma]++;
        }

        final long total = (long) width * height;
        int low = percentile(histogram, total, LOW_PERCENTILE);
        int high = percentile(histogram, total, HIGH_PERCENTILE);
        if (high - low < MIN_SPAN) {
            low = 0;
            high = 255;
        }
        final int[] lookUpTable = createLookUpTable(low, high);
        for (int i = 0; i < pixels.length; i++) {
            final int value = lookUpTable[pixels[i]];
            pixels[i] = 0xff000000 | (value << 16) | (value << 8) | value;
        }

        final Bitmap target = Bitmap.createBitmap(width, height, PictureConverter.BITMAP_CONFIG);
        target.setPixels(pixels, 0, width, 0, 0, width, height);
        source.recycle();
        return target;
    }

    private static int percentile(int[] histogram, long total, int percent) {
        final long threshold = total * percent / 100;
        long sum = 0;
        for (int i = 0; i < histogram.length; i++) {
            sum += histogram[i];
            if (sum >= threshold)
                return i;
        }
        return histogram.length - 1;
    }

    private static int[] createLookUpTable(int low, int high) {
        final int span = high - low;
        final int[] lut = new int[256];
        for (int i = 0; i < lut.length; i++) {
            final int stretched = span <= 0 ? i : (i - low) * 255 / span;
            lut[i] = Math.max(0, Math.min(255, stretched));
        }
        return lut;
    }
}
