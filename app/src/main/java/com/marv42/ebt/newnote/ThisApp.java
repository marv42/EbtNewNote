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
import android.content.IntentFilter;

import com.marv42.ebt.newnote.di.DaggerApplicationComponent;
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

@AcraCore(buildConfigClass = BuildConfig.class)
@AcraMailSender(mailTo = "marv42+acra@gmail.com")
@AcraToast(resText = R.string.crash_text)
public class ThisApp extends DaggerApplication {
    BroadcastReceiver mReceiver;

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
        if (mReceiver == null) {
            mReceiver = new LocationProviderChangedReceiver();
            IntentFilter filter = new IntentFilter(PROVIDERS_CHANGED_ACTION);
            filter.addAction(ACTION_PROVIDER_CHANGED);
            registerReceiver(mReceiver, filter);
            // TODO start timer and stopLocationProviderChangedReceiver
        }
    }

    public void stopLocationProviderChangedReceiver() {
        unregisterReceiver(mReceiver);
        mReceiver = null;
    }

    public void startLocationTask() {
        new LocationTask(this).execute();
    }
}
