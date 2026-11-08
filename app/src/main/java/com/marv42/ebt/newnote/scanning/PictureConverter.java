/*
 Copyright (c) 2010 - 2026 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote.scanning;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
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

import androidx.annotation.NonNull;

class PictureConverter {

    static final Bitmap.Config BITMAP_CONFIG = Bitmap.Config.ARGB_8888;
    private static final double UNDERSHOOT_FACTOR = 0.7;

    private final String path;
    private final int orientation;

    PictureConverter(String path, int orientation) {
        this.path = path;
        this.orientation = orientation;
    }

    static PictureConverter of(String path, ContentResolver contentResolver, Uri photoUri) {
        return new PictureConverter(path, ImageOrientation.of(path, contentResolver, photoUri));
    }

    String scaleAndEncodeToBase64(double targetSizeBytes) throws NoPictureException {
        byte[] bytes = convertAndScale(targetSizeBytes);
        return Base64.encodeToString(bytes, NO_WRAP);
    }

    @NonNull
    private byte[] convertAndScale(double targetSizeBytes) throws NoPictureException {
        Bitmap bitmap = getBitmap();
        byte[] bytes = convert(bitmap);
        int allocationByteCount = bitmap.getAllocationByteCount();
        if (allocationByteCount <= targetSizeBytes)
            return bytes;
        return scale(bitmap, targetSizeBytes, allocationByteCount);
    }

    @NonNull
    Bitmap toBitmap(int maxLongSide) throws NoPictureException {
        Bitmap decoded = decodeSampled(maxLongSide);
        Bitmap rotated = getRotatedBitmap(decoded);
        if (rotated != decoded)
            decoded.recycle();
        return rotated;
    }

    @NonNull
    private Bitmap decodeSampled(int maxLongSide) throws NoPictureException {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, bounds);
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0)
            throw new NoPictureException("R.string.error_reading_picture: " + path);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = getInSampleSize(bounds.outWidth, bounds.outHeight, maxLongSide);
        options.inPreferredConfig = BITMAP_CONFIG;
        Bitmap bitmap = BitmapFactory.decodeFile(path, options);
        if (bitmap == null)
            throw new NoPictureException("R.string.error_reading_picture: " + path);
        return bitmap;
    }

    private int getInSampleSize(int width, int height, int maxLongSide) {
        int longestSide = Math.max(width, height);
        int inSampleSize = 1;
        while (isImageTooBig(longestSide, inSampleSize, maxLongSide)) {
            if (isTargetResolutionReached(longestSide, inSampleSize, maxLongSide))
                break;
            inSampleSize *= 2;
        }
        return inSampleSize;
    }

    private static boolean isTargetResolutionReached(int longestSide, int inSampleSize, int maxLongSide) {
        return longestSide / (inSampleSize * 2) < maxLongSide * UNDERSHOOT_FACTOR;
    }

    private static boolean isImageTooBig(int longestSide, int inSampleSize, int maxLongSide) {
        return longestSide / inSampleSize > maxLongSide;
    }

    private Bitmap getBitmap() throws NoPictureException {
        Bitmap decoded = BitmapFactory.decodeFile(path);
        if (decoded == null)
            throw new NoPictureException("R.string.error_reading_picture: " + path);
        Bitmap rotated = getRotatedBitmap(decoded);
        if (decoded != rotated)
            decoded.recycle();
        return rotated;
    }

    @NonNull
    private Bitmap getRotatedBitmap(Bitmap bitmap) {
        final int width = bitmap.getWidth();
        final int height = bitmap.getHeight();
        return createBitmap(bitmap, 0, 0, width, height, getRotationMatrix(), true);
    }

    @NotNull
    private Matrix getRotationMatrix() {
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

    private byte[] convert(Bitmap bitmap) throws NoPictureException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        if (!bitmap.compress(PNG /*JPEG*/, 100 /*ignored for PNG*/, stream))
            throw new NoPictureException("R.string.error_compressing_picture");
        return stream.toByteArray();
    }

    private byte[] scale(Bitmap bitmap, double targetSizeBytes, int allocationByteCount) throws NoPictureException {
        final double scalingFactor = getScalingFactor(allocationByteCount, targetSizeBytes);
        ByteArrayOutputStream streamOfScaled = scaleImage(bitmap, scalingFactor);
        return streamOfScaled.toByteArray();
    }

    private double getScalingFactor(int allocationByteCount, double targetSizeBytes) {
        double scalingFactor = targetSizeBytes / allocationByteCount;
        return Math.sqrt(scalingFactor);
    }

    @NotNull
    private ByteArrayOutputStream scaleImage(Bitmap bitmap, double scalingFactor) throws NoPictureException {
        final int scaledWidth = (int) (scalingFactor * bitmap.getWidth());
        final int scaledHeight = (int) (scalingFactor * bitmap.getHeight());
        Bitmap scaledImage = createScaledBitmap(bitmap, scaledWidth, scaledHeight, false);
        ByteArrayOutputStream streamOfScaled = new ByteArrayOutputStream();
        if (!scaledImage.compress(PNG, 100, streamOfScaled))
            throw new NoPictureException("R.string.error_compressing_picture");
        return streamOfScaled;
    }
}
