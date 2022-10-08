/*
 Copyright (c) 2010 - 2022 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote;

import com.marv42.ebt.newnote.exceptions.CallResponseException;
import com.marv42.ebt.newnote.exceptions.HttpCallException;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.Request;

public class CountryCode {

    private static final String COUNTRIES_URL = "http://api.countrylayer.com/v2/alpha/";
    private static final String NAME_ELEMENT = "name";

    public String convert(String countryCode, String apiKey) throws HttpCallException, CallResponseException {
        String body = executeCall(countryCode, apiKey);
        return extractName(body);
    }

    private String executeCall(String countryCode, String apiKey) throws HttpCallException {
        String url = COUNTRIES_URL + countryCode + "?access_key=" + apiKey;
        Request request = new Request.Builder().url(url).build();
        return new HttpCaller().call(request);
    }

    @NotNull
    private String extractName(String body) throws CallResponseException {
        try {
            JSONObject json = new JSONObject(body);
            return getName(json);
        } catch (JSONException e) {
            throw new CallResponseException("R.string.server_error: R.string.no_json, " + e.getMessage());
        }
    }

    @NotNull
    private String getName(JSONObject json) throws CallResponseException {
        if (! json.has(NAME_ELEMENT))
            throw new CallResponseException("no '" + NAME_ELEMENT + "' element");
        return json.optString(NAME_ELEMENT);
    }
}
