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
import android.util.Log
import com.googlecode.tesseract.android.TessBaseAPI
import com.marv42.ebt.newnote.R
import com.marv42.ebt.newnote.exceptions.ErrorMessage
import com.marv42.ebt.newnote.exceptions.NoNotificationManagerException
import com.marv42.ebt.newnote.executeAsyncTask
import com.marv42.ebt.newnote.scanning.TextProcessor.NEW_LINE
import kotlinx.coroutines.MainScope
import java.io.File
import java.io.File.separator
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipFile

class OcrHandlerLocal(private val callback: IOcrHandler.Callback, private val context: Context,
                      private val photoPath: String, private val photoUri: Uri?,
                      private val contentResolver: ContentResolver) {

    private val TAG: String? = OcrHandlerLocal::class.java.getSimpleName()
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

    private fun doInBackground(): String {
        return if (!prepareOcr()) ErrorMessage.ERROR + context.getString(R.string.error_initializing_local_ocr)
        else doOcr()
    }

    private fun prepareOcr(): Boolean {
        if (!prepareTessData())
            return false
        tess = TessBaseAPI()
        if (tess!!.init(
                filesPath,
                TESS_DATA_LANGUAGE,
                TessBaseAPI.OEM_LSTM_ONLY)) {
            // https://pyimagesearch.com/2021/11/15/tesseract-page-segmentation-modes-psms-explained-how-to-improve-your-ocr-accuracy/
            tess!!.pageSegMode = TessBaseAPI.PageSegMode.PSM_SPARSE_TEXT
            setVariables()
            return true
        }
        tess!!.recycle()
        return false
    }

    private fun setVariables() {
        if (!tess!!.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, CHAR_WHITELIST))
            Log.w(TAG, "Character whitelist rejected")
        tess!!.setVariable("load_system_dawg", "0")
        tess!!.setVariable("load_freq_dawg", "0")
    }

    private fun prepareTessData() : Boolean {
        filesPath = File(context.getExternalFilesDir(null), "").absolutePath
        val tessDataDir = tessDataDir()
        if (!tessDataDir.isDirectory)
            if (!tessDataDir.mkdirs())
                return false
        val outputFile = outputFile()
        if (outputFile.length() == assetTrainedDataSize())
            return true
        return copyTessData()
    }

    private fun tessDataDir(): File = File(filesPath, TESS_DATA_DIR)

    private fun outputFile(): File = File(tessDataDir(), TRAINED_DATA_FILE)

    private fun assetTrainedDataSize(): Long =
        ZipFile(context.applicationInfo.sourceDir).use { zip ->
            zip.getEntry("assets/$TESS_DATA_DIR/$TRAINED_DATA_FILE")?.size ?: -1L
        }
//        context.assets.openFd(TRAINED_DATA_FILE).use { it.length }

    private fun copyTessData() : Boolean {
        val tmpFile = File(tessDataDir(), TRAINED_DATA_FILE + TMP_SUFFIX)
        // https://github.com/adaptech-cz/Tesseract4Android/blob/master/sample/src/main/java/cz/adaptech/tesseract4android/sample/Assets.java
        val manager = context.assets
        try {
            val inputFileName = TESS_DATA_DIR + separator + TRAINED_DATA_FILE
            manager.open(inputFileName).use { input ->
                FileOutputStream(tmpFile).use { output ->
                    val buffer = ByteArray(1024)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) output.write(buffer, 0, read)
                }
            }
            val outputFile = outputFile()
            if (!tmpFile.renameTo(outputFile)) {
                outputFile.delete()
                if (!tmpFile.renameTo(outputFile))
                    throw IOException("Could not rename $tmpFile to $outputFile")
            }
        } catch (e: IOException) {
            tmpFile.delete()
            Log.w(TAG, e.message!!)
            return false
        }
        return true
    }

    private fun doOcr(): String {
        val orientation = ImageOrientation.of(photoPath, contentResolver, photoUri)
        val bitmap = ImagePreprocessor.grayscaleWithLevelStretch(
            PictureConverter(photoPath, orientation).toBitmap(MAX_LONG_SIDE)
        )
        tess!!.setImage(bitmap)
        var allResults = tess!!.utF8Text
        tess!!.recycle()
        bitmap.recycle()
        if (allResults == null)
            return ErrorMessage.ERROR + context.getString(R.string.ocr_dialog_empty)
        allResults = allResults.replace("\r\n", NEW_LINE)
        return TextProcessor().getOcrResult(allResults)
    }

    private fun onPostExecute(result: String) {
        try {
            callback.onOcrResult(result)
        } catch (e: NoNotificationManagerException) {
            Log.w(TAG, e.message!!)
        }
    }

    companion object {
        private const val TESS_DATA_LANGUAGE = "eng"
        private const val TRAINED_DATA_FILE = "$TESS_DATA_LANGUAGE.traineddata"
        private const val TESS_DATA_DIR = "tessdata"
        private const val TMP_SUFFIX = ".tmp"
        private const val CHAR_WHITELIST = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        private const val MAX_LONG_SIDE = 2048
    }
}
