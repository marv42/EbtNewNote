/*
 Copyright (c) 2010 - 2026 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceDataStore;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.google.crypto.tink.streamingaead.StreamingAeadConfig;
import com.marv42.ebt.newnote.ThisApp;

import java.io.IOException;
import java.security.GeneralSecurityException;

import javax.inject.Inject;

import static android.security.keystore.KeyProperties.BLOCK_MODE_GCM;
import static android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE;
import static android.security.keystore.KeyProperties.PURPOSE_DECRYPT;
import static android.security.keystore.KeyProperties.PURPOSE_ENCRYPT;
import static androidx.preference.PreferenceManager.getDefaultSharedPreferences;
import static androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV;
import static androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM;
import static androidx.security.crypto.MasterKey.DEFAULT_MASTER_KEY_ALIAS;

import kotlin.coroutines.EmptyCoroutineContext;
import kotlinx.coroutines.BuildersKt;

public class EncryptedPreferenceDataStore extends PreferenceDataStore {

    static final String ENCRYPTED_SHARED_PREFERENCES_FILE_NAME = "ebt_new_note_encrypted_shared_prefs";
//    private static final String MASTER_KEY_ALIAS = "ebt_new_note_master_key";
    private static final int KEY_SIZE = 256;
    private final ThisApp app;
    private SharedPreferences sharedPreferences;
    private DataStoreRepository dataStoreRepository;

    @Inject
    public EncryptedPreferenceDataStore(ThisApp app) {
        this.app = app;
        initializeSharedPreferences();
        createDataStoreRepository();
    }

    private void initializeSharedPreferences() {
        try {
            sharedPreferences = EncryptedSharedPreferences.create(app,
                    ENCRYPTED_SHARED_PREFERENCES_FILE_NAME,
                    getMasterKey(), AES256_SIV, AES256_GCM);
        } catch (GeneralSecurityException | IOException e) {
            sharedPreferences = getDefaultSharedPreferences(app);
        }
    }

    private void createDataStoreRepository() {
        final Context context = app.getApplicationContext();
        try {
            StreamingAeadConfig.register();
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
        CryptoManager cryptoManager = new CryptoManager(context);
        dataStoreRepository = new DataStoreRepository(context, cryptoManager);
    }

    private MasterKey getMasterKey() throws GeneralSecurityException, IOException {
        KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(
                DEFAULT_MASTER_KEY_ALIAS, PURPOSE_ENCRYPT | PURPOSE_DECRYPT)
                .setBlockModes(BLOCK_MODE_GCM)
                .setEncryptionPaddings(ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_SIZE)
                .build();
        return new MasterKey.Builder(app)
//                .setKeyScheme(AES256_GCM)
                .setKeyGenParameterSpec(spec)
                .build();
    }

    public SharedPreferences getSharedPreferences() {
        return sharedPreferences;
    }

    @NonNull
    public <T> T get(int keyId, T defValue) {
        return get(app.getString(keyId), defValue);
    }

    @NonNull
    private <T> T get(String key, T defValue) {
        T value;
        if (defValue instanceof String)
            value = (T) getString(key, (String) defValue);
        else if (defValue instanceof Boolean)
            value = (T) (Boolean) getBoolean(key, (Boolean) defValue);
//        else if (defValue instanceof Integer)
//            value = (T) (Integer) getInt(key, (Integer) defValue);
        else
            throw new IllegalArgumentException("defValue " + defValue + " is instance of unhandled class");
        if (value == null)
            return defValue;
        return value;
    }

    // setting happens through PreferenceDataStore via putSomething(key, value)

    @Nullable
    @Override
    public String getString(String key, @Nullable String defValue) {
        final String value = sharedPreferences.getString(key, defValue);
        putStringToProtoDataStore(key, value != null ? value : "");  // migrating
        return getStringFromProtoDataStore(key, defValue);
    }

    @Override
    public void putString(String key, @Nullable String value) {
        putStringToProtoDataStore(key, value);
        sharedPreferences.edit().putString(key, value).apply();
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
        final boolean value = sharedPreferences.getBoolean(key, defValue);
        putBooleanToProtoDataStore(key, value);  // migrating
        return getBooleanFromProtoDataStore(key, defValue);
    }

    @Override
    public void putBoolean(String key, boolean value) {
        putBooleanToProtoDataStore(key, value);
        sharedPreferences.edit().putBoolean(key, value).apply();
    }

    private String getStringFromProtoDataStore(String key, String defValue) {
        try {
            return BuildersKt.runBlocking(
                    EmptyCoroutineContext.INSTANCE,
                    (scope, continuation) -> dataStoreRepository.getString(key, continuation)
            );
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void putStringToProtoDataStore(String key, String value) {
        try {
            BuildersKt.runBlocking(
                    EmptyCoroutineContext.INSTANCE,
                    (scope, continuation) -> dataStoreRepository.putString(key, value, continuation)
            );
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean getBooleanFromProtoDataStore(String key, boolean defValue) {
        try {
            return BuildersKt.runBlocking(
                    EmptyCoroutineContext.INSTANCE,
                    (scope, continuation) -> dataStoreRepository.getBoolean(key, continuation)
            );
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void putBooleanToProtoDataStore(String key, boolean value) {
        try {
            BuildersKt.runBlocking(
                    EmptyCoroutineContext.INSTANCE,
                    (scope, continuation) -> dataStoreRepository.putBoolean(key, value, continuation)
            );
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
