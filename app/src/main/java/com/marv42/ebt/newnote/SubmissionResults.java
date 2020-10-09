/*
 Copyright (c) 2010 - 2020 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote;

import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.reflect.TypeToken;
import com.marv42.ebt.newnote.data.ResultSummary;

import java.util.ArrayList;
import java.util.Collections;

import javax.inject.Inject;

import static androidx.preference.PreferenceManager.getDefaultSharedPreferences;
import static com.google.gson.JsonParser.parseString;

public class SubmissionResults {
    private ThisApp app;
    private ArrayList<SubmissionResult> results = new ArrayList<>();

    @Inject
    public SubmissionResults(ThisApp app, EncryptedPreferenceDataStore dataStore) {
        this.app = app;
        setResults(dataStore);
    }

    private void setResults(EncryptedPreferenceDataStore dataStore) {
        String resultsFromPreferences = getDefaultSharedPreferences(app).getString(app.getString(R.string.pref_results), "");
        if (resultsFromPreferences == null || TextUtils.isEmpty(resultsFromPreferences))
            return;
        JsonArray array = parseString(resultsFromPreferences).getAsJsonArray();
        results = new Gson().fromJson(array, new TypeToken<ArrayList<SubmissionResult>>() {
        }.getType());
        Collections.sort(results, new SubmissionResult.SubmissionComparator());
        setSubListWithMaxNum(dataStore);
    }

    private void setSubListWithMaxNum(EncryptedPreferenceDataStore dataStore) {
        String defValue = app.getResources().getString(R.string.max_show_num);
        int maxShowNum = Integer.parseInt(dataStore.get(R.string.pref_settings_submitted_key, defValue));
        int howMany = Math.min(maxShowNum, results.size());
        int startIndex = results.size() < maxShowNum ? 0 : results.size() - maxShowNum;
        results = new ArrayList<>(results.subList(startIndex, startIndex + howMany));
    }

    void addResult(final SubmissionResult aResult) {
        results.add(aResult);
        if (aResult.isSuccessful(app))
            getDefaultSharedPreferences(app).edit().putString(app.getString(R.string.pref_results),
                    new Gson().toJson(results)).apply();
    }

    ArrayList<SubmissionResult> getResults() {
        return results;
    }

    ResultSummary getSummary() {
        int numberOfHits = 0;
        int numberOfSuccessfull = 0;
        int numberOfFailed = 0;
        for (SubmissionResult result : results) {
            if (result.isSuccessful(app))
                numberOfSuccessfull++;
            else
                numberOfFailed++;
            if (result.isAHit(app))
                numberOfHits++;
        }
        return new ResultSummary(numberOfHits, numberOfSuccessfull, numberOfFailed);
    }
}
