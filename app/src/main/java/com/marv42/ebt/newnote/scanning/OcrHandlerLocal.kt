/*
 Copyright (c) 2010 - 2026 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */
package com.marv42.ebt.newnote.scanning

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import androidx.exifinterface.media.ExifInterface.ORIENTATION_UNDEFINED
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
        else doOcr(photoPath)
    }

    private fun prepareOcr(): Boolean {
        prepareTessData()
        tess = TessBaseAPI()
        if (tess!!.init(
                filesPath,
                TESS_DATA_LANGUAGE,
                TessBaseAPI.OEM_TESSERACT_LSTM_COMBINED)) {
            // https://pyimagesearch.com/2021/11/15/tesseract-page-segmentation-modes-psms-explained-how-to-improve-your-ocr-accuracy/
            tess!!.pageSegMode = TessBaseAPI.PageSegMode.PSM_AUTO
            return true
        }
        tess!!.recycle()
        return false
    }

    private fun prepareTessData() {
        filesPath = File(context.getExternalFilesDir(null), "").absolutePath
        val tessDataPath = getTessDataPath()
        if (!tessDataPath.exists() && !tessDataPath.mkdir())
            assert(tessDataPath.exists())
        val trainedDataFile = File(tessDataPath, TRAINED_DATA_FILE)
        if (!trainedDataFile.exists() || readVersion() != TRAINED_DATA_VERSION)
            copyTessData()
    }

    private fun getTessDataPath(): File = File(filesPath, TESS_DATA_DIR)

    private fun readVersion(): String? =
        try {
            val versionFile = getVersionFile()
            if (versionFile.exists()) versionFile.readText() else null
        } catch (e: IOException) {
            Log.w(TAG, e.message!!)
            null
        }

    private fun getVersionFile(): File = File(getTessDataPath(), VERSION_FILE)

    private fun copyTessData() {
        val inputFileName = TESS_DATA_DIR + File.separator + TRAINED_DATA_FILE
        val tessDataPath = getTessDataPath()
        val tmpFile = File(tessDataPath, TRAINED_DATA_FILE + TMP_SUFFIX)
        val outputFile = File(tessDataPath, TRAINED_DATA_FILE)
        // https://github.com/adaptech-cz/Tesseract4Android/blob/master/sample/src/main/java/cz/adaptech/tesseract4android/sample/Assets.java
        val manager = context.assets
        try {
            manager.open(inputFileName).use { input ->
                FileOutputStream(tmpFile).use { output ->
                    val buffer = ByteArray(1024)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) output.write(buffer, 0, read)
                }
            }
            if (!tmpFile.renameTo(outputFile)) {
                outputFile.delete()
                if (!tmpFile.renameTo(outputFile))
                    throw IOException("Could not rename $tmpFile to $outputFile")
            }
            val versionFile = getVersionFile()
            versionFile.writeText(TRAINED_DATA_VERSION)
        } catch (e: IOException) {
            tmpFile.delete()
            Log.w(TAG, e.message!!)
        }
    }

    private fun doOcr(photoPath: String): String {
        val image: ByteArray = PictureConverter(photoPath, ORIENTATION_UNDEFINED).convertAndScale(1024 * 1024.0)
        val bitmap = BitmapFactory.decodeByteArray(image, 0, image.size)
        tess!!.setImage(bitmap)
        setAllowInterruption()
        var allResults = tess!!.utF8Text
        tess!!.recycle()
        allResults = allResults.replace("\r\n", NEW_LINE)
        return TextProcessor().getOcrResult(allResults)
    }

    private fun setAllowInterruption() {
        tess!!.getHOCRText(0)
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
        private const val VERSION_FILE = ".model_version"
        private const val TRAINED_DATA_VERSION = "combined-20170629" // "best-20170629"
        private const val TMP_SUFFIX = ".tmp"
    }
}
