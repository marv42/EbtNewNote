/*
 Copyright (c) 2010 - 2022 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote.scanning;

import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.marv42.ebt.newnote.scanning.Corrections.LENGTH_THRESHOLD_SERIAL_NUMBER;
import static java.util.regex.Pattern.CASE_INSENSITIVE;

public class PatternAnalyzer {

    static String findPattern(String s) {
        Matcher matcher = getMatcher(s);
        if (matcher.find())
            s = s.substring(matcher.start(), matcher.end());
        return s;
    }

    @NotNull
    private static Matcher getMatcher(String s) {
        Pattern pattern = Pattern.compile("\\w\\d{3}\\w\\d", CASE_INSENSITIVE);
        if (s.length() >= LENGTH_THRESHOLD_SERIAL_NUMBER)
            // we don't support the old number format \\w{1}\\d{11} any more,
            // because if we would, we couldn't fix the 2nd letter of the new format
            pattern = Pattern.compile("\\w{2}\\d{10}", CASE_INSENSITIVE);
        return pattern.matcher(s);
    }
}
