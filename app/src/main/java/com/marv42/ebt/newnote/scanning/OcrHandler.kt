/*
 Copyright (c) 2010 - 2026 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */
package com.marv42.ebt.newnote.scanning

import android.content.ContentResolver
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import com.marv42.ebt.newnote.executeAsyncTask
import com.marv42.ebt.newnote.exceptions.NoNotificationManagerException
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope

abstract class OcrHandler(private val callback: Callback, protected val photoPath: String,
                          private val photoUri: Uri?, private val contentResolver: ContentResolver) {

    private val scope = MainScope()
    private var job: Job? = null

    fun execute() {
        job?.cancel()
        job = scope.executeAsyncTask(
            onPreExecute = {
            }, doInBackground = {
                doInBackground()
            }, onPostExecute = {
                onPostExecute(it)
            })
    }

    fun cancel() {
        job?.cancel()
        job = null
    }

    protected abstract fun doInBackground(): String

    protected fun toBitmap(maxLongSide: Int): Bitmap =
        pictureConverter().toBitmap(maxLongSide)

    protected fun toBase64Picture(targetSizeBytes: Double): String =
        pictureConverter().scaleAndEncodeToBase64(targetSizeBytes)

    protected fun toOcrResult(allResults: String): String = TextProcessor().getOcrResult(allResults)

    private fun pictureConverter(): PictureConverter =
        PictureConverter.of(photoPath, contentResolver, photoUri)

    private fun onPostExecute(result: String) {
        try {
            callback.onOcrResult(result)
        } catch (e: NoNotificationManagerException) {
            Log.w(javaClass.simpleName, e.message ?: "")
        }
    }

    interface Callback {
        fun onOcrResult(result: String)
    }
}
