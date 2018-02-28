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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONObject;

import java.lang.ref.WeakReference;

import javax.inject.Inject;

import static com.marv42.ebt.newnote.EbtNewNote.LOG_TAG;

public class LoginChecker extends AsyncTask<Void, Void, Boolean> {
    private WeakReference<Context> mContext;
    private ApiCaller mApiCaller;

    @Inject
    LoginChecker(final Context context, ApiCaller apiCaller) {
        mContext = new WeakReference<>(context);
        mApiCaller = apiCaller;
    }

    @Override
    protected void onPreExecute() {
        Toast.makeText(mContext.get(), mContext.get().getString(R.string.trying_login), Toast.LENGTH_LONG).show();
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        Log.d(LOG_TAG, "checking login");
        Editor editor = PreferenceManager.getDefaultSharedPreferences(mContext.get()).edit();
        String loginValuesOkKey = mContext.get().getString(R.string.pref_login_values_ok_key);

        if (mApiCaller.callLogin()) {
            if (! editor.putBoolean(loginValuesOkKey, true).commit())
                Log.e(LOG_TAG, "editor's commit failed");
            return true;
        }
        if (! editor.putBoolean(loginValuesOkKey, false).commit())
            Log.e(LOG_TAG, "editor's commit failed");
        return false;
    }

    @Override
    protected void onPostExecute(Boolean loginSuccessful) {
        if (loginSuccessful) {
            Log.d(LOG_TAG, "successful");
            JSONObject result = mApiCaller.getResult();
            Toast.makeText(mContext.get(), mContext.get().getString(R.string.hello) + " " +
                            result.optString("username") + ". " + mContext.get().getString(R.string.logged_in),
                    Toast.LENGTH_LONG).show();
            new LocationValues(result.optString("my_country"),
                    result.optString("my_city"   ),
                    result.optString("my_zip"    ), false, mContext.get());
        } else {
            Log.d(LOG_TAG, "not successful");
            String error = mApiCaller.getError();
            if (error.equals(mContext.get().getString(R.string.wrong_password)))
                showWrongLoginDialog();
            else
                Toast.makeText(mContext.get(), error, Toast.LENGTH_LONG).show();
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext.get());
        String callingLoginKey = mContext.get().getString(R.string.pref_calling_login_key);
        if (! prefs.edit().putBoolean(callingLoginKey, false).commit())
            Log.e(LOG_TAG, "editor's commit failed");
        Log.d(LOG_TAG, callingLoginKey + ": " + prefs.getBoolean(callingLoginKey, false));
    }

    private void showWrongLoginDialog() {
        new AlertDialog.Builder(mContext.get()).setTitle(mContext.get().getString(R.string.info))
                .setMessage(mContext.get().getString(R.string.wrong_login_info))
                .setPositiveButton(mContext.get().getString(R.string.yes),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                mContext.get().startActivity(new Intent(mContext.get(), Settings.class));
                                dialog.dismiss();
                            }
                        }
                )
                .setNegativeButton(mContext.get().getString(R.string.no), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        }
                )
                .show();
    }
}
