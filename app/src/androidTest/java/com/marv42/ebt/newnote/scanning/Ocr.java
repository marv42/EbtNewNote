/*
 Copyright (c) 2010 - 2026 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote.scanning;

import android.content.Context;
import android.graphics.Bitmap;

import com.marv42.ebt.newnote.exceptions.NoPictureException;

/**
 * Runs a picture through the same chain as {@link OcrHandlerLocal}: decoding and rotating,
 * grayscale with level stretch, Tesseract, {@link TextProcessor}.
 */
final class Ocr implements AutoCloseable {

    private final LocalOcr localOcr;

    Ocr(Context context) {
        final String filesPath = TessData.install(context);
        localOcr = filesPath == null ? null : new LocalOcr(filesPath);
    }

    boolean isInitialized() {
        return localOcr != null && localOcr.isInitialized();
    }

    String recognize(String imagePath, int pageSegMode) throws NoPictureException {
        final Bitmap bitmap = ImagePreprocessor.grayscaleWithLevelStretch(
                PictureConverter.of(imagePath, null, null).toBitmap(OcrHandlerLocal.MAX_LONG_SIDE));
        try {
            final String text = localOcr.recognize(bitmap, pageSegMode);
            return text == null ? "" : text;
        } finally {
            bitmap.recycle();
        }
    }

    static String process(String allResults) {
        return new TextProcessor().getOcrResult(allResults.replace("\r\n", TextProcessor.NEW_LINE));
    }

    static int lineCount(String result) {
        return result.isEmpty() ? 0 : result.split(TextProcessor.NEW_LINE).length;
    }

    @Override
    public void close() {
        if (localOcr != null)
            localOcr.recycle();
    }
}
