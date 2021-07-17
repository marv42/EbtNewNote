/*
 Copyright (c) 2010 - 2021 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote.exceptions;

import android.content.Context;

import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ErrorMessage {

    public static final String ERROR = "ERROR";
    private final Context context;

    public ErrorMessage(Context context) {
        this.context = context;
    }

    public String getErrorMessage(String text) {
        if (text == null)
            return "";
        text = removeErrorPrefix(text);
        return findAndReplaceResourceId(text);
    }

    @NotNull
    private String removeErrorPrefix(String text) {
        if (text.startsWith(ERROR))
            text = text.substring(ERROR.length());
        return text;
    }

    @NotNull
    private String findAndReplaceResourceId(String text) {
        Pattern pattern = Pattern.compile("R\\.string\\.(\\w+)");
        Matcher matcher = pattern.matcher(text);
        while (matcher.find())
            text = replace(text, matcher);
        return text;
    }

    @NotNull
    private String replace(String text, Matcher matcher) {
        String name = matcher.group(1);
        int resId = context.getResources().getIdentifier(name, "string", context.getPackageName());
        String target = matcher.group();
        return text.replace(target, context.getString(resId));
    }
}
