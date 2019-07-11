/*******************************************************************************
 * Copyright (c) 2010 marvin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Contributors:
 *     marvin - initial API and implementation
 ******************************************************************************/

package com.marv42.ebt.newnote.scanning;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.AsyncTask;

import androidx.core.app.NotificationCompat;

import com.marv42.ebt.newnote.ApiCaller;
import com.marv42.ebt.newnote.EbtNewNote;
import com.marv42.ebt.newnote.R;
import com.marv42.ebt.newnote.SubmitFragment;
import com.marv42.ebt.newnote.ThisApp;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static android.app.NotificationManager.IMPORTANCE_DEFAULT;
import static android.content.Context.NOTIFICATION_SERVICE;
import static android.preference.PreferenceManager.getDefaultSharedPreferences;
import static com.marv42.ebt.newnote.EbtNewNote.FRAGMENT_TYPE;
import static com.marv42.ebt.newnote.EbtNewNote.NOTIFICATION_OCR_CHANNEL_ID;
import static com.marv42.ebt.newnote.EbtNewNote.OCR_NOTIFICATION_ID;
import static com.marv42.ebt.newnote.JsonHelper.getJsonObject;
import static com.marv42.ebt.newnote.scanning.PictureConverter.convert;

public class OcrHandler extends AsyncTask<Void, Void, String> {
    private static final String OCR_HOST = "https://api.ocr.space/parse/image";

    private ThisApp mApp;
    private String mPhotoPath;

    // TODO @Inject ?
    public OcrHandler(ThisApp app, String photoPath) {
        mApp = app;
        mPhotoPath = photoPath;
    }

    @Override
    protected String doInBackground(Void... voids) {
        String base64Image = convert(mPhotoPath);
        FormBody.Builder formBodyBuilder = new FormBody.Builder();
        formBodyBuilder.add("apikey", getDefaultSharedPreferences(mApp).getString(
                mApp.getString(R.string.pref_settings_ocr_key), ""));
        formBodyBuilder.add("base64Image", "data:image/jpeg;base64," + base64Image);
        FormBody formBody = formBodyBuilder.build();

        Request request = new Request.Builder().url(OCR_HOST).post(formBody).build();
        Call call = new OkHttpClient().newCall(request);
        try (Response response = call.execute()) {
            if (! response.isSuccessful())
                return getJsonObject(ApiCaller.ERROR, mApp.getString(R.string.server_error)
                        + ", server response code: " + response.code()).toString();
            String body = response.body().string();
            JSONObject json = getJsonObject(body);
            if (json == null || json.has("error"))
                return getJsonObject(ApiCaller.ERROR, body).toString();
            return json.toString();
        } catch (IOException e) {
            return getJsonObject(ApiCaller.ERROR, mApp.getString(R.string.io_error)).toString();
        }
    }

    @Override
    protected void onPostExecute(String result) {
        getDefaultSharedPreferences(mApp).edit().putString(
                mApp.getString(R.string.pref_ocr_result), TextProcessor.getOcrResult(result)).apply();
        Intent intent = new Intent(mApp, EbtNewNote.class);
        intent.putExtra(FRAGMENT_TYPE, SubmitFragment.class.getSimpleName());
        PendingIntent contentIntent = PendingIntent.getActivity(mApp, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationManager notificationManager = (NotificationManager) mApp.getSystemService(NOTIFICATION_SERVICE);
        if (notificationManager == null)
            return;
        NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_OCR_CHANNEL_ID,
                "OCR Result Notification Channel", IMPORTANCE_DEFAULT);
//            notificationChannel.setDescription("Channel description");
//            notificationChannel.enableLights(true);
//            notificationChannel.setLightColor(Color.RED);
//            notificationChannel.setVibrationPattern(new long[]{0, 1000, 500, 1000});
//            notificationChannel.enableVibration(true);
        notificationManager.createNotificationChannel(notificationChannel);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(mApp, NOTIFICATION_OCR_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_ebt)
                .setContentTitle(mApp.getString(R.string.ocr_result))
                .setContentText(mApp.getString(R.string.ocr_result_description))
                .setAutoCancel(true)
                .setContentIntent(contentIntent);
        notificationManager.notify(OCR_NOTIFICATION_ID, builder.build());
    }
}
