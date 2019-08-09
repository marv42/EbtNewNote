package com.marv42.ebt.newnote.location;

import android.annotation.SuppressLint;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.text.TextUtils;

import com.marv42.ebt.newnote.R;
import com.marv42.ebt.newnote.ThisApp;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import static android.content.Context.LOCATION_SERVICE;
import static androidx.preference.PreferenceManager.getDefaultSharedPreferences;

public class LocationTask extends AsyncTask<Void, Void, Void> {
    private static final long LOCATION_MAX_WAIT_TIME_MS = 30 * 1000;
    private static final float MIN_LOCATION_DISTANCE_M = 500;
    private static final long MIN_LOCATION_TIME_MS = 10 * 1000;
    private static final int NUMBER_ADDRESSES = 5;

    private ThisApp mApp;
    private LocationManager mLocationManager;
    private LocationListener mLocationListener;
    private Location mPreviousLocation;

    public LocationTask(ThisApp app) {
        mApp = app;
        mLocationManager = (LocationManager) app.getSystemService(LOCATION_SERVICE);
    }

    @SuppressLint("MissingPermission")
    @Override
    protected Void doInBackground(Void... voids) {
        Location lastKnownLocation = getLastKnownLocation();
        if (lastKnownLocation != null) {
            if (mPreviousLocation == null ||
                    lastKnownLocation.getTime() - mPreviousLocation.getTime() > LOCATION_MAX_WAIT_TIME_MS) {
                setLocation(lastKnownLocation);
                return null;
            }
        }
        mLocationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                setLocation(location);
                mLocationManager.removeUpdates(this);
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {}

            public void onProviderEnabled(String provider) {}

            public void onProviderDisabled(String provider) {}
        };
        mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_LOCATION_TIME_MS,
                MIN_LOCATION_DISTANCE_M, mLocationListener, Looper.getMainLooper());
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_LOCATION_TIME_MS,
                MIN_LOCATION_DISTANCE_M, mLocationListener, Looper.getMainLooper());
        return null;
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
        try {
            if (! Geocoder.isPresent())
                return;
            Geocoder geocoder = new Geocoder(mApp, Locale.US);
            List<Address> addresses = geocoder.getFromLocation(l.getLatitude(), l.getLongitude(),
                    NUMBER_ADDRESSES);
            String[] previousLocation = new String[3];
            for (Address a : addresses) {
                if (a == null)
                    continue;
                String countryName = a.getCountryName();
                String locality = a.getLocality();
                String postalCode = a.getPostalCode();
                if ((!TextUtils.isEmpty(countryName) && !countryName.equals(previousLocation[0])) ||
                        (!TextUtils.isEmpty(locality) && !locality.equals(previousLocation[1])) ||
                        (!TextUtils.isEmpty(postalCode) && !postalCode.equals(previousLocation[2]))) {
                    previousLocation = new String[]{countryName, locality, postalCode};
                    mPreviousLocation = l;
                    getDefaultSharedPreferences(mApp).edit()
                            .putString(mApp.getString(R.string.pref_country_key), countryName)
                            .putString(mApp.getString(R.string.pref_city_key), locality)
                            .putString(mApp.getString(R.string.pref_postal_code_key), postalCode).apply();
                }
            }
        } catch (IOException ignored) {
        }
    }
}
