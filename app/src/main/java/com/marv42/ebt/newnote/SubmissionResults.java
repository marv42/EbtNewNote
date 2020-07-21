package com.marv42.ebt.newnote;

import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;

import java.util.ArrayList;
import java.util.Collections;

import javax.inject.Inject;

import static androidx.preference.PreferenceManager.getDefaultSharedPreferences;
import static com.google.gson.JsonParser.parseString;

public class SubmissionResults {
    private ThisApp mApp;
    private ArrayList<SubmissionResult> mResults = new ArrayList<>();

    @Inject
    public SubmissionResults(ThisApp app, EncryptedPreferenceDataStore dataStore) {
        mApp = app;
        String results = getDefaultSharedPreferences(app).getString(app.getString(R.string.pref_results), "");
        if (results == null || TextUtils.isEmpty(results))
            return;
        JsonArray array = parseString(results).getAsJsonArray();
        for (int i = 0; i < array.size(); ++i)
            mResults.add(i, new Gson().fromJson(array.get(i), SubmissionResult.class));
        Collections.sort(mResults, new SubmissionResult.SubmissionComparator());
        String defValue = app.getResources().getString(R.string.max_show_num);
        int maxShowNum = Integer.parseInt(
                dataStore.get(R.string.pref_settings_submitted_key, defValue));
        int howMany = Math.min(maxShowNum, mResults.size());
        int startIndex = mResults.size() < maxShowNum ? 0 : mResults.size() - maxShowNum;
        mResults = new ArrayList<>(mResults.subList(startIndex, startIndex + howMany));
    }

    class ResultSummary {
        final int mHits;
        final int mSuccessful;
        final int mFailed;

        ResultSummary(final int hits, final int successful, final int failed) {
            mHits = hits;
            mSuccessful = successful;
            mFailed = failed;
        }
    }

    void addResult(final SubmissionResult result) {
        mResults.add(result);
        if (result.isSuccessful(mApp))
            getDefaultSharedPreferences(mApp).edit().putString(mApp.getString(R.string.pref_results),
                    new Gson().toJson(mResults)).apply();
    }

    ArrayList<SubmissionResult> getResults() {
        return mResults;
    }

    ResultSummary getSummary() {
        int numberOfHits = 0;
        int numberOfSuccessfull = 0;
        int numberOfFailed = 0;
        for (SubmissionResult result : mResults) {
            if (result.isSuccessful(mApp))
                numberOfSuccessfull++;
            else
                numberOfFailed++;
            if (result.isAHit(mApp))
                numberOfHits++;
        }
        return new ResultSummary(numberOfHits, numberOfSuccessfull, numberOfFailed);
    }
}
