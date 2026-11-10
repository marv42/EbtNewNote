/*
 Copyright (c) 2010 - 2026 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote.scanning;

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 * Initializes Tesseract once for all test cases, because loading the trained data is expensive.
 */
abstract class LocalOcrTestCase {

    private static Ocr ocr;

    @BeforeClass
    public static void initializeOcr() {
        ocr = new Ocr(getInstrumentation().getTargetContext());
        assertTrue("local OCR could not be initialized", ocr.isInitialized());
    }

    @AfterClass
    public static void recycleOcr() {
        if (ocr != null)
            ocr.close();
        ocr = null;
    }

    static Ocr ocr() {
        return ocr;
    }
}
