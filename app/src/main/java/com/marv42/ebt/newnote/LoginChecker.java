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
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.widget.Toast;

import org.json.JSONObject;

import java.lang.ref.WeakReference;

import javax.inject.Inject;

import static android.widget.Toast.LENGTH_LONG;
import static com.marv42.ebt.newnote.ApiCaller.ERROR;

public class LoginChecker extends AsyncTask<Void, Void, Boolean> {
    public interface Callback {
        void onLoginFailed();
    }

    private WeakReference<EbtNewNote> mEbtNewNote;
    private ApiCaller mApiCaller;
    private JSONObject mResponse;

    @Inject
    LoginChecker(final EbtNewNote ebtNewNote, ApiCaller apiCaller) {
        mEbtNewNote = new WeakReference<>(ebtNewNote);
        mApiCaller = apiCaller;
    }

    @Override
    protected void onPreExecute() {
        Toast.makeText(mEbtNewNote.get(), mEbtNewNote.get().getString(R.string.trying_login), LENGTH_LONG).show();
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        EbtNewNote context = mEbtNewNote.get();
        Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        String loginValuesOkKey = context.getString(R.string.pref_login_values_ok_key);
        mResponse = mApiCaller.callLogin();
        if (!mResponse.has(ERROR)) {
            editor.putBoolean(loginValuesOkKey, true).apply();
            return true;
        }
        if (mResponse.has(ERROR) &&
                mResponse.optString(ERROR).equals(context.getString(R.string.wrong_password)))
            editor.putBoolean(loginValuesOkKey, false).apply();
        return false;
    }

    @Override
    protected void onPostExecute(Boolean loginSuccessful) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mEbtNewNote.get());
        if (loginSuccessful) {
            Toast.makeText(mEbtNewNote.get(), mEbtNewNote.get().getString(R.string.hello) + " " +
                            mResponse.optString("username") + ". " + mEbtNewNote.get().getString(R.string.logged_in),
                    LENGTH_LONG).show();
            String countryKey = mEbtNewNote.get().getString(R.string.pref_country_key);
            String cityKey = mEbtNewNote.get().getString(R.string.pref_city_key);
            String postalCodeKey = mEbtNewNote.get().getString(R.string.pref_postal_code_key);
            if (TextUtils.isEmpty(preferences.getString(countryKey, "")) &&
                    TextUtils.isEmpty(preferences.getString(cityKey, "")) &&
                    TextUtils.isEmpty(preferences.getString(postalCodeKey, "")))
                preferences.edit()
                        .putString(countryKey, mResponse.optString("my_country"))
                        .putString(cityKey, mResponse.optString("my_city"))
                        .putString(postalCodeKey, mResponse.optString("my_zip")).apply();
        } else {
            String error = mResponse.optString(ERROR);
            if (error.equals(mEbtNewNote.get().getString(R.string.wrong_password)))
                ((Callback) mEbtNewNote.get()).onLoginFailed();
            else
                Toast.makeText(mEbtNewNote.get(), error, LENGTH_LONG).show();
        }
        preferences.edit().putBoolean(mEbtNewNote.get().getString(R.string.pref_calling_login_key), false).apply();
    }
}
