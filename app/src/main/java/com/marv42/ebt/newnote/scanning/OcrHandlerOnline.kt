/*
 Copyright (c) 2010 - 2026 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */
package com.marv42.ebt.newnote.scanning

import android.content.ContentResolver
import android.net.Uri
import android.provider.MediaStore.MediaColumns
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import androidx.exifinterface.media.ExifInterface.*
import com.marv42.ebt.newnote.HttpCaller
import com.marv42.ebt.newnote.exceptions.*
import com.marv42.ebt.newnote.exceptions.ErrorMessage.ERROR
import com.marv42.ebt.newnote.executeAsyncTask
import kotlinx.coroutines.MainScope
import okhttp3.FormBody
import okhttp3.Request
import java.io.IOException

class OcrHandlerOnline(private val callback: IOcrHandler.Callback, private val photoPath: String,
                       private val photoUri: Uri?, private val contentResolver: ContentResolver,
                       private val apiKey: String) {

    private val TAG: String? = OcrHandlerOnline::class.java.getSimpleName()
    private val scope = MainScope()

    fun execute() {
        scope.executeAsyncTask(
           onPreExecute = {
        }, doInBackground = {
            doInBackground()
        }, onPostExecute = {
            onPostExecute(it)
        })
    }

    private fun doInBackground(): String {
        return try {
            result
        } catch (e: HttpCallException) {
            getHttpCallErrorMessage(e)
        } catch (e: CallResponseException) {
            ERROR + e.message
        } catch (e: NoPictureException) {
            ERROR + e.message
        } catch (e: OcrException) {
            ERROR + "R.string.ocr_error: " + e.message
        }
    }

    private fun getHttpCallErrorMessage(e: HttpCallException): String {
        var errorMessage = e.message
        if (errorMessage == null)
            errorMessage = "R.string.http_error"
        if (errorMessage == WRONG_OCR_KEY_MESSAGE)
            errorMessage += ".\n\nR.string.ocr_wrong_key."
        if (!errorMessage.startsWith("R.string.http_error"))
            errorMessage = "R.string.http_error: $errorMessage"
        return ERROR + errorMessage
    }

    @get:Throws(HttpCallException::class, CallResponseException::class, OcrException::class, NoPictureException::class)
    private val result: String
        get() {
            val formBody = formBody
            val request = Request.Builder().url(OCR_URL).post(formBody).build()
            val body = HttpCaller().call(request)
            val allResults = JsonAnalyzer.analyzeBody(body)
            return TextProcessor().getOcrResult(allResults)
        }

    @get:Throws(NoPictureException::class)
    private val formBody: FormBody
        get() {
            val base64Image = PictureConverter(photoPath, orientation).scaleAndEncodeToBase64(1024 * 1024.0)
            val formBodyBuilder = FormBody.Builder()
            formBodyBuilder.add("apikey", apiKey)
            formBodyBuilder.add("base64Image", "data:image/jpeg;base64,$base64Image")
            return formBodyBuilder.build()
        }

    private fun onPostExecute(result: String) {
        try {
            callback.onOcrResult(result)
        } catch (e: NoNotificationManagerException) {
            Log.w(TAG, e.message!!)
        }
    }

    private val orientation: Int
        get() = when {
            orientationFromExif != ORIENTATION_UNDEFINED -> orientationFromExif
            else -> orientationFromMediaStore
        }

    private val orientationFromMediaStore: Int
        get() = if (photoUri != null)
            orientationFromCursor
        else
            ORIENTATION_UNDEFINED

    private val orientationFromCursor: Int
        get() {
            val columns = arrayOf(MediaColumns.ORIENTATION)
            val cursor = contentResolver.query(photoUri!!, columns, null, null, null)
                ?: return ORIENTATION_UNDEFINED
            var orientation = ORIENTATION_UNDEFINED
            if (cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndex(columns[0])
                if (columnIndex >= 0)
                    orientation = cursor.getInt(columnIndex)
            }
            cursor.close()
            return orientation
        }

    // this is always 0 :-(
    private val orientationFromExif: Int
        get() =
            try {
                val exif = ExifInterface(photoPath)
                exif.getAttributeInt(TAG_ORIENTATION, ORIENTATION_NORMAL)
            } catch (e: IOException) {
                ORIENTATION_UNDEFINED
            }

    companion object {
        private const val OCR_HOST = "api.ocr.space"
        private const val OCR_URL = "https://$OCR_HOST/parse/image"
        private const val WRONG_OCR_KEY_MESSAGE = "R.string.http_error $OCR_HOST, R.string.response_code: 403"
    }
}