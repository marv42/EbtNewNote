/*
 Copyright (c) 2010 - 2026 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote.scanning;

import static org.junit.Assert.assertEquals;

import com.googlecode.tesseract.android.TessBaseAPI;
import com.marv42.ebt.newnote.exceptions.NoPictureException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.ArrayList;
import java.util.List;

@RunWith(Parameterized.class)
public class PsmComparisonTest extends LocalOcrTestCase {

    private final OcrTestCases.Case testCase;

    public PsmComparisonTest(OcrTestCases.Case testCase) {
        this.testCase = testCase;
    }

    @Parameters(name = "{0}")
    public static List<Object[]> cases() {
        return OcrTestCases.parameters();
    }

    @Test
    public void sparseTextIsAtLeastAsGoodAsAuto() throws NoPictureException {
        final String sparse = process(TessBaseAPI.PageSegMode.PSM_SPARSE_TEXT);
        final String auto = process(TessBaseAPI.PageSegMode.PSM_AUTO);
        final List<String> deviations = new ArrayList<>();
        for (String serial : testCase.serials())
            if (auto.contains(serial) && !sparse.contains(serial))
                deviations.add("PSM_SPARSE_TEXT lost " + serial);
        assertEquals(testCase + ": PSM_AUTO=" + auto.replace(TextProcessor.NEW_LINE, " | ")
                        + ", PSM_SPARSE_TEXT=" + sparse.replace(TextProcessor.NEW_LINE, " | "),
                "", String.join("; ", deviations));
    }

    private String process(int pageSegMode) throws NoPictureException {
        return Ocr.process(ocr().recognize(testCase.path(), pageSegMode));
    }
}
