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

import com.marv42.ebt.newnote.di.DaggerApplicationComponent;

import java.util.ArrayList;

import dagger.android.AndroidInjector;
import dagger.android.support.DaggerApplication;

//@AcraCore(buildConfigClass = BuildConfig.class)
//@AcraMailSender(mailTo = "marv42+acra@gmail.com")
//@AcraToast(resText = R.string.crash_toast_text)
public class ThisApp extends DaggerApplication {
   private ArrayList<SubmissionResult> mResults = new ArrayList<>();

   @Override
   protected AndroidInjector<? extends DaggerApplication> applicationInjector() {
      return DaggerApplicationComponent.builder().create(this);
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
      private final int mHits;
      private final int mSuccessful;
      private final int mFailed;

      ResultSummary(final int hits, final int successful, final int failed) {
         mHits       = hits;
         mSuccessful = successful;
         mFailed     = failed;
      }

      int getSuccessful() {
         return mSuccessful;
      }

      int getFailed() {
         return mFailed;
      }

      int getHits() {
         return mHits;
      }
   }

   public boolean addResult(final SubmissionResult result) {
      return mResults.add(result);
   }

   public ArrayList<SubmissionResult> getResults() {
      return mResults;
   }

   public int getNumberOfResults() {
      return mResults.size();
   }

   public ResultSummary getSummary() {
      int numberOfHits        = 0;
      int numberOfSuccessfull = 0;
      int numberOfFailed      = 0;

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
