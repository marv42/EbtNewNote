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
import com.marv42.ebt.newnote.exceptions.CallResponseException
import okhttp3.FormBody
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class AddressTaskTest {

    @Test
    fun putsLongitudeBeforeLatitude() {
        val body = AddressTask.formBody(location(latitude = 52.5, longitude = 13.4))

        assertEquals("13.4,52.5", body.parameter("location"))
    }

    @Test
    fun asksForJsonInEnglish() {
        val body = AddressTask.formBody(location(latitude = 52.5, longitude = 13.4))

        assertEquals("json", body.parameter("f"))
        assertEquals("EN", body.parameter("langCode"))
    }

    @Test
    fun returnsTheAddressElement() {
        val address = AddressTask.addressElement(
                JSONObject("""{"address": {"City": "Berlin", "Postal": "10115"}}"""))

        assertEquals("Berlin", address.optString("City"))
        assertEquals("10115", address.optString("Postal"))
    }

    @Test
    fun failsWithoutAddressElement() {
        assertThrows(CallResponseException::class.java) {
            AddressTask.addressElement(JSONObject("""{"notAddress": {}}"""))
        }
    }

    @Test
    fun failsOnNullAddressElement() {
        assertThrows(CallResponseException::class.java) {
            AddressTask.addressElement(JSONObject("""{"address": null}"""))
        }
    }

    private fun location(latitude: Double, longitude: Double): Location =
            Location("gps").apply {
                this.latitude = latitude
                this.longitude = longitude
            }

    private fun FormBody.parameter(key: String): String? =
            (0 until size).firstOrNull { name(it) == key }?.let { value(it) }
}
