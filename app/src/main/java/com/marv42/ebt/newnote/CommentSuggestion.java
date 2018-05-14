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
import android.os.AsyncTask;
import android.support.v4.util.Pair;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;

import static com.marv42.ebt.newnote.EbtNewNote.LOG_TAG;

public class CommentSuggestion extends AsyncTask<LocationValues, Void, String[]> {
    interface Callback {
        void onSuggestions(String[] suggestions);
    }

    private static final int MAX_NUMBER_SUGGESTIONS = 50;

    private Callback mCallback;
    private WeakReference<Context> mContext;
    private ApiCaller mApiCaller;
    private SharedPreferences mSharedPreferences;

    //@Inject
    CommentSuggestion(Callback callback, Context context, ApiCaller apiCaller, SharedPreferences sharedPreferences) {
        mCallback = callback;
        mContext = new WeakReference<>(context);
        mApiCaller = apiCaller;
        mSharedPreferences = sharedPreferences;
    }

    @Override
    protected String[] doInBackground(LocationValues... params) {
        return getSuggestion(params[0]);
    }

    @Override
    protected void onPostExecute(String[] s) {
        if (s != null && s.length > 0)
            mCallback.onSuggestions(s);
        if (! mSharedPreferences.edit()
                .putBoolean(mContext.get().getString(R.string.pref_calling_my_comments_key), false)
                .commit())
            Log.e(LOG_TAG, "editor's commit failed");
    }

    private String[] getSuggestion(LocationValues l) {
        if (! mApiCaller.callLogin()) {
            mApiCaller.getError();
            return null;
        }
        List<Pair<String, String>> params = new ArrayList<>();
        params.add(new Pair("m", "mycomments"));
        params.add(new Pair("v", "1"));
        params.add(new Pair("PHPSESSID", mApiCaller.getResult().optString("sessionid")));
        params.add(new Pair("city",    l.getCity()      ));
        params.add(new Pair("country", l.getCountry()   ));
        params.add(new Pair("zip",     l.getPostalCode()));
        if (! mApiCaller.callMyComments(params)) {
            mApiCaller.getError();
            return null;
        }

        JSONArray allComments = mApiCaller.getResult().optJSONArray("data");
        List<JSONObject> list = new ArrayList<>();
        for (int i = 0; i < allComments.length(); ++i)
            list.add(allComments.optJSONObject(i));

        Collections.sort(list, new Comparator<JSONObject>() {
            public int compare(JSONObject j1, JSONObject j2) {
                return j2.optInt("amount") - j1.optInt("amount");
            }
        });

        String additionalComment = mSharedPreferences.getString(mContext.get().getString(R.string.pref_settings_comment_key), "").replace("\u00a0", " ");

        // unique wrt additionalComment
        List<String> uniqueList = new ArrayList<>();
        for (int i = 0; i < list.size(); ++i) {
            String value = list.get(i).optString("comment").replace("\u00a0", " ");
            //Log.d(LOG_TAG, value + " (" + list.get(i).optString("amount") + ")");
            if (value.endsWith(additionalComment))
                value = value.substring(0, value.length() - additionalComment.length());
            if (value.length() > 0 && ! uniqueList.contains(value))
                uniqueList.add(value);
        }
        uniqueList = new ArrayList<>(new LinkedHashSet<>(uniqueList));

        int numSuggestions = Math.min(uniqueList.size(), MAX_NUMBER_SUGGESTIONS);
        Log.d(LOG_TAG, numSuggestions + " suggestion(s)");

        String[] s = new String[numSuggestions];
        for (int i = 0; i < numSuggestions; ++i) {
            s[i] = uniqueList.get(i);
            Log.d(LOG_TAG, i + 1 + ".: " + s[i].replace(" ", "_"));
        }
        return s;
    }
}
