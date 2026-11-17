/*
 Copyright (c) 2010 - 2026 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */
package com.marv42.ebt.newnote.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import android.os.SystemClock
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import com.marv42.ebt.newnote.R
import com.marv42.ebt.newnote.ThisApp
import com.marv42.ebt.newnote.executeAsyncTask
import com.marv42.ebt.newnote.mainScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class LocationTask(private val app: ThisApp) {

    private val locationManager: LocationManager = app.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private val scope = mainScope()
    private var job: Job? = null

    fun execute() {
        job?.cancel()
        job = scope.executeAsyncTask(
            doInBackground = {
                requestLocation()
            }, onPostExecute = {
                deliverResult(it)
            }, onError = {
                deliverResult(LocationResult(null, R.string.location_none))
            })
    }

    fun cancel() {
        job?.cancel()
        job = null
    }

    private suspend fun requestLocation(): LocationResult {
        val providers = locationManager.getProviders(true) - LocationManager.PASSIVE_PROVIDER
        if (providers.isEmpty())
            return LocationResult(null, R.string.location_no_provider)
        val result = try {
            lastKnownLocation(providers)?.let { LocationResult(it, R.string.location_last_known) }
                ?: freshLocation(providers)?.let { LocationResult(it, R.string.location_got) }
        } catch (e: SecurityException) {
            null
        }
        return result ?: LocationResult(null, R.string.location_none)
    }

    @SuppressLint("MissingPermission")
    private fun lastKnownLocation(providers: List<String>): Location? =
            mostAccurateLocation(providers.mapNotNull { locationManager.getLastKnownLocation(it) },
                    SystemClock.elapsedRealtimeNanos())

    private suspend fun freshLocation(providers: List<String>): Location? =
            firstFix(providers, TIMEOUT_S) { currentLocation(it) }

    @SuppressLint("MissingPermission")
    private suspend fun currentLocation(provider: String): Location? =
            suspendCancellableCoroutine { continuation ->
                val signal = CancellationSignal()
                continuation.invokeOnCancellation { signal.cancel() }
                locationManager.getCurrentLocation(provider, signal, app.mainExecutor) { location ->
                    continuation.resume(location)
                }
            }

    private fun deliverResult(result: LocationResult) {
        Toast.makeText(app, result.messageResId, LENGTH_LONG).show()
        result.location?.let { app.onLocation(it) }
    }

    private class LocationResult(val location: Location?, val messageResId: Int)

    companion object {
        private val MAX_TIME_LAST_KNOWN_NS = 30.seconds.inWholeNanoseconds
        private val TIMEOUT_S = 5.seconds

        internal suspend fun firstFix(
            providers: List<String>, timeout: Duration,
            get: suspend (String) -> Location?
        ): Location? = coroutineScope {
            val fix = CompletableDeferred<Location>()
            providers.forEach { provider ->
                launch { get(provider)?.let { fix.complete(it) } }
            }
            withTimeoutOrNull(timeout) { fix.await() }
        }

        internal fun mostAccurateLocation(locations: List<Location>, nowNs: Long,
                                          maxAgeNs: Long = MAX_TIME_LAST_KNOWN_NS): Location? =
                locations.filter { nowNs - it.elapsedRealtimeNanos <= maxAgeNs }
                        .minByOrNull { it.accuracy }
    }
}
