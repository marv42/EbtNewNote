/*
 Copyright (c) 2010 - 2020 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

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

import com.marv42.ebt.newnote.exceptions.CallResponseException;
import com.marv42.ebt.newnote.exceptions.ErrorMessage;
import com.marv42.ebt.newnote.exceptions.HttpCallException;

import org.jetbrains.annotations.NotNull;

import static android.widget.Toast.LENGTH_LONG;
import static androidx.preference.PreferenceManager.getDefaultSharedPreferences;
import static com.marv42.ebt.newnote.exceptions.ErrorMessage.ERROR;

public class LoginChecker extends AsyncTask<Void, Void, String> {
    private ThisApp app;
    private ApiCaller apiCaller;

    public LoginChecker(@NonNull final ThisApp app, ApiCaller apiCaller) {
        this.app = app;
        this.apiCaller = apiCaller;
    }

    static void checkLoginInfo(@NonNull FragmentActivity activity) {
        Application app = activity.getApplication();
        if (!getDefaultSharedPreferences(app).getBoolean(app.getString(R.string.pref_login_values_ok_key), false)) {
            new AlertDialog.Builder(activity).setTitle(app.getString(R.string.invalid_login))
                    .setMessage(app.getString(R.string.wrong_login_info) + "." + app.getString(R.string.redirect_to_settings) + ".")
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
        Toast.makeText(app, app.getString(R.string.trying_login), LENGTH_LONG).show();
    }

    @Override
    protected String doInBackground(Void... params) {
        try {
            return tryToLogIn();
        } catch (HttpCallException | CallResponseException e) {
            return ERROR + e.getMessage();
        }
    }

    @NotNull
    private String tryToLogIn() throws HttpCallException, CallResponseException {
        LoginInfo loginInfo = apiCaller.callLogin();
        return setPreferences(loginInfo);
    }

    @NotNull
    private String setPreferences(LoginInfo loginInfo) {
        SharedPreferences preferences = getDefaultSharedPreferences(app);
        Editor editor = preferences.edit();
        String loginValuesOkKey = app.getString(R.string.pref_login_values_ok_key);
        if (loginInfo.sessionId.isEmpty()) {
            editor.putBoolean(loginValuesOkKey, false).apply();
            return ERROR + app.getString(R.string.wrong_login_info);
        }
        editor.putBoolean(loginValuesOkKey, true).apply();
        setLocationInfo(loginInfo, editor);
        return app.getString(R.string.logged_in) + "\n"
                + app.getString(R.string.hello) + " " + loginInfo.userName;
    }

    private void setLocationInfo(LoginInfo loginInfo, Editor editor) {
        editor.putString(app.getString(R.string.pref_country_key), loginInfo.myCountry)
                .putString(app.getString(R.string.pref_city_key), loginInfo.myCity)
                .putString(app.getString(R.string.pref_postal_code_key), loginInfo.myZip).apply();
    }

    @Override
    protected void onPostExecute(String text) {
        if (text.startsWith(ERROR))
            text = new ErrorMessage(app).getErrorMessage(text);
        Toast.makeText(app, text, LENGTH_LONG).show();
    }
}
