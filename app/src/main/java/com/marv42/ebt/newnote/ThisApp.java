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

import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.marv42.ebt.newnote.di.DaggerApplicationComponent;

import java.util.ArrayList;

import dagger.android.AndroidInjector;
import dagger.android.support.DaggerApplication;

import static android.preference.PreferenceManager.getDefaultSharedPreferences;

//@AcraCore(buildConfigClass = BuildConfig.class)
//@AcraMailSender(mailTo = "marv42+acra@gmail.com")
//@AcraToast(resText = R.string.crash_toast_text)
public class ThisApp extends DaggerApplication {
    private static final int MAX_SAVE_NUM = 10;

    private ArrayList<SubmissionResult> mResults = new ArrayList<>();

    @Override
    protected AndroidInjector<? extends DaggerApplication> applicationInjector() {
        return DaggerApplicationComponent.builder().create(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        String results = getDefaultSharedPreferences(this).getString(
                getString(R.string.pref_results), "");
        if (!TextUtils.isEmpty(results)) {
            JsonArray array = new JsonParser().parse(results).getAsJsonArray();
            for (int i = 0; i < array.size() && i < MAX_SAVE_NUM; ++i)
                mResults.add(i, new Gson().fromJson(array.get(i), SubmissionResult.class));
        }
    }
//   @Override
//   protected void attachBaseContext(Context base) {
//       super.attachBaseContext(base);
////       CoreConfigurationBuilder builder = new CoreConfigurationBuilder(this);
////       builder.setBuildConfigClass(BuildConfig.class).setReportFormat(StringFormat.JSON);
////       builder.getPluginConfigurationBuilder(ToastConfigurationBuilder.class).setResText(R.string.crash_toast_text);
//       ACRA.init(this); // , builder);
//   }

    // TODO shouldn't this be somwhere else?
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

    public boolean addResult(final SubmissionResult result) {
        boolean b = mResults.add(result);
        getDefaultSharedPreferences(this).edit()
                .putString(getString(R.string.pref_results), new Gson().toJson(mResults)).apply();
        return b;
    }

    public ArrayList<SubmissionResult> getResults() {
        return mResults;
    }

    public int getNumberOfResults() {
        return mResults.size();
    }

    public ResultSummary getSummary() {
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
