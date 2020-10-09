/*
 Copyright (c) 2010 - 2020 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote;

import android.os.AsyncTask;

import androidx.core.util.Pair;

import com.marv42.ebt.newnote.exceptions.CallResponseException;
import com.marv42.ebt.newnote.exceptions.ErrorMessage;
import com.marv42.ebt.newnote.exceptions.HttpCallException;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import static android.text.TextUtils.isEmpty;

public class NoteDataSubmitter extends AsyncTask<NoteData, Void, SubmissionResult> {
    private ThisApp app;
    private ApiCaller apiCaller;
    private Callback callback;

    @Inject
    public NoteDataSubmitter(final ThisApp app, ApiCaller apiCaller, Callback callback) {
        this.app = app;
        this.apiCaller = apiCaller;
        this.callback = callback;
    }

    @Override
    protected SubmissionResult doInBackground(final NoteData... noteDatas) {
        NoteData noteData = noteDatas[0];
        try {
            return callLoginAndInsert(noteData);
        } catch (HttpCallException | CallResponseException e) {
            return new SubmissionResult(noteData, new ErrorMessage(app).getErrorMessage(e.getMessage()));
        }
    }

    @NotNull
    private SubmissionResult callLoginAndInsert(NoteData noteData) throws HttpCallException, CallResponseException {
        LoginInfo loginInfo = apiCaller.callLogin();
        if (loginInfo.sessionId.isEmpty())
            return new SubmissionResult(noteData, app.getString(R.string.wrong_login_info));
        List<Pair<String, String>> params = getInsertionParams(noteData, loginInfo);
        NoteInsertionData insertionData = apiCaller.callInsertBills(params);
        return assembleReply(noteData, insertionData);
    }

    @NotNull
    private List<Pair<String, String>> getInsertionParams(NoteData noteData, LoginInfo loginInfo) {
        List<Pair<String, String>> params = new ArrayList<>();
        params.add(new Pair<>("m", "insertbills"));
        params.add(new Pair<>("v", "1"));
        params.add(new Pair<>("PHPSESSID", loginInfo.sessionId));
        params.add(new Pair<>("city", noteData.mCity));
        params.add(new Pair<>("zip", noteData.mPostalCode));
        params.add(new Pair<>("country", noteData.mCountry));
        params.add(new Pair<>("serial0", noteData.mSerialNumber));
        params.add(new Pair<>("denomination0", getDenominationValue(noteData.mDenomination)));
        params.add(new Pair<>("shortcode0", noteData.mShortCode));
        params.add(new Pair<>("comment0", noteData.mComment));
        return params;
    }

    @NotNull
    private String getDenominationValue(String denomination) {
        return denomination.substring(0, denomination.length() - 2);
    }

    @NotNull
    private SubmissionResult assembleReply(NoteData noteData, NoteInsertionData insertionData) {
        int billId = insertionData.billId;
        int status = insertionData.status;
        if (status == 0)
            return new SubmissionResult(noteData, app.getString(R.string.has_been_entered), billId);
        if (status == 1)
            return new SubmissionResult(noteData, app.getString(R.string.got_hit), billId);
        String reply = "";
        if ((status & 64) != 0)
            reply += app.getString(R.string.already_entered) + "<br>";
        if ((status & 128) != 0)
            reply += app.getString(R.string.already_entered_serial_number) + "<br>";
        if ((status & 4) != 0)
            reply += app.getString(R.string.invalid_country) + "<br>";
        if ((status & 32) != 0)
            reply += app.getString(R.string.city_missing) + "<br>";
        if ((status & 2) != 0)
            reply += app.getString(R.string.invalid_denomination) + "<br>"; // ;-)
        if ((status & 16) != 0)
            reply += app.getString(R.string.invalid_short_code) + "<br>";
        if ((status & 8) != 0)
            reply += app.getString(R.string.invalid_serial_number) + "<br>";
        if ((status & 32768) != 0)
            reply += app.getString(R.string.inconsistent) + "<br>";
        if (reply.endsWith("<br>"))
            reply = reply.substring(0, reply.length() - 4);
        if (isEmpty(reply))
            reply = "Someone seems to have to debug something here...";
        return new SubmissionResult(noteData, reply, billId);
    }

    @Override
    protected void onPostExecute(final SubmissionResult result) {
        callback.onSubmissionResult(result);
    }

    interface Callback {
        void onSubmissionResult(SubmissionResult result);
    }
}
