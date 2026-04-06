/*
 Copyright (c) 2010 - 2026 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote.preferences

import android.content.Context
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.StreamingAead
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import java.io.InputStream
import java.io.OutputStream
import kotlin.jvm.java

private const val KEYSET_NAME = "master_keyset"
private const val PREFERENCES_FILE_NAME = "master_key_preference"
private const val MASTER_KEY = "android-keystore://master_key"
private const val AES256_GCM = "AES256_GCM_HKDF_4KB"

// https://proandroiddev.com/goodbye-encryptedsharedpreferences-a-2026-migration-guide-4b819b4a537a
class CryptoManager(context: Context) {
    private val aead = AndroidKeysetManager.Builder()
        .withSharedPref(context, KEYSET_NAME, PREFERENCES_FILE_NAME)
        .withKeyTemplate(KeyTemplates.get(AES256_GCM))
        .withMasterKeyUri(MASTER_KEY)
        .build()
        .keysetHandle
        .getPrimitive(RegistryConfiguration.get(), StreamingAead::class.java)
    fun encrypt(outputStream: OutputStream): OutputStream {
        return aead.newEncryptingStream(outputStream, ByteArray(0))
    }
    fun decrypt(inputStream: InputStream): InputStream {
        return aead.newDecryptingStream(inputStream, ByteArray(0))
    }
}
