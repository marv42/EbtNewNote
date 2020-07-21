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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceFragmentCompat;

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
        @Inject
        ApiCaller mApiCaller;
        @Inject
        EncryptedPreferenceDataStore mDataStore;

        @Override
        public void onAttach(@NonNull Context context) {
            AndroidSupportInjection.inject(this);
            super.onAttach(context);
        }

        @Override
        public void onResume() {
            super.onResume();
            SharedPreferences sharedPreferences = mDataStore.getSharedPreferences();
            if (sharedPreferences != null)
                sharedPreferences.registerOnSharedPreferenceChangeListener(this);
            checkOcrKey();
            setEmailSummary();
            setPasswordSummary();
            setCommentSummary();
            setOcrSummary();
            setSubmittedSummary();
        }

        private void checkOcrKey() {
            String ocrKey = mDataStore.get(R.string.pref_settings_ocr_key, "");
            if (ocrKey.isEmpty() && !Keys.OCR_SERVICE.isEmpty())
                mDataStore.putStringById(R.string.pref_settings_ocr_key, Keys.OCR_SERVICE);
        }

        @Override
        public void onPause() {
            SharedPreferences sharedPreferences = mDataStore.getSharedPreferences();
            if (sharedPreferences != null)
                sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
            super.onPause();
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            getPreferenceManager().setPreferenceDataStore(mDataStore);
            addPreferencesFromResource(R.xml.settings);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (sharedPreferences == mDataStore.getSharedPreferences()) {
                String emailKey = getString(R.string.pref_settings_email_key);
                String passwordKey = getString(R.string.pref_settings_password_key);
                if (key.equals(emailKey))
                    setEmailSummary();
                if (key.equals(passwordKey))
                    setPasswordSummary();
                if ((key.equals(emailKey) || key.equals(passwordKey)) &&
                        !TextUtils.isEmpty(mDataStore.getString(emailKey, "")) &&
                        !TextUtils.isEmpty(mDataStore.getString(passwordKey, ""))) {
                    Activity activity = getActivity();
                    if (activity != null)
                        new LoginChecker((ThisApp) activity.getApplicationContext(), mApiCaller).execute();
                }
                if (key.equals(getString(R.string.pref_settings_comment_key)))
                    setCommentSummary();
                if (key.equals(getString(R.string.pref_settings_ocr_key)))
                    setOcrSummary();
                if (key.equals(getString(R.string.pref_settings_submitted_key)))
                    setSubmittedSummary();
            }
        }

        private void setEmailSummary() {
            String emailKey = getString(R.string.pref_settings_email_key);
            EditTextPreference preference = findPreference(emailKey);
            if (preference != null) {
                String email = mDataStore.getString(emailKey, "");
                String summary = getString(R.string.settings_email_summary);
                if (email != null && !TextUtils.isEmpty(email))
                    summary += getString(R.string.settings_currently) + " " + email.trim();
                preference.setSummary(summary);
                preference.setOnBindEditTextListener(editText ->
                        editText.setInputType(TYPE_CLASS_TEXT | TYPE_TEXT_VARIATION_EMAIL_ADDRESS));
            }
        }

        private void setPasswordSummary() {
            String passwordKey = getString(R.string.pref_settings_password_key);
            EditTextPreference preference = findPreference(passwordKey);
            if (preference != null) {
                String summary = getString(R.string.settings_password_summary);
                if (TextUtils.isEmpty(mDataStore.getString(passwordKey, "")))
                    summary += getString(R.string.settings_currently_not_set);
                preference.setSummary(summary);
                preference.setOnBindEditTextListener(editText ->
                        editText.setInputType(TYPE_CLASS_TEXT | TYPE_TEXT_VARIATION_PASSWORD));
            }
        }

        private void setCommentSummary() {
            String commentKey = getString(R.string.pref_settings_comment_key);
            String comment = mDataStore.getString(commentKey, "");
            String summary = getString(R.string.settings_comment_summary);
            if (!TextUtils.isEmpty(comment))
                summary += getString(R.string.settings_currently) + " " + comment;
            EditTextPreference preference = findPreference(commentKey);
            if (preference != null)
                preference.setSummary(summary);
        }

        private void setOcrSummary() {
            String ocrKeyKey = getString(R.string.pref_settings_ocr_key);
            EditTextPreference preference = findPreference(ocrKeyKey);
            if (preference != null) {
                String summary = getString(R.string.settings_ocr_summary);
                if (TextUtils.isEmpty(mDataStore.getString(ocrKeyKey, ""))) {
                    String ocrServiceUrl = getString(R.string.settings_ocr_service_url);
                    summary += " " + getString(R.string.settings_ocr_summary_no_key) + " " + ocrServiceUrl;
                    preference.setIntent(new Intent().setAction(ACTION_VIEW)
                            .setData(Uri.parse(ocrServiceUrl)));
                }
                preference.setSummary(summary);
            }
        }

        private void setSubmittedSummary() {
            String submittedKey = getString(R.string.pref_settings_submitted_key);
            EditTextPreference preference = findPreference(submittedKey);
            if (preference != null) {
                String submitted = mDataStore.getString(submittedKey, "");
                String summary = getString(R.string.settings_submitted_summary);
                if (submitted != null && !TextUtils.isEmpty(submitted))
                    summary += getString(R.string.settings_currently) + " " + submitted.trim();
                preference.setSummary(summary);
                preference.setOnBindEditTextListener(editText ->
                        editText.setInputType(TYPE_CLASS_NUMBER | TYPE_NUMBER_FLAG_DECIMAL));
            }
        }
    }
}