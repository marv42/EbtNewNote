/*
 Copyright (c) 2010 - 2026 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote.scanning;

import static android.graphics.Bitmap.CompressFormat.PNG;

import android.graphics.Bitmap;

import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Base64;

final class TestPictures {

    static final int JPEG_WIDTH = 16;
    static final int JPEG_HEIGHT = 8;

    // 16x8 gray JPEG without EXIF.
    private static final String JPEG_WITHOUT_EXIF =
            "/9j/4AAQSkZJRgABAQAAAQABAAD/2wBDAFA3PEY8MlBGQUZaVVBfeMiCeG5uePWvuZHI////////////"
                    + "////////////////////////////////////////wAALCAAIABABAREA/8QAHwAAAQUBAQEBAQEA"
                    + "AAAAAAAAAAECAwQFBgcICQoL/8QAtRAAAgEDAwIEAwUFBAQAAAF9AQIDAAQRBRIhMUEGE1Fh"
                    + "ByJxFDKBkaEII0KxwRVS0fAkM2JyggkKFhcYGRolJicoKSo0NTY3ODk6Q0RFRkdISUpTVFVWV1hZ"
                    + "WmNkZWZnaGlqc3R1dnd4eXqDhIWGh4iJipKTlJWWl5iZmqKjpKWmp6ipqrKztLW2t7i5usLDxMXG"
                    + "x8jJytLT1NXW19jZ2uHi4+Tl5ufo6erx8vP09fb3+Pn6/9oACAEBAAA/AJ6K/9k=";

    // Same JPEG with EXIF orientation 6 (rotate 90 clockwise).
    private static final String JPEG_WITH_ORIENTATION_90 =
            "/9j/4AAQSkZJRgABAQAAAQABAAD/4QAiRXhpZgAATU0AKgAAAAgAAQESAAMAAAABAAYAAAAAAAD/2wBD"
                    + "AFA3PEY8MlBGQUZaVVBfeMiCeG5uePWvuZHI////////////////////////////////////////////"
                    + "////////////////////wAALCAAIABABAREA/8QAHwAAAQUBAQEBAQEAAAAAAAAAAAECAwQFBgcI"
                    + "CQoL/8QAtRAAAgEDAwIEAwUFBAQAAAF9AQIDAAQRBRIhMUEGE1FhByJxFDKBkaEII0KxwRVS0fAk"
                    + "M2JyggkKFhcYGRolJicoKSo0NTY3ODk6Q0RFRkdISUpTVFVWV1hZWmNkZWZnaGlqc3R1dnd4eXqD"
                    + "hIWGh4iJipKTlJWWl5iZmqKjpKWmp6ipqrKztLW2t7i5usLDxMXGx8jJytLT1NXW19jZ2uHi4+Tl"
                    + "5ufo6erx8vP09fb3+Pn6/9oACAEBAAA/AJ6K/9k=";

    private TestPictures() {
    }

    static File jpegWithoutExif(TemporaryFolder folder) throws IOException {
        return write(folder, "photo.jpg", JPEG_WITHOUT_EXIF);
    }

    static File jpegWithOrientation90(TemporaryFolder folder) throws IOException {
        return write(folder, "photo-rotated.jpg", JPEG_WITH_ORIENTATION_90);
    }

    static File writePng(TemporaryFolder folder, int width, int height) throws IOException {
        File file = folder.newFile("picture-" + width + "x" + height + ".png");
        Bitmap bitmap = Bitmap.createBitmap(width, height, PictureConverter.BITMAP_CONFIG);
        try (FileOutputStream output = new FileOutputStream(file)) {
            if (!bitmap.compress(PNG, 100, output))
                throw new IOException("Could not write " + file);
        } finally {
            bitmap.recycle();
        }
        return file;
    }

    private static File write(TemporaryFolder folder, String name, String base64) throws IOException {
        File file = folder.newFile(name);
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(Base64.getDecoder().decode(base64));
        }
        return file;
    }
}
