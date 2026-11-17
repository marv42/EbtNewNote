/*
 Copyright (c) 2010 - 2026 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.acra.ACRA

private const val TAG = "AsyncTask"

private fun reportError(t: Throwable) {
    Log.e(TAG, t.message, t)
    if (ACRA.isInitialised)
        ACRA.errorReporter.handleException(t)
}

private val errorHandler = CoroutineExceptionHandler { _, t -> reportError(t) }

fun mainScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main + errorHandler)

// https://stackoverflow.com/a/62748880/542235
fun <R> CoroutineScope.executeAsyncTask(
        onPreExecute: () -> Unit = {},
        doInBackground: suspend () -> R,
        onPostExecute: (R) -> Unit,
        onError: (Throwable) -> Unit = {}
): Job = launch {
    onPreExecute() // runs in main thread
    val result = try {
        withContext(Dispatchers.IO) {
            doInBackground() // runs in background thread without blocking the main thread
        }
    } catch (e: CancellationException) {
        throw e // cancelling is not an error, so never report it
    } catch (e: Throwable) {
        reportError(e)
        onError(e)
        return@launch
    }
    onPostExecute(result) // runs in main thread
}
