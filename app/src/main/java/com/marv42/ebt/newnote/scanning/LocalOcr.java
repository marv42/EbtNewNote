/*
 Copyright (c) 2010 - 2026 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote.scanning;

import android.graphics.Bitmap;
import android.util.Log;

import com.googlecode.tesseract.android.TessBaseAPI;

final class LocalOcr {

    private static final String TAG = LocalOcr.class.getSimpleName();
    private static final String CHAR_WHITELIST = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private final TessBaseAPI tess = new TessBaseAPI();
    private final boolean initialized;

    LocalOcr(String filesPath) {
        initialized = tess.init(filesPath, TessData.LANGUAGE, TessBaseAPI.OEM_LSTM_ONLY);
        if (initialized)
            setVariables();
    }

    boolean isInitialized() {
        return initialized;
    }

    String recognize(Bitmap bitmap, int pageSegMode) {
        tess.setPageSegMode(pageSegMode);
        tess.setImage(bitmap);
        return tess.getUTF8Text();
    }

    void recycle() {
        tess.recycle();
    }

    private void setVariables() {
        if (!tess.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, CHAR_WHITELIST))
            Log.w(TAG, "Character whitelist rejected");
        tess.setVariable("load_system_dawg", "0");
        tess.setVariable("load_freq_dawg", "0");
    }
}
