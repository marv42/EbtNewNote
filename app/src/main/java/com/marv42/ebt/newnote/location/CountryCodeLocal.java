/*
 Copyright (c) 2010 - 2026 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote.location;

import android.os.Build;

import androidx.annotation.RequiresApi;

import java.util.Locale;
import java.util.MissingResourceException;

public class CountryCodeLocal {

    @RequiresApi(api = Build.VERSION_CODES.BAKLAVA)
    public String convert(String countryCode) {
        String twoLetterCode = CountryCodeConverter.convertThreeToTwoLetterCode(countryCode);
        if (twoLetterCode != null)
            countryCode = twoLetterCode;
        Locale locale = Locale.of("*", countryCode);
        return locale.getDisplayCountry(Locale.of("eng", "US"));
    }
 }

class CountryCodeConverter {
    static String convertThreeToTwoLetterCode(String threeLetterCode) {
        Locale[] locales = Locale.getAvailableLocales();
        for (Locale l : locales)
            try {
                if (l.getISO3Country().equalsIgnoreCase(threeLetterCode))
                    return l.getCountry();
            }
            catch (MissingResourceException e) {
                // ignore, continue
            }
        return null;
    }
}
