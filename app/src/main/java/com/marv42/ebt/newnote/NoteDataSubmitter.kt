/*
 Copyright (c) 2010 - 2020 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */
package com.marv42.ebt.newnote

import android.text.TextUtils
import androidx.core.util.Pair
import com.marv42.ebt.newnote.data.LoginInfo
import com.marv42.ebt.newnote.data.NoteData
import com.marv42.ebt.newnote.data.NoteInsertionData
import com.marv42.ebt.newnote.exceptions.CallResponseException
import com.marv42.ebt.newnote.exceptions.ErrorMessage
import com.marv42.ebt.newnote.exceptions.HttpCallException
import kotlinx.coroutines.MainScope
import java.util.*
import javax.inject.Inject

class NoteDataSubmitter @Inject constructor(private val app: ThisApp, private val apiCaller: ApiCaller,
                                            private val callback: Callback) {

    private val scope = MainScope()

    fun execute(noteData: NoteData) {
        scope.executeAsyncTask(
           onPreExecute = {
        }, doInBackground = {
            doInBackground(noteData)
        }, onPostExecute = {
            onPostExecute(it)
        })
    }

    private fun doInBackground(vararg noteDatas: NoteData): SubmissionResult {
        val noteData = noteDatas[0]
        return try {
            callLoginAndInsert(noteData)
        } catch (e: HttpCallException) {
            SubmissionResult(noteData, ErrorMessage(app).getErrorMessage(e.message))
        } catch (e: CallResponseException) {
            SubmissionResult(noteData, ErrorMessage(app).getErrorMessage(e.message))
        }
    }

    @Throws(HttpCallException::class, CallResponseException::class)
    private fun callLoginAndInsert(noteData: NoteData): SubmissionResult {
        val loginInfo = apiCaller.callLogin()
        if (loginInfo.sessionId.isEmpty())
            return SubmissionResult(noteData, app.getString(R.string.wrong_login_info))
        val params = getInsertionParams(noteData, loginInfo)
        val insertionData = apiCaller.callInsertBills(params)
        return assembleReply(noteData, insertionData)
    }

    private fun getInsertionParams(noteData: NoteData, loginInfo: LoginInfo): List<Pair<String, String>> {
        val params: MutableList<Pair<String, String>> = ArrayList()
        params.add(Pair("m", "insertbills"))
        params.add(Pair("v", "1"))
        params.add(Pair("PHPSESSID", loginInfo.sessionId))
        params.add(Pair("city", noteData.mCity))
        params.add(Pair("zip", noteData.mPostalCode))
        params.add(Pair("country", noteData.mCountry))
        params.add(Pair("serial0", noteData.mSerialNumber))
        params.add(Pair("denomination0", getDenominationValue(noteData.mDenomination)))
        params.add(Pair("shortcode0", noteData.mShortCode))
        params.add(Pair("comment0", noteData.mComment))
        return params
    }

    private fun getDenominationValue(denomination: String): String {
        return denomination.substring(0, denomination.length - 2)
    }

    private fun assembleReply(noteData: NoteData, insertionData: NoteInsertionData): SubmissionResult {
        val billId = insertionData.billId
        val status = insertionData.status
        if (status == 0)
            return SubmissionResult(noteData, app.getString(R.string.has_been_entered), billId)
        if (status == 1)
            return SubmissionResult(noteData, app.getString(R.string.got_hit), billId)
        var reply = ""
        if (status and 64 != 0)
            reply += app.getString(R.string.already_entered) + "<br>"
        if (status and 128 != 0)
            reply += app.getString(R.string.already_entered_serial_number) + "<br>"
        if (status and 4 != 0)
            reply += app.getString(R.string.invalid_country) + "<br>"
        if (status and 32 != 0)
            reply += app.getString(R.string.city_missing) + "<br>"
        if (status and 2 != 0)
            reply += app.getString(R.string.invalid_denomination) + "<br>" // ;-)
        if (status and 16 != 0)
            reply += app.getString(R.string.invalid_short_code) + "<br>"
        if (status and 8 != 0)
            reply += app.getString(R.string.invalid_serial_number) + "<br>"
        if (status and 32768 != 0)
            reply += app.getString(R.string.inconsistent) + "<br>"
        if (reply.endsWith("<br>"))
            reply = reply.substring(0, reply.length - 4)
        if (TextUtils.isEmpty(reply))
            reply = "Someone seems to have to debug something here..."
        return SubmissionResult(noteData, reply, billId)
    }

    private fun onPostExecute(result: SubmissionResult) {
        callback.onSubmissionResult(result)
    }

    interface Callback {
        fun onSubmissionResult(result: SubmissionResult?)
    }
}