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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

public class ImageOrientationTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void readsOrientationFromExif() throws IOException {
        assertEquals(ORIENTATION_ROTATE_90,
                ImageOrientation.of(TestPictures.jpegWithOrientation90(folder).getPath(), null, null));
    }

    @Test
    public void fallsBackToUndefinedWithoutExif() throws IOException {
        assertEquals(ORIENTATION_UNDEFINED,
                ImageOrientation.of(TestPictures.jpegWithoutExif(folder).getPath(), null, null));
    }

    @Test
    public void fallsBackToUndefinedForMissingFile() {
        File missing = new File(folder.getRoot(), "missing.jpg");
        assertEquals(ORIENTATION_UNDEFINED, ImageOrientation.of(missing.getPath(), null, null));
    }
}
