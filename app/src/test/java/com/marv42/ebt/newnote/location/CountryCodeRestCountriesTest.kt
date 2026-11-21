/*
 Copyright (c) 2010 - 2026 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote.location

import com.marv42.ebt.newnote.exceptions.CallResponseException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CountryCodeRestCountriesTest {

    private val countryCode = CountryCodeRestCountries()

    @Test
    fun usesTheAlpha3Endpoint() {
        val request = CountryCodeRestCountries.request(" deu ", "rest-key")

        assertEquals(
            "https://api.restcountries.com/countries/v5/codes.alpha_3/DEU?response_fields=names.common",
            request.url.toString())
    }

    @Test
    fun sendsTheApiKeyAsBearerToken() {
        val request = CountryCodeRestCountries.request("DEU", "rest-key")

        assertEquals("Bearer rest-key", request.header("Authorization"))
    }

    @Test
    fun extractsTheCommonName() {
        val body = """{"data":{"objects":[{"names":{"common":"Germany"}}]}}"""

        assertEquals("Germany", countryCode.extractName(body))
    }

    @Test
    fun failsWithoutObjects() {
        assertThrows(CallResponseException::class.java) {
            countryCode.extractName("""{"data":{"objects":[]}}""")
        }
    }

    @Test
    fun failsWithoutNames() {
        assertThrows(CallResponseException::class.java) {
            countryCode.extractName("""{"data":{"objects":[{"codes":{"alpha_3":"DEU"}}]}}""")
        }
    }

    @Test
    fun failsWithoutData() {
        assertThrows(CallResponseException::class.java) {
            countryCode.extractName("""{"objects":[]}""")
        }
    }

    @Test
    fun failsOnInvalidJson() {
        assertThrows(CallResponseException::class.java) {
            countryCode.extractName("<html>Not JSON</html>")
        }
    }
}
