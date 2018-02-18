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
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;



public class Settings extends PreferenceActivity
{
   protected SettingsChangeListener mSettingsListener;
   // ... so it will not be gc'd (SharedPreferences keeps listeners in a WeakHashMap)
   
   
   
   @Override
   protected void
   onCreate(Bundle state)
   {
      super.onCreate(state);
      
      addPreferencesFromResource(R.xml.settings);
   }
   
   
   
   @Override
   protected void
   onResume()
   {
      super.onResume();
      
      mSettingsListener = new SettingsChangeListener();
      PreferenceManager.getDefaultSharedPreferences(this)
         .registerOnSharedPreferenceChangeListener(mSettingsListener);
      
      mSettingsListener.setSummary();
   }
   
   
   
   @Override
   protected void
   onPause()
   {
      PreferenceManager.getDefaultSharedPreferences(this)
         .unregisterOnSharedPreferenceChangeListener(mSettingsListener);
      
      finish();
      
      super.onPause();
   }
   
   
   
   private class SettingsChangeListener implements OnSharedPreferenceChangeListener
   {
      public void
      onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
      {
         SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(Settings.this);
         
         if (sharedPreferences == prefs)
         {
            String emailKey = getString(R.string.pref_settings_email_key);
            if (key.equals(emailKey))
               setSummary();
            if (key.equals(emailKey) ||
                key.equals(getString(R.string.pref_settings_password_key)))
            {
               String loginChangedKey = getString(R.string.pref_login_changed_key);
               //Log.d(EbtNewNote.LOG_TARGET, "[SettingsChangeListener] " + loginChangedKey + ": " + prefs.getBoolean(loginChangedKey, true));
               if (! prefs.edit().putBoolean(loginChangedKey, true).commit())
                  Log.e(EbtNewNote.LOG_TARGET, "Editor's commit failed");
            }
         }
      }
      
      
      
      private void
      setSummary()
      {
         String emailKey = getString(R.string.pref_settings_email_key);
         
         String email = PreferenceManager.getDefaultSharedPreferences(Settings.this)
            .getString(emailKey, "").trim();
         
         String summary = getString(R.string.settings_email_summary);
         if (! TextUtils.isEmpty(email))
            summary += getString(R.string.settings_summary_currently) + " " + email;
         
         ((Preference) findPreference(emailKey)).setSummary(summary);
      }
   }
}
