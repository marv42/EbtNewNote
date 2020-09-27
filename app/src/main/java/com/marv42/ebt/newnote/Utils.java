/*
 Copyright (c) 2010 - 2020 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote;

public class Utils {
    public static final int SECONDS_IN_NANOSECONDS = 1000 * 1000 * 1000;

    static final int DAYS_IN_MS = 1000 * 60 * 60 * 24;

    public static String getColoredString(String text, int color) {
        return "<font color=\"" + color + "\">" + text + "</font>";
//        return "<p style=\"color:" + color + "\">" + text + "</p>";
    }
}
