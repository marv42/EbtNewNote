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
import android.preference.PreferenceManager;
import android.support.v4.util.Pair;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ApiCaller {
    static final String ERROR = "ERROR";

    private static final String EBT_API = "https://api.eurobilltracker.com/";

    private WeakReference<ThisApp> mApp;

    @Inject
    public ApiCaller(ThisApp app) {
        mApp = new WeakReference<>(app);
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
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mApp.get());
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(mApp.get().getString(R.string.pref_login_values_ok_key), false).apply();

        List<Pair<String, String>> params = new ArrayList<>();
        params.add(new Pair<>("m", "login"));
        params.add(new Pair<>("v", "2"));
        params.add(new Pair<>("my_email", preferences.getString(mApp.get().getString(R.string.pref_settings_email_key), "").trim()));
        params.add(new Pair<>("my_password", preferences.getString(mApp.get().getString(R.string.pref_settings_password_key), "")));

        JSONObject jsonObject = doBasicCall(params);
        if (jsonObject == null)
            return getJsonObject(ERROR, mApp.get().getString(R.string.couldnt_connect));
        if (jsonObject.has(ERROR) || !jsonObject.has("sessionid"))
            return getJsonObject(ERROR, mApp.get().getString(R.string.wrong_password));

        editor.putBoolean(mApp.get().getString(R.string.pref_login_values_ok_key), true).apply();
        return jsonObject;
    }

    JSONObject callInsertBills(List<Pair<String, String>> params) {
        JSONObject jsonObject = doBasicCall(params);
        if (jsonObject == null)
            return getJsonObject(ERROR, mApp.get().getString(R.string.couldnt_connect));
        if (!jsonObject.has("note0"))
            return getJsonObject(ERROR, mApp.get().getString(R.string.server_error));

        JSONObject note0 = getJsonObject(jsonObject.optString("note0"));
        if (note0 == null)
            return getJsonObject(ERROR, mApp.get().getString(R.string.error_interpreting));
        if (!note0.has("status"))
            return getJsonObject(ERROR, mApp.get().getString(R.string.server_error));

        //Log.d(LOG_TAG, "note0 status: " + note0.get("status"));
        return note0;
    }

    JSONObject callMyComments(List<Pair<String, String>> params) {
        JSONObject jsonObject = doBasicCall(params);
        if (jsonObject == null)
            return getJsonObject(ERROR, mApp.get().getString(R.string.couldnt_connect));
        if (!jsonObject.has("rows") || !jsonObject.has("data"))
            return getJsonObject(ERROR, mApp.get().getString(R.string.server_error));
        return jsonObject;
    }

    private JSONObject getJsonObject(String key, String value) {
        return getJsonObject("{\"" + key + "\":\"" + value + "\"}");
    }

    private JSONObject getJsonObject(String s) {
        JSONObject jsonObject = null;
        try {
            jsonObject = new JSONObject(s);
        } catch (JSONException ignored) {
        }
        return jsonObject;
    }
}
