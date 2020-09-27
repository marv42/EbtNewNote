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
import android.os.SystemClock;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.marv42.ebt.newnote.R;
import com.marv42.ebt.newnote.ThisApp;

import org.jetbrains.annotations.NotNull;

import static android.content.Context.LOCATION_SERVICE;
import static android.widget.Toast.LENGTH_LONG;
import static com.marv42.ebt.newnote.Utils.SECONDS_IN_NANOSECONDS;

public class LocationTask extends AsyncTask<Void, Void, Integer> {
    private static final long MAX_TIME_LAST_KNOWN_NS = 30L * SECONDS_IN_NANOSECONDS;
    private static final long WAIT_TIME_MS = 4 * 1000;
    private static final float MIN_DISTANCE_UPDATES_M = 500;
    private static final long MIN_TIME_UPDATES_MS = 10 * 1000;

    private ThisApp app;
    private LocationManager locationManager;
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
        LocationListener locationListener = getLocationListener();
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_TIME_UPDATES_MS,
                MIN_DISTANCE_UPDATES_M, locationListener, Looper.getMainLooper());
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME_UPDATES_MS,
                MIN_DISTANCE_UPDATES_M, locationListener, Looper.getMainLooper());
        SystemClock.sleep(WAIT_TIME_MS);
        Location lastKnownLocation = getLastKnownLocation();
        if (lastKnownLocation != null) {
            if (location == null || locationIsMuchNewer(lastKnownLocation)) {
                location = lastKnownLocation;
                return R.string.location_last_known;
            }
        }
        return R.string.location_got;
    }

    private boolean locationIsMuchNewer(Location l) {
        return l.getElapsedRealtimeNanos() - location.getElapsedRealtimeNanos() > MAX_TIME_LAST_KNOWN_NS;
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

    @NotNull
    private LocationListener getLocationListener() {
        return new LocationListener() {
            public void onLocationChanged(@NotNull Location location) {
                locationManager.removeUpdates(this);
                LocationTask.this.location = location;
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            public void onProviderEnabled(@NotNull String provider) {
            }

            public void onProviderDisabled(@NotNull String provider) {
            }
        };
    }

    @Override
    protected void onPostExecute(Integer result) {
        if (result != null)
            Toast.makeText(app, app.getString(result), LENGTH_LONG).show();
        if (location != null)
            app.onLocation(location);
    }
}
