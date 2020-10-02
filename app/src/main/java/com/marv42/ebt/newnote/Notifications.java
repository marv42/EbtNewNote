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

import androidx.core.app.NotificationCompat;

import org.jetbrains.annotations.NotNull;

import static android.app.NotificationManager.IMPORTANCE_DEFAULT;

class Notifications {
    static final String NOTE_SUBMISSION_CHANNEL_ID = "default";
    static final String OCR_CHANNEL_ID = "ebt_ocr_channel";
    static final String NOTE_SUBMISSION_CHANNEL_NAME = "Note Submission Result Notification Channel";
    static final String OCR_CHANNEL_NAME = "OCR Result Notification Channel";

    @NotNull
    static NotificationChannel getNotificationChannel(String channelId, String name) {
//            notificationChannel.setDescription("Channel description");
//            notificationChannel.enableLights(true);
//            notificationChannel.setLightColor(Color.RED);
//            notificationChannel.setVibrationPattern(new long[]{0, 1000, 500, 1000});
//            notificationChannel.enableVibration(true);
        return new NotificationChannel(channelId, name, IMPORTANCE_DEFAULT);
    }

    static NotificationCompat.Builder createBuilder(
            Context context, String channelId, CharSequence title, CharSequence content, PendingIntent intent) {
        return new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_stat_ebt)
                .setContentTitle(title)
                .setContentText(content)
                .setAutoCancel(true)
                .setContentIntent(intent);
    }
}