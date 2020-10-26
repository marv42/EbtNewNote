/*
 Copyright (c) 2010 - 2020 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote.scanning;

import android.text.TextUtils;

import com.marv42.ebt.newnote.JsonHelper;
import com.marv42.ebt.newnote.exceptions.CallResponseException;
import com.marv42.ebt.newnote.exceptions.NoJsonElementException;
import com.marv42.ebt.newnote.exceptions.OcrException;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static com.marv42.ebt.newnote.scanning.Corrections.correct;

public class TextProcessor {

    private static final String PARSED_TEXT_ELEMENT = "ParsedText";
    private static final String FILE_PARSE_EXIT_CODE_ELEMENT = "FileParseExitCode";
    private static final String OCR_EXIT_CODE_ELEMENT = "OCRExitCode";
    private static final String PARSED_RESULTS_ELEMENT = "ParsedResults";
    private static final String ERROR_MESSAGE_ELEMENT = "ErrorMessage";
    private static final String ERROR_DETAILS_ELEMENT = "ErrorDetails";

    static String getOcrResult(String body) throws OcrException, CallResponseException {
        String result = analyzeBody(body);
        if (TextUtils.isEmpty(result))
            return "";
        // TODO split result at line breaks and treat them separately
        return correct(result);
    }

    // cf. https://ocr.space/ocrapi#Response
    private static String analyzeBody(String body) throws OcrException, CallResponseException {
        try {
            JSONObject json = new JSONObject(body);
            return extractResult(json);
        } catch (JSONException e) {
            throw new CallResponseException("R.string.no_json: " + e.getMessage());
        } catch (NoJsonElementException e) {
            throw new CallResponseException("R.string.server_error: " + e.getMessage());
        }
    }

    @NotNull
    private static String extractResult(JSONObject json) throws OcrException, NoJsonElementException, CallResponseException {
        int exitCode = new JsonHelper(json).getElement(int.class, OCR_EXIT_CODE_ELEMENT);
        if (exitCode == 1 || exitCode == 2) {
            JSONArray parsedResults = new JsonHelper(json).getElement(JSONArray.class, PARSED_RESULTS_ELEMENT);
            return getResult(parsedResults);
        }
        if (exitCode == 3 || exitCode == 4) {
            String errorMessage = new JsonHelper(json).getElement(String.class, ERROR_MESSAGE_ELEMENT);
            String errorDetails = new JsonHelper(json).getElement(String.class, ERROR_DETAILS_ELEMENT);
            throw new OcrException(errorMessage + " " + errorDetails);
        }
        throw new CallResponseException("Undefined " + OCR_EXIT_CODE_ELEMENT);
    }

    private static String getResult(JSONArray parsedResults) throws CallResponseException, NoJsonElementException {
        for (int i = 0; i < parsedResults.length(); i++) {
            if (parsedResults.isNull(i))
                continue;
            JSONObject aResult = parsedResults.optJSONObject(i);
            int fileParseExitCode = new JsonHelper(aResult).getElement(int.class, FILE_PARSE_EXIT_CODE_ELEMENT);
            if (fileParseExitCode == 1)
                return getParsedText(aResult);
        }
        throw new CallResponseException("No result with '" + FILE_PARSE_EXIT_CODE_ELEMENT + "' 1");
    }

    private static String getParsedText(JSONObject aResult) throws NoJsonElementException {
        String text = new JsonHelper(aResult).getElement(String.class, PARSED_TEXT_ELEMENT);
        text = removeSpaceTabsFormFeed(text);
        return text;
    }

    @NotNull
    private static String removeSpaceTabsFormFeed(String s) {
        s = s.replaceAll("[ \\t\\x0B\\f]+", "");
        return s.trim();
    }
}
