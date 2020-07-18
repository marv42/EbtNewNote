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
import android.os.AsyncTask;
import android.widget.Toast;

import androidx.core.util.Pair;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

import static android.widget.Toast.LENGTH_LONG;
import static com.marv42.ebt.newnote.ApiCaller.ERROR;

public class CommentSuggestion extends AsyncTask<LocationValues, Void, String[]> {
    interface Callback {
        void onSuggestions(String[] suggestions);
    }

    private static final int MAX_NUMBER_SUGGESTIONS = 50;

    private Callback mCallback;
    private ApiCaller mApiCaller;
    private SharedPreferencesHandler mSharedPreferencesHandler;

    CommentSuggestion(ApiCaller apiCaller, SharedPreferencesHandler sharedPreferencesHandler, Callback callback) {
        mApiCaller = apiCaller;
        mSharedPreferencesHandler = sharedPreferencesHandler;
        mCallback = callback;
    }

    @Override
    protected String[] doInBackground(LocationValues... locationValues) {
        JSONObject json = mApiCaller.callLogin();
        if (json.has(ERROR))
            return new String[]{ ERROR, json.optString(ERROR) };
        LocationValues lv = locationValues[0];
        List<Pair<String, String>> params = new ArrayList<>();
        params.add(new Pair<>("m", "mycomments"));
        params.add(new Pair<>("v", "1"));
        params.add(new Pair<>("PHPSESSID", json.optString("sessionid")));
        params.add(new Pair<>("city", lv.mCity));
        params.add(new Pair<>("country", lv.mCountry));
        params.add(new Pair<>("zip", lv.mPostalCode));
        json = mApiCaller.callMyComments(params);
        if (json.has(ERROR))
            return new String[]{ ERROR, json.optString(ERROR) };
        JSONArray allComments = json.optJSONArray("data");
        List<JSONObject> list = new ArrayList<>();
        for (int i = 0; allComments != null && i < allComments.length(); ++i)
            list.add(allComments.optJSONObject(i));
        Collections.sort(list, (j1, j2) -> j2.optInt("amount") - j1.optInt("amount"));
        String additionalComment = mSharedPreferencesHandler.get(R.string.pref_settings_comment_key, "")
                .replace("\u00a0", " ");
        // unique wrt additionalComment
        List<String> uniqueList = new ArrayList<>();
        for (int i = 0; i < list.size(); ++i) {
            String value = list.get(i).optString("comment").replace("\u00a0", " ");
            if (value.endsWith(additionalComment))
                value = value.substring(0, value.length() - additionalComment.length());
            if (value.length() > 0 && ! uniqueList.contains(value))
                uniqueList.add(value);
        }
        uniqueList = new ArrayList<>(new LinkedHashSet<>(uniqueList));
        int numSuggestions = Math.min(uniqueList.size(), MAX_NUMBER_SUGGESTIONS);
        String[] s = new String[numSuggestions];
        for (int i = 0; i < numSuggestions; ++i)
            s[i] = uniqueList.get(i);
        return s;
    }

    @Override
    protected void onPostExecute(String[] s) {
        Context context = (Context) mCallback;
        if (s == null || s.length < 1 || s[0].equals(ERROR)) {
            Toast.makeText(context, context.getString(R.string.no_comment_suggestions), LENGTH_LONG).show();
            return;
        }
        Toast.makeText(context, context.getString(R.string.comment_suggestions_set), LENGTH_LONG).show();
        mCallback.onSuggestions(s);
    }
}
