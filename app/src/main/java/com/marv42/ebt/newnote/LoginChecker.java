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

import org.json.JSONObject;

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



public class LoginChecker extends AsyncTask<Void, Void, Boolean>
{
   private final Context mContext;



   public LoginChecker(final Context context)
   {
      mContext = context;
   }



   @Override
   protected void
   onPreExecute()
   {
      Toast.makeText(mContext, mContext.getString(R.string.trying_login), Toast.LENGTH_LONG).show();
   }



   @Override
   protected Boolean
   doInBackground(Void... params)
   {
      Log.d(EbtNewNote.LOG_TARGET, "checking login");
      Editor editor = PreferenceManager.getDefaultSharedPreferences(mContext).edit();

      String loginValuesOkKey = mContext.getString(R.string.pref_login_values_ok_key);

      if (ApiCaller.getInstance().callLogin())
      {
         if (! editor.putBoolean(loginValuesOkKey, true).commit())
            Log.e(EbtNewNote.LOG_TARGET, "editor's commit failed");

         return true;
      }

      if (! editor.putBoolean(loginValuesOkKey, false).commit())
         Log.e(EbtNewNote.LOG_TARGET, "editor's commit failed");

      return false;
   }



   @Override
   protected void
   onPostExecute(Boolean loginSuccessful)
   {
      ApiCaller apiCaller = ApiCaller.getInstance();

      if (loginSuccessful)
      {
         Log.d(EbtNewNote.LOG_TARGET, "successful");
         JSONObject result = apiCaller.getResult();

         Toast.makeText(mContext, mContext.getString(R.string.hello) + " " + result.optString("username") + ". " + mContext.getString(R.string.logged_in), Toast.LENGTH_LONG).show();

         new LocationValues(result.optString("my_country"),
                            result.optString("my_city"   ),
                            result.optString("my_zip"    ), false, mContext);
      }
      else
      {
         Log.d(EbtNewNote.LOG_TARGET, "not successful");
         String error = apiCaller.getError();
         if (error.equals(mContext.getString(R.string.wrong_password)))
            showWrongLoginDialog();
         else
            Toast.makeText(mContext, error, Toast.LENGTH_LONG).show();
      }

      SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
      String callingLoginKey = mContext.getString(R.string.pref_calling_login_key);
      if (! prefs.edit().putBoolean(callingLoginKey, false).commit())
         Log.e(EbtNewNote.LOG_TARGET, "editor's commit failed");
      Log.d(EbtNewNote.LOG_TARGET, callingLoginKey + ": " + prefs.getBoolean(callingLoginKey, false));
   }



   private void
   showWrongLoginDialog()
   {
      new AlertDialog.Builder(mContext).setTitle(mContext.getString(R.string.info))
         .setMessage(mContext.getString(R.string.wrong_login_info))
         .setPositiveButton(mContext.getString(R.string.yes),
            new DialogInterface.OnClickListener()
            {
               public void
               onClick(DialogInterface dialog, int which)
               {
                  mContext.startActivity(new Intent(mContext, Settings.class));
                  dialog.dismiss();
               }
            }
         )
         .setNegativeButton(mContext.getString(R.string.no),
            new DialogInterface.OnClickListener()
            {
               public void
               onClick(DialogInterface dialog, int which)
               {
                  dialog.dismiss();
               }
            }
         )
         .show();
   }
}
