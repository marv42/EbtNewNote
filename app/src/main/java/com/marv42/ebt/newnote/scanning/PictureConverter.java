/*******************************************************************************
 * Copyright (c) 2010 marvin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Contributors:
 *     marvin - initial API and implementation
 ******************************************************************************/

package com.marv42.ebt.newnote.scanning;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.support.media.ExifInterface;
import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static android.graphics.Bitmap.CompressFormat.PNG;
import static android.graphics.Bitmap.createBitmap;
import static android.support.media.ExifInterface.ORIENTATION_NORMAL;
import static android.support.media.ExifInterface.ORIENTATION_ROTATE_180;
import static android.support.media.ExifInterface.ORIENTATION_ROTATE_270;
import static android.support.media.ExifInterface.ORIENTATION_ROTATE_90;
import static android.support.media.ExifInterface.TAG_ORIENTATION;
import static android.util.Base64.NO_WRAP;
import static com.marv42.ebt.newnote.EbtNewNote.LOG_TAG;

class PictureConverter {
    private static final double TARGET_SIZE_BYTE = 1024*1024; // 1024 MB

    static String convert(String path) {
        Bitmap image = BitmapFactory.decodeFile(path);

        try {
            ExifInterface exif = new ExifInterface(path);
            int orientation = exif.getAttributeInt(TAG_ORIENTATION, ORIENTATION_NORMAL);
            int rotation = orientation == ORIENTATION_ROTATE_270 ? 270 :
                    orientation == ORIENTATION_ROTATE_180 ? 180 :
                    orientation == ORIENTATION_ROTATE_90 ? 90 : 0;
            Log.d(LOG_TAG, "rotating image by " + rotation);
            Matrix matrix = new Matrix();
            matrix.postRotate(rotation);
            image = createBitmap(image, 0, 0, image.getWidth(), image.getHeight(), matrix, true);
        } catch (IOException e) {
            Log.w(LOG_TAG, "Error getting exif information, did not rotate the image");
            e.printStackTrace();
        }

        int allocationByteCount = image.getAllocationByteCount();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        if (! image.compress(PNG, 100 /*ignored for PNG*/, stream)) {
            Log.e(LOG_TAG, "Error compressing image");
            return null;
        }
        Log.d(LOG_TAG, "image data: byteCount: " + image.getByteCount());
        Log.d(LOG_TAG, "image data: allocationByteCount: " + allocationByteCount);
        Log.d(LOG_TAG, "image data: width: " + image.getWidth());
        Log.d(LOG_TAG, "image data: height: " + image.getHeight());
        Log.d(LOG_TAG, "image data: density: " + image.getDensity());
        byte[] b = stream.toByteArray();

        if (allocationByteCount > TARGET_SIZE_BYTE) {
            double scalingFactor = TARGET_SIZE_BYTE / allocationByteCount;
            Log.d(LOG_TAG, "scaling image by factor " + scalingFactor);
            scalingFactor = Math.sqrt(scalingFactor);
            Bitmap scaledImage = Bitmap.createScaledBitmap(image, (int) (scalingFactor * image.getWidth()),
                    (int) (scalingFactor * image.getHeight()), false);
            ByteArrayOutputStream streamOfScaled = new ByteArrayOutputStream();
            if (! scaledImage.compress(PNG, 100 /*ignored for PNG*/, streamOfScaled)) {
                Log.e(LOG_TAG, "Error compressing scaled image");
                return null;
            }
            Log.d(LOG_TAG, "scaled image data: byteCount: " + scaledImage.getByteCount());
            Log.d(LOG_TAG, "scaled image data: allocationByteCount: " + scaledImage.getAllocationByteCount());
            Log.d(LOG_TAG, "scaled image data: width: " + scaledImage.getWidth());
            Log.d(LOG_TAG, "scaled image data: height: " + scaledImage.getHeight());
            Log.d(LOG_TAG, "scaled image data: density: " + scaledImage.getDensity());
            b = streamOfScaled.toByteArray();
        }

        return Base64.encodeToString(b, NO_WRAP);
    }
}
