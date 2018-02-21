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

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.List;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.content.Context.LOCATION_SERVICE;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.location.LocationManager.GPS_PROVIDER;
import static android.location.LocationManager.NETWORK_PROVIDER;
import static android.preference.PreferenceManager.getDefaultSharedPreferences;
import static android.support.v4.content.ContextCompat.checkSelfPermission;
import static android.widget.Toast.LENGTH_LONG;
import static com.marv42.ebt.newnote.EbtNewNote.LOG_TAG;

public class LocationTask extends AsyncTask<Void, String, Location> {
    interface Callback {
        void onNewLocation(LocationValues locationValues);
    }

    private static final long MAX_WAIT_TIME = 30 * 1000;
    private static final long SLEEP_TIME = 5000;

    private static final int NUMBER_ADDRESSES = 5;

    private LocationManager mLocationManager;
    private LocationResult mLocationResult;

    private Location mGpsLocation;
    private Location mNetworkLocation;

    private MyLocationListener mGpsLocationListener;
    private MyLocationListener mNetworkLocationListener;

    private WeakReference<Context> mContext;
    private Callback mCallback;

    public static abstract class LocationResult {
        public abstract void
        gotLocation(Location location, String provider);
    }

    LocationTask(final Context context, Callback callback) {
        mContext = new WeakReference<>(context);
        mCallback = callback;
    }

    private boolean permissionsGranted() {
        if(checkSelfPermission(mContext.get(), ACCESS_FINE_LOCATION) == PERMISSION_GRANTED ||
                checkSelfPermission(mContext.get(), ACCESS_COARSE_LOCATION) == PERMISSION_GRANTED)
            return true;
        // TODO: Consider calling
        //    ActivityCompat#requestPermissions
        // here to request the missing permissions, and then overriding
        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
        //                                          int[] grantResults)
        // to handle the case where the user grants the permission. See the documentation
        // for ActivityCompat#requestPermissions for more details.
        return false;
    }

    @Override
    protected void onPreExecute() {
        mLocationManager = (LocationManager) mContext.get().getSystemService(LOCATION_SERVICE);
        mLocationResult = new LocationResult() {
            @Override
            public void gotLocation(final Location l, final String provider) {
                if (provider.equals(GPS_PROVIDER))
                    mGpsLocation = l;
                if (provider.equals(NETWORK_PROVIDER))
                    mNetworkLocation = l;
            }
        };
        if(!permissionsGranted())
            return;
        if (isGpsEnabled()) {
            Log.d(LOG_TAG, "GPS enabled");
            mGpsLocationListener = new MyLocationListener(GPS_PROVIDER);
            mLocationManager.requestLocationUpdates(GPS_PROVIDER,
                    0, 0, mGpsLocationListener);
        }
        if (isNetworkEnabled()) {
            Log.d(LOG_TAG, "network location enabled");
            mNetworkLocationListener = new MyLocationListener(NETWORK_PROVIDER);
            mLocationManager.requestLocationUpdates(NETWORK_PROVIDER,
                    0, 0, mNetworkLocationListener);
        }
    }

    @Override
    protected Location doInBackground(Void... params) {
        return getLocation();
    }

    protected void onProgressUpdate(String... text) {
        Toast.makeText(mContext.get(), text[0], LENGTH_LONG).show();
    }

    @Override
    protected void onPostExecute(final Location location) {
        if (location == null) {
            Log.d(LOG_TAG, "location: null");
            Toast.makeText(mContext.get(), mContext.get().getString(R.string.no_location), LENGTH_LONG).show();
            return;
        }
        if (mGpsLocationListener != null)
            mLocationManager.removeUpdates(mGpsLocationListener);
        if (mNetworkLocationListener != null)
            mLocationManager.removeUpdates(mNetworkLocationListener);
        Log.d(LOG_TAG, "stopped all location updates");

//      mGpsLocationListener     = null;
//      mNetworkLocationListener = null;
//      mLocationManager         = null;
        // TODO Do we need to delete these?

        setLocationValues(location);
        getDefaultSharedPreferences(mContext.get()).edit()
                .putBoolean(mContext.get().getString(R.string.pref_getting_location_key), false).apply();
    }

    private boolean isGpsEnabled() {
        return mLocationManager.isProviderEnabled(GPS_PROVIDER);
    }

    private boolean isNetworkEnabled() {
        return mLocationManager.isProviderEnabled(NETWORK_PROVIDER);
    }

    private Location getLocation() {
        Long t = Calendar.getInstance().getTimeInMillis();

        while (Calendar.getInstance().getTimeInMillis() - t < MAX_WAIT_TIME &&
                mGpsLocation == null &&
                (mNetworkLocation == null || isGpsEnabled()) &&
                (isNetworkEnabled() || isGpsEnabled()))
            SystemClock.sleep(SLEEP_TIME);

        if (mGpsLocation != null) {
            publishProgress(mContext.get().getString(R.string.location_accurate));
            Log.d(LOG_TAG, "got a GPS location");
            return mGpsLocation;
        }

        if (mNetworkLocation != null) {
            publishProgress(mContext.get().getString(R.string.location_no_accurate));
            Log.d(LOG_TAG, "got a network location");
            return mNetworkLocation;
        }

        publishProgress(mContext.get().getString(R.string.location_no_up_to_date));
        Log.d(LOG_TAG, "no up-to-date location");

        if(!permissionsGranted())
            return null;
        mGpsLocation = mLocationManager.getLastKnownLocation(GPS_PROVIDER);
        mNetworkLocation = mLocationManager.getLastKnownLocation(NETWORK_PROVIDER);

        if (mGpsLocation != null && mNetworkLocation == null)
            return mGpsLocation;
        if (mGpsLocation == null && mNetworkLocation != null)
            return mNetworkLocation;
        if (mGpsLocation != null && //mNetworkLocation != null &&
                mGpsLocation.getTime() > mNetworkLocation.getTime())
            return mGpsLocation;
        else
            return mNetworkLocation;
    }

    private void setLocationValues(Location l) {
        Log.d(LOG_TAG, "location: " + l.getLatitude() + ", " + l.getLongitude());
        try {
            final Geocoder gc = new Geocoder(mContext.get());
            List<Address> addresses = gc.getFromLocation(
                    l.getLatitude(), l.getLongitude(), NUMBER_ADDRESSES);
            Log.d(LOG_TAG, "Geocoder got " + addresses.size() + " address(es)");

            if (addresses.size() == 0)
                Toast.makeText(mContext.get(), mContext.get().getString(R.string.no_address) + ": " + l.getLatitude() + ", " + l.getLongitude() + ".", LENGTH_LONG).show();

            for (Address a : addresses) {
                if (a == null)
                    continue;
                mCallback.onNewLocation(
                        new LocationValues(a.getCountryName(), a.getLocality(), a.getPostalCode(), true));
            }
        } catch (IOException e) {
            Log.e(LOG_TAG, "Geocoder IOException: " + e);
            Toast.makeText(mContext.get(), mContext.get().getString(R.string.geocoder_exception) + ": " + e.getMessage() + ".", LENGTH_LONG).show();
        }
    }

    private final class MyLocationListener implements LocationListener {
        private String mProvider;

        MyLocationListener(String provider)
        {
            mProvider = provider;
        }

        public void onLocationChanged(Location l) {
            Log.d(LOG_TAG, "location changed");
            mLocationResult.gotLocation(l, mProvider);
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {}

        public void onProviderEnabled(String provider) {}

        public void onProviderDisabled(String provider) {}
    }
}
