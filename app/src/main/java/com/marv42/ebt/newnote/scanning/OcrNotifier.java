/*
 Copyright (c) 2010 - 2020 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote.scanning;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;

import androidx.core.app.NotificationCompat;

import com.marv42.ebt.newnote.R;
import com.marv42.ebt.newnote.SubmitFragment;
import com.marv42.ebt.newnote.ThisApp;
import com.marv42.ebt.newnote.exceptions.NoNotificationManagerException;

import static android.content.Context.NOTIFICATION_SERVICE;
import static com.marv42.ebt.newnote.Notifications.OCR_CHANNEL_ID;
import static com.marv42.ebt.newnote.Notifications.OCR_CHANNEL_NAME;
import static com.marv42.ebt.newnote.Notifications.OCR_NOTIFICATION_ID;
import static com.marv42.ebt.newnote.Notifications.createBuilder;
import static com.marv42.ebt.newnote.Notifications.getNotificationChannel;
import static com.marv42.ebt.newnote.Notifications.getPendingIntent;

public class OcrNotifier {

    public void showNotification(ThisApp app) throws NoNotificationManagerException {
        NotificationManager notificationManager =
                (NotificationManager) app.getSystemService(NOTIFICATION_SERVICE);
        if (notificationManager == null)
            throw new NoNotificationManagerException();
        NotificationChannel notificationChannel = getNotificationChannel(OCR_CHANNEL_ID, OCR_CHANNEL_NAME);
        notificationManager.createNotificationChannel(notificationChannel);
        NotificationCompat.Builder builder = getNotificationBuilder(app);
        notificationManager.notify(OCR_NOTIFICATION_ID, builder.build());
    }

    private NotificationCompat.Builder getNotificationBuilder(ThisApp app) {
        final String title = app.getString(R.string.ocr_result);
        final String content = app.getString(R.string.ocr_result_description);
        PendingIntent intent = getPendingIntent(app, SubmitFragment.class);
        return createBuilder(app, OCR_CHANNEL_ID, title, content, intent);
    }
}
