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

import com.marv42.ebt.newnote.exceptions.NoNotificationManagerException;

import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;

import static android.content.Context.NOTIFICATION_SERVICE;
import static androidx.core.text.HtmlCompat.FROM_HTML_MODE_COMPACT;
import static androidx.core.text.HtmlCompat.fromHtml;
import static com.marv42.ebt.newnote.EbtNewNote.NOTE_NOTIFICATION_ID;
import static com.marv42.ebt.newnote.Notifications.NOTE_SUBMISSION_CHANNEL_ID;
import static com.marv42.ebt.newnote.Notifications.NOTE_SUBMISSION_CHANNEL_NAME;
import static com.marv42.ebt.newnote.Notifications.createBuilder;
import static com.marv42.ebt.newnote.Notifications.getNotificationChannel;
import static com.marv42.ebt.newnote.Notifications.getPendingIntent;

public class SubmissionResultHandler implements NoteDataSubmitter.Callback {
    private ThisApp app;
    private SubmissionResults submissionResults;

    @Inject
    public SubmissionResultHandler(@NonNull ThisApp app, @NonNull SubmissionResults submissionResults) {
        this.app = app;
        this.submissionResults = submissionResults;
    }

    @Override
    public void onSubmissionResult(SubmissionResult result) {
        submissionResults.addResult(result);
        try {
            showNotification(result);
        } catch (NoNotificationManagerException e) {
            e.printStackTrace();
        }
    }

    private void showNotification(SubmissionResult result) throws NoNotificationManagerException {
        NotificationManager notificationManager = getNotificationManager();
        NotificationCompat.Builder builder = getNotificationBuilder(result);
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

    private NotificationCompat.Builder getNotificationBuilder(SubmissionResult result) {
        final CharSequence contentTitle = getContentTitle(result);
        final CharSequence content = getSummaryText();
        PendingIntent contentIntent = getPendingIntent(app, SubmittedFragment.class.getSimpleName());
        return createBuilder(app, NOTE_SUBMISSION_CHANNEL_ID, contentTitle, content, contentIntent);
    }

    @NotNull
    private CharSequence getContentTitle(SubmissionResult result) {
        String title = app.getString(R.string.note) + " " + app.getString(R.string.sent) + ": "
                + result.getResult(app);
        return fromHtml(title, FROM_HTML_MODE_COMPACT);
    }

    private CharSequence getSummaryText() {
        final ResultSummary summary = submissionResults.getSummary();
        final String prefix = app.getString(R.string.total) + ": ";
        String s = prefix;
        if (summary.hits > 0)
            s += getHitsText(summary);
        if (summary.successful > 0) {
            s = checkComma(s, prefix);
            s += summary.successful + " " + app.getString(R.string.successful);
        }
        if (summary.failed > 0) {
            s = checkComma(s, prefix);
            s += summary.failed + " " + app.getString(R.string.failed);
        }
        return fromHtml(s, FROM_HTML_MODE_COMPACT);
    }

    @NotNull
    private String getHitsText(ResultSummary summary) {
        return String.format(app.getResources().getQuantityString(
                R.plurals.xHits, summary.hits), summary.hits);
    }

    @NotNull
    private String checkComma(String s, String prefix) {
        if (s.length() > prefix.length())
            s += ", ";
        return s;
    }
}
