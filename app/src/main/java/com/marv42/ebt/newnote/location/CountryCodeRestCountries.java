/*
 Copyright (c) 2010 - 2026 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote.location;

import com.marv42.ebt.newnote.HttpCaller;
import com.marv42.ebt.newnote.exceptions.CallResponseException;
import com.marv42.ebt.newnote.exceptions.HttpCallException;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

import okhttp3.Request;

public class CountryCodeRestCountries implements CountryCodeService {

    static final String REST_COUNTRIES_API_HOST = "api.restcountries.com";
    private static final String COUNTRIES_URL = "https://" + REST_COUNTRIES_API_HOST + "/countries/v5/codes.alpha_3/";
    private static final String RESPONSE_FIELDS = "?response_fields=names.common";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String DATA_ELEMENT = "data";
    private static final String OBJECTS_ELEMENT = "objects";
    private static final String NAMES_ELEMENT = "names";
    private static final String COMMON_ELEMENT = "common";

    @Override
    public String convert(String countryCode, String apiKey) throws HttpCallException, CallResponseException {
        String body = executeCall(countryCode, apiKey);
        return extractName(body);
    }

    private String executeCall(String countryCode, String apiKey) throws HttpCallException {
        return new HttpCaller().call(request(countryCode, apiKey));
    }

    static Request request(String countryCode, String apiKey) {
        String url = COUNTRIES_URL + normalize(countryCode) + RESPONSE_FIELDS;
        return new Request.Builder()
                .url(url)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + apiKey)
                .build();
    }

    private static String normalize(String countryCode) {
        return countryCode == null ? "" : countryCode.trim().toUpperCase(Locale.ROOT);
    }

    @NotNull
    String extractName(String body) throws CallResponseException {
        try {
            JSONObject json = new JSONObject(body);
            JSONObject data = json.optJSONObject(DATA_ELEMENT);
            if (data == null)
                throw new CallResponseException("R.string.server_error: no '" + DATA_ELEMENT + "' element");
            JSONArray objects = data.optJSONArray(OBJECTS_ELEMENT);
            if (objects == null)
                throw new CallResponseException("R.string.server_error: no '" + OBJECTS_ELEMENT + "' element");
            if (objects.length() == 0)
                throw new CallResponseException("no country found");
            JSONObject country = objects.optJSONObject(0);
            JSONObject names = country == null ? null : country.optJSONObject(NAMES_ELEMENT);
            if (names == null)
                throw new CallResponseException("R.string.server_error: no '" + NAMES_ELEMENT + "' element");
            String name = names.optString(COMMON_ELEMENT).trim();
            if (name.isEmpty())
                throw new CallResponseException("R.string.server_error: no '" + COMMON_ELEMENT + "' element");
            return name;
        } catch (JSONException e) {
            throw new CallResponseException("R.string.server_error: R.string.no_json, " + e.getMessage());
        }
    }
}
