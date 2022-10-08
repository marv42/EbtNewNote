/*
 Copyright (c) 2010 - 2022 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote.scanning;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.util.Base64;

import com.marv42.ebt.newnote.exceptions.NoPictureException;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;

import static android.graphics.Bitmap.CompressFormat.PNG;
import static android.graphics.Bitmap.createBitmap;
import static android.graphics.Bitmap.createScaledBitmap;
import static android.util.Base64.NO_WRAP;
import static androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180;
import static androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270;
import static androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90;

class PictureConverter {

    private static final double TARGET_SIZE_BYTE = 1024 * 1024; // 1024 MB
    private final String path;
    private final int orientation;

    public PictureConverter(String path, int orientation) {
        this.path = path;
        this.orientation = orientation;
    }

    String convert() throws NoPictureException {
        Bitmap bitmap = getBitmap();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        if (!bitmap.compress(PNG, 100 /*ignored for PNG*/, stream))
            throw new NoPictureException("R.string.error_compressing_picture");
        byte[] bytes = stream.toByteArray();
        int allocationByteCount = bitmap.getAllocationByteCount();
        if (allocationByteCount > TARGET_SIZE_BYTE) {
            ByteArrayOutputStream streamOfScaled = scaleImage(bitmap, allocationByteCount);
            bytes = streamOfScaled.toByteArray();
        }
        return Base64.encodeToString(bytes, NO_WRAP);
    }

    private Bitmap getBitmap() {
        Bitmap image = BitmapFactory.decodeFile(path);
        final int width = image.getWidth();
        final int height = image.getHeight();
        Matrix matrix = rotateImage();
        return createBitmap(image, 0, 0, width, height, matrix, true);
    }

    @NotNull
    private Matrix rotateImage() {
        int degrees = getDegrees();
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        return matrix;
    }

    private int getDegrees() {
        return orientation == ORIENTATION_ROTATE_270 ? 270
                : orientation == ORIENTATION_ROTATE_180 ? 180
                : orientation == ORIENTATION_ROTATE_90 ? 90 : 0;
    }

    @NotNull
    private ByteArrayOutputStream scaleImage(Bitmap image, int allocationByteCount) throws NoPictureException {
        double scalingFactor = TARGET_SIZE_BYTE / allocationByteCount;
        scalingFactor = Math.sqrt(scalingFactor);
        final int scaledWidth = (int) (scalingFactor * image.getWidth());
        final int scaledHeight = (int) (scalingFactor * image.getHeight());
        Bitmap scaledImage = createScaledBitmap(image, scaledWidth, scaledHeight, false);
        ByteArrayOutputStream streamOfScaled = new ByteArrayOutputStream();
        if (!scaledImage.compress(PNG, 100, streamOfScaled))
            throw new NoPictureException("R.string.error_compressing_picture");
        return streamOfScaled;
    }
}
