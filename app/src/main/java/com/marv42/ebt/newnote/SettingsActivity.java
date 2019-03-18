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

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.takisoft.fix.support.v7.preference.PreferenceFragmentCompat;

import javax.inject.Inject;

import dagger.android.support.AndroidSupportInjection;
import dagger.android.support.DaggerAppCompatActivity;

public class SettingsActivity extends DaggerAppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment()).commit();
    }

    public static class SettingsFragment extends PreferenceFragmentCompat
            implements SharedPreferences.OnSharedPreferenceChangeListener {
        @Inject
        ApiCaller mApiCaller;

        @Override
        public void onAttach(Context context) {
            AndroidSupportInjection.inject(this);
            super.onAttach(context);
        }

        @Override
        public void onResume() {
            super.onResume();
            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
            setEmailSummary();
            setPasswordSummary();
            setCommentSummary();
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
                String passwordKey = getString(R.string.pref_settings_password_key);
                if (key.equals(emailKey))
                    setEmailSummary();
                if (key.equals(passwordKey))
                    setPasswordSummary();
                if (key.equals(getString(R.string.pref_settings_comment_key)))
                    setCommentSummary();
                if ((key.equals(emailKey) || key.equals(passwordKey)) &&
                        ! TextUtils.isEmpty(sharedPreferences.getString(emailKey, "")) &&
                        ! TextUtils.isEmpty(sharedPreferences.getString(passwordKey, "")))
                    new LoginChecker((ThisApp) getActivity().getApplicationContext(), mApiCaller).execute();
            }
        }

        private void setEmailSummary() {
            String emailKey = getString(R.string.pref_settings_email_key);
            String email = getPreferenceScreen().getSharedPreferences().getString(emailKey, "").trim();
            String summary = getString(R.string.settings_email_summary);
            if (! TextUtils.isEmpty(email))
                summary += getString(R.string.settings_currently) + " " + email;
            (findPreference(emailKey)).setSummary(summary);
        }

        private void setPasswordSummary() {
            String passwordKey = getString(R.string.pref_settings_password_key);
            String summary = getString(R.string.settings_password_summary);
            if (TextUtils.isEmpty(getPreferenceScreen().getSharedPreferences().getString(
                    passwordKey, "")))
                summary += getString(R.string.settings_currently_not_set);
            (findPreference(passwordKey)).setSummary(summary);
        }

        private void setCommentSummary() {
            String commentKey = getString(R.string.pref_settings_comment_key);
            String comment = getPreferenceScreen().getSharedPreferences().getString(commentKey, "");
            String summary = getString(R.string.settings_comment_summary);
            if (! TextUtils.isEmpty(comment))
                summary += getString(R.string.settings_currently) + " " + comment;
            (findPreference(commentKey)).setSummary(summary);
        }
    }
}
