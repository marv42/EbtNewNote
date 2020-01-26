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

import static android.content.Context.LOCATION_SERVICE;
import static android.widget.Toast.LENGTH_LONG;

public class LocationTask extends AsyncTask<Void, Void, Integer> {
    private static final long LOCATION_MAX_WAIT_TIME_MS = 30 * 1000;
    private static final float MIN_LOCATION_DISTANCE_M = 500;
    private static final long MIN_LOCATION_TIME_MS = 10 * 1000;

    private ThisApp mApp;
    private LocationManager mLocationManager;
    private LocationListener mLocationListener;
    private Location mLocation;

    public LocationTask(@NonNull ThisApp app) {
        mApp = app;
        mLocationManager = (LocationManager) mApp.getSystemService(LOCATION_SERVICE);
    }

    @SuppressLint("MissingPermission")
    @Override
    protected Integer doInBackground(Void... voids) {
        if (! Geocoder.isPresent())
            return R.string.location_no_geocoder;
        Location lastKnownLocation = getLastKnownLocation();
        if (lastKnownLocation != null) {
            if (mLocation == null ||
                    lastKnownLocation.getTime() - mLocation.getTime() > LOCATION_MAX_WAIT_TIME_MS) {
                setLocation(lastKnownLocation);
                return R.string.location_last_known;
            }
        }
        mLocationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                mLocationManager.removeUpdates(this);
                setLocation(location);
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {}
            public void onProviderEnabled(String provider) {}
            public void onProviderDisabled(String provider) {}
        };
        mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_LOCATION_TIME_MS,
                MIN_LOCATION_DISTANCE_M, mLocationListener, Looper.getMainLooper());
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_LOCATION_TIME_MS,
                MIN_LOCATION_DISTANCE_M, mLocationListener, Looper.getMainLooper());
        return R.string.location_got;
    }

    @SuppressLint("MissingPermission")
    private Location getLastKnownLocation() {
        Location lastKnownGpsLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        Location lastKnownNetworkLocation = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        if (lastKnownGpsLocation != null && lastKnownNetworkLocation != null) {
            if (lastKnownNetworkLocation.getAccuracy() > lastKnownGpsLocation.getAccuracy())
                return lastKnownNetworkLocation;
            else
                return lastKnownGpsLocation;
        }
        return lastKnownGpsLocation != null ? lastKnownGpsLocation : lastKnownNetworkLocation;
    }

    private void setLocation(Location l) {
        mLocation = l;
    }

    @Override
    protected void onPostExecute(Integer result) {
        if (result != null)
            Toast.makeText(mApp, mApp.getString(result), LENGTH_LONG).show();
        mApp.onLocation(mLocation);
    }
}
