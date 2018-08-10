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
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;

import com.takisoft.fix.support.v7.preference.PreferenceFragmentCompat;

import static com.marv42.ebt.newnote.EbtNewNote.LOG_TAG;

public class Settings extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }

    public static class SettingsFragment extends PreferenceFragmentCompat
            implements SharedPreferences.OnSharedPreferenceChangeListener {
        @Override
        public void onResume() {
            super.onResume();
            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
            setSummary();
        }

        @Override
        public void onPause() {
            getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
            super.onPause();
        }

        @Override
        public void onCreatePreferencesFix(@Nullable Bundle savedInstanceState, String rootKey) {
            addPreferencesFromResource(R.xml.settings);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (sharedPreferences == getPreferenceScreen().getSharedPreferences()) {
                String emailKey = getString(R.string.pref_settings_email_key);
                if (key.equals(emailKey))
                    setSummary();
                if (key.equals(emailKey) || key.equals(getString(R.string.pref_settings_password_key))) {
                    String loginChangedKey = getString(R.string.pref_login_changed_key);
                    Log.d(LOG_TAG, loginChangedKey + ": " + sharedPreferences.getBoolean(loginChangedKey, true));
                    getPreferenceScreen().getSharedPreferences().edit().putBoolean(loginChangedKey, true).apply();
                }
            }
        }

        private void setSummary() {
            String emailKey = getString(R.string.pref_settings_email_key);
            String email = getPreferenceScreen().getSharedPreferences().getString(emailKey, "").trim();
            String summary = getString(R.string.settings_email_summary);
            if (! TextUtils.isEmpty(email))
                summary += getString(R.string.settings_summary_currently) + " " + email;
            (findPreference(emailKey)).setSummary(summary);
        }
    }
}
