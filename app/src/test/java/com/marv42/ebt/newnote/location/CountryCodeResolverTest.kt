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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test

class CountryCodeResolverTest {

    @Test
    fun prefersRestCountries() {
        var countryLayerCalled = false
        val resolver = CountryCodeResolver(
            service { _, _ -> "Germany" },
            service { _, _ ->
                countryLayerCalled = true
                "Fallback"
            })

        assertEquals("Germany", resolver.convert("DEU", "rest-key", "layer-key"))
        assertFalse(countryLayerCalled)
    }

    @Test
    fun fallsBackToCountryLayer() {
        val resolver = CountryCodeResolver(
            service { _, _ -> throw CallResponseException("REST Countries failed") },
            service { _, _ -> "Germany" })

        assertEquals("Germany", resolver.convert("DEU", "rest-key", "layer-key"))
    }

    @Test
    fun skipsRestCountriesWithoutKey() {
        val resolver = CountryCodeResolver(
            service { _, _ -> throw AssertionError("REST Countries must not be called") },
            service { _, _ -> "Germany" })

        assertEquals("Germany", resolver.convert("DEU", "", "layer-key"))
    }

    @Test
    fun throwsTheLastError() {
        val resolver = CountryCodeResolver(
            service { _, _ -> throw CallResponseException("REST Countries failed") },
            service { _, _ -> throw CallResponseException("CountryLayer failed") })

        val exception = assertThrows(CallResponseException::class.java) {
            resolver.convert("DEU", "rest-key", "layer-key")
        }

        assertEquals("CountryLayer failed", exception.message)
    }

    @Test
    fun failsWithoutAnyKey() {
        val resolver = CountryCodeResolver(
            service { _, _ -> throw AssertionError("REST Countries must not be called") },
            service { _, _ -> throw AssertionError("CountryLayer must not be called") })

        assertThrows(CallResponseException::class.java) {
            resolver.convert("DEU", "", "")
        }
    }

    private fun service(block: (String, String) -> String): CountryCodeService =
        CountryCodeService { countryCode, apiKey -> block(countryCode, apiKey) }
}
