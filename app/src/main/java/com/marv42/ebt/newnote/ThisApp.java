/*
 Copyright (c) 2010 - 2026 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote;

import static android.content.Intent.ACTION_PROVIDER_CHANGED;
import static android.location.LocationManager.PROVIDERS_CHANGED_ACTION;
import static android.widget.Toast.LENGTH_LONG;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.location.Location;
import android.widget.Toast;

import com.marv42.ebt.newnote.di.DaggerApplicationComponent;
import com.marv42.ebt.newnote.location.AddressTask;
import com.marv42.ebt.newnote.location.LocationProviderChangedReceiver;
import com.marv42.ebt.newnote.location.LocationTask;
import com.marv42.ebt.newnote.preferences.SharedPreferenceDataStore;
import com.marv42.ebt.newnote.preferences.SharedPreferencesHandler;
import com.marv42.ebt.newnote.retry.RetryManager;

import org.acra.ACRA;
import org.acra.config.CoreConfigurationBuilder;
import org.acra.config.MailSenderConfigurationBuilder;
import org.acra.config.ToastConfigurationBuilder;
import org.acra.data.StringFormat;

import java.util.Arrays;

import javax.inject.Inject;
import javax.inject.Provider;

import dagger.android.AndroidInjector;
import dagger.android.support.DaggerApplication;

public class ThisApp extends DaggerApplication {

    @Inject
    Provider<SharedPreferenceDataStore> dataStoreProvider;
    @Inject
    SharedPreferencesHandler preferences;
    @Inject
    RetryManager retryManager;

    BroadcastReceiver broadcastReceiver;
    LocationTask locationTask;
    AddressTask addressTask;

    @Override
    public void onCreate() {
        super.onCreate();
        retryManager.init();
    }

    @Override
    protected AndroidInjector<? extends DaggerApplication> applicationInjector() {
        return DaggerApplicationComponent.factory().create(this);
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        initAcra();
    }

    private void initAcra() {
        CoreConfigurationBuilder builder = new CoreConfigurationBuilder()
                .withReportFormat(StringFormat.JSON)
                .withPluginConfigurations(
                        new ToastConfigurationBuilder()
                                .withText(getString(R.string.crash_text)).build(),
                        new MailSenderConfigurationBuilder()
                                .withMailTo(getString(R.string.crash_email_address)).build())
                .withExcludeMatchingSharedPreferencesKeys(Arrays.asList(
                        getString(R.string.pref_results_key),
                        getString(R.string.pref_settings_email_key),
                        getString(R.string.pref_settings_password_key),
                        getString(R.string.pref_settings_ocr_service_key_key),
                        getString(R.string.pref_settings_country_layer_key_key),
                        getString(R.string.pref_settings_rest_countries_key_key)));
        ACRA.init(this, builder);
    }

    public void startLocationProviderChangedReceiver() {
        if (broadcastReceiver == null) {
            broadcastReceiver = new LocationProviderChangedReceiver();
            IntentFilter filter = new IntentFilter(PROVIDERS_CHANGED_ACTION);
            filter.addAction(ACTION_PROVIDER_CHANGED);
            registerReceiver(broadcastReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        }
    }

    public void stopLocationProviderChangedReceiver() {
        unregisterReceiver(broadcastReceiver);
        broadcastReceiver = null;
    }

    public void startLocationTask() {
        Toast.makeText(this, R.string.location_start, LENGTH_LONG).show();
        cancelLocationTask();
        locationTask = new LocationTask(this);
        locationTask.execute();
    }

    private void cancelLocationTask() {
        if (locationTask != null) {
            locationTask.cancel();
            locationTask = null;
        }
    }

    public void onLocation(Location location) {
        cancelAddressTask();
        addressTask = new AddressTask(this, dataStoreProvider.get(), preferences);
        addressTask.execute(location);
    }

    private void cancelAddressTask() {
        if (addressTask != null) {
            addressTask.cancel();
            addressTask = null;
        }
    }
}
