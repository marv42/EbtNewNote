/*
 Copyright (c) 2010 - 2026 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote.scanning;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.marv42.ebt.newnote.exceptions.NoPictureException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.List;

@RunWith(Parameterized.class)
public class LocalOcrTest extends LocalOcrTestCase {

    private final OcrTestCases.Case testCase;

    public LocalOcrTest(OcrTestCases.Case testCase) {
        this.testCase = testCase;
    }

    @Parameters(name = "{0}")
    public static List<Object[]> cases() {
        return OcrTestCases.parameters();
    }

    @Test
    public void findsExpectedSerials() throws NoPictureException {
        final String result = Ocr.process(
                ocr().recognize(testCase.path(), OcrHandlerLocal.PAGE_SEG_MODE));
        if (testCase.serials().isEmpty()) {
            assertEquals("expected no result for " + testCase + " but got: " + result, "", result);
            return;
        }
        for (String serial : testCase.serials())
            assertTrue("missing " + serial + " for " + testCase + ", got: " + result,
                    result.contains(serial));
    }
}
