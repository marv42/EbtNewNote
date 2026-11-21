/*
 Copyright (c) 2010 - 2026 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote;

import static android.content.Context.NOTIFICATION_SERVICE;
import static android.widget.Toast.LENGTH_LONG;
import static com.marv42.ebt.newnote.Notifications.EBT_NOTIFICATION_ID;
import static com.marv42.ebt.newnote.Notifications.EBT_SUBMISSION_CHANNEL_ID;
import static com.marv42.ebt.newnote.Notifications.EBT_SUBMISSION_CHANNEL_NAME;
import static com.marv42.ebt.newnote.Notifications.createBuilder;
import static com.marv42.ebt.newnote.Notifications.getNotificationChannel;
import static com.marv42.ebt.newnote.Notifications.getEbtPendingIntent;
import static com.marv42.ebt.newnote.Notifications.getNotificationManager;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.marv42.ebt.newnote.exceptions.NoNotificationManagerException;
import com.marv42.ebt.newnote.retry.RetryManager;

import java.util.ArrayList;

import javax.inject.Inject;

public class SubmissionResultHandler implements NoteDataSubmitterTask.Callback {

    private static final String TAG = SubmissionResultHandler.class.getSimpleName();
    private final ThisApp app;
    private final AllResults allResults;
    private final RetryManager retryManager;
    private final ArrayList<SubmissionResult> notifiedResults = new ArrayList<>();

    @Inject
    public SubmissionResultHandler(@NonNull ThisApp app, @NonNull AllResults allResults,
                                   @NonNull RetryManager retryManager) {
        this.app = app;
        this.allResults = allResults;
        this.retryManager = retryManager;
    }

    @Override
    public void onSubmissionResult(SubmissionResult result) {
        if (result.isRetryable()) {
            Toast.makeText(app, result.mReason, LENGTH_LONG).show();
            retryManager.scheduleSubmission(result.mNoteData);
            Toast.makeText(app, R.string.sending_later, LENGTH_LONG).show();
            return;
        }
        allResults.addResult(result);
        notifiedResults.add(result);
        try {
            showNotification();
        } catch (NoNotificationManagerException e) {
            Log.w(TAG, e.getMessage());
        }
    }

    private void showNotification() throws NoNotificationManagerException {
        NotificationManager notificationManager = getNotificationManager(app,
                EBT_SUBMISSION_CHANNEL_ID, EBT_SUBMISSION_CHANNEL_NAME);
        NotificationCompat.Builder builder = getNotificationBuilder();
        notificationManager.notify(EBT_NOTIFICATION_ID, builder.build());
    }

    private NotificationCompat.Builder getNotificationBuilder() {
        NotificationTexts notificationText = new NotificationTexts(app);
        final CharSequence title = notificationText.getTitle(notifiedResults);
        final CharSequence content = notificationText.getContent(allResults.getResults());
        PendingIntent intent = getEbtPendingIntent(app, ResultsFragment.class);
        return createBuilder(app, EBT_SUBMISSION_CHANNEL_ID, title, content, intent);
    }
}
