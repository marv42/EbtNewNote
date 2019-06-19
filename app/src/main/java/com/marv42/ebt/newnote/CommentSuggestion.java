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
import android.os.AsyncTask;

import androidx.core.util.Pair;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;

import static com.marv42.ebt.newnote.ApiCaller.ERROR;

public class CommentSuggestion extends AsyncTask<LocationValues, Void, String[]> {
    interface Callback {
        void onSuggestions(String[] suggestions);
    }

    private static final int MAX_NUMBER_SUGGESTIONS = 50;

    private Callback mCallback;
    private ApiCaller mApiCaller;
    private SharedPreferences mSharedPreferences;
    private String mPreferenceCommentKey;

    // TODO @Inject
    CommentSuggestion(Callback callback, ApiCaller apiCaller, SharedPreferences sharedPreferences,
                      String preferenceCommentKey) {
        mCallback = callback;
        mApiCaller = apiCaller;
        mSharedPreferences = sharedPreferences;
        mPreferenceCommentKey = preferenceCommentKey;
    }

    @Override
    protected String[] doInBackground(LocationValues... params) {
        return getSuggestion(params[0]);
    }

    @Override
    protected void onPostExecute(String[] s) {
        if (s == null || s.length < 1 || s[0].equals(ERROR))
            return;
        mCallback.onSuggestions(s);
    }

    private String[] getSuggestion(LocationValues l) {
        JSONObject json = mApiCaller.callLogin();
        if (json.has(ERROR))
            return new String[]{ ERROR, json.optString(ERROR) };

        List<Pair<String, String>> params = new ArrayList<>();
        params.add(new Pair<>("m", "mycomments"));
        params.add(new Pair<>("v", "1"));
        params.add(new Pair<>("PHPSESSID", json.optString("sessionid")));
        params.add(new Pair<>("city", l.mCity));
        params.add(new Pair<>("country", l.mCountry));
        params.add(new Pair<>("zip", l.mPostalCode));
        json = mApiCaller.callMyComments(params);
        if (json.has(ERROR))
            return new String[]{ ERROR, json.optString(ERROR) };

        JSONArray allComments = json.optJSONArray("data");
        List<JSONObject> list = new ArrayList<>();
        for (int i = 0; i < allComments.length(); ++i)
            list.add(allComments.optJSONObject(i));

        Collections.sort(list, new Comparator<JSONObject>() {
            public int compare(JSONObject j1, JSONObject j2) {
                return j2.optInt("amount") - j1.optInt("amount");
            }
        });

        String additionalComment = mSharedPreferences.getString(mPreferenceCommentKey, "")
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
        for (int i = 0; i < numSuggestions; ++i) {
            s[i] = uniqueList.get(i);
        }
        return s;
    }
}
