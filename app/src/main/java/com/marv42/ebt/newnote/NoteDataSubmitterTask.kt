/*
 Copyright (c) 2010 - 2026 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */
package com.marv42.ebt.newnote

import com.marv42.ebt.newnote.data.NoteData
import kotlinx.coroutines.Job
import javax.inject.Inject

class NoteDataSubmitterTask @Inject constructor(private val app: ThisApp, private val noteSubmitter: NoteSubmitter,
                                                private val callback: Callback) {

    interface Callback {
        fun onSubmissionResult(result: SubmissionResult?)
    }

    private val scope = mainScope()
    private var job: Job? = null

    fun execute(noteData: NoteData) {
        job?.cancel()
        job = scope.executeAsyncTask(
           doInBackground = {
            noteSubmitter.submit(noteData)
        }, onPostExecute = {
            deliverResult(it)
        }, onError = {
            deliverResult(SubmissionResult(noteData, app.getString(R.string.internal_error)))
        })
    }

    fun cancel() {
        job?.cancel()
        job = null
    }

    private fun deliverResult(result: SubmissionResult) {
        callback.onSubmissionResult(result)
    }
}
