/*
 Copyright (c) 2010 - 2020 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote;

import android.text.TextUtils;

import androidx.lifecycle.ViewModelProvider;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;

import javax.inject.Inject;

import static androidx.preference.PreferenceManager.getDefaultSharedPreferences;
import static com.google.gson.JsonParser.parseString;

public class AllResults {

    private final ThisApp app;
    private final ViewModelProvider viewModelProvider;
    private ArrayList<SubmissionResult> results = new ArrayList<>();

    @Inject
    public AllResults(ThisApp app, EncryptedPreferenceDataStore dataStore,
                      ViewModelProvider viewModelProvider) {
        this.app = app;
        this.viewModelProvider= viewModelProvider;
        setResults(dataStore);
        setResultsToViewModel();
    }

    protected void setResults(EncryptedPreferenceDataStore dataStore) {
        String resultsFromPreferences = loadFromPreferences();
        if (resultsFromPreferences == null || TextUtils.isEmpty(resultsFromPreferences))
            return;
        JsonArray array = parseString(resultsFromPreferences).getAsJsonArray();
        results = new Gson().fromJson(array, new TypeToken<ArrayList<SubmissionResult>>() {
        }.getType());
        results.sort(new SubmissionResult.SubmissionComparator());
        setSubListWithMaxNum(dataStore);
    }

    private String loadFromPreferences() {
        return getDefaultSharedPreferences(app).getString(app.getString(R.string.pref_results_key), "");
    }

    private void setSubListWithMaxNum(EncryptedPreferenceDataStore dataStore) {
        String defValue = app.getResources().getString(R.string.max_show_num);
        int maxShowNum = Integer.parseInt(dataStore.get(R.string.pref_settings_submitted_key, defValue));
        int howMany = Math.min(maxShowNum, results.size());
        int startIndex = results.size() < maxShowNum ? 0 : results.size() - maxShowNum;
        results = new ArrayList<>(results.subList(startIndex, startIndex + howMany));
    }

    private void setResultsToViewModel() {
        ResultsViewModel viewModel = viewModelProvider.get(ResultsViewModel.class);
        viewModel.setResults(results);
    }

    void addResult(final SubmissionResult aResult) {
        results.add(aResult);
        setResultsToViewModel();
        if (aResult.isSuccessful(app))
            saveToPreferences();
    }

    private void saveToPreferences() {
        getDefaultSharedPreferences(app).edit().putString(app.getString(R.string.pref_results_key),
                new Gson().toJson(results)).apply();
    }

    ArrayList<SubmissionResult> getResults() {
        return results;
    }
}
