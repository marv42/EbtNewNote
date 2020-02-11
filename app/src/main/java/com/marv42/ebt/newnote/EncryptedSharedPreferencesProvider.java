package com.marv42.ebt.newnote;

import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import java.io.IOException;
import java.security.GeneralSecurityException;

import javax.inject.Inject;

public class EncryptedSharedPreferencesProvider {
    private static final String ENCRYPTED_SHARED_PREFERENCES_FILE_NAME = "encrypted_shared_prefs";

    private ThisApp mApp;

    @Inject
    public EncryptedSharedPreferencesProvider(ThisApp app) {
        mApp = app;
    }

    SharedPreferences getEncryptedSharedPreferences() throws GeneralSecurityException, IOException {
        String masterKeyAlias;
        masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
        return EncryptedSharedPreferences.create(ENCRYPTED_SHARED_PREFERENCES_FILE_NAME,
                masterKeyAlias, mApp,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
    }
}
