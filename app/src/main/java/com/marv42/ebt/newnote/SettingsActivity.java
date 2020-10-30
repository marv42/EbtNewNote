/*
 Copyright (c) 2010 - 2020 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.marv42.ebt.newnote.exceptions.NoPreferenceException;
import com.marv42.ebt.newnote.scanning.Keys;

import javax.inject.Inject;

import dagger.android.support.AndroidSupportInjection;
import dagger.android.support.DaggerAppCompatActivity;

import static android.content.Intent.ACTION_VIEW;
import static android.text.InputType.TYPE_CLASS_NUMBER;
import static android.text.InputType.TYPE_CLASS_TEXT;
import static android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL;
import static android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS;
import static android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD;

public class SettingsActivity extends DaggerAppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment()).commit();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    public static class SettingsFragment extends PreferenceFragmentCompat
            implements SharedPreferences.OnSharedPreferenceChangeListener {

        private static final String TAG = SettingsFragment.class.getSimpleName();
        @Inject
        ApiCaller apiCaller;
        @Inject
        EncryptedPreferenceDataStore dataStore;

        @Override
        public void onAttach(@NonNull Context context) {
            AndroidSupportInjection.inject(this);
            super.onAttach(context);
        }

        @Override
        public void onResume() {
            super.onResume();
            SharedPreferences sharedPreferences = dataStore.getSharedPreferences();
            if (sharedPreferences != null)
                sharedPreferences.registerOnSharedPreferenceChangeListener(this);
            checkOcrKey();
            checkEmailSummary();
            checkPasswordSummary();
            checkCommentSummary();
            checkOcrSummary();
            checkSubmittedSummary();
        }

        @Override
        public void onPause() {
            SharedPreferences sharedPreferences = dataStore.getSharedPreferences();
            if (sharedPreferences != null)
                sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
            super.onPause();
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            getPreferenceManager().setPreferenceDataStore(dataStore);
            addPreferencesFromResource(R.xml.settings);
            Context context = getContext();
            if (context != null)
                PreferenceManager.setDefaultValues(context, R.xml.settings, false);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (sharedPreferences == dataStore.getSharedPreferences()) {
                String emailKey = getString(R.string.pref_settings_email_key);
                String passwordKey = getString(R.string.pref_settings_password_key);
                if (key.equals(emailKey))
                    checkEmailSummary();
                if (key.equals(passwordKey))
                    checkPasswordSummary();
                if ((key.equals(emailKey) || key.equals(passwordKey)) &&
                        isEmailAndPasswordSet()) {
                    Activity activity = getActivity();
                    if (activity != null)
                        new LoginChecker((ThisApp) activity.getApplicationContext(), apiCaller).execute();
                }
                if (key.equals(getString(R.string.pref_settings_comment_key)))
                    checkCommentSummary();
                if (key.equals(getString(R.string.pref_settings_ocr_key)))
                    checkOcrSummary();
                if (key.equals(getString(R.string.pref_settings_show_submitted_key)))
                    checkSubmittedSummary();
            }
        }

        private EditTextPreference getPreference(int resourceId) throws NoPreferenceException {
            String key = getString(resourceId);
            EditTextPreference preference = findPreference(key);
            if (preference == null)
                throw new NoPreferenceException("No preference for " + key);
            return preference;
        }

        private void checkOcrKey() {
            try {
                String ocrKey = dataStore.get(R.string.pref_settings_ocr_key, "");
                if (ocrKey.isEmpty() && !Keys.OCR_SERVICE.isEmpty()) {
                    EditTextPreference preference = getPreference(R.string.pref_settings_ocr_key);
                    preference.setText(Keys.OCR_SERVICE);
                }
            } catch (NoPreferenceException e) {
                Log.w(TAG, e.getMessage());
                e.printStackTrace();
            }
        }

        private void checkEmailSummary() {
            try {
                EditTextPreference preference = getPreference(R.string.pref_settings_email_key);
                setEmailSummary(preference);
            } catch (NoPreferenceException e) {
                Log.w(TAG, e.getMessage());
                e.printStackTrace();
            }
        }

        private void setEmailSummary(EditTextPreference preference) {
            String email = dataStore.get(R.string.pref_settings_email_key, "");
            String summary = getString(R.string.settings_email_summary);
            if (!TextUtils.isEmpty(email))
                summary += getString(R.string.settings_currently) + " " + email.trim();
            preference.setSummary(summary);
            preference.setOnBindEditTextListener(editText ->
                    editText.setInputType(TYPE_CLASS_TEXT | TYPE_TEXT_VARIATION_EMAIL_ADDRESS));
        }

        private void checkPasswordSummary() {
            try {
                EditTextPreference preference = getPreference(R.string.pref_settings_password_key);
                setPasswordSummary(preference);
            } catch (NoPreferenceException e) {
                Log.w(TAG, e.getMessage());
                e.printStackTrace();
            }
        }

        private void setPasswordSummary(EditTextPreference preference) {
            String summary = getString(R.string.settings_password_summary);
            if (TextUtils.isEmpty(dataStore.get(R.string.pref_settings_password_key, "")))
                summary += getString(R.string.settings_currently_not_set);
            preference.setSummary(summary);
            preference.setOnBindEditTextListener(editText ->
                    editText.setInputType(TYPE_CLASS_TEXT | TYPE_TEXT_VARIATION_PASSWORD));
        }

        private void checkCommentSummary() {
            try {
                EditTextPreference preference = getPreference(R.string.pref_settings_comment_key);
                setCommentSummary(preference);
            } catch (NoPreferenceException e) {
                Log.w(TAG, e.getMessage());
                e.printStackTrace();
            }
        }

        private void setCommentSummary(EditTextPreference preference) {
            String comment = dataStore.get(R.string.pref_settings_comment_key, "");
            String summary = getString(R.string.settings_comment_summary);
            if (!TextUtils.isEmpty(comment))
                summary += getString(R.string.settings_currently) + " " + comment;
            preference.setSummary(summary);
        }

        private void checkOcrSummary() {
            try {
                EditTextPreference preference = getPreference(R.string.pref_settings_ocr_key);
                setOcrSummary(preference);
            } catch (NoPreferenceException e) {
                Log.w(TAG, e.getMessage());
                e.printStackTrace();
            }
        }

        private void setOcrSummary(EditTextPreference preference) {
            String summary = getString(R.string.settings_ocr_summary);
            if (TextUtils.isEmpty(dataStore.get(R.string.pref_settings_ocr_key, ""))) {
                String ocrServiceUrl = getString(R.string.settings_ocr_service_url);
                summary += " " + getString(R.string.settings_ocr_summary_no_key) + " " + ocrServiceUrl;
                preference.setIntent(new Intent().setAction(ACTION_VIEW)
                        .setData(Uri.parse(ocrServiceUrl)));
            }
            preference.setSummary(summary);
        }

        private void checkSubmittedSummary() {
            try {
                EditTextPreference preference = getPreference(R.string.pref_settings_show_submitted_key);
                setSubmittedSummary(preference);
            } catch (NoPreferenceException e) {
                Log.w(TAG, e.getMessage());
                e.printStackTrace();
            }
        }

        private void setSubmittedSummary(EditTextPreference preference) {
            String submitted = dataStore.get(R.string.pref_settings_show_submitted_key, "");
            String summary = getString(R.string.settings_submitted_summary);
            if (!TextUtils.isEmpty(submitted))
                summary += getString(R.string.settings_currently) + " " + submitted.trim();
            preference.setSummary(summary);
            preference.setOnBindEditTextListener(editText ->
                    editText.setInputType(TYPE_CLASS_NUMBER | TYPE_NUMBER_FLAG_DECIMAL));
        }

        private boolean isEmailAndPasswordSet() {
            return !TextUtils.isEmpty(dataStore.get(R.string.pref_settings_email_key, "")) &&
                    !TextUtils.isEmpty(dataStore.get(R.string.pref_settings_password_key, ""));
        }
    }
}
