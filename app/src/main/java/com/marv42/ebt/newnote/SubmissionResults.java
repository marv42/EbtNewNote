package com.marv42.ebt.newnote;

import android.content.SharedPreferences;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.Collections;

import javax.inject.Inject;

import static android.preference.PreferenceManager.getDefaultSharedPreferences;

public class SubmissionResults {
    private static final int MAX_SHOW_NUM = 100;

    private ArrayList<SubmissionResult> mResults = new ArrayList<>();
    private SharedPreferences mSharedPreferences;
    private String mPrefResults;

    @Inject
    public SubmissionResults(ThisApp app) {
        mSharedPreferences = getDefaultSharedPreferences(app);
        mPrefResults = app.getString(R.string.pref_results);
        String results = mSharedPreferences.getString(app.getString(R.string.pref_results), "");
        if (! TextUtils.isEmpty(results)) {
            JsonArray array = new JsonParser().parse(results).getAsJsonArray();
            for (int i = 0; i < array.size(); ++i)
                mResults.add(i, new Gson().fromJson(array.get(i), SubmissionResult.class));
            Collections.sort(mResults, new SubmissionResult.SubmissionComparator());
            int howMany = Math.min(MAX_SHOW_NUM, mResults.size());
            int startIndex = mResults.size() < MAX_SHOW_NUM ? 0 : mResults.size() - MAX_SHOW_NUM;
            mResults = new ArrayList<>(mResults.subList(startIndex, startIndex + howMany));
        }
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
        if (result.mSuccessful) {
            mSharedPreferences.edit().putString(mPrefResults, new Gson().toJson(mResults)).apply();
        }
    }

    ArrayList<SubmissionResult> getResults() {
        return mResults;
    }

    ResultSummary getSummary() {
        int numberOfHits = 0;
        int numberOfSuccessfull = 0;
        int numberOfFailed = 0;

        for (SubmissionResult result : mResults) {
            if (result.mSuccessful)
                numberOfSuccessfull++;
            else
                numberOfFailed++;
            if (result.mHit)
                numberOfHits++;
        }
        return new ResultSummary(numberOfHits, numberOfSuccessfull, numberOfFailed);
    }
}
