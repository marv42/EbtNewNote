package com.marv42.ebt.newnote;

import org.json.JSONObject;

import okhttp3.Request;

import static com.marv42.ebt.newnote.ErrorMessage.ERROR;
import static com.marv42.ebt.newnote.JsonHelper.getJsonObject;

public class CountryCode {
    private static final String COUNTRIES_URL = "https://restcountries.eu/rest/v2/alpha/";
    private static final String ELEMENT_NAME = "name";

    public String convert(String countryCode) {
        Request request = new Request.Builder().url(COUNTRIES_URL + countryCode).build();
        String body = new HttpCaller().call(request);
        if (body.startsWith(ERROR)) {
            return body;
        }
        JSONObject json = getJsonObject(body);
        if (json == null || ! json.has(ELEMENT_NAME)) {
            return ERROR + "R.string.server_error";
        }
        return json.optString(ELEMENT_NAME);
    }
}
