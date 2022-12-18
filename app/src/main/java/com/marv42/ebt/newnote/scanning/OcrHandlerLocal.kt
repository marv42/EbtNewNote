/*
 Copyright (c) 2010 - 2022 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */
package com.marv42.ebt.newnote.scanning

import android.content.Context
import com.googlecode.tesseract.android.TessBaseAPI
import com.marv42.ebt.newnote.R
import com.marv42.ebt.newnote.exceptions.ErrorMessage
import com.marv42.ebt.newnote.exceptions.NoNotificationManagerException
import com.marv42.ebt.newnote.executeAsyncTask
import com.marv42.ebt.newnote.scanning.TextProcessor.NEW_LINE
import kotlinx.coroutines.MainScope
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class OcrHandlerLocal(private val callback: IOcrHandler.Callback, private val context: Context,
                      private val photoPath: String) {

    private val scope = MainScope()
    private var tess: TessBaseAPI? = null
    private var filesPath: String? = null

    fun execute() {
        scope.executeAsyncTask(
            onPreExecute = {
            }, doInBackground = {
                doInBackground()
            }, onPostExecute = {
                onPostExecute(it)
            })
    }

    private fun doInBackground(): String  {
        return if (!prepareOcr()) ErrorMessage.ERROR + context.getString(R.string.error_initializing_local_ocr)
        else doOcr(photoPath)
    }

    private fun prepareOcr(): Boolean {
        prepareTessData()
        tess = TessBaseAPI()
        if (tess!!.init(
                filesPath,
                TESS_DATA_LANGUAGE,
                TessBaseAPI.OEM_TESSERACT_LSTM_COMBINED
            )
        ) return true
        tess!!.recycle()
        return false
    }

    private fun prepareTessData() {
        filesPath = File(context.getExternalFilesDir(null), "").absolutePath
        val filesDir = filesPath?.let { File(it) }
        if (filesDir != null) {
            assert(filesDir.exists())
        }
        val tessDataPath = File(filesDir, TESS_DATA_DIR)
        if (tessDataPath.exists()) return else if (!tessDataPath.mkdir()) assert(tessDataPath.exists())
        copyTessData(tessDataPath)
    }

    private fun copyTessData(tessDataPath: File) {
        val inputFileName = TESS_DATA_DIR + File.separator + TRAINED_DATA_FILE
        val outputFile = File(tessDataPath, TRAINED_DATA_FILE)
        // https://github.com/adaptech-cz/Tesseract4Android/blob/master/sample/src/main/java/cz/adaptech/tesseract4android/sample/Assets.java
        val manager = context.assets
        try {
            manager.open(inputFileName).use { `in` ->
                FileOutputStream(outputFile).use { out ->
                    val buffer = ByteArray(1024)
                    var read: Int
                    while (`in`.read(buffer).also { read = it } != -1) out.write(buffer, 0, read)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun doOcr(photoPath: String): String {
        tess!!.setImage(File(photoPath))
        tess!!.getHOCRText(0)
        var allResults = tess!!.utF8Text
        tess!!.recycle()
        allResults = allResults.replace("\n", NEW_LINE)
        return TextProcessor().getOcrResult(allResults)
    }

    private fun onPostExecute(result: String) {
        try {
            callback.onOcrResult(result)
        } catch (e: NoNotificationManagerException) {
            e.printStackTrace()
        }
    }

    companion object {
        const val TESS_DATA_LANGUAGE = "eng"
        private const val TRAINED_DATA_FILE = "$TESS_DATA_LANGUAGE.traineddata"
        private const val TESS_DATA_DIR = "tessdata"
    }
}