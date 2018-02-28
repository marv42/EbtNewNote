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
//@ReportsCrashes(//formKey      = "dDRCTmUtdWxjcnhQdWNpT3A0WEhZaHc6MQ",
//        mailTo = "marv42+acra@gmail.com",
//        mode         = ReportingInteractionMode.TOAST,
//        resToastText = R.string.crash_toast_text)
public class ThisApp extends DaggerApplication {
   private ArrayList<SubmissionResult> mResults = new ArrayList<>();
   private LocationValues mLocationValues = new LocationValues();

   @Override
   protected AndroidInjector<? extends DaggerApplication> applicationInjector() {
      return DaggerApplicationComponent.builder().create(this);
   }

//   @Override
//   protected void attachBaseContext(Context base) {
//      super.attachBaseContext(base);
//      ACRA.init(this);
//   }

   public class ResultSummary {
      private final int mHits;
      private final int mSuccessful;
      private final int mFailed;

      ResultSummary(final int hits, final int successful, final int failed) {
         mHits       = hits;
         mSuccessful = successful;
         mFailed     = failed;
      }

      int getSuccessful()
      {
         return mSuccessful;
      }

      public int getFailed()
      {
         return mFailed;
      }

      int getHits()
      {
         return mHits;
      }
   }

   public boolean addResult(final SubmissionResult result)
   {
      return mResults.add(result);
   }

   public ArrayList<SubmissionResult> getResults()
   {
      return mResults;
   }

   public int getNumberOfResults()
   {
      return mResults.size();
   }

   public ResultSummary getSummary() {
      int numberOfHits        = 0;
      int numberOfSuccessfull = 0;
      int numberOfFailed      = 0;

      for (SubmissionResult result : mResults) {
         if (result.wasSuccessful())
            numberOfSuccessfull++;
         else
            numberOfFailed++;
         if (result.wasHit())
            numberOfHits++;
      }
      return new ResultSummary(numberOfHits, numberOfSuccessfull, numberOfFailed);
   }

   public void setLocationValues(LocationValues lv)
   {
      mLocationValues = lv;
   }

   public LocationValues getLocationValues() {
      LocationValues lv = mLocationValues;
      mLocationValues = new LocationValues();
      return lv;
   }
}
