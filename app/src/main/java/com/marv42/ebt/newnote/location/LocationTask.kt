/*
 Copyright (c) 2010 - 2022 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */
package com.marv42.ebt.newnote.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import com.marv42.ebt.newnote.R
import com.marv42.ebt.newnote.ThisApp
import com.marv42.ebt.newnote.Utils
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class LocationTask(private val app: ThisApp) : CoroutineScope {

    private val locationManager: LocationManager = app.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private var location: Location? = null

    private var job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    fun execute() = launch {
        val result = doInBackground()
        onPostExecute(result)
    }

    @SuppressLint("MissingPermission")
    private suspend fun doInBackground(): Int {
        if (!Geocoder.isPresent())
            return R.string.location_no_geocoder
        val locationListener = locationListener
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_TIME_UPDATES_MS,
                MIN_DISTANCE_UPDATES_M, locationListener, Looper.getMainLooper())
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME_UPDATES_MS,
                MIN_DISTANCE_UPDATES_M, locationListener, Looper.getMainLooper())
        delay(WAIT_TIME_MS)
        val lastKnownLocation: Location? = lastKnownLocation
        if (lastKnownLocation != null)
            if (location == null || locationIsMuchNewer(lastKnownLocation)) {
                location = lastKnownLocation
                return R.string.location_last_known
            }
        return R.string.location_got
    }

    private fun locationIsMuchNewer(l: Location): Boolean {
        return l.elapsedRealtimeNanos - location!!.elapsedRealtimeNanos > MAX_TIME_LAST_KNOWN_NS
    }

    @get:SuppressLint("MissingPermission")
    private val lastKnownLocation: Location?
        get() {
            val lastKnownGpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val lastKnownNetworkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            return if (lastKnownGpsLocation != null && lastKnownNetworkLocation != null) {
                if (lastKnownNetworkLocation.accuracy > lastKnownGpsLocation.accuracy)
                    lastKnownNetworkLocation
                else
                    lastKnownGpsLocation
            } else
                lastKnownGpsLocation ?: lastKnownNetworkLocation
        }

    private val locationListener: LocationListener
        get() = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                locationManager.removeUpdates(this)
//                this@LocationTask.location = location
            }

            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

    private fun onPostExecute(result: Int?) {
        if (result != null)
            Toast.makeText(app, result, LENGTH_LONG).show()
        if (location != null)
            app.onLocation(location)
    }

    companion object {
        private const val MAX_TIME_LAST_KNOWN_NS = 30L * Utils.SECONDS_IN_NANOSECONDS
        private const val WAIT_TIME_MS = 4 * 1000.toLong()
        private const val MIN_DISTANCE_UPDATES_M = 500f
        private const val MIN_TIME_UPDATES_MS = 10 * 1000.toLong()
    }
}