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
import com.marv42.ebt.newnote.exceptions.ErrorMessage
import com.marv42.ebt.newnote.exceptions.NoNotificationManagerException
import com.marv42.ebt.newnote.executeAsyncTask
import com.marv42.ebt.newnote.mainScope
import kotlinx.coroutines.Job

abstract class OcrHandler(private val callback: Callback, protected val photoPath: String,
                          protected val photoUri: Uri?, protected val contentResolver: ContentResolver) {

    private val scope = mainScope()
    private var job: Job? = null

    fun execute() {
        job?.cancel()
        job = scope.executeAsyncTask(
            doInBackground = {
                scan()
            }, onPostExecute = {
                deliverResult(it)
            }, onError = {
                deliverResult(OcrResult(ErrorMessage.ERROR + "R.string.ocr_error"))
            })
    }

    fun cancel() {
        job?.cancel()
        job = null
    }

    protected abstract fun scan(): OcrResult

    protected fun toBitmap(maxLongSide: Int): Bitmap =
        pictureConverter().toBitmap(maxLongSide)

    protected fun toOcrResult(allResults: String): String = TextProcessor().getOcrResult(allResults)

    private fun pictureConverter(): PictureConverter =
        PictureConverter.of(photoPath, contentResolver, photoUri)

    private fun deliverResult(result: OcrResult) {
        try {
            callback.onOcrResult(result)
        } catch (e: NoNotificationManagerException) {
            Log.w(javaClass.simpleName, e.message ?: "")
        }
    }

    interface Callback {
        fun onOcrResult(result: OcrResult)
    }
}
