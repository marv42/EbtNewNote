/*
 Copyright (c) 2010 - 2026 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote.scanning;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

final class TessData {

    static final String LANGUAGE = "eng";

    private static final String TAG = TessData.class.getSimpleName();
    private static final String TRAINED_DATA_FILE = LANGUAGE + ".traineddata";
    private static final String TESSDATA_DIR = "tessdata";
    private static final String TMP_SUFFIX = ".tmp";

    private TessData() {
    }

    static String install(Context context) {
        final File externalDir = context.getExternalFilesDir(null);
        if (externalDir == null)
            return null;
        final String filesPath = externalDir.getAbsolutePath();
        final File tessDataDir = new File(filesPath, TESSDATA_DIR);
        if (!tessDataDir.isDirectory() && !tessDataDir.mkdirs())
            return null;
        final File outputFile = new File(tessDataDir, TRAINED_DATA_FILE);
        if (outputFile.length() == assetTrainedDataSize(context))
            return filesPath;
        return copyTessData(context, tessDataDir, outputFile) ? filesPath : null;
    }

    private static long assetTrainedDataSize(Context context) {
        try (ZipFile zip = new ZipFile(context.getApplicationInfo().sourceDir)) {
            final ZipEntry entry = zip.getEntry("assets/" + TESSDATA_DIR + "/" + TRAINED_DATA_FILE);
            return entry == null ? -1 : entry.getSize();
        } catch (IOException e) {
            Log.w(TAG, e.getMessage() == null ? "" : e.getMessage());
            return -1;
        }
    }

    private static boolean copyTessData(Context context, File dir, File outputFile) {
        final File tmpFile = new File(dir, TRAINED_DATA_FILE + TMP_SUFFIX);
        try {
            copyAsset(context, tmpFile);
            if (!tmpFile.renameTo(outputFile)) {
                outputFile.delete();
                if (!tmpFile.renameTo(outputFile))
                    throw new IOException("Could not rename " + tmpFile + " to " + outputFile);
            }
            return true;
        } catch (IOException e) {
            tmpFile.delete();
            Log.w(TAG, e.getMessage() == null ? "" : e.getMessage());
            return false;
        }
    }

    private static void copyAsset(Context context, File target) throws IOException {
        // https://github.com/adaptech-cz/Tesseract4Android/blob/master/sample/src/main/java/cz/adaptech/tesseract4android/sample/Assets.java
        try (InputStream input = context.getAssets().open(TESSDATA_DIR + File.separator + TRAINED_DATA_FILE);
             FileOutputStream output = new FileOutputStream(target)) {
            final byte[] buffer = new byte[1024];
            int read;
            while ((read = input.read(buffer)) != -1)
                output.write(buffer, 0, read);
        }
    }
}
