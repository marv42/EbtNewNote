/*
 Copyright (c) 2010 - 2020 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote;

import androidx.core.util.Pair;

import com.marv42.ebt.newnote.data.LocationValues;
import com.marv42.ebt.newnote.data.LoginInfo;
import com.marv42.ebt.newnote.data.NoteInsertionData;
import com.marv42.ebt.newnote.exceptions.CallResponseException;
import com.marv42.ebt.newnote.exceptions.HttpCallException;
import com.marv42.ebt.newnote.exceptions.NoJsonElementException;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import okhttp3.FormBody;
import okhttp3.Request;

public class ApiCaller {

    private static final String EBT_API = "https://api.eurobilltracker.com/";
    private static final String SESSION_ID_ELEMENT = "sessionid";
    private static final String USER_NAME_ELEMENT = "username";
    private static final String NOTE_0_ELEMENT = "note0";
    private static final String STATUS_ELEMENT = "status";
    private static final String BILL_ID_ELEMENT = "billId";
    private static final String MY_COUNTRY_ELEMENT = "my_country";
    private static final String MY_CITY_ELEMENT = "my_city";
    private static final String MY_ZIP_ELEMENT = "my_zip";

    private final EncryptedPreferenceDataStore dataStore;

    @Inject
    public ApiCaller(EncryptedPreferenceDataStore dataStore) {
        this.dataStore = dataStore;
    }

    private synchronized String executeHttpCall(List<Pair<String, String>> params) throws HttpCallException {
        Request request = getRequest(params);
        return new HttpCaller().call(request);
    }

    @NotNull
    private Request getRequest(List<Pair<String, String>> params) {
        FormBody.Builder formBodyBuilder = getFormBodyBuilder(params);
        FormBody formBody = formBodyBuilder.build();
        return new Request.Builder().url(EBT_API).post(formBody).build();
    }

    @NotNull
    private FormBody.Builder getFormBodyBuilder(List<Pair<String, String>> params) {
        FormBody.Builder formBodyBuilder = new FormBody.Builder();
        for (Pair<String, String> pair : params)
            if (pair.first != null && pair.second != null)
                formBodyBuilder.add(pair.first, pair.second);
        return formBodyBuilder;
    }

    LoginInfo callLogin() throws HttpCallException {
        List<Pair<String, String>> params = getLoginParams();
        String body = executeHttpCall(params);
        return getLoginInfo(body);
    }

    private LoginInfo getLoginInfo(String body) {
        try {
            JSONObject jsonObject = getJson(body);
            return getLoginInfoFromJson(jsonObject);
        } catch (JSONException e) {
            return new LoginInfo();
        }
    }

    private LoginInfo getLoginInfoFromJson(JSONObject jsonObject) {
        if (!jsonObject.has(SESSION_ID_ELEMENT))
            return new LoginInfo();
        return new LoginInfo(
                jsonObject.optString(SESSION_ID_ELEMENT),
                jsonObject.optString(USER_NAME_ELEMENT),
                new LocationValues(
                        jsonObject.optString(MY_COUNTRY_ELEMENT),
                        jsonObject.optString(MY_CITY_ELEMENT),
                        jsonObject.optString(MY_ZIP_ELEMENT)));
    }

    private JSONObject getJson(String body) throws JSONException {
        return new JSONObject(body);
    }

    @NotNull
    private List<Pair<String, String>> getLoginParams() {
        List<Pair<String, String>> params = new ArrayList<>();
        params.add(new Pair<>("m", "login"));
        params.add(new Pair<>("v", "2"));
        String email = dataStore.get(R.string.pref_settings_email_key, "");
        params.add(new Pair<>("my_email", email.trim()));
        String password = dataStore.get(R.string.pref_settings_password_key, "");
        params.add(new Pair<>("my_password", password));
        return params;
    }

    NoteInsertionData callInsertBills(List<Pair<String, String>> params) throws HttpCallException, CallResponseException {
        try {
            String body = executeHttpCall(params);
            JSONObject json = getJson(body);
            return getNoteInsertionDataFromJson(json);
        } catch (JSONException e) {
            throw new CallResponseException("R.string.no_json: " + e.getMessage());
        }
    }

    @NotNull
    private NoteInsertionData getNoteInsertionDataFromJson(JSONObject json) throws CallResponseException {
        JSONObject note0 = getNote0(json);
        return getNoteInsertionData(note0);
    }

    private JSONObject getNote0(JSONObject json) throws CallResponseException {
        try {
            return new JsonHelper(json).getElement(JSONObject.class, NOTE_0_ELEMENT);
        } catch (NoJsonElementException e) {
            throw new CallResponseException("R.string.no_serial_number");
        }
    }

    @NotNull
    private NoteInsertionData getNoteInsertionData(JSONObject note0) throws CallResponseException {
        try {
            int status = new JsonHelper(note0).getElement(int.class, STATUS_ELEMENT);
            int billId = note0.optInt(BILL_ID_ELEMENT);
            return new NoteInsertionData(billId, status);
        } catch (NoJsonElementException e) {
            throw new CallResponseException("R.string.server_error: " + e.getMessage());
        }
    }

    String callMyComments(List<Pair<String, String>> params) throws HttpCallException {
        return executeHttpCall(params);
    }
}
