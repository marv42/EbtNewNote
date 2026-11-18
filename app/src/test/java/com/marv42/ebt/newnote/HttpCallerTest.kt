/*
 Copyright (c) 2010 - 2026 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HttpCallerTest {

    @Test
    fun extractsTheApiErrorMessage() {
        assertEquals("You have exceeded the monthly usage limit", HttpCaller.parseApiErrorMessage(
                """{"error": {"code": "usage_limit_reached", "message": "You have exceeded the monthly usage limit"}}"""))
    }

    @Test
    fun extractsATopLevelMessage() {
        assertEquals("Something went wrong",
                HttpCaller.parseApiErrorMessage("""{"message": "Something went wrong"}"""))
    }

    @Test
    fun flattensTheMessageToOneLine() {
        assertEquals("line one line two",
                HttpCaller.parseApiErrorMessage("""{"error": {"message": " line one\n line two "}}"""))
    }

    @Test
    fun ignoresABodyWithoutMessage() {
        assertNull(HttpCaller.parseApiErrorMessage("""{"success": false}"""))
    }

    @Test
    fun ignoresANonJsonBody() {
        assertNull(HttpCaller.parseApiErrorMessage("<html>Forbidden</html>"))
    }

    @Test
    fun ignoresAnEmptyBody() {
        assertNull(HttpCaller.parseApiErrorMessage(null))
        assertNull(HttpCaller.parseApiErrorMessage(""))
    }
}
