/*
 Copyright (c) 2010 - 2026 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote.scanning;

import static androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90;
import static androidx.exifinterface.media.ExifInterface.ORIENTATION_UNDEFINED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.marv42.ebt.newnote.exceptions.NoPictureException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

import java.io.File;
import java.util.Base64;

@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application.class)
public class PictureConverterTest {

    private static final int MAX_LONG_SIDE = 2048;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void toBitmapKeepsSmallPictureSize() throws Throwable {
        File photo = TestPictures.writePng(folder, 100, 50);

        Bitmap bitmap = new PictureConverter(photo.getPath(), ORIENTATION_UNDEFINED).toBitmap(MAX_LONG_SIDE);

        assertEquals(100, bitmap.getWidth());
        assertEquals(50, bitmap.getHeight());
    }

    @Test
    public void toBitmapDownsamplesLargePicture() throws Throwable {
        File photo = TestPictures.writePng(folder, 1000, 500);

        Bitmap bitmap = new PictureConverter(photo.getPath(), ORIENTATION_UNDEFINED).toBitmap(512);

        assertEquals(500, bitmap.getWidth());
        assertEquals(250, bitmap.getHeight());
    }

    @Test
    public void toBitmapRotatesByOrientation() throws Throwable {
        File photo = TestPictures.jpegWithOrientation90(folder);

        Bitmap bitmap = new PictureConverter(photo.getPath(), ORIENTATION_ROTATE_90).toBitmap(MAX_LONG_SIDE);

        assertEquals(TestPictures.JPEG_HEIGHT, bitmap.getWidth());
        assertEquals(TestPictures.JPEG_WIDTH, bitmap.getHeight());
    }

    @Test
    public void ofReadsOrientationFromExif() throws Throwable {
        File photo = TestPictures.jpegWithOrientation90(folder);

        Bitmap bitmap = PictureConverter.of(photo.getPath(), null, null).toBitmap(MAX_LONG_SIDE);

        assertEquals(TestPictures.JPEG_HEIGHT, bitmap.getWidth());
        assertEquals(TestPictures.JPEG_WIDTH, bitmap.getHeight());
    }

    @Test
    public void base64PictureIsRotatedByOrientation() throws Throwable {
        File photo = TestPictures.jpegWithOrientation90(folder);

        Bitmap decoded = decodeBase64(new PictureConverter(photo.getPath(), ORIENTATION_ROTATE_90)
                .scaleAndEncodeToBase64(1024));

        assertEquals(TestPictures.JPEG_HEIGHT, decoded.getWidth());
        assertEquals(TestPictures.JPEG_WIDTH, decoded.getHeight());
    }

    @Test
    public void base64PictureIsKeptWhenWithinBudget() throws Throwable {
        File photo = TestPictures.writePng(folder, 100, 50);

        Bitmap decoded = decodeBase64(new PictureConverter(photo.getPath(), ORIENTATION_UNDEFINED)
                .scaleAndEncodeToBase64(200_000));

        assertEquals(100, decoded.getWidth());
        assertEquals(50, decoded.getHeight());
    }

    @Test
    public void base64PictureIsScaledToBudget() throws Throwable {
        File photo = TestPictures.writePng(folder, 1000, 500);
        final double budget = 200_000;

        Bitmap decoded = decodeBase64(new PictureConverter(photo.getPath(), ORIENTATION_UNDEFINED)
                .scaleAndEncodeToBase64(budget));

        assertTrue(decoded.getAllocationByteCount() <= budget);
        assertEquals(316, decoded.getWidth());
        assertEquals(158, decoded.getHeight());
    }

    @Test
    public void toBitmapFailsForMissingPicture() {
        File missing = new File(folder.getRoot(), "missing.jpg");

        assertThrows(NoPictureException.class,
                () -> new PictureConverter(missing.getPath(), ORIENTATION_UNDEFINED).toBitmap(MAX_LONG_SIDE));
    }

    @Test
    public void base64FailsForMissingPicture() {
        File missing = new File(folder.getRoot(), "missing.jpg");

        assertThrows(NoPictureException.class,
                () -> new PictureConverter(missing.getPath(), ORIENTATION_UNDEFINED)
                        .scaleAndEncodeToBase64(1024));
    }

    private static Bitmap decodeBase64(String encoded) {
        byte[] png = Base64.getDecoder().decode(encoded);
        Bitmap bitmap = BitmapFactory.decodeByteArray(png, 0, png.length);
        if (bitmap == null)
            throw new AssertionError("Could not decode picture");
        return bitmap;
    }
}
