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

import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

import org.json.JSONObject;

import static android.widget.Toast.LENGTH_LONG;
import static androidx.preference.PreferenceManager.getDefaultSharedPreferences;
import static com.marv42.ebt.newnote.ApiCaller.ERROR;

public class LoginChecker extends AsyncTask<Void, Void, String> {
    private ThisApp mApp;
    private ApiCaller mApiCaller;

    // TODO @Inject
    public LoginChecker(@NonNull final ThisApp app, ApiCaller apiCaller) {
        mApp = app;
        mApiCaller = apiCaller;
    }

    static void checkLoginInfo(@NonNull FragmentActivity activity) {
        Application app = activity.getApplication();
        if (!getDefaultSharedPreferences(app).getBoolean(app.getString(R.string.pref_login_values_ok_key), false)) {
            new AlertDialog.Builder(activity).setTitle(app.getString(R.string.invalid_login))
                    .setMessage(app.getString(R.string.wrong_login_info) + app.getString(R.string.change_login_info))
                    .setPositiveButton(app.getString(R.string.ok),
                            (dialog, which) -> {
                                activity.startActivity(new Intent(activity.getApplicationContext(),
                                        SettingsActivity.class));
                                dialog.dismiss();
                            })
                    .show();
        }
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
        editor.putString(mApp.getString(R.string.pref_country_key), response.optString("my_country"))
                .putString(mApp.getString(R.string.pref_city_key), response.optString("my_city"))
                .putString(mApp.getString(R.string.pref_postal_code_key), response.optString("my_zip")).apply();
        return mApp.getString(R.string.hello) + " " +
                response.optString("username") + ". " + mApp.getString(R.string.logged_in);
    }

    @Override
    protected void onPostExecute(String string) {
        Toast.makeText(mApp, string, LENGTH_LONG).show();
    }
}
