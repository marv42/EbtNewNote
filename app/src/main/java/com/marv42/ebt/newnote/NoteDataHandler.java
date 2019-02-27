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

package com.marv42.ebt.newnote;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.util.Pair;

import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import static android.app.NotificationManager.IMPORTANCE_DEFAULT;
import static android.content.Context.NOTIFICATION_SERVICE;
import static android.preference.PreferenceManager.getDefaultSharedPreferences;
import static android.text.Html.FROM_HTML_MODE_COMPACT;
import static android.text.Html.fromHtml;
import static android.text.TextUtils.isEmpty;
import static com.marv42.ebt.newnote.ApiCaller.ERROR;
import static com.marv42.ebt.newnote.EbtNewNote.EBT_NOTIFICATION_ID;
import static com.marv42.ebt.newnote.EbtNewNote.FRAGMENT_TYPE;

public class NoteDataHandler extends AsyncTask<NoteData, Void, SubmissionResult> {
    private static final String CHANNEL_ID = "default";

    private WeakReference<Context> mContext;
    private ApiCaller mApiCaller;

    // TODO @Inject
    NoteDataHandler(final Context context, ApiCaller apiCaller) {
        mContext = new WeakReference<>(context);
        mApiCaller = apiCaller;
    }

    @Override
    protected SubmissionResult doInBackground(final NoteData... params) {
        return submit(params[0]);
    }

    @Override
    protected void onPostExecute(final SubmissionResult result) {
        ThisApp app = (ThisApp) mContext.get().getApplicationContext();
        app.addResult(result);

        final int n = app.getNumberOfResults();
        String contentTitle = String.format(mContext.get().getResources().getQuantityString(R.plurals.xNotes, n) + " " + mContext.get().getString(R.string.sent), n);

        Intent intent = new Intent(app, EbtNewNote.class);
        intent.putExtra(FRAGMENT_TYPE, SubmittedFragment.class.getSimpleName());
        PendingIntent contentIntent = PendingIntent.getActivity(app, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationManager notificationManager = (NotificationManager) app.getSystemService(NOTIFICATION_SERVICE);
        if (notificationManager == null)
            return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID, "EBT Notification Channel", IMPORTANCE_DEFAULT);
//            notificationChannel.setDescription("Channel description");
//            notificationChannel.enableLights(true);
//            notificationChannel.setLightColor(Color.RED);
//            notificationChannel.setVibrationPattern(new long[]{0, 1000, 500, 1000});
//            notificationChannel.enableVibration(true);
            notificationManager.createNotificationChannel(notificationChannel);
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(app, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_ebt)
                .setContentTitle(contentTitle)
                .setContentText(getSummary(app.getSummary()))
                .setAutoCancel(true)
                .setContentIntent(contentIntent);
        notificationManager.notify(EBT_NOTIFICATION_ID, builder.build());
    }

    private SubmissionResult submit(NoteData noteData) {
        NoteData submittedNoteData = new NoteData(
                noteData.getCountry(),
                noteData.getCity(),
                noteData.getPostalCode(),
                noteData.getDenomination(),
                noteData.getShortCode(),
                noteData.getSerialNumber(),
                noteData.getComment() +
                        getDefaultSharedPreferences(mContext.get()).getString(
                                mContext.get().getString(R.string.pref_settings_comment_key), ""));

        JSONObject json = mApiCaller.callLogin();
        if (json.has(ERROR))
            return new SubmissionResult(submittedNoteData, false, json.optString(ERROR));

        List<Pair<String, String>> params = new ArrayList<>();
        params.add(new Pair<>("m", "insertbills"));
        params.add(new Pair<>("v", "1"));
        params.add(new Pair<>("PHPSESSID", json.optString("sessionid")));
        params.add(new Pair<>("city", noteData.getCity()));
        params.add(new Pair<>("zip", noteData.getPostalCode()));
        params.add(new Pair<>("country", noteData.getCountry()));
        params.add(new Pair<>("serial0", noteData.getSerialNumber()));
        params.add(new Pair<>("denomination0",
                noteData.getDenomination().substring(0, noteData.getDenomination().length() - 2)));
        params.add(new Pair<>("shortcode0", noteData.getShortCode()));
        params.add(new Pair<>("comment0", submittedNoteData.getComment()));

        json = mApiCaller.callInsertBills(params);
        if (json.has(ERROR))
            return new SubmissionResult(submittedNoteData, false, json.optString(ERROR));

        int billId = json.optInt("billId");
        int status = json.optInt("status");

        if (status == 0)
            return new SubmissionResult(submittedNoteData, true,
                    mContext.get().getString(R.string.has_been_entered), billId);

        if (status == 1)
            return new SubmissionResult(submittedNoteData, true,
                    mContext.get().getString(R.string.got_hit), billId, true);

        String reply = "";
        if ((status &  64) != 0)
            reply += mContext.get().getString(R.string.already_entered      ) + "<br>";
        if ((status & 128) != 0)
            reply += mContext.get().getString(R.string.different_short_code ) + "<br>";
        if ((status &   4) != 0)
            reply += mContext.get().getString(R.string.invalid_country      ) + "<br>";
        if ((status &  32) != 0)
            reply += mContext.get().getString(R.string.city_missing         ) + "<br>";
        if ((status &   2) != 0)
            reply += mContext.get().getString(R.string.invalid_denomination ) + "<br>"; // ;-)
        if ((status &  16) != 0)
            reply += mContext.get().getString(R.string.invalid_short_code   ) + "<br>";
        if ((status &   8) != 0)
            reply += mContext.get().getString(R.string.invalid_serial_number) + "<br>";
        if (reply.endsWith("<br>"))
            reply = reply.substring(0, reply.length() - 4);
        if (isEmpty(reply))
            reply = "Someone seems to have to debug something here...";

        return new SubmissionResult(submittedNoteData, false, reply, billId);
    }

    private CharSequence getSummary(final ThisApp.ResultSummary summary) {
        String s = "";
        int numHits = summary.getHits();
        if (numHits > 0) {
            s = "<font color=\"green\">" + String.format(mContext.get().getResources().getQuantityString(
                    R.plurals.xHits, numHits), numHits) + "</font>";
        }
        int numFailed = summary.getFailed();
        if (numFailed > 0) {
            if (s.length() > 0)
                s += ", ";
            s += "<font color=\"red\">" + Integer.toString(numFailed) + " " +
                    mContext.get().getString(R.string.failed) + "</font>";
        }
        int numSuccessful = summary.getSuccessful();
        if (numSuccessful > 0) {
            if (s.length() > 0)
                s += ", ";
            s += Integer.toString(numSuccessful) + " " +
                    mContext.get().getString(R.string.successful);
        }
        return fromHtml(s, FROM_HTML_MODE_COMPACT);
    }
}
