package com.marv42.ebt.newnote;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import java.io.IOException;
import java.security.GeneralSecurityException;

class EncryptedSharedPreferencesProvider {
    private static final String SECRET_SHARED_PREFERENCES_FILE_NAME = "secret_shared_prefs";

    static SharedPreferences getEncryptedSharedPreferences(Context context) {
        String masterKeyAlias;
        try {
            masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
            return null;
        }
        try {
            return EncryptedSharedPreferences.create(SECRET_SHARED_PREFERENCES_FILE_NAME,
                    masterKeyAlias, context,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
