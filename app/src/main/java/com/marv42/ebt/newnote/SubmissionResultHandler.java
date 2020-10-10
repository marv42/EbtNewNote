/*
 Copyright (c) 2010 - 2020 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.marv42.ebt.newnote.data.ResultSummary;
import com.marv42.ebt.newnote.exceptions.NoNotificationManagerException;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

import javax.inject.Inject;

import static android.content.Context.NOTIFICATION_SERVICE;
import static androidx.core.content.ContextCompat.getColor;
import static androidx.core.text.HtmlCompat.FROM_HTML_MODE_COMPACT;
import static androidx.core.text.HtmlCompat.fromHtml;
import static com.marv42.ebt.newnote.Notifications.NOTE_NOTIFICATION_ID;
import static com.marv42.ebt.newnote.Notifications.NOTE_SUBMISSION_CHANNEL_ID;
import static com.marv42.ebt.newnote.Notifications.NOTE_SUBMISSION_CHANNEL_NAME;
import static com.marv42.ebt.newnote.Notifications.createBuilder;
import static com.marv42.ebt.newnote.Notifications.getNotificationChannel;
import static com.marv42.ebt.newnote.Notifications.getPendingIntent;
import static com.marv42.ebt.newnote.Utils.getColoredString;

public class SubmissionResultHandler implements NoteDataSubmitter.Callback {

    private ThisApp app;
    private SubmissionResults allResults;
    private ArrayList<SubmissionResult> notifiedResults = new ArrayList<>();

    @Inject
    public SubmissionResultHandler(@NonNull ThisApp app, @NonNull SubmissionResults submissionResults) {
        this.app = app;
        this.allResults = submissionResults;
    }

    @Override
    public void onSubmissionResult(SubmissionResult result) {
        allResults.addResult(result);
        notifiedResults.add(result);
        try {
            showNotification();
        } catch (NoNotificationManagerException e) {
            e.printStackTrace();
        }
    }

    void reset() {
        notifiedResults.clear();
    }

    private void showNotification() throws NoNotificationManagerException {
        NotificationManager notificationManager = getNotificationManager();
        NotificationCompat.Builder builder = getNotificationBuilder();
        notificationManager.notify(NOTE_NOTIFICATION_ID, builder.build());
    }

    private NotificationManager getNotificationManager() throws NoNotificationManagerException {
        NotificationManager notificationManager = (NotificationManager) app.getSystemService(NOTIFICATION_SERVICE);
        if (notificationManager == null)
            throw new NoNotificationManagerException();
        NotificationChannel notificationChannel = getNotificationChannel(
                NOTE_SUBMISSION_CHANNEL_ID, NOTE_SUBMISSION_CHANNEL_NAME);
        notificationManager.createNotificationChannel(notificationChannel);
        return notificationManager;
    }

    private NotificationCompat.Builder getNotificationBuilder() {
        final CharSequence contentTitle = getContentTitle();
        final CharSequence content = getContent();
        PendingIntent contentIntent = getPendingIntent(app, SubmittedFragment.class.getSimpleName());
        return createBuilder(app, NOTE_SUBMISSION_CHANNEL_ID, contentTitle, content, contentIntent);
    }

    @NotNull
    private CharSequence getContentTitle() {
        String title = getHowManyNotes() + ": " + getSummaryText(getSummary(notifiedResults), true, false);
        return getHtml(title);
    }

    @NotNull
    private String getHowManyNotes() {
        final int quantity = notifiedResults.size();
        return String.format(app.getResources().getQuantityString(R.plurals.xNotes, quantity)
                + " " + app.getString(R.string.sent), quantity);
    }

    @NotNull
    private CharSequence getHtml(String s) {
        return fromHtml(s, FROM_HTML_MODE_COMPACT);
    }

    @NotNull
    private CharSequence getContent() {
        String content = app.getString(R.string.total) + ": "
                + getSummaryText(getSummary(allResults.getResults()), false, true);
        return getHtml(content);
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
