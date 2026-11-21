/*
 Copyright (c) 2010 - 2026 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;
import androidx.fragment.app.Fragment;

import com.marv42.ebt.newnote.exceptions.NoNotificationManagerException;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

import static android.app.NotificationManager.IMPORTANCE_DEFAULT;
import static android.content.Context.NOTIFICATION_SERVICE;
import static android.app.PendingIntent.FLAG_IMMUTABLE;
import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static android.graphics.Color.YELLOW;
import static com.marv42.ebt.newnote.EbtNewNote.FRAGMENT_TYPE;

public class Notifications {

    static final int EBT_NOTIFICATION_ID = 1;
    static final int OCR_NOTIFICATION_ID = 2;
    static final String EBT_SUBMISSION_CHANNEL_ID = "ebt";
    static final String EBT_SUBMISSION_CHANNEL_NAME = "Note Submission Result Notification Channel";
    static final String OCR_RESULT_CHANNEL_ID = "ocr";
    static final String OCR_RESULT_CHANNEL_NAME = "OCR Result Notification Channel";
    private static final int EBT_REQUEST_CODE = 0;
    private static final int OCR_REQUEST_CODE = 1;

    @NotNull
    public static NotificationChannel getNotificationChannel(String channelId, String name) {
        final NotificationChannel channel = new NotificationChannel(channelId, name, IMPORTANCE_DEFAULT);
//        channel.setDescription("Channel description");
//        channel.enableLights(true);
        channel.setLightColor(YELLOW);
//        channel.setVibrationPattern(new long[]{0, 1000, 500, 1000});
//        channel.enableVibration(true);
        return channel;
    }

    public static NotificationCompat.Builder createBuilder(
            Context context, String channelId, CharSequence title, CharSequence content, PendingIntent intent) {
        return new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_stat_ebt)
                .setContentTitle(title)
                .setContentText(content)
                .setAutoCancel(true)
                .setContentIntent(intent);
    }

    public static PendingIntent getEbtPendingIntent(ThisApp app, Class<?> className) {
        return getPendingIntent(app, className, EBT_REQUEST_CODE);
    }

    private static PendingIntent getPendingIntent(ThisApp app, Class<?> className, int requestCode) {
        Intent intent = new Intent(app, EbtNewNote.class);
        intent.putExtra(FRAGMENT_TYPE, className.getSimpleName());
        return PendingIntent.getActivity(app, requestCode, intent, FLAG_UPDATE_CURRENT | FLAG_IMMUTABLE);
    }

    public static void showSubmissionResult(ThisApp app, ArrayList<SubmissionResult> results)
            throws NoNotificationManagerException {
        NotificationTexts notificationText = new NotificationTexts(app);
        final CharSequence title = notificationText.getTitle(results);
        final CharSequence content = notificationText.getContent(results);
        showNotification(app, EBT_SUBMISSION_CHANNEL_ID, EBT_SUBMISSION_CHANNEL_NAME, EBT_REQUEST_CODE, EBT_NOTIFICATION_ID,
                title, content, ResultsFragment.class);
    }

    public static void showOcrResult(ThisApp app) throws NoNotificationManagerException {
        showNotification(app, OCR_RESULT_CHANNEL_ID, OCR_RESULT_CHANNEL_NAME, OCR_REQUEST_CODE, OCR_NOTIFICATION_ID,
                app.getString(R.string.ocr_result), app.getString(R.string.ocr_result_description),
                SubmitFragment.class);
    }

    private static void showNotification(ThisApp app, String channelId, String channelName, int requestCode,
                                         int notificationId, CharSequence title, CharSequence content,
                                         Class<? extends Fragment> fragmentClass)
            throws NoNotificationManagerException {
        PendingIntent intent = getPendingIntent(app, fragmentClass, requestCode);
        NotificationManager notificationManager = getNotificationManager(app, channelId, channelName);
        notificationManager.notify(notificationId, createBuilder(app, channelId, title, content, intent).build());
    }

    static NotificationManager getNotificationManager(
            ThisApp app, String channelId, String channelName) throws NoNotificationManagerException {
        NotificationManager notificationManager =
                (NotificationManager) app.getSystemService(NOTIFICATION_SERVICE);
        if (notificationManager == null)
            throw new NoNotificationManagerException();
        NotificationChannel notificationChannel = getNotificationChannel(channelId, channelName);
        notificationManager.createNotificationChannel(notificationChannel);
        return notificationManager;
    }
}
