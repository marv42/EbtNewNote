/*
 Copyright (c) 2010 - 2026 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote.scanning;

import static org.junit.Assert.assertEquals;

import android.app.Application;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class)
public class TextProcessorTest {

    @Test
    public void dropsEmptyShortAndEuroLines() {
        assertEquals("XA1234567890", new TextProcessor().getOcrResult("XA1234567890\n\nZZ\nEU20"));
    }

    @Test
    public void keepsMultipleSerialNumbersInOrder() {
        assertEquals("XA1234567890\nYB9876543210",
                new TextProcessor().getOcrResult("XA1234567890\nYB9876543210"));
    }

    @Test
    public void correctsEachLine() {
        assertEquals("XA0123456789", new TextProcessor().getOcrResult("XAO 123456789"));
    }

    @Test
    public void returnsEmptyResultForNonSerialNumbers() {
        assertEquals("", new TextProcessor().getOcrResult(""));
    }
}
