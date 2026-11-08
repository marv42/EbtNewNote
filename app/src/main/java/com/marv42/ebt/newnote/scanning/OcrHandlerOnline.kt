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
import com.marv42.ebt.newnote.HttpCaller
import com.marv42.ebt.newnote.exceptions.*
import com.marv42.ebt.newnote.exceptions.ErrorMessage.ERROR
import okhttp3.FormBody
import okhttp3.Request

class OcrHandlerOnline(callback: Callback, photoPath: String, photoUri: Uri?,
                       contentResolver: ContentResolver, private val apiKey: String) :
    OcrHandler(callback, photoPath, photoUri, contentResolver) {

    override fun doInBackground(): String {
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
            return toOcrResult(allResults)
        }

    @get:Throws(NoPictureException::class)
    private val formBody: FormBody
        get() {
            val base64Image = toBase64Picture(MAX_IMAGE_BYTES)
            val formBodyBuilder = FormBody.Builder()
            formBodyBuilder.add("apikey", apiKey)
            formBodyBuilder.add("base64Image", "data:image/png;base64,$base64Image")
            return formBodyBuilder.build()
        }

    companion object {
        private const val OCR_HOST = "api.ocr.space"
        private const val OCR_URL = "https://$OCR_HOST/parse/image"
        private const val WRONG_OCR_KEY_MESSAGE = "R.string.http_error $OCR_HOST, R.string.response_code: 403"
        private const val MAX_IMAGE_BYTES = 1024 * 1024.0
    }
}
