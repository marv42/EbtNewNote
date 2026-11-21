/*
 Copyright (c) 2010 - 2026 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote.location;

import com.marv42.ebt.newnote.exceptions.CallResponseException;
import com.marv42.ebt.newnote.exceptions.HttpCallException;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class CountryCodeResolver {

    private final CountryCodeService restCountries;
    private final CountryCodeService countryLayer;

    public CountryCodeResolver() {
        this(new CountryCodeRestCountries(), new CountryCodeCountryLayer());
    }

    CountryCodeResolver(CountryCodeService restCountries, CountryCodeService countryLayer) {
        this.restCountries = restCountries;
        this.countryLayer = countryLayer;
    }

    public String convert(String countryCode, String restCountriesKey, String countryLayerKey)
            throws HttpCallException, CallResponseException {
        List<Throwable> errors = new ArrayList<>();
        String name = tryConvert(restCountries, countryCode, restCountriesKey, errors);
        if (name != null)
            return name;
        name = tryConvert(countryLayer, countryCode, countryLayerKey, errors);
        if (name != null)
            return name;
        Throwable lastError = lastError(errors);
        if (lastError instanceof HttpCallException)
            throw (HttpCallException) lastError;
        if (lastError instanceof CallResponseException)
            throw (CallResponseException) lastError;
        throw new CallResponseException("no country resolution service key");
    }

    private String tryConvert(CountryCodeService service, String countryCode, String apiKey,
                              List<Throwable> errors) {
        if (apiKey.isEmpty())
            return null;
        try {
            return service.convert(countryCode, apiKey);
        } catch (HttpCallException | CallResponseException e) {
            errors.add(e);
            return null;
        }
    }

    @NotNull
    private Throwable lastError(List<Throwable> errors) {
        if (errors.isEmpty())
            return new CallResponseException("no country resolution service key");
        return errors.get(errors.size() - 1);
    }
}
