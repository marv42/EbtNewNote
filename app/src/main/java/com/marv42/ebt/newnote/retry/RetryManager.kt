/*
 Copyright (c) 2010 - 2026 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */
package com.marv42.ebt.newnote.retry

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET
import android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED
import android.net.NetworkRequest
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.marv42.ebt.newnote.NoteSubmitter
import com.marv42.ebt.newnote.Notifications
import com.marv42.ebt.newnote.R
import com.marv42.ebt.newnote.SubmissionResult
import com.marv42.ebt.newnote.ThisApp
import com.marv42.ebt.newnote.data.NoteData
import com.marv42.ebt.newnote.exceptions.ErrorMessage
import com.marv42.ebt.newnote.exceptions.NoNotificationManagerException
import com.marv42.ebt.newnote.mainScope
import com.marv42.ebt.newnote.preferences.SharedPreferenceDataStore
import com.marv42.ebt.newnote.scanning.OcrResult
import com.marv42.ebt.newnote.scanning.OnlineOcr
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import java.util.concurrent.CopyOnWriteArraySet
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min
import kotlin.time.Duration.Companion.milliseconds

@Singleton
class RetryManager @Inject constructor(
    private val app: ThisApp,
    private val noteSubmitter: NoteSubmitter,
    private val onlineOcr: OnlineOcr,
    private val dataStore: SharedPreferenceDataStore
) {

    interface Listener {
        fun onRetryFinished()
    }

    private val scope: CoroutineScope = mainScope()
    private val store = RetryStore(app)
    private val jobs = mutableListOf<RetryJob>()
    private val listeners = CopyOnWriteArraySet<Listener>()
    private val wakeups = Channel<Unit>(Channel.CONFLATED)
    private var loop: Job? = null
    private var initialized = false
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    fun init() {
        synchronized(this) {
            if (initialized)
                return
            initialized = true
        }
        jobs.addAll(store.loadPending())
        networkCallback = registerNetworkCallback()
        if (jobs.isNotEmpty())
            ensureLoop()
    }

    fun addListener(listener: Listener) {
        listeners.add(listener)
    }

    fun removeListener(listener: Listener) {
        listeners.remove(listener)
    }

    fun scheduleSubmission(noteData: NoteData) {
        val job = RetryJob()
        job.id = UUID.randomUUID().toString()
        job.kind = RetryJob.KIND_SUBMISSION
        job.noteData = noteData
        schedule(job)
    }

    fun scheduleOcr(photoPath: String, photoUri: String?) {
        val job = RetryJob()
        job.id = UUID.randomUUID().toString()
        job.kind = RetryJob.KIND_OCR
        job.photoPath = photoPath
        job.photoUri = photoUri
        schedule(job)
    }

    private fun schedule(job: RetryJob) {
        job.nextAttemptAt = 0L
        jobs.add(job)
        persist()
        ensureLoop()
        wakeups.trySend(Unit)
    }

    fun drainCompletedOcr(): List<String> = store.drainCompletedOcr()

    fun cancelPendingOcr() {
        val removed = jobs.removeAll { it.kind == RetryJob.KIND_OCR }
        if (removed) {
            persist()
            wakeups.trySend(Unit)
        }
    }

    private fun ensureLoop() {
        if (loop?.isActive == true)
            return
        loop = scope.launch { runLoop() }
    }

    private suspend fun runLoop() {
        try {
            while (true) {
                val job = jobs.minByOrNull { it.nextAttemptAt } ?: break
                val wait = job.nextAttemptAt - System.currentTimeMillis()
                if (wait > 0) {
                    withTimeoutOrNull(wait.milliseconds) { wakeups.receive() }
                    continue
                }
                if (!hasValidatedNetwork()) {
                    wakeups.receive()
                    continue
                }
                execute(job)
            }
        } finally {
            loop = null
        }
    }

    private suspend fun execute(job: RetryJob) {
//        if (job.attempts == 0)
        Toast.makeText(app, R.string.trying_again, Toast.LENGTH_SHORT).show()
        job.attempts += 1
        persist()
        val outcome = withContext(Dispatchers.IO) { run(job) }
        if (!jobs.contains(job))
            return
        if (outcome.isRetryable() && !isRetryLimitReached(job)) {
            val message = outcome.submissionResult?.reason
                ?: ErrorMessage(app).getErrorMessage(outcome.ocrResult!!.text)
            Toast.makeText(app, message, Toast.LENGTH_LONG).show()
            job.nextAttemptAt = System.currentTimeMillis() + backoffMillis(job.attempts + 1)
            persist()
        } else
            finish(job, outcome)
    }

    private fun run(job: RetryJob): Outcome {
        return try {
            when (job.kind) {
                RetryJob.KIND_SUBMISSION -> runSubmission(job)
                else -> runOcr(job)
            }
        } catch (e: Exception) {
            Log.e(TAG, e.message ?: "", e)
            when (job.kind) {
                RetryJob.KIND_SUBMISSION -> Outcome(
                    submissionResult = SubmissionResult(
                        job.noteData ?: NoteData("", "", "", "", "", "", ""),
                        app.getString(R.string.internal_error)))
                else -> Outcome(ocrResult = OcrResult(ErrorMessage.ERROR + "R.string.internal_error"))
            }
        }
    }

    private fun runSubmission(job: RetryJob): Outcome {
        val noteData = job.noteData ?: return Outcome(skip = true)
        return Outcome(submissionResult = noteSubmitter.submit(noteData))
    }

    private fun runOcr(job: RetryJob): Outcome {
        val photoPath = job.photoPath ?: return Outcome(skip = true)
        return Outcome(ocrResult = onlineOcr.scan(photoPath, toUri(job.photoUri), app.contentResolver))
    }

    private fun toUri(photoUri: String?): Uri? {
        if (photoUri.isNullOrEmpty())
            return null
        return Uri.parse(photoUri)
    }

    private fun finish(job: RetryJob, outcome: Outcome) {
        jobs.remove(job)
        persist()
        if (outcome.skip)
            return
        val submissionResult = outcome.submissionResult
        if (submissionResult != null) {
            store.addCompletedSubmission(submissionResult)
            showSubmissionNotification(submissionResult)
        }
        val ocr = outcome.ocrResult
        if (ocr != null) {
            store.addCompletedOcr(ocr.text)
            showOcrNotification()
        }
        for (listener in listeners)
            listener.onRetryFinished()
    }

    private fun isRetryLimitReached(job: RetryJob): Boolean {
        val maxRetries = maxRetries()
        return maxRetries > 0 && job.attempts >= maxRetries
    }

    private fun maxRetries(): Int {
        val default = app.getString(R.string.max_retries_default)
        val value = dataStore.get(R.string.pref_settings_max_retries_key, default)
        return value.toIntOrNull() ?: default.toInt()
    }

    private fun backoffMillis(retryNumber: Int): Long {
        if (retryNumber <= 1)
            return 0L
        val exponent = (retryNumber - 2).coerceIn(0, 10)
        return min(MIN_DELAY_MS shl exponent, MAX_DELAY_MS)
    }

    private fun persist() {
        store.savePending(jobs)
    }

    private fun hasValidatedNetwork(): Boolean {
        val manager = app.getSystemService(ConnectivityManager::class.java) ?: return true
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NET_CAPABILITY_VALIDATED)
    }

    private fun registerNetworkCallback(): ConnectivityManager.NetworkCallback? {
        val manager = app.getSystemService(ConnectivityManager::class.java) ?: return null
        val request = NetworkRequest.Builder()
            .addCapability(NET_CAPABILITY_INTERNET)
            .addCapability(NET_CAPABILITY_VALIDATED)
            .build()
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                retryNow()
            }

            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                if (capabilities.hasCapability(NET_CAPABILITY_VALIDATED))
                    wakeups.trySend(Unit)
            }
        }
        return try {
            manager.registerNetworkCallback(request, callback)
            callback
        } catch (e: RuntimeException) {
            Log.w(TAG, e.message ?: "", e)
            null
        }
    }

    private fun retryNow() {
        var changed = false
        val now = System.currentTimeMillis()
        for (job in jobs) {
            if (job.nextAttemptAt > now) {
                job.nextAttemptAt = 0L
                changed = true
            }
        }
        if (changed)
            persist()
        wakeups.trySend(Unit)
    }

    private fun showSubmissionNotification(result: SubmissionResult) {
        try {
            Notifications.showSubmissionResult(app, arrayListOf(result))
        } catch (e: NoNotificationManagerException) {
            Log.w(TAG, e.message ?: "")
        }
    }

    private fun showOcrNotification() {
        try {
            Notifications.showOcrResult(app)
        } catch (e: NoNotificationManagerException) {
            Log.w(TAG, e.message ?: "")
        }
    }

    private class Outcome(
        val submissionResult: SubmissionResult? = null,
        val ocrResult: OcrResult? = null,
        val skip: Boolean = false
    ) {
        fun isRetryable(): Boolean =
            submissionResult?.isRetryable == true || ocrResult?.retryableConnectionFailure == true
    }

    companion object {
        private const val TAG = "RetryManager"
        private const val MIN_DELAY_MS = 10_000L
        private const val MAX_DELAY_MS = 900_000L
    }
}
