/*
 Copyright (c) 2010 - 2022 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import androidx.lifecycle.ViewModelProvider;

import com.marv42.ebt.newnote.R;
import com.marv42.ebt.newnote.ThisApp;
import com.marv42.ebt.newnote.ui.SubmitViewModel;

import javax.inject.Inject;

public class MySharedPreferencesListener implements SharedPreferences.OnSharedPreferenceChangeListener {

    private final Context context;
    private final EncryptedPreferenceDataStore dataStore;
    private final SharedPreferencesHandler sharedPreferencesHandler;
    private final ViewModelProvider viewModelProvider;

    @Inject
    public MySharedPreferencesListener(ThisApp app,
                                       EncryptedPreferenceDataStore dataStore,
                                       SharedPreferencesHandler sharedPreferencesHandler,
                                       ViewModelProvider viewModelProvider) {
        context = app;
        this.dataStore = dataStore;
        this.sharedPreferencesHandler = sharedPreferencesHandler;
        this.viewModelProvider = viewModelProvider;
    }

    public void register() {
        SharedPreferences preferences = dataStore.getSharedPreferences();
        if (preferences != null)
            preferences.registerOnSharedPreferenceChangeListener(this);
        sharedPreferencesHandler.getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    public void unregister() {
        SharedPreferences preferences = dataStore.getSharedPreferences();
        if (preferences != null)
            preferences.unregisterOnSharedPreferenceChangeListener(this);
        sharedPreferencesHandler.getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (sharedPreferences == sharedPreferencesHandler.getSharedPreferences())
            if (key.equals(context.getString(R.string.pref_country_key)) ||
                    key.equals(context.getString(R.string.pref_city_key)) ||
                    key.equals(context.getString(R.string.pref_postal_code_key)))
                setLocation();
    }

    public void setLocation() {
        setCountry();
        setCity();
        setPostalCode();
    }

    private void setCountry() {
        String country = sharedPreferencesHandler.get(R.string.pref_country_key, "");
        SubmitViewModel viewModel = viewModelProvider.get(SubmitViewModel.class);
        if (IsNotEqual(country, viewModel.getCountry().toString()))
            viewModel.setCountry(country);
    }

    private void setCity() {
        String city = sharedPreferencesHandler.get(R.string.pref_city_key, "");
        SubmitViewModel viewModel = viewModelProvider.get(SubmitViewModel.class);
        if (IsNotEqual(city, viewModel.getCity().toString()))
            viewModel.setCity(city);
    }

    private void setPostalCode() {
        String postalCode = sharedPreferencesHandler.get(R.string.pref_postal_code_key, "");
        SubmitViewModel viewModel = viewModelProvider.get(SubmitViewModel.class);
        if (IsNotEqual(postalCode, viewModel.getPostalCode().toString()))
            viewModel.setPostalCode(postalCode);
    }

    private boolean IsNotEqual(String value, String valueFromViewModel) {
        return !TextUtils.isEmpty(value) && !TextUtils.equals(value, valueFromViewModel);
    }
}
