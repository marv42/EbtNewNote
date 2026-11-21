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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceDataStore;

import com.google.crypto.tink.streamingaead.StreamingAeadConfig;
import com.marv42.ebt.newnote.R;
import com.marv42.ebt.newnote.ThisApp;

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import javax.inject.Inject;

import static androidx.preference.PreferenceManager.getDefaultSharedPreferences;

public class SharedPreferenceDataStore extends PreferenceDataStore {

    private final ThisApp app;
    private final SharedPreferences sharedPreferences;
    private final Set<String> sensitiveKeys;
    private final Map<SharedPreferences.OnSharedPreferenceChangeListener, Boolean> listeners =
            new WeakHashMap<>();
    private final Object listenersLock = new Object();
    // must stay a field: SharedPreferences keeps its listeners weakly, a local would be collected
    private final SharedPreferences.OnSharedPreferenceChangeListener forwarder =
            (preferences, key) -> notifyListeners(key);

    private DataStoreRepository dataStoreRepository;

    @Inject
    public SharedPreferenceDataStore(ThisApp app) {
        this.app = app;
        sharedPreferences = getDefaultSharedPreferences(app);
        sensitiveKeys = new HashSet<>(Arrays.asList(
                getResource(R.string.pref_settings_password_key),
                getResource(R.string.pref_settings_email_key),
                getResource(R.string.pref_settings_ocr_service_key_key),
                getResource(R.string.pref_settings_country_layer_key_key),
                getResource(R.string.pref_settings_rest_countries_key_key)));
        sharedPreferences.registerOnSharedPreferenceChangeListener(forwarder);
    }

    private String getResource(int resId) {
        return app.getString(resId);
    }

    private boolean isSensitive(@Nullable String key) {
        return sensitiveKeys.contains(key);
    }

    private synchronized DataStoreRepository getRepository() {
        if (dataStoreRepository == null) {
            try {
                StreamingAeadConfig.register();
            } catch (GeneralSecurityException e) {
                throw new RuntimeException(e);
            }
            Context context = app.getApplicationContext();
            dataStoreRepository = new DataStoreRepository(context, new CryptoManager(context));
        }
        return dataStoreRepository;
    }

    public SharedPreferences getSharedPreferences() {
        return sharedPreferences;
    }

    public void registerOnSharedPreferenceChangeListener(
            @NonNull SharedPreferences.OnSharedPreferenceChangeListener listener) {
        synchronized (listenersLock) {
            listeners.put(listener, Boolean.TRUE);
        }
    }

    public void unregisterOnSharedPreferenceChangeListener(
            @NonNull SharedPreferences.OnSharedPreferenceChangeListener listener) {
        synchronized (listenersLock) {
            listeners.remove(listener);
        }
    }

    private void notifyListeners(@Nullable String key) {
        final List<SharedPreferences.OnSharedPreferenceChangeListener> snapshot;
        synchronized (listenersLock) {
            snapshot = new ArrayList<>(listeners.keySet());
        }
        for (SharedPreferences.OnSharedPreferenceChangeListener listener : snapshot)
            listener.onSharedPreferenceChanged(sharedPreferences, key);
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
        else
            throw new IllegalArgumentException("defValue " + defValue + " is instance of unhandled class");
        if (value == null)
            return defValue;
        return value;
    }

    @Nullable
    @Override
    public String getString(String key, @Nullable String defValue) {
        if (isSensitive(key))
            return getRepository().getStringBlocking(key);
        return sharedPreferences.getString(key, defValue);
    }

    @Override
    public void putString(String key, @Nullable String value) {
        final String value_or_empty = value != null ? value : "";
        if (isSensitive(key)) {
            getRepository().putStringBlocking(key, value_or_empty);
            notifyListeners(key);
            return;
        }
        sharedPreferences.edit().putString(key, value).apply();
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
        if (isSensitive(key))
            throw new IllegalStateException("'" + key + "' has no encrypted boolean value");
        return sharedPreferences.getBoolean(key, defValue);
    }

    @Override
    public void putBoolean(String key, boolean value) {
        if (isSensitive(key))
            throw new IllegalStateException("'" + key + "' has no encrypted boolean value");
        sharedPreferences.edit().putBoolean(key, value).apply();
    }
}
