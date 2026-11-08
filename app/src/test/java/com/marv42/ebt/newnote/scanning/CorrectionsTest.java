/*
 Copyright (c) 2010 - 2026 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote.scanning;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class CorrectionsTest {

    @Test
    public void removesWhitespacesAndUppercases() {
        assertEquals("XB1234567890", Corrections.correct(" xb 1234 5678 90 "));
    }

    @Test
    public void correctsLetterInDigitPosition() {
        assertEquals("XA0123456789", Corrections.correct("XAO123456789"));
    }

    @Test
    public void correctsSymbolInLetterPosition() {
        assertEquals("XS1234567890", Corrections.correct("X$1234567890"));
    }

    @Test
    public void correctsAmbiguousCharacter() {
        assertEquals("XX1234567890", Corrections.correct("X%1234567890"));
    }

    @Test
    public void prefixesYaForLeadingQuote() {
        assertEquals("YA1234567890", Corrections.correct("\"1234567890"));
    }

    @Test
    public void cutsOutSerialNumberPattern() {
        assertEquals("XA1234567890", Corrections.correct("XA1234567890 junk"));
    }
}
