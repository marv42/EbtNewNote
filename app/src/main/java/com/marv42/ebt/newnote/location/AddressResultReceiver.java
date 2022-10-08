/*
 Copyright (c) 2010 - 2022 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote.location;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.widget.Toast;

import com.google.gson.Gson;
import com.marv42.ebt.newnote.R;
import com.marv42.ebt.newnote.data.LocationValues;
import com.marv42.ebt.newnote.exceptions.ErrorMessage;

import static android.widget.Toast.LENGTH_LONG;
import static androidx.preference.PreferenceManager.getDefaultSharedPreferences;
import static com.marv42.ebt.newnote.ThisApp.RESULT_CODE_SUCCESS;
import static com.marv42.ebt.newnote.exceptions.ErrorMessage.ERROR;
import static com.marv42.ebt.newnote.location.FetchAddressIntentService.RESULT_DATA_KEY;

public class AddressResultReceiver extends ResultReceiver {

    private final Context context;

    public AddressResultReceiver(Context context, Handler handler) {
        super(handler);
        this.context = context;
    }

    @Override
    protected void onReceiveResult(int resultCode, Bundle resultData) {
        if (resultCode != RESULT_CODE_SUCCESS) {
            showErrorMessage(resultData);
            return;
        }
        String addressOutput = resultData.getString(RESULT_DATA_KEY);
        if (addressOutput == null) {
            Toast.makeText(context, R.string.internal_error, LENGTH_LONG).show();
            return;
        }
        putLocationValues(addressOutput);
    }

    private void showErrorMessage(Bundle resultData) {
        String text = resultData.getString(RESULT_DATA_KEY);
        if (text == null)
            text = context.getString(R.string.internal_error);
        Toast.makeText(context, new ErrorMessage(context).getErrorMessage(text), LENGTH_LONG).show();
    }

    private void putLocationValues(String addressOutput) {
        LocationValues location = new Gson().fromJson(addressOutput, LocationValues.class);
        String country = location.country;
        if (country.startsWith(ERROR))
            Toast.makeText(context, new ErrorMessage(context).getErrorMessage(country), LENGTH_LONG).show();
        else
            getDefaultSharedPreferences(context).edit()
                    .putString(context.getString(R.string.pref_country_key), country)
                    .apply();
        getDefaultSharedPreferences(context).edit()
                .putString(context.getString(R.string.pref_city_key), location.city)
                .putString(context.getString(R.string.pref_postal_code_key), location.postalCode)
                .apply();
    }
}
