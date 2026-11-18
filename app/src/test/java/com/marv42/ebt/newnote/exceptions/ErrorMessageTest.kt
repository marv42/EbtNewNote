/*
 Copyright (c) 2010 - 2026 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote.exceptions

import android.app.Application
import com.marv42.ebt.newnote.R
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ErrorMessageTest {

    private val app: Application = RuntimeEnvironment.getApplication()

    @Test
    fun includesDescriptionReasonAndApiMessage() {
        val error = HttpCallException(
            "R.string.http_error api.countrylayer.com, R.string.response_code: 403 Forbidden",
            "api.countrylayer.com", 403,
            "Forbidden", "You have exceeded the monthly usage limit"
        )

        assertEquals("Country resolution failed\n" +
                "Got 'Forbidden' (403) calling api.countrylayer.com\n" +
                "You have exceeded the monthly usage limit",
                ErrorMessage(app).getHttpCallErrorMessage(error, R.string.location_no_country))
    }

    @Test
    fun worksWithoutDescriptionAndReason() {
        val error = HttpCallException(
            "R.string.http_error api.ocr.space, R.string.response_code: 403",
            "api.ocr.space", 403
        )

        assertEquals("Got an error (403) calling api.ocr.space",
                ErrorMessage(app).getHttpCallErrorMessage(error))
    }

    @Test
    fun prefixesTimeoutMessages() {
        val error = HttpCallException("R.string.error_no_connection api.example.com:\ntimeout")

        assertEquals("Insufficient internet connection contacting api.example.com:\ntimeout",
                ErrorMessage(app).getHttpCallErrorMessage(error))
    }

    @Test
    fun includesDescriptionForOtherMessages() {
        assertEquals("Country resolution failed\n" +
                "Unusable response from the server: no 'name' element",
                ErrorMessage(app).getErrorMessage(
                        "R.string.server_error: no 'name' element", R.string.location_no_country))
    }

    @Test
    fun splitsLinesIntoSeparateMessages() {
        val messages = ErrorMessage(app).getErrorMessages(
                "ERRORCountry resolution failed\n" +
                        "Got 'Forbidden' (403) calling api.countrylayer.com\n" +
                        "You have exceeded the monthly usage limit")

        assertEquals(listOf(
                "Country resolution failed",
                "Got 'Forbidden' (403) calling api.countrylayer.com",
                "You have exceeded the monthly usage limit"), messages)
    }

    @Test
    fun doesNotSplitAtColonsOrAbbreviations() {
        val messages = ErrorMessage(app).getErrorMessages("ERRORUse e.g. this: that")

        assertEquals(listOf("Use e.g. this: that"), messages)
    }

    @Test
    fun splitsNewlinesIntoSeparateMessages() {
        val messages = ErrorMessage(app).getErrorMessages("ERRORfirst line\n\nsecond line")

        assertEquals(listOf("first line", "second line"), messages)
    }

    @Test
    fun keepsShortMessageAsSingleToast() {
        val messages = ErrorMessage(app).getErrorMessages("ERRORJust one sentence.")

        assertEquals(listOf("Just one sentence."), messages)
    }
}
