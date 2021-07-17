/*
 Copyright (c) 2010 - 2021 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote;

import android.content.SharedPreferences;
import android.text.TextUtils;

import androidx.lifecycle.ViewModelProvider;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;

import javax.inject.Inject;

import static androidx.preference.PreferenceManager.getDefaultSharedPreferences;
import static com.google.gson.JsonParser.parseString;

public class AllResults implements SharedPreferences.OnSharedPreferenceChangeListener {

    static final int MAX_LOAD_NUM = 9999;
    private final ThisApp app;
    private final ViewModelProvider viewModelProvider;
    private final EncryptedPreferenceDataStore dataStore;
    private ArrayList<SubmissionResult> results = new ArrayList<>();

    @Inject
    public AllResults(ThisApp app, EncryptedPreferenceDataStore dataStore, ViewModelProvider viewModelProvider) {
        this.app = app;
        this.viewModelProvider = viewModelProvider;
        this.dataStore = dataStore;
        initResults();
        setResultsToViewModel();
        registerOnSharedPreferenceListener();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (sharedPreferences == dataStore.getSharedPreferences())
            if (key.equals(app.getString(R.string.pref_settings_show_submitted_key)))
                setResultsToViewModel();
    }

    void addResult(final SubmissionResult aResult) {
        results.add(aResult);
        setResultsToViewModel();
        if (aResult.isSuccessful(app))
            saveToPreferences();
    }

    private void initResults() {
        String resultsFromPreferences = loadFromPreferences();
        if (resultsFromPreferences == null || TextUtils.isEmpty(resultsFromPreferences))
            return;
        JsonArray array = parseString(resultsFromPreferences).getAsJsonArray();
        results = new Gson().fromJson(array, new TypeToken<ArrayList<SubmissionResult>>() {}.getType());
        sortAndFilterResults();
    }

    private String loadFromPreferences() {
        return getDefaultSharedPreferences(app).getString(app.getString(R.string.pref_results_key), "");
    }

    private void sortAndFilterResults() {
        if (results == null || results.isEmpty())
            return;
        results.sort(new SubmissionResult.SubmissionComparator());
        results = getMaxNumResults(results, MAX_LOAD_NUM);
    }

    private static ArrayList<SubmissionResult> getMaxNumResults(ArrayList<SubmissionResult> results, int maxNum) {
        int howMany = Math.min(maxNum, results.size());
        int startIndex = results.size() < maxNum ? 0 : results.size() - maxNum;
        return new ArrayList<>(results.subList(startIndex, startIndex + howMany));
    }

    private void registerOnSharedPreferenceListener() {
        SharedPreferences sharedPreferences = dataStore.getSharedPreferences();
        if (sharedPreferences != null)
            sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    private void setResultsToViewModel() {
        int maxShowNum = getMaxShowNum();
        ArrayList<SubmissionResult> resultsToShow = getMaxNumResults(results, maxShowNum);
        ResultsViewModel viewModel = viewModelProvider.get(ResultsViewModel.class);
        viewModel.setResults(resultsToShow);
    }

    private int getMaxShowNum() {
        String defaultValue = app.getResources().getString(R.string.max_show_num);
        return Integer.parseInt(dataStore.get(R.string.pref_settings_show_submitted_key, defaultValue));
    }

    private void saveToPreferences() {
        getDefaultSharedPreferences(app).edit().putString(app.getString(R.string.pref_results_key),
                new Gson().toJson(results)).apply();
    }

    ArrayList<SubmissionResult> getResults() {
        return results;
    }
}
