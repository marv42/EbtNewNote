/*
 Copyright (c) 2010 - 2026 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */
package com.marv42.ebt.newnote.location

import android.location.Location
import android.os.Build
import android.util.Log
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import com.marv42.ebt.newnote.HttpCaller
import com.marv42.ebt.newnote.R
import com.marv42.ebt.newnote.ThisApp
import com.marv42.ebt.newnote.data.LocationValues
import com.marv42.ebt.newnote.exceptions.CallResponseException
import com.marv42.ebt.newnote.exceptions.ErrorMessage
import com.marv42.ebt.newnote.exceptions.ErrorMessage.ERROR
import com.marv42.ebt.newnote.exceptions.HttpCallException
import com.marv42.ebt.newnote.executeAsyncTask
import com.marv42.ebt.newnote.mainScope
import com.marv42.ebt.newnote.preferences.SharedPreferenceDataStore
import com.marv42.ebt.newnote.preferences.SharedPreferencesHandler
import kotlinx.coroutines.Job
import okhttp3.FormBody
import okhttp3.Request
import org.json.JSONException
import org.json.JSONObject

private const val TAG = "AddressTask"

class AddressTask(private val app: ThisApp, private val dataStore: SharedPreferenceDataStore,
                  private val preferences: SharedPreferencesHandler) {

    private val scope = mainScope()
    private var job: Job? = null

    fun execute(location: Location) {
        job?.cancel()
        job = scope.executeAsyncTask(
            doInBackground = {
                fetchAddress(location)
            }, onPostExecute = {
                deliverResult(it)
            }, onError = {
                showError(it.message)
            })
    }

    fun cancel() {
        job?.cancel()
        job = null
    }

    // expected failures (offline, bad key, unknown address) are results, not errors
    private fun fetchAddress(location: Location): AddressResult =
        try {
            AddressResult.Success(getLocationValues(location))
        } catch (e: HttpCallException) {
            AddressResult.Failure(e.message)
        } catch (e: CallResponseException) {
            AddressResult.Failure(e.message)
        }

    private fun getLocationValues(location: Location): LocationValues {
        val jsonAddress = getAddress(executeHttpCall(location))
        return LocationValues(
            countryName(jsonAddress.optString("CountryCode")),
            jsonAddress.optString("City"),
            jsonAddress.optString("Postal"))
    }

    private fun executeHttpCall(location: Location): String {
        val request = Request.Builder().url(GEOCODING_URL).post(formBody(location)).build()
        val body = HttpCaller().call(request)
        if (body.startsWith(ERROR))
            throw HttpCallException(body)
        return body
    }

    private fun getAddress(body: String): JSONObject =
        try {
            addressElement(JSONObject(body))
        } catch (e: JSONException) {
            throw CallResponseException(geocodingError(e, body))
        } catch (e: CallResponseException) {
            throw CallResponseException(geocodingError(e, body))
        }

    private fun geocodingError(e: Throwable, body: String): String =
        ERROR + app.getString(R.string.server_error) + " " + GEOCODING_HOST + ": " + e.message + ": " + body

    private fun countryName(countryCode: String): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA)
            CountryCodeLocal().convert(countryCode).ifEmpty { noCountry() }
        else
            remoteCountryName(countryCode)

    private fun remoteCountryName(countryCode: String): String =
        try {
            CountryCode().convert(countryCode,
                dataStore.get(R.string.pref_settings_country_service_key_key, ""))
        } catch (e: HttpCallException) {
            logAndNoCountry(e)
        } catch (e: CallResponseException) {
            logAndNoCountry(e)
        }

    private fun logAndNoCountry(e: Throwable): String {
        Log.w(TAG, e.message ?: e.toString())
        return noCountry()
    }

    private fun noCountry(): String = ERROR + app.getString(R.string.location_no_country)

    private fun deliverResult(result: AddressResult) {
        when (result) {
            is AddressResult.Success -> storeLocationValues(result.values)
            is AddressResult.Failure -> showError(result.message)
        }
    }

    private fun storeLocationValues(values: LocationValues) {
        if (values.country.startsWith(ERROR))
            showError(values.country)
        else
            preferences.set(R.string.pref_country_key, values.country)
        preferences.set(R.string.pref_city_key, values.city)
        preferences.set(R.string.pref_postal_code_key, values.postalCode)
    }

    private fun showError(message: String?) {
        val text = message ?: app.getString(R.string.internal_error)
        Toast.makeText(app, ErrorMessage(app).getErrorMessage(text), LENGTH_LONG).show()
    }

    private sealed class AddressResult {
        class Success(val values: LocationValues) : AddressResult()
        class Failure(val message: String?) : AddressResult()
    }

    companion object {
        private const val GEOCODING_HOST = "geocode.arcgis.com"
        private const val GEOCODING_URL =
            "https://$GEOCODING_HOST/arcgis/rest/services/World/GeocodeServer/reverseGeocode"
        private const val ADDRESS_ELEMENT = "address"

        internal fun formBody(location: Location): FormBody =
            FormBody.Builder()
                .add("f", "json")
                .add("langCode", "EN")
                .add("location", "${location.longitude},${location.latitude}")
                .build()

        internal fun addressElement(json: JSONObject): JSONObject {
            if (!json.has(ADDRESS_ELEMENT))
                throw CallResponseException("no '$ADDRESS_ELEMENT' element")
            return json.optJSONObject(ADDRESS_ELEMENT)
                ?: throw CallResponseException("empty '$ADDRESS_ELEMENT' element")
        }
    }
}
