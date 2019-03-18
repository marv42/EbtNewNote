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

import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.CASE_INSENSITIVE;

public class TextProcessor {
    public static final String EMPTY = "<empty>";

    static String getOcrResult(String s) {
        if (s.startsWith("Error"))
            return s;
        String serialNumber = extractEssentials(s);
        if (serialNumber.equals(EMPTY) || serialNumber.startsWith("Error"))
            return serialNumber;
        return correct(serialNumber.replaceAll("\\s+", "").trim());
    }

    private static String extractEssentials(String s) {
        s = getResult(s);
        if (TextUtils.isEmpty(s))
            s = EMPTY;
        return s;
    }

    private static String getResult(String s) {
        StringBuilder result = new StringBuilder();
        try {
            JSONObject json = new JSONObject(s);
            int exitCode = json.getInt("OCRExitCode");
            if (exitCode == 3 || exitCode == 4) {
                String errorMessage = json.getString("ErrorMessage");
                String errorDetails = json.getString("ErrorDetails");
                result = result.append("Error: ").append(errorMessage).append(" ").append(errorDetails);
            } else if (exitCode == 1 || exitCode == 2) {
                JSONArray parsedResults = json.getJSONArray("ParsedResults");
                for (int i = 0; i < parsedResults.length(); i++) {
                    JSONObject aResult = parsedResults.getJSONObject(i);
                    int fileParseExitCode = aResult.getInt("FileParseExitCode");
                    if (fileParseExitCode == 1) {
                        String parsedText = aResult.getString("ParsedText");
                        result = result.append(parsedText);
                    }
                }
            }
        } catch (JSONException ignored) {
        }
        return result.toString();
    }

    private static String correct(String s) {
        // when we don't know whether the result must be a letter or a digit
        Map<String, String> char2unambiguous = new HashMap<>();
        // char2unambiguous.put("$", "S");
        // char2unambiguous.put("$", "5");
        char2unambiguous.put("W", "U");
        char2unambiguous.put("K", "X");
        char2unambiguous.put("%", "X");
        char2unambiguous.put("@", "0");
        char2unambiguous.put("i", "1");
        char2unambiguous.put("I", "1");
        char2unambiguous.put("t", "1");
        char2unambiguous.put("#", "4");
        char2unambiguous.put("s", "5");
        char2unambiguous.put("*", "5");
        char2unambiguous.put("?", "7");
        char2unambiguous.put("f", "7");
        char2unambiguous.put("a", "8");
        char2unambiguous.put("&", "8");

        // when we know the result must be a letter
        Map<String, String> char2letter = new HashMap<>();
        char2letter.put("4", "N");
        char2letter.put("0", "O");
        char2letter.put("W", "U");
        char2letter.put("K", "X");
        char2letter.put("%", "X");

        // when we know the result must be a digit
        Map<String, String> char2digit = new HashMap<>();
        char2digit.put("D", "0");
        char2digit.put("O", "0");
        char2digit.put("o", "0");
        char2digit.put("@", "0");
        char2digit.put("i", "1");
        char2digit.put("I", "1");
        char2digit.put("t", "1");
        char2digit.put("Z", "2");
        //char2digit.put("s", "3");
        //char2digit.put("s", "5");
        char2digit.put("S", "5");
        char2digit.put("$", "5");
        char2digit.put("*", "5");
        char2digit.put("?", "7");
        char2digit.put("f", "7");
        char2digit.put("a", "8");
        char2digit.put("A", "8");
        char2digit.put("B", "8");

        List<Integer> letterIndices = new ArrayList<>();
        letterIndices.add(0);
        Pattern pattern = Pattern.compile("\\w\\d{3}\\w\\d", CASE_INSENSITIVE);
        if (s.length() > 9) {
            letterIndices.add(1); // probably a serial number
            pattern = Pattern.compile("\\w{2}\\d{10}", CASE_INSENSITIVE);
        } else
            letterIndices.add(4); // probably a short code

        for (int i = 0; i < s.length(); ++i) {
            s = s.substring(0, i) + correctCharacter(s.charAt(i), char2unambiguous) + s.substring(i+1);
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
