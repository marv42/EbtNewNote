/*
 Copyright (c) 2010 - 2020 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote;

import android.app.NotificationChannel;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

import org.jetbrains.annotations.NotNull;

import static android.app.NotificationManager.IMPORTANCE_DEFAULT;
import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP;
import static android.graphics.Color.RED;
import static android.graphics.Color.YELLOW;
import static com.marv42.ebt.newnote.EbtNewNote.FRAGMENT_TYPE;

public class Notifications {

    public static final String OCR_CHANNEL_ID = "ebt_ocr_channel";
    public static final String OCR_CHANNEL_NAME = "OCR Result Notification Channel";
    public static final int OCR_NOTIFICATION_ID = 2;
    static final int NOTE_NOTIFICATION_ID = 1;
    static final String NOTE_SUBMISSION_CHANNEL_ID = "default";
    static final String NOTE_SUBMISSION_CHANNEL_NAME = "Note Submission Result Notification Channel";
    private static final int REQUEST_CODE = 0;

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

    public static PendingIntent getPendingIntent(ThisApp app, Class<?> className) {
        Intent intent = new Intent(app, EbtNewNote.class);
        intent.putExtra(FRAGMENT_TYPE, className.getSimpleName());
        return PendingIntent.getActivity(app, REQUEST_CODE, intent, FLAG_UPDATE_CURRENT);
    }
}
