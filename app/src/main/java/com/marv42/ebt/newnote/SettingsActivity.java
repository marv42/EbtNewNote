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
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.security.crypto.EncryptedSharedPreferences;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.security.GeneralSecurityException;

import javax.inject.Inject;

import dagger.android.support.AndroidSupportInjection;
import dagger.android.support.DaggerAppCompatActivity;

import static android.content.Intent.ACTION_VIEW;
import static android.text.InputType.TYPE_CLASS_NUMBER;
import static android.text.InputType.TYPE_CLASS_TEXT;
import static android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL;
import static android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS;
import static android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD;
import static com.marv42.ebt.newnote.scanning.Keys.OCR_SERVICE;

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
    public boolean onSupportNavigateUp(){
        finish();
        return true;
    }

    public static class SettingsFragment extends PreferenceFragmentCompat
            implements SharedPreferences.OnSharedPreferenceChangeListener {
        @Inject
        ApiCaller mApiCaller;
//        @Inject
//        EncryptedSharedPreferencesProvider mEncryptedSharedPreferencesProvider;

        private SharedPreferences mSharedPreferences;

        @Override
        public void onAttach(@NonNull Context context) {
            AndroidSupportInjection.inject(this);
            super.onAttach(context);
        }

        @Override
        public void onResume() {
            super.onResume();
            mSharedPreferences = getPreferenceScreen().getSharedPreferences();
//            try {
//                mSharedPreferences = mEncryptedSharedPreferencesProvider.getEncryptedSharedPreferences();
//            } catch (GeneralSecurityException | IOException e) {
//                e.printStackTrace();
//            }
            mSharedPreferences.registerOnSharedPreferenceChangeListener(this);
            checkOcrKey();
            setEmailSummary();
            setPasswordSummary();
            setCommentSummary();
            setOcrSummary();
            setSubmittedSummary();
        }

        private void checkOcrKey() {
            String ocrKeyKey = getString(R.string.pref_settings_ocr_key);
            String ocrKey = mSharedPreferences.getString(ocrKeyKey, "");
            if (ocrKey != null && ocrKey.isEmpty() && ! OCR_SERVICE.isEmpty())
                mSharedPreferences.edit().putString(ocrKeyKey, OCR_SERVICE).apply();
        }

        @Override
        public void onPause() {
            if (mSharedPreferences != null)
                mSharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
            super.onPause();
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            addPreferencesFromResource(R.xml.settings);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (sharedPreferences == mSharedPreferences) {
                String emailKey = getString(R.string.pref_settings_email_key);
                String passwordKey = getString(R.string.pref_settings_password_key);
                if (key.equals(emailKey))
                    setEmailSummary();
                if (key.equals(passwordKey))
                    setPasswordSummary();
                if ((key.equals(emailKey) || key.equals(passwordKey)) &&
                        ! TextUtils.isEmpty(sharedPreferences.getString(emailKey, "")) &&
                        ! TextUtils.isEmpty(sharedPreferences.getString(passwordKey, "")))
                    new LoginChecker((ThisApp) getActivity().getApplicationContext(), mApiCaller).execute();
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
                String email = mSharedPreferences.getString(emailKey, "");
                String summary = getString(R.string.settings_email_summary);
                if (email != null && ! TextUtils.isEmpty(email))
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
                if (TextUtils.isEmpty(mSharedPreferences.getString(passwordKey, "")))
                    summary += getString(R.string.settings_currently_not_set);
                preference.setSummary(summary);
                preference.setOnBindEditTextListener(editText ->
                        editText.setInputType(TYPE_CLASS_TEXT | TYPE_TEXT_VARIATION_PASSWORD));
            }
        }

        private void setCommentSummary() {
            String commentKey = getString(R.string.pref_settings_comment_key);
            String comment = mSharedPreferences.getString(commentKey, "");
            String summary = getString(R.string.settings_comment_summary);
            if (! TextUtils.isEmpty(comment))
                summary += getString(R.string.settings_currently) + " " + comment;
            EditTextPreference preference = findPreference(commentKey);
            if (preference != null)
                preference.setSummary(summary);
        }

        private void setOcrSummary() {
            String ocrKeyKey = getString(R.string.pref_settings_ocr_key);
            EditTextPreference preference = findPreference(ocrKeyKey);
            if (preference != null ) {
                String ocrServiceKey = mSharedPreferences.getString(ocrKeyKey, "");
                String summary = getString(R.string.settings_ocr_summary);
                String ocrServiceUrl = getString(R.string.settings_ocr_service_url);
                if (TextUtils.isEmpty(ocrServiceKey))
                    summary += " " + getString(R.string.settings_ocr_summary_no_key) + " " + ocrServiceUrl;
                preference.setSummary(summary);
                if (TextUtils.isEmpty(ocrServiceKey))
                    preference.setIntent(new Intent().setAction(ACTION_VIEW)
                            .setData(Uri.parse(ocrServiceUrl)));
            }
        }

        private void setSubmittedSummary() {
            String submittedKey = getString(R.string.pref_settings_submitted_key);
            EditTextPreference preference = findPreference(submittedKey);
            if (preference != null) {
                String submitted = mSharedPreferences.getString(submittedKey, "");
                String summary = getString(R.string.settings_submitted_summary);
                if (submitted != null && ! TextUtils.isEmpty(submitted))
                    summary += getString(R.string.settings_currently) + " " + submitted.trim();
                preference.setSummary(summary);
                preference.setOnBindEditTextListener(editText ->
                        editText.setInputType(TYPE_CLASS_NUMBER | TYPE_NUMBER_FLAG_DECIMAL));
            }
        }
    }
}
