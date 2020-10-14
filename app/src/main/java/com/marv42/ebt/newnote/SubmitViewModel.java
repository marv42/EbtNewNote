/*
 Copyright (c) 2010 - 2020 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote;

import android.app.Application;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import static androidx.preference.PreferenceManager.getDefaultSharedPreferences;

public class SubmitViewModel extends AndroidViewModel {

    private final SharedPreferences preferences;
    private SharedPreferencesStringLiveData countryLiveData;
    private SharedPreferencesStringLiveData cityLiveData;
    private SharedPreferencesStringLiveData postalCodeLiveData;
    private SharedPreferencesStringLiveData denominationLiveData;
    private SharedPreferencesStringLiveData shortCodeLiveData;
    private SharedPreferencesStringLiveData serialNumberLiveData;
    private SharedPreferencesStringLiveData commentLiveData;
    private final String countryKey;
    private final String cityKey;
    private final String postalCodeKey;
    private final String denominationKey;
    private final String shortCodeKey;
    private final String serialNumberKey;
    private final String commentKey;

    public SubmitViewModel(@NonNull Application application) {
        super(application);
        preferences = getDefaultSharedPreferences(application);
        countryKey = application.getString(R.string.pref_country_key);
        cityKey = application.getString(R.string.pref_city_key);
        postalCodeKey = application.getString(R.string.pref_postal_code_key);
        denominationKey = application.getString(R.string.pref_denomination_key);
        shortCodeKey = application.getString(R.string.pref_short_code_key);
        serialNumberKey = application.getString(R.string.pref_serial_number_key);
        commentKey = application.getString(R.string.pref_comment_key);
    }

    public LiveData<String> getCountry() {
        if (countryLiveData == null)
            countryLiveData = new SharedPreferencesStringLiveData(preferences, countryKey, "");
        return countryLiveData;
    }

    public LiveData<String> getCity() {
        if (cityLiveData == null)
            cityLiveData = new SharedPreferencesStringLiveData(preferences, cityKey, "");
        return cityLiveData;
    }

    public LiveData<String> getPostalCode() {
        if (postalCodeLiveData == null)
            postalCodeLiveData = new SharedPreferencesStringLiveData(preferences, postalCodeKey, "");
        return postalCodeLiveData;
    }

    public LiveData<String> getDenomination() {
        if (denominationLiveData == null)
            denominationLiveData = new SharedPreferencesStringLiveData(preferences, denominationKey, "");
        return denominationLiveData;
    }

    public LiveData<String> getShortCode() {
        if (shortCodeLiveData == null)
            shortCodeLiveData = new SharedPreferencesStringLiveData(preferences, shortCodeKey, "");
        return shortCodeLiveData;
    }

    public LiveData<String> getSerialNumber() {
        if (serialNumberLiveData == null)
            serialNumberLiveData = new SharedPreferencesStringLiveData(preferences, serialNumberKey, "");
        return serialNumberLiveData;
    }

    public LiveData<String> getComment() {
        if (commentLiveData == null)
            commentLiveData = new SharedPreferencesStringLiveData(preferences, commentKey, "");
        return commentLiveData;
    }
}
