/*
 Copyright (c) 2010 - 2024 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import com.marv42.ebt.newnote.preferences.EncryptedPreferenceDataStore;
import com.marv42.ebt.newnote.ui.ResultsViewModel;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import static androidx.preference.PreferenceManager.getDefaultSharedPreferences;
import static com.google.gson.JsonParser.parseString;
import static java.lang.Integer.max;

public class AllResults {
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
    }

    void addResult(final SubmissionResult aResult) {
        results.add(aResult);
        setResultsToViewModel();
        saveToPreferences();
    }

    void replaceResult(SubmissionResult sr, boolean removable) {
        int index = results.indexOf(sr);
        if (index == -1)
            return;
        sr.mRemovable = removable;
        results.set(index, sr);
        setResultsToViewModel();
        saveToPreferences();
    }

    private void initResults() {
        String resultsFromPreferences = loadFromPreferences();
        if (TextUtils.isEmpty(resultsFromPreferences))
            return;
        JsonArray array = parseString(resultsFromPreferences).getAsJsonArray();
        updateResults(array);
        results = new Gson().fromJson(array, new TypeToken<ArrayList<SubmissionResult>>() {
        }.getType());
    }

    private String loadFromPreferences() {
        final String key = app.getString(R.string.pref_results_key);
        return getDefaultSharedPreferences(app).getString(key, "");
    }

    private void updateResults(JsonArray array) {
        for (JsonElement element : array) {
            final String removable = "mRemovable";
            if (!element.getAsJsonObject().has(removable))
                element.getAsJsonObject().addProperty(removable, true);
        }
    }

    private void setResultsToViewModel() {
        ResultsViewModel viewModel = viewModelProvider.get(ResultsViewModel.class);
        viewModel.setResults(results);
    }

    private void saveToPreferences() {
        List<SubmissionResult> resultsToSave = getSuccessfulResults();
        resultsToSave.sort(new SubmissionResult.SubmissionComparator());
        resultsToSave = filterResults(resultsToSave);
        getDefaultSharedPreferences(app).edit().putString(app.getString(R.string.pref_results_key),
                new Gson().toJson(resultsToSave)).apply();
    }

    private List<SubmissionResult> filterResults(List<SubmissionResult> results) {
        if (results == null)
            return results;
        final int tooMany = results.size() - getMaxNum();
        if (tooMany > 0)
            deleteTooMany(tooMany, results);
        return results;
    }

    private int getMaxNum() {
        return max(getMaxNumFromPreferences(), getNumberOfResultsToKeep());
    }

    private int getMaxNumFromPreferences() {
        String defaultValue = app.getResources().getString(R.string.max_show_num_default);
        return Integer.parseInt(dataStore.get(R.string.pref_settings_show_submitted_key, defaultValue));
    }

    private void deleteTooMany(int tooMany, List<SubmissionResult> results) {
        Iterator<SubmissionResult> it = results.iterator();
        while (it.hasNext() && tooMany > 0) {
            final SubmissionResult next = it.next();
            if (!next.mRemovable)
                continue;
            results.remove(next);
            --tooMany;
        }
    }

    private @NonNull List<SubmissionResult> getHitResults() {
        return results.stream().filter(p -> p.isAHit(app)).collect(Collectors.toList());
    }

    private @NonNull List<SubmissionResult> getSuccessfulResults() {
        return results.stream().filter(p -> p.isSuccessful(app)).collect(Collectors.toList());
    }

    private @NonNull List<SubmissionResult> getFailedResults() {
        return results.stream().filter(p -> !p.isSuccessful(app)).collect(Collectors.toList());
    }

    private @NonNull List<SubmissionResult> getResultsToKeep() {
        return results.stream().filter(p -> !p.mRemovable).collect(Collectors.toList());
    }

    int getNumberOfHits() {
        final List<SubmissionResult> resultsWithHits = getHitResults();
        return resultsWithHits.size();
    }

    int getNumberOfSuccessful() {
        final List<SubmissionResult> successfulResults = getSuccessfulResults();
        return successfulResults.size();
    }

    int getNumberOfFailed() {
        final List<SubmissionResult> failedResults = getFailedResults();
        return failedResults.size();
    }

    private int getNumberOfResultsToKeep() {
        final List<SubmissionResult> resultsToKeep = getResultsToKeep();
        return resultsToKeep.size();
    }

    ArrayList<SubmissionResult> getResults() {
        return results;
    }
}
