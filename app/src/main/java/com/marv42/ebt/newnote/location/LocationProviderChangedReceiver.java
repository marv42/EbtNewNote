/*
 Copyright (c) 2010 - 2020 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote.location;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;

import com.marv42.ebt.newnote.ThisApp;

import static android.content.Context.LOCATION_SERVICE;
import static android.location.LocationManager.GPS_PROVIDER;
import static android.location.LocationManager.NETWORK_PROVIDER;
import static android.location.LocationManager.PROVIDERS_CHANGED_ACTION;

public class LocationProviderChangedReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (PROVIDERS_CHANGED_ACTION.equals(intent.getAction())) {
            LocationManager locationManager = (LocationManager) context.getSystemService(LOCATION_SERVICE);
            boolean gpsProviderEnabled = locationManager.isProviderEnabled(GPS_PROVIDER);
            boolean networkProviderEnabled = locationManager.isProviderEnabled(NETWORK_PROVIDER);
            if (gpsProviderEnabled || networkProviderEnabled) {
                ThisApp app = (ThisApp) context;
                app.startLocationTask();
                app.stopLocationProviderChangedReceiver();
            }
        }
    }
}
