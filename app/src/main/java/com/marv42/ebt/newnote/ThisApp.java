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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.widget.Toast;

import com.google.gson.JsonArray;
import com.marv42.ebt.newnote.di.DaggerApplicationComponent;
import com.marv42.ebt.newnote.location.FetchAddressIntentService;
import com.marv42.ebt.newnote.location.LocationProviderChangedReceiver;
import com.marv42.ebt.newnote.location.LocationTask;

import org.acra.ACRA;
import org.acra.annotation.AcraCore;
import org.acra.annotation.AcraMailSender;
import org.acra.annotation.AcraToast;

import dagger.android.AndroidInjector;
import dagger.android.support.DaggerApplication;

import static android.content.Intent.ACTION_PROVIDER_CHANGED;
import static android.location.LocationManager.PROVIDERS_CHANGED_ACTION;
import static android.widget.Toast.LENGTH_LONG;
import static androidx.preference.PreferenceManager.getDefaultSharedPreferences;
import static com.google.gson.JsonParser.parseString;
import static com.marv42.ebt.newnote.location.FetchAddressIntentService.LOCATION_DATA_EXTRA;
import static com.marv42.ebt.newnote.location.FetchAddressIntentService.RECEIVER;
import static com.marv42.ebt.newnote.location.FetchAddressIntentService.RESULT_DATA_KEY;
import static com.marv42.ebt.newnote.location.FetchAddressIntentService.SUCCESS_RESULT;

@AcraCore(buildConfigClass = BuildConfig.class)
@AcraMailSender(mailTo = "marv42+acra@gmail.com")
@AcraToast(resText = R.string.crash_text)
public class ThisApp extends DaggerApplication {
    BroadcastReceiver mBroadcastReceiver;

    private AddressResultReceiver mAddressResultReceiver;

    @Override
    protected AndroidInjector<? extends DaggerApplication> applicationInjector() {
        return DaggerApplicationComponent.factory().create(this);
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        ACRA.init(this);
    }

    void startLocationProviderChangedReceiver() {
        if (mBroadcastReceiver == null) {
            mBroadcastReceiver = new LocationProviderChangedReceiver();
            IntentFilter filter = new IntentFilter(PROVIDERS_CHANGED_ACTION);
            filter.addAction(ACTION_PROVIDER_CHANGED);
            registerReceiver(mBroadcastReceiver, filter);
        }
    }

    public void stopLocationProviderChangedReceiver() {
        unregisterReceiver(mBroadcastReceiver);
        mBroadcastReceiver = null;
    }

    public void startLocationTask() {
        Toast.makeText(this, getString(R.string.location_start), LENGTH_LONG).show();
        new LocationTask(this).execute();
    }

    public void onLocation(Location location) {
        Intent intent = new Intent(this, FetchAddressIntentService.class);
        intent.putExtra(RECEIVER, new AddressResultReceiver(new Handler()));
        intent.putExtra(LOCATION_DATA_EXTRA, location);
        startService(intent);
    }

    class AddressResultReceiver extends ResultReceiver {
        AddressResultReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            if (resultCode != SUCCESS_RESULT) {
                String text = getString(resultCode);
                if (resultData != null)
                    text += " (\"" + resultData.getString(RESULT_DATA_KEY) + "\")";
                Toast.makeText(ThisApp.this, text, LENGTH_LONG).show();
                return;
            }
            if (resultData == null)
                return; // dann hätte addresses -> Gson in FetchAddressIntentService nicht geklappt
            String addressOutput = resultData.getString(RESULT_DATA_KEY);
            if (addressOutput == null) {
                return; // ?
            }
            JsonArray array = parseString(addressOutput).getAsJsonArray();
            JsonArray firstElement = array.get(0).getAsJsonArray(); // TODO Let the user decide on multiple results
            getDefaultSharedPreferences(ThisApp.this).edit()
                    .putString(ThisApp.this.getString(R.string.pref_country_key), firstElement.get(0).getAsString())
                    .putString(ThisApp.this.getString(R.string.pref_city_key), firstElement.get(1).getAsString())
                    .putString(ThisApp.this.getString(R.string.pref_postal_code_key), firstElement.get(2).getAsString())
                    .apply();
        }
    }
}
