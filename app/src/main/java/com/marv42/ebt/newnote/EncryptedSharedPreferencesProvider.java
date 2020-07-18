package com.marv42.ebt.newnote;

import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;

import javax.inject.Inject;

import static android.security.keystore.KeyProperties.BLOCK_MODE_GCM;
import static android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE;
import static android.security.keystore.KeyProperties.PURPOSE_DECRYPT;
import static android.security.keystore.KeyProperties.PURPOSE_ENCRYPT;
import static androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV;
import static androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM;
import static androidx.security.crypto.MasterKey.DEFAULT_MASTER_KEY_ALIAS;

public class EncryptedSharedPreferencesProvider {
    private static final String ENCRYPTED_SHARED_PREFERENCES_FILE_NAME = "encrypted_shared_prefs";
    private static final int KEY_SIZE = 256;

    private ThisApp mApp;

    @Inject
    public EncryptedSharedPreferencesProvider(ThisApp app) {
        mApp = app;
    }

    private MasterKey getMasterKey() throws GeneralSecurityException, IOException {
        KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(
                DEFAULT_MASTER_KEY_ALIAS, PURPOSE_ENCRYPT | PURPOSE_DECRYPT)
                .setBlockModes(BLOCK_MODE_GCM)
                .setEncryptionPaddings(ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_SIZE)
                .build();
        return new MasterKey.Builder(mApp)
//                    .setKeyScheme(AES256_GCM)
                .setKeyGenParameterSpec(spec)
                .build();
    }

    SharedPreferences getEncryptedSharedPreferences() throws GeneralSecurityException, IOException {
        return EncryptedSharedPreferences.create(mApp, ENCRYPTED_SHARED_PREFERENCES_FILE_NAME,
                getMasterKey(), AES256_SIV, AES256_GCM);
    }
}
