/*
 Copyright (c) 2010 - 2022 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote.scanning;

import org.jetbrains.annotations.NotNull;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.marv42.ebt.newnote.scanning.PatternAnalyzer.findPattern;
import static com.marv42.ebt.newnote.scanning.ReplacementMaps.getAmbiguousMap;
import static com.marv42.ebt.newnote.scanning.ReplacementMaps.getDigitMap;
import static com.marv42.ebt.newnote.scanning.ReplacementMaps.getLetterMap;
import static java.text.Normalizer.*;

import androidx.annotation.NonNull;

public class Corrections {

    public static final int LENGTH_THRESHOLD_SERIAL_NUMBER = 8;

    static String correct(String s) {
        s = removeWhitespaces(s);
        s = correctChars(s);
        s = findPattern(s);
        return s.toUpperCase();
    }

    private static String removeWhitespaces(String s) {
        return s.replace(" ","");
    }

    @NotNull
    private static String correctChars(String s) {
        List<Integer> letterIndices = getLetterIndices(s);
        for (int i = 0; i < s.length(); ++i) {
            if (letterIndices.contains(i))
                s = correctLetter(s, i);
            else
                s = correctDigit(s, i);
            s = correctAmbiguous(s, i);
        }
        return s;
    }

    @NotNull
    private static List<Integer> getLetterIndices(String s) {
        List<Integer> letterIndices = new ArrayList<>();
        letterIndices.add(0);
        if (s.length() >= LENGTH_THRESHOLD_SERIAL_NUMBER)
            letterIndices.add(1);
        else
            letterIndices.add(4);
        return letterIndices;
    }

    @NotNull
    private static String correctAmbiguous(String s, int i) {
        return correctCharInString(s, i, getAmbiguousMap());
    }

    @NotNull
    private static String correctDigit(String s, int i) {
        return correctDigitOrLetter(s, i, getDigitMap(), "\\d");
    }

    @NotNull
    private static String correctLetter(String s, int i) {
        s = normalizeTransliteration(s, i);
        return correctDigitOrLetter(s, i, getLetterMap(), "\\w");
    }

    @NonNull
    private static String normalizeTransliteration(String s, int i) {
        final String letter = s.substring(i, i + 1);
        if (!isAnAllowedLetter(letter))
            s = replace(s, i, Normalizer.normalize(letter, Form.NFD));
        s = s.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return s;
    }

    private static boolean isAnAllowedLetter(String s) {
        for (String l: getAllowedLetters())
            if (s.equals(l))
                return true;
        return false;
    }

    private static List<String> getAllowedLetters() {
        List<String> allowedLetters = new ArrayList<>();
        for (int i = 'A' ; i <= 'Z'; i++)
            allowedLetters.add(Character.toString((char)i));
        for (int i = 'a' ; i <= 'z'; i++)
            allowedLetters.add(Character.toString((char)i));
        return allowedLetters;
    }

    @NotNull
    private static String correctDigitOrLetter(String s, int i, Map<String, String> char2something, String regex) {
        if (!s.substring(i, i + 1).matches(regex))
            s = correctCharInString(s, i, char2something);
        return s;
    }

    @NotNull
    private static String correctCharInString(String s, int i, Map<String, String> char2something) {
        return replace(s, i, correctCharacter(s.charAt(i), char2something));
    }

    private static String correctCharacter(char c, Map<String, String> char2something) {
        String s = Character.toString(c);
        if (char2something.containsKey(s))
            return char2something.get(s);
        return s;
    }

    private static String replace(String inString, int atIndex, String withString) {
        return inString.substring(0, atIndex) + withString + inString.substring(atIndex + withString.length());
    }
}
