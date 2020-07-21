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

import androidx.core.util.Pair;

import org.json.JSONObject;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import static com.marv42.ebt.newnote.JsonHelper.getJsonObject;

public class ApiCaller {
    public static final String ERROR = "ERROR";

    private static final String EBT_API = "https://api.eurobilltracker.com/";

    private EncryptedPreferenceDataStore mDataStore;
    private final String mPrefSettingsEmailKey;
    private final String mPrefSettingsPasswordKey;
    private final String mNoConnection;
    private final String mHttpError;
    private final String mWrongLogin;
    private final String mServerError;
    private final String mNoSerialNumber;
    private final String mInternalError;

    @Inject
    public ApiCaller(ThisApp app, EncryptedPreferenceDataStore dataStore) {
        mDataStore = dataStore;
        mPrefSettingsEmailKey = app.getString(R.string.pref_settings_email_key);
        mPrefSettingsPasswordKey = app.getString(R.string.pref_settings_password_key);
        mNoConnection = app.getString(R.string.error_no_connection);
        mHttpError = app.getString(R.string.http_error);
        mWrongLogin = app.getString(R.string.wrong_login_info);
        mServerError = app.getString(R.string.server_error);
        mNoSerialNumber = app.getString(R.string.no_serial_number);
        mInternalError = app.getString(R.string.internal_error);
    }

    private synchronized JSONObject doBasicCall(List<Pair<String, String>> params) {
        FormBody.Builder formBodyBuilder = new FormBody.Builder();
        for (Pair<String, String> pair : params)
            if (pair.first != null && pair.second != null)
                formBodyBuilder.add(pair.first, pair.second);
        FormBody formBody = formBodyBuilder.build();
        Request request = new Request.Builder().url(EBT_API).post(formBody).build();
        Call call = new OkHttpClient().newCall(request);
        try (Response response = call.execute()) {
            if (! response.isSuccessful())
                return getJsonObject(ERROR, mHttpError + " " + response.code());
            ResponseBody responseBody = response.body();
            String body = responseBody != null ? responseBody.string() : "";
            JSONObject json = getJsonObject(body);
            if (json == null)
                return getJsonObject(ERROR, body);
            return json;
        } catch (SocketTimeoutException e) {
            return getJsonObject(ERROR, mNoConnection);
        } catch (IOException e) {
            return getJsonObject(ERROR, mInternalError + ": " + e.getMessage());
        }
    }

    JSONObject callLogin() {
        List<Pair<String, String>> params = new ArrayList<>();
        params.add(new Pair<>("m", "login"));
        params.add(new Pair<>("v", "2"));
        String email = mDataStore.getString(mPrefSettingsEmailKey, "");
        params.add(new Pair<>("my_email", email != null ? email.trim() : ""));
        String password = mDataStore.getString(mPrefSettingsPasswordKey, "");
        params.add(new Pair<>("my_password", password != null ? password : ""));
        JSONObject jsonObject = doBasicCall(params);
        if (jsonObject == null)
            return getJsonObject(ERROR, mInternalError);
        if (jsonObject.optString(ERROR).equals("false") || !jsonObject.has("sessionid"))
            return getJsonObject(ERROR, mWrongLogin);
        return jsonObject;
    }

    JSONObject callInsertBills(List<Pair<String, String>> params) {
        JSONObject jsonObject = doBasicCall(params);
        if (jsonObject == null)
            return getJsonObject(ERROR, mInternalError);
        if (jsonObject.has(ERROR))
            return jsonObject;
        if (!jsonObject.has("note0"))
            return getJsonObject(ERROR, mNoSerialNumber);
        JSONObject note0 = getJsonObject(jsonObject.optString("note0"));
        if (note0 == null)
            return getJsonObject(ERROR, mNoSerialNumber);
        if (!note0.has("status"))
            return getJsonObject(ERROR, mServerError + ": no 'status' element in the 'note0' element");
        return note0;
    }

    JSONObject callMyComments(List<Pair<String, String>> params) {
        JSONObject jsonObject = doBasicCall(params);
        if (jsonObject == null)
            return getJsonObject(ERROR, mInternalError);
        if (jsonObject.has(ERROR))
            return jsonObject;
        if (!jsonObject.has("rows"))
            return getJsonObject(ERROR, mServerError + ": no 'rows' element");
        if (!jsonObject.has("data"))
            return getJsonObject(ERROR, mServerError + ": no 'data' element");
        return jsonObject;
    }
}
