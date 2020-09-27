/*
 Copyright (c) 2010 - 2020 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

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

import com.google.gson.Gson;
import com.marv42.ebt.newnote.di.DaggerApplicationComponent;
import com.marv42.ebt.newnote.exceptions.ErrorMessage;
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
import static com.marv42.ebt.newnote.exceptions.ErrorMessage.ERROR;
import static com.marv42.ebt.newnote.location.FetchAddressIntentService.LOCATION_DATA_EXTRA;
import static com.marv42.ebt.newnote.location.FetchAddressIntentService.RECEIVER;
import static com.marv42.ebt.newnote.location.FetchAddressIntentService.RESULT_DATA_KEY;

@AcraCore(buildConfigClass = BuildConfig.class)
@AcraMailSender(mailTo = "marv42+acra@gmail.com")
@AcraToast(resText = R.string.crash_text)
public class ThisApp extends DaggerApplication {
    public static final int RESULT_CODE_SUCCESS = 0;
    public static final int RESULT_CODE_ERROR = 1;

    BroadcastReceiver broadcastReceiver;

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
        if (broadcastReceiver == null) {
            broadcastReceiver = new LocationProviderChangedReceiver();
            IntentFilter filter = new IntentFilter(PROVIDERS_CHANGED_ACTION);
            filter.addAction(ACTION_PROVIDER_CHANGED);
            registerReceiver(broadcastReceiver, filter);
        }
    }

    public void stopLocationProviderChangedReceiver() {
        unregisterReceiver(broadcastReceiver);
        broadcastReceiver = null;
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
            if (resultCode != RESULT_CODE_SUCCESS) {
                showErrorMessage(resultData);
                return;
            }
            String addressOutput = resultData.getString(RESULT_DATA_KEY);
            if (addressOutput == null) {
                Toast.makeText(ThisApp.this, getString(R.string.internal_error),LENGTH_LONG).show();
                return;
            }
            putLocationValues(addressOutput);
        }

        private void showErrorMessage(Bundle resultData) {
            String text = resultData.getString(RESULT_DATA_KEY);
            if (text == null)
                text = getString(R.string.internal_error);
            Toast.makeText(ThisApp.this, new ErrorMessage(
                    ThisApp.this).getErrorMessage(text), LENGTH_LONG).show();
        }

        private void putLocationValues(String addressOutput) {
            LocationValues location = new Gson().fromJson(addressOutput, LocationValues.class);
            String country = location.country;
            if (country.startsWith(ERROR))
                Toast.makeText(ThisApp.this,
                        new ErrorMessage(ThisApp.this).getErrorMessage(country), LENGTH_LONG).show();
            else
                getDefaultSharedPreferences(ThisApp.this).edit()
                        .putString(ThisApp.this.getString(R.string.pref_country_key), country)
                        .apply();
            getDefaultSharedPreferences(ThisApp.this).edit()
                    .putString(ThisApp.this.getString(R.string.pref_city_key), location.city)
                    .putString(ThisApp.this.getString(R.string.pref_postal_code_key), location.postalCode)
                    .apply();
        }
    }
}
