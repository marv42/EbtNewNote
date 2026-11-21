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
import com.marv42.ebt.newnote.R
import com.marv42.ebt.newnote.ThisApp
import com.marv42.ebt.newnote.exceptions.CallResponseException
import com.marv42.ebt.newnote.exceptions.ErrorMessage
import com.marv42.ebt.newnote.exceptions.ErrorMessage.ERROR
import com.marv42.ebt.newnote.exceptions.HttpCallException
import com.marv42.ebt.newnote.exceptions.NoPictureException
import com.marv42.ebt.newnote.exceptions.OcrException
import com.marv42.ebt.newnote.preferences.SharedPreferenceDataStore
import okhttp3.FormBody
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OnlineOcr @Inject constructor(
    private val app: ThisApp,
    private val dataStore: SharedPreferenceDataStore
) {

    fun scan(photoPath: String, photoUri: Uri?, contentResolver: ContentResolver): OcrResult {
        return try {
            OcrResult(result(photoPath, photoUri, contentResolver))
        } catch (e: HttpCallException) {
            OcrResult(httpCallErrorMessage(e), e.retryable)
        } catch (e: CallResponseException) {
            OcrResult(ERROR + e.message)
        } catch (e: NoPictureException) {
            OcrResult(ERROR + e.message)
        } catch (e: OcrException) {
            OcrResult(ERROR + "R.string.ocr_error: " + e.message)
        }
    }

    private fun httpCallErrorMessage(e: HttpCallException): String {
        var errorMessage = ErrorMessage(app).getHttpCallErrorMessage(e)
        if (e.responseCode == FORBIDDEN)
            errorMessage += "\n\n" + app.getString(R.string.ocr_wrong_key) + "."
        return ERROR + errorMessage
    }

    @Throws(HttpCallException::class, CallResponseException::class, OcrException::class, NoPictureException::class)
    private fun result(photoPath: String, photoUri: Uri?, contentResolver: ContentResolver): String {
        val formBody = formBody(photoPath, photoUri, contentResolver)
        val request = Request.Builder().url(OCR_URL).post(formBody).build()
        val body = HttpCaller().call(request)
        val allResults = JsonAnalyzer.analyzeBody(body)
        return TextProcessor().getOcrResult(allResults)
    }

    @Throws(NoPictureException::class)
    private fun formBody(photoPath: String, photoUri: Uri?, contentResolver: ContentResolver): FormBody {
        val base64Image = PictureConverter.of(photoPath, contentResolver, photoUri)
            .scaleAndEncodeToBase64(MAX_IMAGE_BYTES)
        val formBodyBuilder = FormBody.Builder()
        formBodyBuilder.add("apikey", apiKey)
        formBodyBuilder.add("base64Image", "data:image/png;base64,$base64Image")
        return formBodyBuilder.build()
    }

    private val apiKey: String
        get() = dataStore.get(R.string.pref_settings_ocr_service_key_key, "")

    companion object {
        private const val OCR_HOST = "api.ocr.space"
        private const val OCR_URL = "https://$OCR_HOST/parse/image"
        private const val FORBIDDEN = 403
        private const val MAX_IMAGE_BYTES = 1024 * 1024.0
    }
}
