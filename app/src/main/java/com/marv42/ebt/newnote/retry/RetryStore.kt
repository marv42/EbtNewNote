/*
 Copyright (c) 2010 - 2026 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */
package com.marv42.ebt.newnote.retry

import androidx.preference.PreferenceManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.marv42.ebt.newnote.R
import com.marv42.ebt.newnote.SubmissionResult
import com.marv42.ebt.newnote.ThisApp
import javax.inject.Inject
import androidx.core.content.edit

class RetryStore @Inject constructor(private val app: ThisApp) {

    private val gson = Gson()
    private val preferences = PreferenceManager.getDefaultSharedPreferences(app)
    private val pendingKey = app.getString(R.string.pref_retry_pending_key)
    private val completedSubmissionsKey = app.getString(R.string.pref_retry_completed_submissions_key)
    private val completedOcrKey = app.getString(R.string.pref_retry_completed_ocr_key)
    private val jobListType = object : TypeToken<ArrayList<RetryJob>>() {}.type
    private val submissionListType = object : TypeToken<ArrayList<SubmissionResult>>() {}.type
    private val stringListType = object : TypeToken<ArrayList<String>>() {}.type

    fun loadPending(): MutableList<RetryJob> {
        val json = preferences.getString(pendingKey, null)
        if (json.isNullOrEmpty()) return mutableListOf()
        return try {
            gson.fromJson<ArrayList<RetryJob>>(json, jobListType) ?: mutableListOf()
        } catch (e: RuntimeException) {
            mutableListOf()
        }
    }

    fun savePending(jobs: List<RetryJob>) {
        preferences.edit { putString(pendingKey, gson.toJson(jobs)) }
    }

    private fun loadCompletedSubmissions(): MutableList<SubmissionResult> {
        val json = preferences.getString(completedSubmissionsKey, null)
        if (json.isNullOrEmpty()) return mutableListOf()
        return try {
            gson.fromJson<ArrayList<SubmissionResult>>(json, submissionListType) ?: mutableListOf()
        } catch (e: RuntimeException) {
            mutableListOf()
        }
    }

    fun addCompletedSubmission(result: SubmissionResult) {
        val completed = loadCompletedSubmissions()
        completed.add(result)
        preferences.edit { putString(completedSubmissionsKey, gson.toJson(completed)) }
    }

    fun drainCompletedSubmissions(): MutableList<SubmissionResult> {
        val completed = loadCompletedSubmissions()
        if (completed.isNotEmpty())
            preferences.edit { remove(completedSubmissionsKey) }
        return completed
    }

    private fun loadCompletedOcr(): MutableList<String> {
        val json = preferences.getString(completedOcrKey, null)
        if (json.isNullOrEmpty()) return mutableListOf()
        return try {
            gson.fromJson<ArrayList<String>>(json, stringListType) ?: mutableListOf()
        } catch (e: RuntimeException) {
            mutableListOf()
        }
    }

    fun addCompletedOcr(text: String) {
        val completed = loadCompletedOcr()
        completed.add(text)
        preferences.edit { putString(completedOcrKey, gson.toJson(completed)) }
    }

    fun drainCompletedOcr(): MutableList<String> {
        val completed = loadCompletedOcr()
        if (completed.isNotEmpty())
            preferences.edit { remove(completedOcrKey) }
        return completed
    }
}
