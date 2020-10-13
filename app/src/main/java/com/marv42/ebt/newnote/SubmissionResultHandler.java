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

import java.util.ArrayList;

import javax.inject.Inject;

import static android.content.Context.NOTIFICATION_SERVICE;
import static com.marv42.ebt.newnote.Notifications.NOTE_NOTIFICATION_ID;
import static com.marv42.ebt.newnote.Notifications.NOTE_SUBMISSION_CHANNEL_ID;
import static com.marv42.ebt.newnote.Notifications.NOTE_SUBMISSION_CHANNEL_NAME;
import static com.marv42.ebt.newnote.Notifications.createBuilder;
import static com.marv42.ebt.newnote.Notifications.getNotificationChannel;
import static com.marv42.ebt.newnote.Notifications.getPendingIntent;

public class SubmissionResultHandler implements NoteDataSubmitter.Callback {

    private final ThisApp app;
    private final AllResults allResults;
    private final ArrayList<SubmissionResult> notifiedResults = new ArrayList<>();

    @Inject
    public SubmissionResultHandler(@NonNull ThisApp app, @NonNull AllResults allResults) {
        this.app = app;
        this.allResults = allResults;
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
        NotificationTexts notificationText = new NotificationTexts(app);
        final CharSequence contentTitle = notificationText.getContentTitle(notifiedResults);
        final CharSequence content = notificationText.getContent(allResults.getResults());
        PendingIntent intent = getPendingIntent(app, SubmittedFragment.class);
        return createBuilder(app, NOTE_SUBMISSION_CHANNEL_ID, contentTitle, content, intent);
    }
}
