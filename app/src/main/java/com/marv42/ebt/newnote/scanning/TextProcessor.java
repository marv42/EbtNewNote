/*
 Copyright (c) 2010 - 2020 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote.scanning;

import android.text.TextUtils;

import com.marv42.ebt.newnote.exceptions.CallResponseException;
import com.marv42.ebt.newnote.exceptions.OcrException;

public class TextProcessor {

    public static final String NEW_LINE = "\r\n";
    private static final String EURO = "EU20";
    private static final int MIN_RESULT_LENGTH = 4;
    private final StringBuilder result = new StringBuilder();

    String getOcrResult(String body) throws OcrException, CallResponseException {
        String allResults = JsonAnalyzer.analyzeBody(body);
        CorrectAllResults(allResults);
        return result.toString();
    }

    private void CorrectAllResults(String allResultsInBody) {
        String[] allResults = allResultsInBody.split(NEW_LINE);
        for (String aResult : allResults) {
            String correctedResult = Corrections.correct(aResult);
            addResult(correctedResult);
        }
    }

    private void addResult(String correctedResult) {
        if (! resultIsEuro(correctedResult) && ! resultIsTooShort(correctedResult)) {
            checkPrefixNewLine();
            result.append(correctedResult);
        }
    }

    private static boolean resultIsEuro(String result) {
        return result.equals(EURO);
    }

    private boolean resultIsTooShort(String result) {
        return result.length() < MIN_RESULT_LENGTH;
    }

    private void checkPrefixNewLine() {
        if (! TextUtils.isEmpty(result))
            result.append(NEW_LINE);
    }
}
