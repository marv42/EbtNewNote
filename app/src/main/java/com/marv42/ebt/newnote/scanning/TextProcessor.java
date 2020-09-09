/*******************************************************************************
 * Copyright (c) 2010 marvin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Contributors:
 *     marvin - initial API and implementation
 ******************************************************************************/

package com.marv42.ebt.newnote.scanning;

import android.app.Application;
import android.text.TextUtils;

import com.marv42.ebt.newnote.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.marv42.ebt.newnote.ErrorMessage.ERROR;
import static com.marv42.ebt.newnote.JsonHelper.getJsonObject;
import static java.util.regex.Pattern.CASE_INSENSITIVE;

public class TextProcessor {
    public static final String EMPTY = "<empty>";

    static String getOcrResult(String jsonResult, Application app) {
        try {
            JSONObject json = new JSONObject(jsonResult);
            String error = json.optString(ERROR);
            if (!TextUtils.isEmpty(error))
                return error;
            JSONObject resultJson = getResult(json, app);
            error = resultJson.optString(ERROR);
            if (!TextUtils.isEmpty(error))
                return ERROR + error;
            String result = resultJson.getString("ParsedText").replaceAll("[ \\t\\x0B\\f]+", "").trim();
            if (TextUtils.isEmpty(result))
                return EMPTY;
            // TODO split result at line breaks and treat them separately
            return correct(result);
        } catch (JSONException e) {
            return ERROR + app.getString(R.string.internal_error) + ": " + e.getMessage();
        }
    }

    // cf. https://ocr.space/ocrapi#Response
    private static JSONObject getResult(JSONObject json, Application app) throws JSONException {
        int exitCode = json.getInt("OCRExitCode");
        if (exitCode == 3 || exitCode == 4) {
            String errorMessage = json.getString("ErrorMessage");
            String errorDetails = json.getString("ErrorDetails");
            return getJsonObject(ERROR, errorMessage + " " + errorDetails);
        } else if (exitCode == 1 || exitCode == 2) {
            JSONArray parsedResults = json.getJSONArray("ParsedResults");
            for (int i = 0; i < parsedResults.length(); i++) {
                JSONObject aResult = parsedResults.getJSONObject(i);
                int fileParseExitCode = aResult.getInt("FileParseExitCode");
                if (fileParseExitCode == 1)
                    return aResult;
            }
        }
        return getJsonObject(ERROR, app.getString(R.string.ocr_failed_internal) + ": "
                + app.getString(R.string.ocr_failed_undefined_exit_code));
    }

    private static String correct(String s) {
        Map<String, String> char2ambiguous = new HashMap<>();
//        char2ambiguous.put("$", "S");
//        char2ambiguous.put("$", "5");
//        char2ambiguous.put("W", "U");
        char2ambiguous.put("K", "X");
        char2ambiguous.put("%", "X");
        char2ambiguous.put("@", "0");
        char2ambiguous.put("i", "1");
        char2ambiguous.put("I", "1");
        char2ambiguous.put("t", "1");
        char2ambiguous.put("#", "4");
//        char2ambiguous.put("s", "5");
        char2ambiguous.put("*", "5");
        char2ambiguous.put(">", "5");
        char2ambiguous.put("?", "7");
        char2ambiguous.put("f", "7");
        char2ambiguous.put("a", "8");
        char2ambiguous.put("&", "8");

        Map<String, String> char2letter = new HashMap<>();
        char2letter.put("8", "A");
        char2letter.put("3", "B");
        char2letter.put("4", "N");
        char2letter.put("0", "O");
        char2letter.put("1", "U");
        char2letter.put("W", "U");
        char2letter.put("K", "X");
        char2letter.put("%", "X");
        char2letter.put("2", "Z");
//        char2letter.put("1", "Z");

        Map<String, String> char2digit = new HashMap<>();
        char2digit.put("D", "0");
        char2digit.put("O", "0");
        char2digit.put("o", "0");
        char2digit.put("@", "0");
        char2digit.put("i", "1");
        char2digit.put("\"", "11");
        char2digit.put("I", "1");
        char2digit.put("t", "1");
        char2digit.put("Z", "2");
//        char2digit.put("s", "3");
//        char2digit.put("s", "5");
        char2digit.put("S", "5");
        char2digit.put("$", "5");
        char2digit.put("*", "5");
        char2digit.put(">", "5");
        char2digit.put("?", "7");
        char2digit.put("f", "7");
        char2digit.put("T", "7");
        char2digit.put("Y", "7");
        char2digit.put("a", "8");
        char2digit.put("A", "8");
        char2digit.put("B", "8");

        List<Integer> letterIndices = new ArrayList<>();
        letterIndices.add(0);
        Pattern pattern = Pattern.compile("\\w\\d{3}\\w\\d", CASE_INSENSITIVE);
        if (s.length() > 9) {
            letterIndices.add(1); // probably a serial number
            // we don't support the old number format \\w{1}\\d{11} any more,
            // because if we would, we couldn't fix the 2nd letter of the new format
            pattern = Pattern.compile("\\w{2}\\d{10}", CASE_INSENSITIVE);
        } else
            letterIndices.add(4); // probably a short code
        for (int i = 0; i < s.length(); ++i) {
            s = s.substring(0, i) + correctCharacter(s.charAt(i), char2ambiguous) + s.substring(i+1);
            if (letterIndices.contains(i)) {
                if (! s.substring(i, i+1).matches("\\w"))
                    s = s.substring(0, i) + correctCharacter(s.charAt(i), char2letter) + s.substring(i+1);
            } else
                if (! s.substring(i, i+1).matches("\\d"))
                    s = s.substring(0, i) + correctCharacter(s.charAt(i), char2digit) + s.substring(i+1);
        }
        Matcher matcher = pattern.matcher(s);
        if (matcher.find())
            s = s.substring(matcher.start(), matcher.end());
        return s;
    }

    private static String correctCharacter(char c, Map<String, String> char2char) {
        String sC = Character.toString(c);
        if (char2char.containsKey(sC))
            return char2char.get(sC);
        return sC;
    }
}
