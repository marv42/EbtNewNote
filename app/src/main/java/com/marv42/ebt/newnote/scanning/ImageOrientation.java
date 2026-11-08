/*
 Copyright (c) 2010 - 2026 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote.scanning;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import androidx.exifinterface.media.ExifInterface;

import java.io.IOException;

import static android.provider.MediaStore.MediaColumns.ORIENTATION;
import static androidx.exifinterface.media.ExifInterface.ORIENTATION_UNDEFINED;
import static androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION;

final class ImageOrientation {

    private static final String TAG = ImageOrientation.class.getSimpleName();

    private ImageOrientation() {
    }

    static int of(String photoPath, ContentResolver contentResolver, Uri photoUri) {
        final int fromExif = fromExif(photoPath);
        return fromExif != ORIENTATION_UNDEFINED ? fromExif :
                fromMediaStore(contentResolver, photoUri);
    }

    static int fromExif(String photoPath) {
        try {
            return new ExifInterface(photoPath).getAttributeInt(TAG_ORIENTATION, ORIENTATION_UNDEFINED);
        } catch (IOException e) {
            Log.w(TAG, e.getMessage());
            return ORIENTATION_UNDEFINED;
        }
    }

    private static int fromMediaStore(ContentResolver contentResolver, Uri photoUri) {
        if (photoUri == null)
            return ORIENTATION_UNDEFINED;
        return fromCursor(contentResolver, photoUri);
    }

    private static int fromCursor(ContentResolver contentResolver, Uri photoUri) {
        Cursor cursor = contentResolver.query(photoUri, new String[]{ORIENTATION}, null, null, null);
        try (cursor) {
            if (cursor == null) {
                Log.w(TAG, "No MediaStore entry for " + photoUri);
                return ORIENTATION_UNDEFINED;
            }
            if (!cursor.moveToFirst())
                return ORIENTATION_UNDEFINED;
            int columnIndex = cursor.getColumnIndex(ORIENTATION);
            return columnIndex >= 0 ? cursor.getInt(columnIndex) : ORIENTATION_UNDEFINED;
        }
    }
}
