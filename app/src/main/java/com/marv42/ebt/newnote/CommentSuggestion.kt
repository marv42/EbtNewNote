/*
 Copyright (c) 2010 - 2022 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */
package com.marv42.ebt.newnote

import android.content.Context
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import androidx.core.util.Pair
import com.marv42.ebt.newnote.data.LocationValues
import com.marv42.ebt.newnote.exceptions.CallResponseException
import com.marv42.ebt.newnote.exceptions.ErrorMessage
import com.marv42.ebt.newnote.exceptions.HttpCallException
import com.marv42.ebt.newnote.preferences.EncryptedPreferenceDataStore
import kotlinx.coroutines.MainScope
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.*

internal class CommentSuggestion(private val apiCaller: ApiCaller, private val callback: Callback, dataStore: EncryptedPreferenceDataStore) {

    private val additionalComment: String = dataStore.get(R.string.pref_settings_comment_key, "")
            .replace("\u00a0", " ")
    private val scope = MainScope()

    fun execute(locationValues: LocationValues) {
        scope.executeAsyncTask(
           onPreExecute = {
        }, doInBackground = {
            doInBackground(locationValues)
        }, onPostExecute = {
            onPostExecute(it)
        })
    }

    private fun doInBackground(locationValues: LocationValues): Array<String?>? {
        return try {
            getSuggestions(locationValues)
        } catch (e: CallResponseException) {
            getErrorStrings(e.message)
        }
    }

    @Throws(CallResponseException::class)
    private fun getSuggestions(locationValues: LocationValues): Array<String?> {
        val json = getJson(locationValues)
        val allSuggestions = json.optJSONArray(DATA_ELEMENT)
        var uniques = getUniquesWrtAdditionalComments(allSuggestions)
        uniques = ArrayList(LinkedHashSet(uniques))
        return uniques.toTypedArray()
    }

    @Throws(CallResponseException::class)
    private fun getJson(locationValues: LocationValues): JSONObject {
        return try {
            val loginInfo = apiCaller.callLogin()
            val params = getLocationParams(loginInfo.sessionId, locationValues)
            val body = apiCaller.callMyComments(params)
            val json = JSONObject(body)
            if (!json.has(DATA_ELEMENT))
                throw CallResponseException("R.string.server_error: no '$DATA_ELEMENT' element")
            json
        } catch (e: HttpCallException) {
            throw CallResponseException(e.message)
        } catch (e: CallResponseException) {
            throw CallResponseException(e.message)
        } catch (e: JSONException) {
            throw CallResponseException(e.message)
        }
    }

    private fun getUniquesWrtAdditionalComments(allSuggestions: JSONArray?): List<String?> {
        val suggestions = getJsonList(allSuggestions)
        suggestions.sortWith { j1: JSONObject, j2: JSONObject -> j2.optInt(AMOUNT_ELEMENT) - j1.optInt(AMOUNT_ELEMENT) }
        val uniques: MutableList<String?> = ArrayList()
        for (i in suggestions.indices) {
            var value = suggestions[i].optString(COMMENT_ELEMENT).replace("\u00a0", " ")
            if (value.endsWith(additionalComment))
                value = value.substring(0, value.length - additionalComment.length)
            if (value.isNotEmpty() && !uniques.contains(value))
                uniques.add(value)
        }
        return uniques
    }

    private fun getJsonList(allComments: JSONArray?): MutableList<JSONObject> {
        val list: MutableList<JSONObject> = ArrayList()
        var i = 0
        while (allComments != null && i < allComments.length()) {
            list.add(allComments.optJSONObject(i))
            ++i
        }
        return list
    }

    private fun getErrorStrings(s: String?): Array<String?> {
        return arrayOf(ErrorMessage.ERROR, s)
    }

    private fun getLocationParams(sessionId: String, locationValues: LocationValues): List<Pair<String, String>> {
        val params: MutableList<Pair<String, String>> = ArrayList()
        params.add(Pair("m", "mycomments"))
        params.add(Pair("v", "1"))
        params.add(Pair("PHPSESSID", sessionId))
        params.add(Pair("country", locationValues.country))
        params.add(Pair("city", locationValues.city))
        params.add(Pair("zip", locationValues.postalCode))
        return params
    }

    private fun onPostExecute(s: Array<String?>?) {
        val context = callback as Context
        if (s == null || s.isEmpty()) {
            Toast.makeText(context, R.string.no_comment_suggestions, LENGTH_LONG).show()
            return
        }
        if (s[0] == ErrorMessage.ERROR) {
            Toast.makeText(context, R.string.comment_suggestions_error, LENGTH_LONG).show()
            return
        }
        Toast.makeText(context, R.string.comment_suggestions_set, LENGTH_LONG).show()
        callback.onSuggestions(s)
    }

    internal interface Callback {
        fun onSuggestions(suggestions: Array<String?>?)
    }

    companion object {
        private const val DATA_ELEMENT = "data"
        private const val AMOUNT_ELEMENT = "amount"
        private const val COMMENT_ELEMENT = "comment"
    }
}