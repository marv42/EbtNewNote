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
import android.content.Intent;
import android.os.AsyncTask;

import androidx.core.app.NotificationCompat;
import androidx.core.util.Pair;

import com.marv42.ebt.newnote.exceptions.CallResponseException;
import com.marv42.ebt.newnote.exceptions.ErrorMessage;
import com.marv42.ebt.newnote.exceptions.HttpCallException;
import com.marv42.ebt.newnote.exceptions.NoNotificationManagerException;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import static android.content.Context.NOTIFICATION_SERVICE;
import static android.text.Html.FROM_HTML_MODE_COMPACT;
import static android.text.Html.fromHtml;
import static android.text.TextUtils.isEmpty;
import static com.marv42.ebt.newnote.EbtNewNote.FRAGMENT_TYPE;
import static com.marv42.ebt.newnote.EbtNewNote.NOTE_NOTIFICATION_ID;
import static com.marv42.ebt.newnote.Notifications.NOTE_SUBMISSION_CHANNEL_ID;
import static com.marv42.ebt.newnote.Notifications.NOTE_SUBMISSION_CHANNEL_NAME;
import static com.marv42.ebt.newnote.Notifications.createBuilder;
import static com.marv42.ebt.newnote.Notifications.getNotificationChannel;

public class NoteDataSubmitter extends AsyncTask<NoteData, Void, SubmissionResult> {
    private ThisApp app;
    private ApiCaller apiCaller;
    private SubmissionResults submissionResults;
    private EncryptedPreferenceDataStore dataStore;

    @Inject
    public NoteDataSubmitter(final ThisApp app, ApiCaller apiCaller, SubmissionResults submissionResults,
                             EncryptedPreferenceDataStore dataStore) {
        this.app = app;
        this.apiCaller = apiCaller;
        this.submissionResults = submissionResults;
        this.dataStore = dataStore;
    }

    @Override
    protected SubmissionResult doInBackground(final NoteData... noteDatas) {
        NoteData noteData = noteDatas[0];
        NoteData submittedNoteData = getSubmittedNoteData(noteData);
        try {
            return callLoginAndInsert(noteData, submittedNoteData);
        } catch (HttpCallException | CallResponseException e) {
            return new SubmissionResult(submittedNoteData,
                    new ErrorMessage(app).getErrorMessage(e.getMessage()));
        }
    }

    @NotNull
    private SubmissionResult callLoginAndInsert(NoteData noteData, NoteData submittedNoteData) throws HttpCallException, CallResponseException {
        LoginInfo loginInfo = apiCaller.callLogin();
        if (loginInfo.sessionId.isEmpty())
            return new SubmissionResult(submittedNoteData, app.getString(R.string.wrong_login_info));
        List<Pair<String, String>> params = getInsertionParams(noteData, submittedNoteData, loginInfo);
        NoteInsertionData insertionData = apiCaller.callInsertBills(params);
        return assembleReply(submittedNoteData, insertionData);
    }

    @NotNull
    private NoteData getSubmittedNoteData(NoteData noteData) {
        return new NoteData(
                noteData.mCountry,
                noteData.mCity,
                noteData.mPostalCode,
                noteData.mDenomination,
                noteData.mShortCode,
                noteData.mSerialNumber,
                noteData.mComment + dataStore.get(R.string.pref_settings_comment_key, ""));
    }

    @NotNull
    private SubmissionResult assembleReply(NoteData submittedNoteData, NoteInsertionData insertionData) {
        int billId = insertionData.billId;
        int status = insertionData.status;
        if (status == 0)
            return new SubmissionResult(submittedNoteData,
                    app.getString(R.string.has_been_entered), billId);
        if (status == 1)
            return new SubmissionResult(submittedNoteData, app.getString(R.string.got_hit), billId);
        String reply = "";
        if ((status &  64) != 0)
            reply += app.getString(R.string.already_entered) + "<br>";
        if ((status & 128) != 0)
            reply += app.getString(R.string.already_entered_serial_number) + "<br>";
        if ((status &   4) != 0)
            reply += app.getString(R.string.invalid_country) + "<br>";
        if ((status &  32) != 0)
            reply += app.getString(R.string.city_missing) + "<br>";
        if ((status &   2) != 0)
            reply += app.getString(R.string.invalid_denomination) + "<br>"; // ;-)
        if ((status &  16) != 0)
            reply += app.getString(R.string.invalid_short_code) + "<br>";
        if ((status &   8) != 0)
            reply += app.getString(R.string.invalid_serial_number) + "<br>";
        if ((status & 32768) != 0)
            reply += app.getString(R.string.inconsistent) + "<br>";
        if (reply.endsWith("<br>"))
            reply = reply.substring(0, reply.length() - 4);
        if (isEmpty(reply))
            reply = "Someone seems to have to debug something here...";
        return new SubmissionResult(submittedNoteData, reply, billId);
    }

    @NotNull
    private List<Pair<String, String>> getInsertionParams(NoteData noteData, NoteData submittedNoteData, LoginInfo loginInfo) {
        List<Pair<String, String>> params = new ArrayList<>();
        params.add(new Pair<>("m", "insertbills"));
        params.add(new Pair<>("v", "1"));
        params.add(new Pair<>("PHPSESSID", loginInfo.sessionId));
        params.add(new Pair<>("city", noteData.mCity));
        params.add(new Pair<>("zip", noteData.mPostalCode));
        params.add(new Pair<>("country", noteData.mCountry));
        params.add(new Pair<>("serial0", noteData.mSerialNumber));
        params.add(new Pair<>("denomination0",
                noteData.mDenomination.substring(0, noteData.mDenomination.length() - 2)));
        params.add(new Pair<>("shortcode0", noteData.mShortCode));
        params.add(new Pair<>("comment0", submittedNoteData.mComment));
        return params;
    }

    @Override
    protected void onPostExecute(final SubmissionResult result) {
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
        String content = app.getString(R.string.total) + ": " + getSummary(submissionResults.getSummary());
        String contentTitle = fromHtml(app.getString(R.string.note) + " " +
                app.getString(R.string.sent) + ": " + result.getResult(app), FROM_HTML_MODE_COMPACT).toString();
        PendingIntent contentIntent = getPendingIntent();
        return createBuilder(app, NOTE_SUBMISSION_CHANNEL_ID, contentTitle, content, contentIntent);
    }

    private CharSequence getSummary(final SubmissionResults.ResultSummary summary) {
        String s = "";
        if (summary.hits > 0)
            s = getColoredString(String.format(app.getResources().getQuantityString(
                    R.plurals.xHits, summary.hits), summary.hits), "green");
        if (summary.successful > 0) {
            if (s.length() > 0)
                s += ", ";
            s += summary.successful + " " + app.getString(R.string.successful);
        }
        if (summary.failed > 0) {
            if (s.length() > 0)
                s += ", ";
            s += getColoredString(summary.failed + " " + app.getString(R.string.failed), "red");
        }
        return fromHtml(s, FROM_HTML_MODE_COMPACT);
    }

    @NotNull
    private String getColoredString(String text, String color) {
        return "<font color=\"" + color + "\">" + text + "</font>";
    }

    private PendingIntent getPendingIntent() {
        Intent intent = new Intent(app, EbtNewNote.class);
        intent.putExtra(FRAGMENT_TYPE, SubmittedFragment.class.getSimpleName());
        return PendingIntent.getActivity(app, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
