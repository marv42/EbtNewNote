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
            ThisApp app = (ThisApp) context;
            if (gpsProviderEnabled || networkProviderEnabled) {
                app.startLocationTask();
                app.stopLocationProviderChangedReceiver();
            }
        }
    }
}
