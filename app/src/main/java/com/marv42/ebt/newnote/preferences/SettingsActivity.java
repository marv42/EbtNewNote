/*
 Copyright (c) 2010 - 2026 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote.preferences;

import static android.content.Intent.ACTION_VIEW;
import static android.text.InputType.TYPE_CLASS_NUMBER;
import static android.text.InputType.TYPE_CLASS_TEXT;
import static android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS;
import static android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD;

import static com.marv42.ebt.newnote.MyOnApplyWindowInsetsListener.getOnApplyWindowInsetsListener;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreference;

import com.marv42.ebt.newnote.ApiCaller;
import com.marv42.ebt.newnote.LoginChecker;
import com.marv42.ebt.newnote.R;
import com.marv42.ebt.newnote.ThisApp;
import com.marv42.ebt.newnote.exceptions.NoPreferenceException;
import com.marv42.ebt.newnote.scanning.Keys;

import javax.inject.Inject;

import dagger.android.support.AndroidSupportInjection;
import dagger.android.support.DaggerAppCompatActivity;

public class SettingsActivity extends DaggerAppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        ViewGroup content = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(content, getOnApplyWindowInsetsListener());
        getSupportFragmentManager()
                .beginTransaction()
                .setReorderingAllowed(true)
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
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
            registerOnSharedPreferenceChangeListener();
            setInputTypes();
            checkOcrKey();
            checkCountryKey();
            checkEmailSummary();
            checkPasswordSummary();
            checkCommentSummary();
            checkOcrServiceKeySummary();
            checkOcrOnline();
            checkCountrySummary();
            checkSubmittedSummary();
        }

        private void registerOnSharedPreferenceChangeListener() {
            SharedPreferences sharedPreferences = dataStore.getSharedPreferences();
            if (sharedPreferences != null)
                sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onPause() {
            unregisterOnSharedPreferenceChangeListener();
            super.onPause();
        }

        private void unregisterOnSharedPreferenceChangeListener() {
            SharedPreferences sharedPreferences = dataStore.getSharedPreferences();
            if (sharedPreferences != null)
                sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
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
            if (sharedPreferences != dataStore.getSharedPreferences())
                return;
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
            if (key.equals(getString(R.string.pref_settings_ocr_online_key)))
                checkOcrOnline();
            if (key.equals(getString(R.string.pref_settings_ocr_service_key)))
                checkOcrServiceKeySummary();
            if (key.equals(getString(R.string.pref_settings_country_key)))
                checkCountrySummary();
            if (key.equals(getString(R.string.pref_settings_show_submitted_key)))
                checkSubmittedSummary();
        }

        private void setInputTypes() {
            setOnBindListeners(R.string.pref_settings_email_key, TYPE_CLASS_TEXT | TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
            setOnBindListeners(R.string.pref_settings_password_key, TYPE_CLASS_TEXT | TYPE_TEXT_VARIATION_PASSWORD);
            setOnBindListeners(R.string.pref_settings_show_submitted_key, TYPE_CLASS_NUMBER);
        }

        private void setOnBindListeners(int resourceId, int type) {
            try {
                EditTextPreference preference = (EditTextPreference) getPreference(resourceId);
                preference.setOnBindEditTextListener(editText -> editText.setInputType(type));
            } catch (NoPreferenceException e) {
                Log.w(TAG, e.getMessage());
            }
        }

        @NonNull
        private Preference getPreference(int resourceId) throws NoPreferenceException {
            String key = getString(resourceId);
            Preference preference = findPreference(key);
            if (preference == null)
                throw new NoPreferenceException("No preference for " + key);
            return preference;
        }

        private void checkOcrKey() {
            try {
                EditTextPreference ocrKeyPreference = (EditTextPreference) getPreference(R.string.pref_settings_ocr_service_key);
                ocrKeyPreference.setEnabled(false);
                String ocrKey = getOcrServiceKey();
                if (ocrKey.isEmpty() && !Keys.OCR_SERVICE.isEmpty())
                    ocrKeyPreference.setText(Keys.OCR_SERVICE);
            } catch (NoPreferenceException e) {
                Log.w(TAG, e.getMessage());
            }
        }

        private void checkCountryKey() {
            try {
                String countryKey = dataStore.get(R.string.pref_settings_country_key, "");
                if (countryKey.isEmpty() && !Keys.COUNTRY_SERVICE.isEmpty()) {
                    EditTextPreference countryPreference = (EditTextPreference) getPreference(R.string.pref_settings_country_key);
                    countryPreference.setText(Keys.COUNTRY_SERVICE);
                }
            } catch (NoPreferenceException e) {
                Log.w(TAG, e.getMessage());
            }
        }

        private void checkEmailSummary() {
            try {
                Preference emailPreference = getPreference(R.string.pref_settings_email_key);
                setEmailSummary(emailPreference);
            } catch (NoPreferenceException e) {
                Log.w(TAG, e.getMessage());
            }
        }

        private void setEmailSummary(Preference preference) {
            String email = dataStore.get(R.string.pref_settings_email_key, "");
            String summary = getString(R.string.settings_email_summary);
            if (!TextUtils.isEmpty(email))
                summary += getString(R.string.settings_currently) + " " + email.trim();
            preference.setSummary(summary);
        }

        private void checkPasswordSummary() {
            try {
                Preference passwordPreference = getPreference(R.string.pref_settings_password_key);
                setPasswordSummary(passwordPreference);
            } catch (NoPreferenceException e) {
                Log.w(TAG, e.getMessage());
            }
        }

        private void setPasswordSummary(Preference preference) {
            String summary = getString(R.string.settings_password_summary);
            if (TextUtils.isEmpty(dataStore.get(R.string.pref_settings_password_key, "")))
                summary += getString(R.string.settings_currently_not_set);
            preference.setSummary(summary);
        }

        private void checkCommentSummary() {
            try {
                Preference commentPreference = getPreference(R.string.pref_settings_comment_key);
                setCommentSummary(commentPreference);
            } catch (NoPreferenceException e) {
                Log.w(TAG, e.getMessage());
            }
        }

        private void setCommentSummary(Preference preference) {
            String comment = dataStore.get(R.string.pref_settings_comment_key, "");
            String summary = getString(R.string.settings_comment_summary);
            if (!TextUtils.isEmpty(comment))
                summary += getString(R.string.settings_currently) + " " + comment;
            preference.setSummary(summary);
        }

        private void checkOcrOnline() {
            try {
                final String key = getString(R.string.pref_settings_ocr_online_key);
                SwitchPreference preference = findPreference(key);
                if (preference == null)
                    throw new NoPreferenceException("No preference for " + key);
                Preference serviceKeyPreference = getPreference(R.string.pref_settings_ocr_service_key);
                serviceKeyPreference.setEnabled(preference.isChecked());
                Preference postponeOcrPreference = getPreference(R.string.pref_settings_ocr_postpone_key);
                postponeOcrPreference.setEnabled(preference.isChecked());
            } catch (NoPreferenceException e) {
                Log.w(TAG, e.getMessage());
            }
        }

        private void checkOcrServiceKeySummary() {
            try {
                Preference ocrKeyPreference = getPreference(R.string.pref_settings_ocr_service_key);
                setOcrServiceKeySummary(ocrKeyPreference);
            } catch (NoPreferenceException e) {
                Log.w(TAG, e.getMessage());
            }
        }

        private void setOcrServiceKeySummary(Preference preference) {
            setServiceKeySummary(preference, R.string.settings_ocr_service_key_summary,
                    R.string.settings_ocr_service_url, R.string.settings_ocr_summary_no_key);
        }

        private void setServiceKeySummary(Preference preference, int resourceIdSummary,
                                          int resourceIdUrl, int resourceIdSummaryNoKey) {
            String summary = getString(resourceIdSummary);
            if (isServiceKeyNotSet(resourceIdSummary)) {
                String serviceUrl = getString(resourceIdUrl);
                summary += ".\n" + getString(resourceIdSummaryNoKey) + " " + serviceUrl;
                preference.setIntent(new Intent().setAction(ACTION_VIEW).setData(Uri.parse(serviceUrl)));
            } else
                preference.setIntent(null);
            preference.setSummary(summary);
        }

        private boolean isServiceKeyNotSet(int resourceIdSummary) {
            if (resourceIdSummary == R.string.settings_ocr_service_key_summary)
                return getOcrServiceKey().isEmpty();
            if (resourceIdSummary == R.string.settings_country_summary)
                return getCountryResolutionServiceKey().isEmpty();
            throw new IllegalArgumentException("resourceIdSummary");
        }

        @NonNull
        private String getOcrServiceKey() {
            return dataStore.get(R.string.pref_settings_ocr_service_key, "");
        }

        @NonNull
        private String getCountryResolutionServiceKey() {
            return dataStore.get(R.string.pref_settings_country_key, "");
        }

        private void checkCountrySummary() {
            try {
                Preference preference = getPreference(R.string.pref_settings_country_key);
                setCountrySummary(preference);
            } catch (NoPreferenceException e) {
                Log.w(TAG, e.getMessage());
            }
        }

        private void setCountrySummary(Preference preference) {
            setServiceKeySummary(preference, R.string.settings_country_summary,
                    R.string.settings_country_service_url, R.string.settings_country_summary_no_key);
        }

        private void checkSubmittedSummary() {
            try {
                Preference preference = getPreference(R.string.pref_settings_show_submitted_key);
                setSubmittedSummary(preference);
            } catch (NoPreferenceException e) {
                Log.w(TAG, e.getMessage());
            }
        }

        private void setSubmittedSummary(Preference preference) {
            String submitted = dataStore.get(R.string.pref_settings_show_submitted_key, "");
            String summary = getString(R.string.settings_submitted_summary);
            if (!TextUtils.isEmpty(submitted))
                summary += getString(R.string.settings_currently) + " " + submitted.trim();
            preference.setSummary(summary);
        }

        private boolean isEmailAndPasswordSet() {
            return !TextUtils.isEmpty(dataStore.get(R.string.pref_settings_email_key, "")) &&
                    !TextUtils.isEmpty(dataStore.get(R.string.pref_settings_password_key, ""));
        }
    }
}
