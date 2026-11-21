/*
 Copyright (c) 2010 - 2026 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.marv42.ebt.newnote.R
import com.marv42.ebt.newnote.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

const val USER_PREFERENCES_FILE_NAME = "user_prefs.pb"

@Singleton
class DataStoreRepository @Inject constructor(
    private val context: Context,
    cryptoManager: CryptoManager
) {
    private val Context.userPreferencesStore: DataStore<UserPreferences> by dataStore(
        fileName = USER_PREFERENCES_FILE_NAME,
        serializer = CryptedPreferencesSerializer(cryptoManager) as Serializer<UserPreferences>,
    )

    private val OCR_SERVICE_KEY by lazy { getResource(R.string.pref_settings_ocr_service_key_key) }
    private val COUNTRY_LAYER_KEY by lazy { getResource(R.string.pref_settings_country_layer_key_key) }
    private val REST_COUNTRIES_KEY by lazy { getResource(R.string.pref_settings_rest_countries_key_key) }
    private val EMAIL_KEY by lazy { getResource(R.string.pref_settings_email_key) }
    private val PASSWORD_KEY by lazy { getResource(R.string.pref_settings_password_key) }

    private fun getResource(resId: Int): String = context.getString(resId)

    private val preferencesFlow: Flow<UserPreferences> = context.userPreferencesStore.data
        .catch { e -> if (e is IOException) emit(UserPreferences.getDefaultInstance()) else throw e }

    private val ocrServiceKeyFlow: Flow<String> = preferencesFlow.map { it.prefSettingsOcrServiceKey }
    private val countryLayerKeyFlow: Flow<String> = preferencesFlow.map { it.prefSettingsCountryLayerKey }
    private val restCountriesKeyFlow: Flow<String> = preferencesFlow.map { it.prefSettingsRestCountriesKey }
    private val emailFlow: Flow<String> = preferencesFlow.map { it.prefSettingsEmail }
    private val passwordFlow: Flow<String> = preferencesFlow.map { it.prefSettingsPassword }

    fun getStringBlocking(key: String): String = blocking {
        when (key) {
            OCR_SERVICE_KEY -> ocrServiceKeyFlow.first()
            COUNTRY_LAYER_KEY -> countryLayerKeyFlow.first()
            REST_COUNTRIES_KEY -> restCountriesKeyFlow.first()
            EMAIL_KEY -> emailFlow.first()
            PASSWORD_KEY -> passwordFlow.first()
            else -> throw IllegalArgumentException("key")
        }
    }

    fun putStringBlocking(key: String, value: String) = blocking {
        when (key) {
            OCR_SERVICE_KEY -> update { it.toBuilder().setPrefSettingsOcrServiceKey(value).build() }
            COUNTRY_LAYER_KEY -> update { it.toBuilder().setPrefSettingsCountryLayerKey(value).build() }
            REST_COUNTRIES_KEY -> update { it.toBuilder().setPrefSettingsRestCountriesKey(value).build() }
            EMAIL_KEY -> update { it.toBuilder().setPrefSettingsEmail(value).build() }
            PASSWORD_KEY -> update { it.toBuilder().setPrefSettingsPassword(value).build() }
            else -> throw IllegalArgumentException("key")
        }
    }

    private suspend fun update(transform: (UserPreferences) -> UserPreferences) {
        context.userPreferencesStore.updateData { prefs -> transform(prefs) }
    }

    private fun <T> blocking(block: suspend () -> T): T {
        try {
            return runBlocking { block() }
        } catch (e: InterruptedException) {
            throw RuntimeException(e)
        }
    }
}
