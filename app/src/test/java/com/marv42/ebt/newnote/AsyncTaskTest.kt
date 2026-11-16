/*
 Copyright (c) 2010 - 2026 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote

import android.app.Application
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class AsyncTaskTest {

    @Test
    fun deliversErrorToOnError() {
        val error = AtomicReference<Throwable>()
        val delivered = CountDownLatch(1)

        mainScope().executeAsyncTask<String>(
            doInBackground = {
                throw IllegalStateException("boom")
            }, onPostExecute = {
                fail("onPostExecute must not run after a failure")
            }, onError = {
                error.set(it)
                delivered.countDown()
            })

        runMainTasksUntil(delivered)

        assertEquals("boom", error.get()?.message)
    }

    // Cancelling is not a failure: onError must stay silent, otherwise every
    // cancel() would show an error to the user.
    @Test
    fun cancelDoesNotReportError() {
        val gate = CountDownLatch(1)
        val started = CountDownLatch(1)
        val error = AtomicReference<Throwable>()

        val job = mainScope().executeAsyncTask(
            doInBackground = {
                started.countDown()
                gate.await()
                "result"
            }, onPostExecute = {
            }, onError = {
                error.set(it)
            })

        runMainTasksUntil(started)
        job.cancel()
        gate.countDown()
        pumpMainLooper(500)

        assertNull(error.get())
    }

    private fun pumpMainLooper(millis: Long) {
        val deadline = System.currentTimeMillis() + millis
        while (System.currentTimeMillis() < deadline) {
            ShadowLooper.shadowMainLooper().idle()
            Thread.sleep(10)
        }
    }

    // The work runs on a background thread, the callbacks are posted to the main looper,
    // which Robolectric does not execute on its own.
    private fun runMainTasksUntil(latch: CountDownLatch) {
        val deadline = System.currentTimeMillis() + 10_000
        while (!latch.await(50, TimeUnit.MILLISECONDS)) {
            if (System.currentTimeMillis() > deadline)
                fail("Latch was not counted down")
            ShadowLooper.shadowMainLooper().idle()
        }
    }
}
