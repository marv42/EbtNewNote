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
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private const val USER_PREFERENCES_FILE_NAME = "user_prefs.pb"

@Singleton
class DataStoreRepository @Inject constructor(
    private val context: Context,
    cryptoManager: CryptoManager
) {
    private val Context.userPreferencesStore: DataStore<UserPreferences> by dataStore(
        fileName = USER_PREFERENCES_FILE_NAME,
        serializer = CryptedPreferencesSerializer(cryptoManager) as Serializer<UserPreferences>,
        // we would need to decrypt the values first
        // instead, we save the values in EncryptedPreferenceDataStore
//        produceMigrations =
    )

    private val OCR_ONLINE_KEY by lazy { getResource(R.string.pref_settings_ocr_online_key) }
    private val IMAGES_KEY by lazy { getResource(R.string.pref_settings_images_key) }
    private val SHOW_SUBMITTED_KEY by lazy { getResource(R.string.pref_settings_show_submitted_key) }
    private val OCR_SERVICE_KEY by lazy { getResource(R.string.pref_settings_ocr_service_key) }
    private val COUNTRY_KEY by lazy { getResource(R.string.pref_settings_country_key) }
    private val EMAIL_KEY by lazy { getResource(R.string.pref_settings_email_key) }
    private val PASSWORD_KEY by lazy { getResource(R.string.pref_settings_password_key) }
    private val COMMENT_KEY by lazy { getResource(R.string.pref_settings_comment_key) }

    private fun getResource(resId: Int): String = context.getString(resId)

    val userPreferencesFlow: Flow<UserPreferences> = context.userPreferencesStore.data
        .catch { e -> if (e is IOException) emit(UserPreferences.getDefaultInstance()) else throw e
        }

    // https://www.baeldung.com/kotlin/suspend-functions-from-java#bd-kotlinx-coroutine
//    fun getStringAsync(key: String, defValue: String): CompletableFuture<String> = GlobalScope.future { getString(key) }

    // helper functions for SharedPreferences transition
    suspend fun getString(key: String): String {
        if (key == SHOW_SUBMITTED_KEY)
            return showSubmittedFlow.first()
        if (key == OCR_SERVICE_KEY)
            return ocrServiceFlow.first()
        if (key == COUNTRY_KEY)
            return countryFlow.first()
        if (key == EMAIL_KEY)
            return emailFlow.first()
        if (key == PASSWORD_KEY)
            return passwordFlow.first()
        if (key == COMMENT_KEY)
            return commentFlow.first()
        throw IllegalArgumentException("key")
    }

    suspend fun putString(key: String, value: String) {
        if (key == SHOW_SUBMITTED_KEY)
            updateShowSubmitted(value)
        else if (key == OCR_SERVICE_KEY)
            updateOcrService(value)
        else if (key == COUNTRY_KEY)
            updateCountry(value)
        else if (key == EMAIL_KEY)
            updateEmail(value)
        else if (key == PASSWORD_KEY)
            updatePassword(value)
        else if (key == COMMENT_KEY)
            updateComment(value)
        else
            throw IllegalArgumentException("key")
    }

//    fun getBooleanAsync(key: String, defValue: Boolean): CompletableFuture<Boolean> = GlobalScope.future { getBoolean(key) }

    suspend fun getBoolean(key: String): Boolean {
        if (key == OCR_ONLINE_KEY)
            return ocrOnlineFlow.first()
        if (key == IMAGES_KEY)
            return showImagesFlow.first()
        throw IllegalArgumentException("key")
    }

    suspend fun putBoolean(key: String, value: Boolean) {
        if (key == OCR_ONLINE_KEY)
            updateOcrOnline(value)
        else if (key == IMAGES_KEY)
            updateShowImages(value)
        else
            throw IllegalArgumentException("key")
    }

    val ocrOnlineFlow: Flow<Boolean> = context.userPreferencesStore.data
        .catch { e -> if (e is IOException) emit(UserPreferences.getDefaultInstance()) else throw e }
        .map { prefs -> prefs.prefSettingsOcrOnline }

    suspend fun updateOcrOnline(ocrOnline: Boolean) {
        context.userPreferencesStore.updateData { prefs ->
            prefs.toBuilder()
                .setPrefSettingsOcrOnline(ocrOnline)
                .build()
        }
    }

    val showImagesFlow: Flow<Boolean> = context.userPreferencesStore.data
        .catch { e -> if (e is IOException) emit(UserPreferences.getDefaultInstance()) else throw e }
        .map { prefs -> prefs.prefSettingsImages }
    // TODO How do we set the default to true? (Probably not here)

    suspend fun updateShowImages(showImages: Boolean) {
        context.userPreferencesStore.updateData { prefs ->
            prefs.toBuilder()
                .setPrefSettingsImages(showImages)
                .build()
        }
    }

    val showSubmittedFlow: Flow<String> = context.userPreferencesStore.data
        .catch { e -> if (e is IOException) emit(UserPreferences.getDefaultInstance()) else throw e }
        .map { prefs -> prefs.prefSettingsShowSubmitted.ifBlank { maxShowNumDefault } }

    private val maxShowNumDefault = getResource(R.string.max_show_num_default)

    suspend fun updateShowSubmitted(showSubmitted: String) {
        context.userPreferencesStore.updateData { prefs ->
            prefs.toBuilder()
                .setPrefSettingsShowSubmitted(showSubmitted)
                .build()
        }
    }

    val ocrServiceFlow: Flow<String> = context.userPreferencesStore.data
        .catch { e -> if (e is IOException) emit(UserPreferences.getDefaultInstance()) else throw e }
        .map { prefs -> prefs.prefSettingsOcrService }

    suspend fun updateOcrService(ocrService: String) {
        context.userPreferencesStore.updateData { prefs ->
            prefs.toBuilder()
                .setPrefSettingsOcrService(ocrService)
                .build()
        }
    }

    val countryFlow: Flow<String> = context.userPreferencesStore.data
        .catch { e -> if (e is IOException) emit(UserPreferences.getDefaultInstance()) else throw e }
        .map { prefs -> prefs.prefSettingsCountry }

    suspend fun updateCountry(country: String) {
        context.userPreferencesStore.updateData { prefs ->
            prefs.toBuilder()
                .setPrefSettingsCountry(country)
                .build()
        }
    }

    val emailFlow: Flow<String> = context.userPreferencesStore.data
        .catch { e -> if (e is IOException) emit(UserPreferences.getDefaultInstance()) else throw e }
        .map { prefs -> prefs.prefSettingsEmail }

    suspend fun updateEmail(email: String) {
        context.userPreferencesStore.updateData { prefs ->
            prefs.toBuilder()
                .setPrefSettingsEmail(email)
                .build()
        }
    }

    val passwordFlow: Flow<String> = context.userPreferencesStore.data
        .catch { e -> if (e is IOException) emit(UserPreferences.getDefaultInstance()) else throw e }
        .map { prefs -> prefs.prefSettingsPassword }

    suspend fun updatePassword(password: String) {
        context.userPreferencesStore.updateData { prefs ->
            prefs.toBuilder()
                .setPrefSettingsPassword(password)
                .build()
        }
    }

    val commentFlow: Flow<String> = context.userPreferencesStore.data
        .catch { e -> if (e is IOException) emit(UserPreferences.getDefaultInstance()) else throw e }
        .map { prefs -> prefs.prefSettingsComment }

    suspend fun updateComment(comment: String) {
        context.userPreferencesStore.updateData { prefs ->
            prefs.toBuilder()
                .setPrefSettingsComment(comment)
                .build()
        }
    }
}
