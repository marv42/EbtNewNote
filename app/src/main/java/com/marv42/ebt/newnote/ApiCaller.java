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

import android.content.SharedPreferences;
import android.support.v4.util.Pair;

import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static com.marv42.ebt.newnote.JsonHelper.getJsonObject;

public class ApiCaller {
    public static final String ERROR = "ERROR";

    private static final String EBT_API = "https://api.eurobilltracker.com/";

    private SharedPreferences mSharedPreferences;
    private String mPrefSettingsEmailKey;
    private String mPrefSettingsPasswordKey;
    private String mCouldntConnect;
    private String mWrongLogin;
    private String mServerError;
    private String mErrorInterpreting;

    @Inject
    public ApiCaller(ThisApp app, SharedPreferences sharedPreferences) {
        mSharedPreferences = sharedPreferences;
        mPrefSettingsEmailKey = app.getString(R.string.pref_settings_email_key);
        mPrefSettingsPasswordKey = app.getString(R.string.pref_settings_password_key);
        mCouldntConnect = app.getString(R.string.couldnt_connect);
        mWrongLogin = app.getString(R.string.wrong_login_info);
        mServerError = app.getString(R.string.server_error);
        mErrorInterpreting = app.getString(R.string.error_interpreting);
    }

    private JSONObject doBasicCall(List<Pair<String, String>> params) {
        FormBody.Builder formBodyBuilder = new FormBody.Builder();
        for (Pair<String, String> pair : params)
            formBodyBuilder.add(pair.first, pair.second);
        FormBody formBody = formBodyBuilder.build();
        Request request = new Request.Builder().url(EBT_API).post(formBody).build();
        Call call = new OkHttpClient().newCall(request);
        try (Response response = call.execute()) {
            if (! response.isSuccessful())
                return null;
            String body = response.body().string();
            JSONObject json = getJsonObject(body);
            if (json == null)
                return getJsonObject(ERROR, body);
            return json;
        } catch (IOException e) {
            return null;
        }
    }

    JSONObject callLogin() {
        List<Pair<String, String>> params = new ArrayList<>();
        params.add(new Pair<>("m", "login"));
        params.add(new Pair<>("v", "2"));
        params.add(new Pair<>("my_email", mSharedPreferences.getString(mPrefSettingsEmailKey, "").trim()));
        params.add(new Pair<>("my_password", mSharedPreferences.getString(mPrefSettingsPasswordKey, "")));
        JSONObject jsonObject = doBasicCall(params);
        if (jsonObject == null)
            return getJsonObject(ERROR, mCouldntConnect);
        if (jsonObject.has(ERROR) || !jsonObject.has("sessionid"))
            return getJsonObject(ERROR, mWrongLogin);
        return jsonObject;
    }

    JSONObject callInsertBills(List<Pair<String, String>> params) {
        JSONObject jsonObject = doBasicCall(params);
        if (jsonObject == null)
            return getJsonObject(ERROR, mCouldntConnect);
        if (!jsonObject.has("note0"))
            return getJsonObject(ERROR, mServerError);

        JSONObject note0 = getJsonObject(jsonObject.optString("note0"));
        if (note0 == null)
            return getJsonObject(ERROR, mErrorInterpreting);
        if (!note0.has("status"))
            return getJsonObject(ERROR, mServerError);
        return note0;
    }

    JSONObject callMyComments(List<Pair<String, String>> params) {
        JSONObject jsonObject = doBasicCall(params);
        if (jsonObject == null)
            return getJsonObject(ERROR, mCouldntConnect);
        if (!jsonObject.has("rows") || !jsonObject.has("data"))
            return getJsonObject(ERROR, mServerError);
        return jsonObject;
    }
}
