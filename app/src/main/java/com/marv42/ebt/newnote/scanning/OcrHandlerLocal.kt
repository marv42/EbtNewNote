/*
 Copyright (c) 2010 - 2026 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */
package com.marv42.ebt.newnote.scanning

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.googlecode.tesseract.android.TessBaseAPI
import com.marv42.ebt.newnote.R
import com.marv42.ebt.newnote.exceptions.ErrorMessage
import com.marv42.ebt.newnote.scanning.TextProcessor.NEW_LINE

class OcrHandlerLocal(callback: Callback, private val context: Context,
                      photoPath: String, photoUri: Uri?, contentResolver: ContentResolver) :
    OcrHandler(callback, photoPath, photoUri, contentResolver) {

    override fun doInBackground(): String {
        val filesPath = TessData.install(context) ?: return initializationError()
        val localOcr = LocalOcr(filesPath)
        if (!localOcr.isInitialized) {
            localOcr.recycle()
            return initializationError()
        }
        return try {
            doOcr(localOcr)
        } finally {
            localOcr.recycle()
        }
    }

    private fun initializationError(): String =
        ErrorMessage.ERROR + context.getString(R.string.error_initializing_local_ocr)

    private fun doOcr(localOcr: LocalOcr): String {
        val bitmap = ImagePreprocessor.grayscaleWithLevelStretch(toBitmap(MAX_LONG_SIDE))
        val allResults = try {
            localOcr.recognize(bitmap, PAGE_SEG_MODE)
        } finally {
            bitmap.recycle()
        } ?: return ErrorMessage.ERROR + context.getString(R.string.ocr_dialog_empty)
        return toOcrResult(allResults.replace("\r\n", NEW_LINE))
    }

    companion object {
        const val MAX_LONG_SIDE = 2048
        const val PAGE_SEG_MODE = TessBaseAPI.PageSegMode.PSM_SPARSE_TEXT
    }
}
