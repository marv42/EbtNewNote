/*
 Copyright (c) 2010 - 2021 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote;

import static android.content.Intent.ACTION_PROVIDER_CHANGED;
import static android.location.LocationManager.PROVIDERS_CHANGED_ACTION;
import static android.widget.Toast.LENGTH_LONG;
import static com.marv42.ebt.newnote.location.FetchAddressIntentService.LOCATION_DATA_EXTRA;
import static com.marv42.ebt.newnote.location.FetchAddressIntentService.RECEIVER;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Handler;
import android.widget.Toast;

import com.marv42.ebt.newnote.di.DaggerApplicationComponent;
import com.marv42.ebt.newnote.location.AddressResultReceiver;
import com.marv42.ebt.newnote.location.FetchAddressIntentService;
import com.marv42.ebt.newnote.location.LocationProviderChangedReceiver;
import com.marv42.ebt.newnote.location.LocationTask;

import org.acra.ACRA;
import org.acra.BuildConfig;
import org.acra.config.CoreConfigurationBuilder;
import org.acra.config.MailSenderConfigurationBuilder;
import org.acra.config.ToastConfigurationBuilder;
import org.acra.data.StringFormat;

import dagger.android.AndroidInjector;
import dagger.android.support.DaggerApplication;

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
        CoreConfigurationBuilder builder = new CoreConfigurationBuilder()
                .withBuildConfigClass(BuildConfig.class)
                .withReportFormat(StringFormat.JSON)
                .withPluginConfigurations(
                        new ToastConfigurationBuilder()
                                .withText(getString(R.string.crash_text)).build(),
                        new MailSenderConfigurationBuilder()
                                .withMailTo(getString(R.string.crash_email_address)).build());
        ACRA.init(this, builder);
    }

    public void startLocationProviderChangedReceiver() {
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
        Toast.makeText(this, R.string.location_start, LENGTH_LONG).show();
        new LocationTask(this).execute();
    }

    public void onLocation(Location location) {
        Intent intent = new Intent(this, FetchAddressIntentService.class);
        intent.putExtra(RECEIVER, new AddressResultReceiver(this, new Handler()));
        intent.putExtra(LOCATION_DATA_EXTRA, location);
        startService(intent);
    }
}
