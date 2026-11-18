/*
 Copyright (c) 2010 - 2026 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote.exceptions;

import android.content.Context;
import android.widget.Toast;

import com.marv42.ebt.newnote.R;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ErrorMessage {

    public static final String ERROR = "ERROR";
    private static final Pattern SPLIT_PATTERN = Pattern.compile("\\n+");
    private final Context context;

    public ErrorMessage(Context context) {
        this.context = context;
    }

    public void showErrorToasts(@Nullable String text) {
        String message = text == null ? context.getString(R.string.internal_error) : text;
        for (String part : getErrorMessages(message))
            Toast.makeText(context, part, Toast.LENGTH_LONG).show();
    }

    @NotNull
    public List<String> getErrorMessages(String text) {
        List<String> messages = new ArrayList<>();
        for (String part : SPLIT_PATTERN.split(getErrorMessage(text))) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty())
                messages.add(trimmed);
        }
        return messages;
    }

    public String getErrorMessage(String text) {
        if (text == null)
            return "";
        text = removeErrorPrefix(text);
        return findAndReplaceResourceId(text);
    }

    @NotNull
    public String getErrorMessage(@Nullable String text, @Nullable Integer generalDescriptionResId) {
        List<String> parts = new ArrayList<>();
        if (generalDescriptionResId != null)
            parts.add(context.getString(generalDescriptionResId));
        addPart(parts, getErrorMessage(text));
        return join(parts);
    }

    private static void addPart(List<String> parts, String part) {
        if (part != null && !part.trim().isEmpty())
            parts.add(part.trim());
    }

    @NotNull
    private static String join(List<String> parts) {
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (builder.length() > 0)
                builder.append("\n");
            builder.append(removeTrailingPeriods(part));
        }
        return builder.toString();
    }

    @NotNull
    public String getHttpCallErrorMessage(HttpCallException e) {
        return getHttpCallErrorMessage(e, null);
    }

    @NotNull
    public String getHttpCallErrorMessage(HttpCallException e, @Nullable Integer generalDescriptionResId) {
        List<String> parts = new ArrayList<>();
        if (generalDescriptionResId != null)
            parts.add(context.getString(generalDescriptionResId));
        if (e.getResponseCode() == 0)
            addPart(parts, getErrorMessage(e.getMessage()));
        else
            addPart(parts, context.getString(R.string.http_error_details,
                    reason(e), String.valueOf(e.getResponseCode()), server(e)));
        String apiErrorMessage = e.getApiErrorMessage();
        if (apiErrorMessage != null && !apiErrorMessage.trim().isEmpty())
            parts.add(apiErrorMessage.trim());
        return join(parts);
    }

    @NotNull
    private String reason(HttpCallException e) {
        String reason = e.getReason();
        if (reason == null || reason.trim().isEmpty())
            return context.getString(R.string.http_error_unknown_reason);
        return "'" + reason.trim() + "'";
    }

    @NotNull
    private String server(HttpCallException e) {
        String server = e.getServer();
        return server == null ? "" : server;
    }

    @NotNull
    private static String removeTrailingPeriods(String text) {
        while (text.endsWith("."))
            text = text.substring(0, text.length() - 1);
        return text;
    }

    @NotNull
    private static String removeErrorPrefix(String text) {
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
