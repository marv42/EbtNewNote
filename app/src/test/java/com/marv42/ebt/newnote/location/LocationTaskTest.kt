/*
 Copyright (c) 2010 - 2026 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote.location

import android.app.Application
import android.location.Location
import com.marv42.ebt.newnote.Utils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class LocationTaskTest {

    private val now = 1_000_000_000_000L

    @Test
    fun prefersTheSmallerAccuracy() {
        val coarse = location("network", accuracy = 100f, ageNanos = 5L * Utils.SECONDS_IN_NANOSECONDS)
        val fine = location("gps", accuracy = 10f, ageNanos = 5L * Utils.SECONDS_IN_NANOSECONDS)

        assertEquals(fine, LocationTask.mostAccurateLocation(listOf(coarse, fine), now))
    }

    @Test
    fun ignoresStaleLocations() {
        val stale = location("gps", accuracy = 5f, ageNanos = 600L * Utils.SECONDS_IN_NANOSECONDS)
        val fresh = location("network", accuracy = 100f, ageNanos = 5L * Utils.SECONDS_IN_NANOSECONDS)

        assertEquals(fresh, LocationTask.mostAccurateLocation(listOf(stale, fresh), now))
    }

    @Test
    fun returnsNullWithoutFreshLocations() {
        assertNull(LocationTask.mostAccurateLocation(emptyList(), now))
        assertNull(LocationTask.mostAccurateLocation(
                listOf(location("gps", accuracy = 5f, ageNanos = 600L * Utils.SECONDS_IN_NANOSECONDS)),
                now))
    }

    @Test
    fun ignoresAProviderWithoutFix() = runBlocking {
        val fix = location("gps", accuracy = 10f, ageNanos = 0)

        val result = LocationTask.firstFix(listOf("network", "gps"), TIMEOUT_S) { provider ->
            if (provider == "network") null else delayAndReturn(FIX_DELAY_MS, fix)
        }

        assertEquals(fix, result)
    }

    @Test
    fun returnsNullIfNoProviderDeliversAFix() = runBlocking {
        val result = LocationTask.firstFix(listOf("network", "gps"), FIX_DELAY_MS) { null }

        assertNull(result)
    }

    @Test
    fun takesTheFirstFix() = runBlocking {
        val slow = location("gps", accuracy = 10f, ageNanos = 0)
        val fast = location("network", accuracy = 100f, ageNanos = 0)

        val result = LocationTask.firstFix(listOf("gps", "network"), TIMEOUT_S) { provider ->
            if (provider == "gps") delayAndReturn(FIX_DELAY_MS, slow)
            else delayAndReturn(FIX_DELAY_MS / 10, fast)
        }

        assertEquals(fast, result)
    }

    private suspend fun delayAndReturn(duration: Duration, location: Location): Location {
        delay(duration)
        return location
    }

    private fun location(provider: String, accuracy: Float, ageNanos: Long): Location =
            Location(provider).apply {
                this.accuracy = accuracy
                elapsedRealtimeNanos = now - ageNanos
            }

    private companion object {
        private val TIMEOUT_S = 5.seconds
        private val FIX_DELAY_MS = 200.milliseconds
    }
}
