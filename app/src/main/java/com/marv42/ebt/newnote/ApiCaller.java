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

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v4.util.Pair;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

import javax.inject.Inject;

import static com.marv42.ebt.newnote.EbtNewNote.LOG_TAG;

public class ApiCaller {
    private static final String EBT_API = "https://api.eurobilltracker.com/";

    private final Semaphore mMayCall = new Semaphore(1, true);
    private WeakReference<Context> mContext;
    private SharedPreferences mSharedPreferences;

    private JSONObject mResult = null;
    private String mError = "";

    @Inject
    public ApiCaller(Context context, SharedPreferences sharedPreferences) {
        mContext = new WeakReference<>(context);
        mSharedPreferences = sharedPreferences;
        mError = mContext.get().getString(R.string.error);
    }

    String getError() {
        Log.d(LOG_TAG, "release, ApiCaller.getError: " + mError);
        mMayCall.release();
        return mError;
    }

    JSONObject getResult() {
        Log.d(LOG_TAG, "release, ApiCaller.getResult: " + mResult);
        mMayCall.release();
        return mResult;
    }

    private JSONObject doBasicCall(List<Pair<String, String>> params) {
        String response = executeRequest(params);
        JSONObject jsonObject = getJsonObject(response);
        if (jsonObject == null)
            mError = mContext.get().getString(R.string.error_interpreting);
        else
            Log.d(LOG_TAG, "response: " + jsonObject.toString());
        return jsonObject;
    }

    private void acquire() {
        Log.d(LOG_TAG, "acquire");
        try {
            mMayCall.acquire();
        } catch (InterruptedException e) {
            Log.d(LOG_TAG, "acquire interrupted: " + e);
        }
    }

    boolean callLogin() {
        acquire();
        Log.d(LOG_TAG, "ApiCaller.callLogin");
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        if (! editor.putBoolean(mContext.get().getString(R.string.pref_login_values_ok_key), false)
                .commit())
            Log.e(LOG_TAG, "editor's commit failed");

        List<Pair<String, String>> params = new ArrayList<>();
        params.add(new Pair("m", "login"));
        params.add(new Pair("v", "2"));
        params.add(new Pair("my_email", mSharedPreferences.getString(mContext.get().getString(R.string.pref_settings_email_key), "").trim()));
        params.add(new Pair("my_password", mSharedPreferences.getString(mContext.get().getString(R.string.pref_settings_password_key), "")));

        JSONObject jsonObject = doBasicCall(params);
        if (jsonObject == null)
            return false;

        if (!jsonObject.has("sessionid")) {
            mError = mContext.get().getString(R.string.wrong_password);
            return false;
        }
        Log.d(LOG_TAG, "login successful, sessionId: " + jsonObject.optString("sessionid"));

        mResult = jsonObject;
        if (! editor.putBoolean(mContext.get().getString(R.string.pref_login_values_ok_key), true)
                .commit())
            Log.e(LOG_TAG, "editor's commit failed");
        return true;
    }

    boolean callInsertBills(List<Pair<String, String>> params) {
        acquire();
        Log.d(LOG_TAG, "ApiCaller.callInsertBill");

        JSONObject jsonObject = doBasicCall(params);
        if (jsonObject == null)
            return false;

        if (!jsonObject.has("note0")) {
            mError = mContext.get().getString(R.string.server_error);
            return false;
        }

        JSONObject note0 = null;
        try {
            note0 = new JSONObject(jsonObject.optString("note0"));
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Error getting note0: " + e);
        }

        if (note0 == null) {
            mError = mContext.get().getString(R.string.error_interpreting);
            return false;
        }

        if (!note0.has("status")) {
            mError = mContext.get().getString(R.string.server_error);
            return false;
        }

        mResult = note0;
        //Log.d(LOG_TAG, "note0 status: " + note0.get("status"));
        return true;
    }

    boolean callMyComments(List<Pair<String, String>> params) {
        acquire();
        Log.d(LOG_TAG, "ApiCaller.callMyComments");
        JSONObject jsonObject = doBasicCall(params);
        if (jsonObject == null)
            return false;
        if (!jsonObject.has("rows") || !jsonObject.has("data"))
            return false;
        mResult = jsonObject;
        return true;
    }

    private String getQuery(List<Pair<String, String>> params) {
        StringBuilder result = new StringBuilder();
        boolean first = true;

        for (Pair<String, String> pair : params) {
            if (first)
                first = false;
            else
                result.append("&");
            try {
                result.append(URLEncoder.encode(pair.first, "UTF-8"));
                result.append("=");
                result.append(URLEncoder.encode(pair.second, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                // utf-8 is supported
            }
        }

        return result.toString();
    }

    private static String executePost(String url, String urlParameters) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("Content-Length", "" + Integer.toString(urlParameters.getBytes().length));
            connection.setRequestProperty("Content-Language", "en-US");
            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setDoOutput(true);

            DataOutputStream dataOutputStream = new DataOutputStream(connection.getOutputStream());
            dataOutputStream.writeBytes(urlParameters);
            dataOutputStream.flush();
            dataOutputStream.close();

            InputStream inputStream = connection.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            StringBuilder response = new StringBuilder();
            while ((line = bufferedReader.readLine()) != null) {
                response.append(line);
                response.append('\r');
            }
            bufferedReader.close();
            return response.toString();
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error executing POST request: " + e);
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String executeRequest(List<Pair<String, String>> params) {
        return executePost(EBT_API, getQuery(params));
    }

    private JSONObject getJsonObject(String response) {
        JSONObject jsonObject = null;
        try {
            jsonObject = new JSONObject(response);
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Error parsing response into JSONObject: " + e);
        }
        return jsonObject;
    }
}
