/*******************************************************************************
 * Copyright (c) 2010 marvin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Contributors:
 *     marvin - initial API and implementation
 ******************************************************************************/

package com.marv42.ebt.newnote;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.widget.Toast;

import org.json.JSONObject;

import static android.widget.Toast.LENGTH_LONG;
import static androidx.preference.PreferenceManager.getDefaultSharedPreferences;
import static com.marv42.ebt.newnote.ApiCaller.ERROR;

public class LoginChecker extends AsyncTask<Void, Void, String> {
    private ThisApp mApp;
    private ApiCaller mApiCaller;

    LoginChecker(final ThisApp app, ApiCaller apiCaller) {
        mApp = app;
        mApiCaller = apiCaller;
    }

    @Override
    protected void onPreExecute() {
        Toast.makeText(mApp, mApp.getString(R.string.trying_login), LENGTH_LONG).show();
    }

    @Override
    protected String doInBackground(Void... params) {
        SharedPreferences preferences = getDefaultSharedPreferences(mApp);
        Editor editor = preferences.edit();
        String loginValuesOkKey = mApp.getString(R.string.pref_login_values_ok_key);
        JSONObject response = mApiCaller.callLogin();
        if (response.has(ERROR)) {
            String wrongLogin = mApp.getString(R.string.wrong_login_info);
            if (response.optString(ERROR).equals(wrongLogin)) {
                editor.putBoolean(loginValuesOkKey, false).apply();
                return wrongLogin;
            }
            return response.optString(ERROR);
        }
        editor.putBoolean(loginValuesOkKey, true).apply();
        String countryKey = mApp.getString(R.string.pref_country_key);
        String cityKey = mApp.getString(R.string.pref_city_key);
        String postalCodeKey = mApp.getString(R.string.pref_postal_code_key);
        if (TextUtils.isEmpty(preferences.getString(countryKey, "")) &&
                TextUtils.isEmpty(preferences.getString(cityKey, "")) &&
                TextUtils.isEmpty(preferences.getString(postalCodeKey, "")))
            editor.putString(countryKey, response.optString("my_country"))
                    .putString(cityKey, response.optString("my_city"))
                    .putString(postalCodeKey, response.optString("my_zip")).apply();
        return mApp.getString(R.string.hello) + " " +
                response.optString("username") + ". " + mApp.getString(R.string.logged_in);
    }

    @Override
    protected void onPostExecute(String string) {
        Toast.makeText(mApp, string, LENGTH_LONG).show();
    }
}
