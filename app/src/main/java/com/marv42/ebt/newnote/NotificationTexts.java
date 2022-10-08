/*
 Copyright (c) 2010 - 2022 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote;

import androidx.annotation.NonNull;

import com.marv42.ebt.newnote.data.ResultSummary;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

import static androidx.core.content.ContextCompat.getColor;
import static androidx.core.text.HtmlCompat.FROM_HTML_MODE_COMPACT;
import static androidx.core.text.HtmlCompat.fromHtml;
import static com.marv42.ebt.newnote.Utils.getColoredString;

public class NotificationTexts {

    private final ThisApp app;

    public NotificationTexts(@NonNull ThisApp app) {
        this.app = app;
    }

    @NotNull
    CharSequence getContentTitle(@NonNull ArrayList<SubmissionResult> notifiedResults) {
        String title = getHowManyNotes(notifiedResults) + ": " + getSummaryText(getSummary(notifiedResults), true, false);
        return getHtml(title);
    }

    @NotNull
    CharSequence getContent(@NonNull ArrayList<SubmissionResult> allResults) {
        String content = app.getString(R.string.total) + ": "
                + getSummaryText(getSummary(allResults), false, true);
        return getHtml(content);
    }

    @NotNull
    private String getHowManyNotes(@NonNull ArrayList<SubmissionResult> notifiedResults) {
        final int quantity = notifiedResults.size();
        return String.format(app.getResources().getQuantityString(R.plurals.xNotes, quantity)
                + " " + app.getString(R.string.sent), quantity);
    }

    @NotNull
    private CharSequence getHtml(String s) {
        return fromHtml(s, FROM_HTML_MODE_COMPACT);
    }

    private ResultSummary getSummary(ArrayList<SubmissionResult> results) {
        int numberOfHits = 0;
        int numberOfSuccessful = 0;
        int numberOfFailed = 0;
        for (SubmissionResult result : results) {
            if (result.isSuccessful(app))
                numberOfSuccessful++;
            else
                numberOfFailed++;
            if (result.isAHit(app))
                numberOfHits++;
        }
        return new ResultSummary(numberOfHits, numberOfSuccessful, numberOfFailed);
    }

    private CharSequence getSummaryText(ResultSummary summary, boolean colored, boolean showNumber) {
        String text = "";
        if (summary.hits > 0)
            text += getColoredTextOrNot(getHitsText(summary), colored, R.color.success);
        if (summary.successful > 0) {
            text = checkComma(text);
            String successful = getHowMany(summary.successful, R.string.successful, summary, showNumber);
            text += getColoredTextOrNot(successful, colored, R.color.success);
        }
        if (summary.failed > 0) {
            text = checkComma(text);
            String failed = getHowMany(summary.failed, R.string.failed, summary, showNumber);
            text += getColoredTextOrNot(failed, colored, R.color.failed);
        }
        return text;
    }

    @NotNull
    private String getHowMany(int number, int resId, ResultSummary summary, boolean showNumber) {
        String text = app.getString(resId);
        if (summary.getTotal() > 1 || showNumber)
            text = number + " " + text;
        return text;
    }

    private String getColoredTextOrNot(String text, boolean colored, int color) {
        if (!colored)
            return text;
        return getColoredString(text, getColor(app, color));
    }

    @NotNull
    private String getHitsText(ResultSummary summary) {
        return String.format(app.getResources().getQuantityString(
                R.plurals.xHits, summary.hits), summary.hits);
    }

    @NotNull
    private String checkComma(String s) {
        if (s.length() > 0)
            s += ", ";
        return s;
    }
}
