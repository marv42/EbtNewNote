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

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;

import static android.content.Context.LOCATION_SERVICE;


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

   private final Context mApplicationContext;
   private Callback mCallback;

   public static abstract class LocationResult {
      public abstract void
      gotLocation(Location location, String provider);
   }

   LocationTask(final Context context, Callback callback) {
      mApplicationContext = context;
      mCallback = callback;
   }

   @Override
   protected void onPreExecute() {
      mLocationManager = (LocationManager) mApplicationContext.getSystemService(LOCATION_SERVICE);
      mLocationResult = new LocationResult() {
         @Override
         public void gotLocation(final Location l, final String provider) {
            if (provider.equals(LocationManager.GPS_PROVIDER))
               mGpsLocation = l;
            if (provider.equals(LocationManager.NETWORK_PROVIDER))
               mNetworkLocation = l;
         }
      };

      if (ActivityCompat.checkSelfPermission(mApplicationContext, Manifest.permission.ACCESS_FINE_LOCATION)
              != PackageManager.PERMISSION_GRANTED &&
              ActivityCompat.checkSelfPermission(mApplicationContext, Manifest.permission.ACCESS_COARSE_LOCATION)
                      != PackageManager.PERMISSION_GRANTED) {
         // TODO: Consider calling
         //    ActivityCompat#requestPermissions
         // here to request the missing permissions, and then overriding
         //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
         //                                          int[] grantResults)
         // to handle the case where the user grants the permission. See the documentation
         // for ActivityCompat#requestPermissions for more details.
         return;
      }

      if (isGpsEnabled()) {
         //Log.d(EbtNewNote.LOG_TARGET, "GPS enabled");
         mGpsLocationListener = new MyLocationListener(LocationManager.GPS_PROVIDER);
         mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                 0, 0, mGpsLocationListener);
      }
      if (isNetworkEnabled()) {
         //Log.d(EbtNewNote.LOG_TARGET, "network location enabled");
         mNetworkLocationListener = new MyLocationListener(LocationManager.NETWORK_PROVIDER);
         mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                 0, 0, mNetworkLocationListener);
      }
   }

   @Override
   protected Location doInBackground(Void... params) {
      return getLocation();
   }

   protected void onProgressUpdate(String... text) {
      Toast.makeText(mApplicationContext, text[0], Toast.LENGTH_LONG).show();
   }

   @Override
   protected void onPostExecute(final Location l) {
      if (mGpsLocationListener     != null)
         mLocationManager.removeUpdates(mGpsLocationListener);
      if (mNetworkLocationListener != null)
         mLocationManager.removeUpdates(mNetworkLocationListener);
      //Log.d(EbtNewNote.LOG_TARGET, "[LocationTask] stopped all location updates");

//      mGpsLocationListener     = null;
//      mNetworkLocationListener = null;
//      mLocationManager         = null;
      // TODO Do we need to delete these?

      setLocationValues(l);

      if (! PreferenceManager.getDefaultSharedPreferences(mApplicationContext).edit()
              .putBoolean(mApplicationContext.getString(R.string.pref_getting_location_key), false)
              .commit())
         Log.e(EbtNewNote.LOG_TARGET, "Editor's commit failed");
   }

   private boolean isGpsEnabled() {
      return mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
   }

   private boolean isNetworkEnabled() {
      return mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
   }

   private Location getLocation() {
      Long t = Calendar.getInstance().getTimeInMillis();

      while (Calendar.getInstance().getTimeInMillis() - t < MAX_WAIT_TIME &&
              mGpsLocation == null &&
              (mNetworkLocation == null || isGpsEnabled()) &&
              (isNetworkEnabled() || isGpsEnabled()))
         SystemClock.sleep(SLEEP_TIME);

      if (mGpsLocation != null) {
         publishProgress(mApplicationContext.getString(R.string.location_accurate));
         //Log.d(EbtNewNote.LOG_TARGET, "got a GPS location");
         return mGpsLocation;
      }

      if (mNetworkLocation != null) {
         publishProgress(mApplicationContext.getString(R.string.location_no_accurate));
         //Log.d(EbtNewNote.LOG_TARGET, "got a network location");
         return mNetworkLocation;
      }

      publishProgress(mApplicationContext.getString(R.string.location_no_up_to_date));
      //Log.d(EbtNewNote.LOG_TARGET, "no up-to-date location");

      mGpsLocation     = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER    );
      mNetworkLocation = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

      if (mGpsLocation != null && mNetworkLocation == null)
         return mGpsLocation;
      if (mGpsLocation == null && mNetworkLocation != null)
         return mNetworkLocation;
      if (mGpsLocation != null && mNetworkLocation != null &&
              mGpsLocation.getTime() > mNetworkLocation.getTime())
         return mGpsLocation;
      else
         return mNetworkLocation;
   }

   private void setLocationValues(Location l) {
      if (l == null) {
         //Log.d(LOG_TARGET, "location: null");
         Toast.makeText(mApplicationContext, mApplicationContext.getString(R.string.no_location), Toast.LENGTH_LONG).show();
         return;
      }

      //Log.d(EbtNewNote.LOG_TARGET, "location: " + l.getLatitude() + ", " + l.getLongitude());
      try {
         final Geocoder gc = new Geocoder(mApplicationContext);
         List<Address> addresses = gc.getFromLocation(
                 l.getLatitude(), l.getLongitude(), NUMBER_ADDRESSES);
         //Log.d(EbtNewNote.LOG_TARGET, "Geocoder got " + addresses.size() + " address(es)");

         if (addresses.size() == 0)
            Toast.makeText(mApplicationContext, mApplicationContext.getString(R.string.no_address) + ": " + l.getLatitude() + ", " + l.getLongitude() + ".", Toast.LENGTH_LONG).show();

         for (Address a : addresses) {
            if (a == null)
               continue;

            mCallback.onNewLocation(
                    new LocationValues(a.getCountryName(), a.getLocality(), a.getPostalCode(), true));
         }
      } catch (IOException e) {
         Log.e(EbtNewNote.LOG_TARGET, "Geocoder IOException: " + e.getMessage());
         Toast.makeText(mApplicationContext, mApplicationContext.getString(R.string.geocoder_exception) + ": " + e.getMessage() + ".", Toast.LENGTH_LONG).show();
      }
   }

   private final class MyLocationListener implements LocationListener {
      private String mProvider;

      MyLocationListener(String provider)
      {
         mProvider = provider;
      }

      public void onLocationChanged(Location l) {
         //Log.d(EbtNewNote.LOG_TARGET, "location changed");
         mLocationResult.gotLocation(l, mProvider);
      }

      public void onStatusChanged(String provider, int status, Bundle extras) {}

      public void onProviderEnabled(String provider) {}

      public void onProviderDisabled(String provider) {}
   }
}
