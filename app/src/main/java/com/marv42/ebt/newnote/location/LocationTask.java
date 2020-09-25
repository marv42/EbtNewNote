/*
 Copyright (c) 2010 - 2020 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote.location;

import android.annotation.SuppressLint;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.marv42.ebt.newnote.R;
import com.marv42.ebt.newnote.ThisApp;

import org.jetbrains.annotations.NotNull;

import static android.content.Context.LOCATION_SERVICE;
import static android.widget.Toast.LENGTH_LONG;

public class LocationTask extends AsyncTask<Void, Void, Integer> {
    private static final long LOCATION_MAX_WAIT_TIME_MS = 30 * 1000;
    private static final float MIN_LOCATION_DISTANCE_M = 500;
    private static final long MIN_LOCATION_TIME_MS = 10 * 1000;

    private ThisApp app;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private Location location;

    public LocationTask(@NonNull ThisApp app) {
        this.app = app;
        locationManager = (LocationManager) this.app.getSystemService(LOCATION_SERVICE);
    }

    @SuppressLint("MissingPermission")
    @Override
    protected Integer doInBackground(Void... voids) {
        if (! Geocoder.isPresent())
            return R.string.location_no_geocoder;
        Location lastKnownLocation = getLastKnownLocation();
        if (lastKnownLocation != null) {
            if (location == null ||
                    lastKnownLocation.getTime() - location.getTime() > LOCATION_MAX_WAIT_TIME_MS) {
                location = lastKnownLocation;
                return R.string.location_last_known;
            }
        }
        locationListener = new LocationListener() {
            public void onLocationChanged(@NotNull Location location) {
                locationManager.removeUpdates(this);
                LocationTask.this.location = location;
            }
            public void onStatusChanged(String provider, int status, Bundle extras) {}
            public void onProviderEnabled(@NotNull String provider) {}
            public void onProviderDisabled(@NotNull String provider) {}
        };
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_LOCATION_TIME_MS,
                MIN_LOCATION_DISTANCE_M, locationListener, Looper.getMainLooper());
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_LOCATION_TIME_MS,
                MIN_LOCATION_DISTANCE_M, locationListener, Looper.getMainLooper());
        return R.string.location_got;
    }

    @SuppressLint("MissingPermission")
    private Location getLastKnownLocation() {
        Location lastKnownGpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        Location lastKnownNetworkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        if (lastKnownGpsLocation != null && lastKnownNetworkLocation != null) {
            if (lastKnownNetworkLocation.getAccuracy() > lastKnownGpsLocation.getAccuracy())
                return lastKnownNetworkLocation;
            else
                return lastKnownGpsLocation;
        }
        return lastKnownGpsLocation != null ? lastKnownGpsLocation : lastKnownNetworkLocation;
    }

    @Override
    protected void onPostExecute(Integer result) {
        if (result != null)
            Toast.makeText(app, app.getString(result), LENGTH_LONG).show();
        if (location != null)
            app.onLocation(location);
    }
}
