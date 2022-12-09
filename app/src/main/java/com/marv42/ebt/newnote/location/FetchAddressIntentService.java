/*
 Copyright (c) 2010 - 2022 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote.location;

import static com.marv42.ebt.newnote.BuildConfig.APPLICATION_ID;
import static com.marv42.ebt.newnote.ThisApp.RESULT_CODE_ERROR;
import static com.marv42.ebt.newnote.ThisApp.RESULT_CODE_SUCCESS;
import static com.marv42.ebt.newnote.exceptions.ErrorMessage.ERROR;

import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.ResultReceiver;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.marv42.ebt.newnote.HttpCaller;
import com.marv42.ebt.newnote.R;
import com.marv42.ebt.newnote.data.LocationValues;
import com.marv42.ebt.newnote.exceptions.CallResponseException;
import com.marv42.ebt.newnote.exceptions.HttpCallException;
import com.marv42.ebt.newnote.exceptions.NoIntentException;
import com.marv42.ebt.newnote.exceptions.NoLocationException;
import com.marv42.ebt.newnote.preferences.EncryptedPreferenceDataStore;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import javax.inject.Inject;

import dagger.android.DaggerIntentService;
import okhttp3.FormBody;
import okhttp3.Request;

public class FetchAddressIntentService extends DaggerIntentService {

    public static final String TAG = FetchAddressIntentService.class.getSimpleName();
    public static final String RECEIVER = APPLICATION_ID + ".RECEIVER";
    public static final String RESULT_DATA_KEY = APPLICATION_ID + ".RESULT_DATA_KEY";
    public static final String LOCATION_DATA_EXTRA = APPLICATION_ID + ".LOCATION_DATA_EXTRA";
    private static final String GEOCODING_HOST = "geocode.arcgis.com";
    private static final String GEOCODING_URL = "https://" + GEOCODING_HOST + "/arcgis/rest/services/World/GeocodeServer/reverseGeocode";
    private static final String ADDRESS_ELEMENT = "address";
    private ResultReceiver receiver;
    private String result;
    private int resultCode = RESULT_CODE_ERROR;
    @Inject
    EncryptedPreferenceDataStore dataStore;

    public FetchAddressIntentService() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        try {
            fetchAddress(intent);
        } catch (NoIntentException | NoLocationException | HttpCallException | CallResponseException e) {
            result = e.getMessage();
        } finally {
            deliverResultToReceiver();
        }
    }

    private void fetchAddress(Intent intent) throws NoIntentException, NoLocationException, HttpCallException, CallResponseException {
        getReceiver(intent);
        Location location = getLocation(intent);
        LocationValues locationValues = getLocationValues(location);
        result = new Gson().toJson(locationValues);
        resultCode = RESULT_CODE_SUCCESS;
    }

    private void getReceiver(Intent intent) throws NoIntentException {
        if (intent == null)
            throw new NoIntentException(ERROR + getString(R.string.internal_error));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            receiver = intent.getParcelableExtra(RECEIVER, ResultReceiver.class);
        else
            receiver = intent.getParcelableExtra(RECEIVER);
    }

    private Location getLocation(Intent intent) throws NoLocationException {
        Location l;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            l = intent.getParcelableExtra(LOCATION_DATA_EXTRA, Location.class);
        else
            l = intent.getParcelableExtra(LOCATION_DATA_EXTRA);
        if (l == null)
            throw new NoLocationException(ERROR + getString(R.string.location_none));
        return l;
    }

    private LocationValues getLocationValues(Location location) throws HttpCallException, CallResponseException {
        String body = executeHttpCall(location);
        JSONObject jsonAddress = getAddress(body);
        String countryCode = jsonAddress.optString("CountryCode");
        String city = jsonAddress.optString("City");
        String postalCode = jsonAddress.optString("Postal");
        String countryName = ERROR + getString(R.string.location_no_country);
        // TODO String countryName = new CountryCodeLocal().convert(countryCode);
        try {
            String apiKey = dataStore.get(R.string.pref_settings_country_key, "");
            countryName = new CountryCode().convert(countryCode, apiKey);
        } catch (HttpCallException | CallResponseException e) {
            e.printStackTrace();
        }
        return new LocationValues(countryName, city, postalCode);
    }

    private String executeHttpCall(Location location) throws HttpCallException {
        FormBody formBody = getFormBody(location);
        Request request = new Request.Builder().url(GEOCODING_URL).post(formBody).build();
        String body = new HttpCaller().call(request);
        if (body.startsWith(ERROR))
            throw new HttpCallException(body);
        return body;
    }

    private JSONObject getAddress(String body) throws CallResponseException {
        try {
            JSONObject json = new JSONObject(body);
            return getAddressElement(json);
        } catch (JSONException | CallResponseException e) {
            throw new CallResponseException(ERROR + getString(R.string.server_error) + " "
                    + GEOCODING_HOST + ": " + e.getMessage() + ": " + body);
        }
    }

    @NotNull
    private JSONObject getAddressElement(JSONObject json) throws CallResponseException {
        if (!json.has(ADDRESS_ELEMENT))
            throw new CallResponseException("no '" + ADDRESS_ELEMENT + "' element");
        JSONObject jsonAddress = json.optJSONObject(ADDRESS_ELEMENT);
        if (jsonAddress == null)
            throw new CallResponseException("empty '" + ADDRESS_ELEMENT + "' element");
        return jsonAddress;
    }

    @NotNull
    private FormBody getFormBody(Location location) {
        FormBody.Builder formBodyBuilder = getFormBodyBuilder(location);
        return formBodyBuilder.build();
    }

    @NotNull
    private FormBody.Builder getFormBodyBuilder(Location location) {
        String latitude = String.valueOf(location.getLatitude());
        String longitude = String.valueOf(location.getLongitude());
        FormBody.Builder formBodyBuilder = new FormBody.Builder();
        formBodyBuilder.add("f", "json");
        formBodyBuilder.add("langCode", "EN");
        formBodyBuilder.add("location", longitude + "," + latitude);
        return formBodyBuilder;
    }

    private void deliverResultToReceiver() {
        Bundle bundle = new Bundle();
        bundle.putString(RESULT_DATA_KEY, result);
        receiver.send(resultCode, bundle);
    }
}
