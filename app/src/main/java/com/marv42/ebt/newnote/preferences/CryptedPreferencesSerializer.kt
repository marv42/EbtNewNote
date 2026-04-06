/*
 Copyright (c) 2010 - 2026 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote.preferences

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.crypto.tink.shaded.protobuf.InvalidProtocolBufferException
import com.marv42.ebt.newnote.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CryptedPreferencesSerializer @Inject constructor(
    private val cryptoManager: CryptoManager
) : Serializer<UserPreferences> {
    override val defaultValue: UserPreferences = UserPreferences.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): UserPreferences {
        return try {
            val decryptedStream = cryptoManager.decrypt(input)
            UserPreferences.parseFrom(decryptedStream)
        } catch (e: InvalidProtocolBufferException) {
            throw CorruptionException("Decryption error", e)
        }
    }

    override suspend fun writeTo(t: UserPreferences, output: OutputStream) {
        val encryptedStream = cryptoManager.encrypt(output)
        t.writeTo(encryptedStream)
        withContext(Dispatchers.IO) {
            encryptedStream.close()
        }
    }
}
