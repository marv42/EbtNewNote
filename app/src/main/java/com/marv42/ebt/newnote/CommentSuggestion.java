/*
 Copyright (c) 2010 - 2020 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import androidx.core.util.Pair;

import com.marv42.ebt.newnote.data.LocationValues;
import com.marv42.ebt.newnote.data.LoginInfo;
import com.marv42.ebt.newnote.exceptions.CallResponseException;
import com.marv42.ebt.newnote.exceptions.HttpCallException;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

import static android.widget.Toast.LENGTH_LONG;
import static com.marv42.ebt.newnote.exceptions.ErrorMessage.ERROR;

class CommentSuggestion extends AsyncTask<LocationValues, Void, String[]> {

    private static final String DATA_ELEMENT = "data";
    private static final String AMOUNT_ELEMENT = "amount";
    private static final String COMMENT_ELEMENT = "comment";
    private ApiCaller apiCaller;
    private Callback callback;
    private String additionalComment;

    CommentSuggestion(ApiCaller apiCaller, Callback callback, EncryptedPreferenceDataStore dataStore) {
        this.apiCaller = apiCaller;
        this.callback = callback;
        additionalComment = dataStore.get(R.string.pref_settings_comment_key, "")
                .replace("\u00a0", " ");
    }

    @Override
    protected String[] doInBackground(LocationValues... locationValues) {
        try {
            return getSuggestions(locationValues);
        } catch (CallResponseException e) {
            return getErrorStrings(e.getMessage());
        }
    }

    @NotNull
    private String[] getSuggestions(LocationValues[] locationValues) throws CallResponseException {
        JSONObject json = getJson(locationValues);
        JSONArray allSuggestions = json.optJSONArray(DATA_ELEMENT);
        List<String> uniques = getUniquesWrtAdditionalComments(allSuggestions);
        uniques = new ArrayList<>(new LinkedHashSet<>(uniques));
        return uniques.toArray(new String[0]);
    }

    private JSONObject getJson(LocationValues[] locationValues) throws CallResponseException {
        try {
            LoginInfo loginInfo = apiCaller.callLogin();
            List<Pair<String, String>> params = getLocationParams(loginInfo.sessionId, locationValues);
            String body = apiCaller.callMyComments(params);
            JSONObject json = new JSONObject(body);
            if (!json.has(DATA_ELEMENT))
                throw new CallResponseException("R.string.server_error: no '" + DATA_ELEMENT + "' element");
            return json;
        } catch (HttpCallException | CallResponseException | JSONException e) {
            throw new CallResponseException(e.getMessage());
        }
    }

    @NotNull
    private List<String> getUniquesWrtAdditionalComments(JSONArray allSuggestions) {
        List<JSONObject> suggestions = getJsonList(allSuggestions);
        Collections.sort(suggestions, (j1, j2) -> j2.optInt(AMOUNT_ELEMENT) - j1.optInt(AMOUNT_ELEMENT));
        List<String> uniques = new ArrayList<>();
        for (int i = 0; i < suggestions.size(); ++i) {
            String value = suggestions.get(i).optString(COMMENT_ELEMENT).replace("\u00a0", " ");
            if (value.endsWith(additionalComment))
                value = value.substring(0, value.length() - additionalComment.length());
            if (value.length() > 0 && !uniques.contains(value))
                uniques.add(value);
        }
        return uniques;
    }

    @NotNull
    private List<JSONObject> getJsonList(JSONArray allComments) {
        List<JSONObject> list = new ArrayList<>();
        for (int i = 0; allComments != null && i < allComments.length(); ++i)
            list.add(allComments.optJSONObject(i));
        return list;
    }

    @NotNull
    private String[] getErrorStrings(String s) {
        return new String[]{ERROR, s};
    }

    @NotNull
    private List<Pair<String, String>> getLocationParams(String sessionId, LocationValues[] locationValues) {
        LocationValues lv = locationValues[0];
        List<Pair<String, String>> params = new ArrayList<>();
        params.add(new Pair<>("m", "mycomments"));
        params.add(new Pair<>("v", "1"));
        params.add(new Pair<>("PHPSESSID", sessionId));
        params.add(new Pair<>("country", lv.country));
        params.add(new Pair<>("city", lv.city));
        params.add(new Pair<>("zip", lv.postalCode));
        return params;
    }

    @Override
    protected void onPostExecute(String[] s) {
        Context context = (Context) callback;
        if (s == null || s.length < 1) {
            Toast.makeText(context, context.getString(R.string.no_comment_suggestions), LENGTH_LONG).show();
            return;
        }
        if (s[0].equals(ERROR)) {
            Toast.makeText(context, context.getString(R.string.comment_suggestions_error), LENGTH_LONG).show();
            return;
        }
        Toast.makeText(context, context.getString(R.string.comment_suggestions_set), LENGTH_LONG).show();
        callback.onSuggestions(s);
    }

    interface Callback {
        void onSuggestions(String[] suggestions);
    }
}
