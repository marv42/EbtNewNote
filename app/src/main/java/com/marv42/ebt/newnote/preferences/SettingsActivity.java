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
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.marv42.ebt.newnote.ApiCaller;
import com.marv42.ebt.newnote.LoginChecker;
import com.marv42.ebt.newnote.Predefines;
import com.marv42.ebt.newnote.R;
import com.marv42.ebt.newnote.ThisApp;
import com.marv42.ebt.newnote.exceptions.NoPreferenceException;

import java.util.function.Consumer;

import javax.inject.Inject;

import dagger.android.support.AndroidSupportInjection;
import dagger.android.support.DaggerAppCompatActivity;

public class SettingsActivity extends DaggerAppCompatActivity {

    @Inject
    EncryptedPreferenceDataStore dataStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Color.fromKey(dataStore.get(R.string.pref_settings_color_key,
                Color.DEFAULT.getKey())).applyTo(this);
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
            checkKeys();
            checkSummaries();
            checkOcrOnline();
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA)
                removeMiscCategory();
            Context context = getContext();
            if (context != null)
                PreferenceManager.setDefaultValues(context, R.xml.settings, false);
        }

        private void removeMiscCategory() {
            Preference misc = findPreference(getString(R.string.pref_settings_category_misc_key));
            PreferenceScreen screen = getPreferenceScreen();
            if (misc != null && screen != null)
                screen.removePreference(misc);
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
            if (key.equals(getString(R.string.pref_settings_ocr_online_key)))
                checkOcrOnline();
            if (key.equals(getString(R.string.pref_settings_ocr_service_key)))
                checkOcrServiceKeySummary();
            if (key.equals(getString(R.string.pref_settings_color_key))) {
                Activity activity = getActivity();
                if (activity != null)
                    activity.getWindow().getDecorView().post(activity::recreate);
            }
            if (key.equals(getString(R.string.pref_settings_country_key)))
                checkCountrySummary();
            if (key.equals(getString(R.string.pref_settings_show_submitted_key)))
                checkSubmittedSummary();
            if (key.equals(getString(R.string.pref_settings_comment_key)))
                checkCommentSummary();
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

        private void checkKeys() {
            checkEmailKey();
            checkOcrKey();
            checkCountryKey();
            checkCommentKey();
        }

        private void checkEmailKey() {
            checkKey(R.string.pref_settings_email_key, Predefines.EMAIL);
        }

        private void checkOcrKey() {
            checkKey(R.string.pref_settings_ocr_service_key, Predefines.OCR_SERVICE);
        }

        private void checkCountryKey() {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.BAKLAVA)
                checkKey(R.string.pref_settings_country_key, Predefines.COUNTRY_SERVICE);
        }

        private void checkCommentKey() {
            checkKey(R.string.pref_settings_comment_key, Predefines.ADDITIONAL_COMMENT);
        }

        private void checkKey(int preferenceKey, String predefine) {
            try {
                String value = dataStore.get(preferenceKey, "");
                if (value.isEmpty() && !predefine.isEmpty())
                    value = predefine;
                EditTextPreference preference = (EditTextPreference) getPreference(preferenceKey);
                preference.setText(value);
            } catch (NoPreferenceException e) {
                Log.w(TAG, e.getMessage());
            }
        }

        private void checkSummaries() {
            checkEmailSummary();
            checkPasswordSummary();
            checkOcrServiceKeySummary();
            checkSubmittedSummary();
            checkCountrySummary();
            checkCommentSummary();
        }

        private void checkEmailSummary() {
            checkSummary(R.string.pref_settings_email_key, this::setEmailSummary);
        }

        private void checkPasswordSummary() {
            checkSummary(R.string.pref_settings_password_key, this::setPasswordSummary);
        }

        private void checkOcrServiceKeySummary() {
            checkSummary(R.string.pref_settings_ocr_service_key, this::setOcrServiceKeySummary);
        }

        private void checkSubmittedSummary() {
            checkSummary(R.string.pref_settings_show_submitted_key, this::setSubmittedSummary);
        }

        private void checkCountrySummary() {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.BAKLAVA)
                checkSummary(R.string.pref_settings_country_key, this::setCountrySummary);
        }

        private void checkCommentSummary() {
            checkSummary(R.string.pref_settings_comment_key, this::setCommentSummary);
        }

        private void checkSummary(int preferenceKey, Consumer<Preference> setSummary) {
            try {
                setSummary.accept(getPreference(preferenceKey));
            } catch (NoPreferenceException e) {
                Log.w(TAG, e.getMessage());
            }
        }

        private void setEmailSummary(Preference preference) {
            setSummary(preference, R.string.pref_settings_email_key,
                    R.string.settings_email_summary, true);
        }

        private void setPasswordSummary(Preference preference) {
            String summary = getString(R.string.settings_password_summary);
            if (TextUtils.isEmpty(dataStore.get(R.string.pref_settings_password_key, "")))
                summary += getString(R.string.settings_currently_not_set);
            preference.setSummary(summary);
        }

        private void setCommentSummary(Preference preference) {
            setSummary(preference, R.string.pref_settings_comment_key,
                    R.string.settings_comment_summary, false);
        }

        private void setOcrServiceKeySummary(Preference preference) {
            setServiceKeySummary(preference, R.string.pref_settings_ocr_service_key,
                    R.string.settings_ocr_service_key_summary,
                    R.string.settings_ocr_service_url, R.string.settings_ocr_summary_no_key);
        }

        private void setCountrySummary(Preference preference) {
            setServiceKeySummary(preference, R.string.pref_settings_country_key,
                    R.string.settings_country_summary,
                    R.string.settings_country_service_url, R.string.settings_country_summary_no_key);
        }

        private void setServiceKeySummary(Preference preference, int preferenceKey,
                                          int summaryResId, int urlResId, int summaryNoKeyResId) {
            String summary = getString(summaryResId);
            if (dataStore.get(preferenceKey, "").isEmpty()) {
                String serviceUrl = getString(urlResId);
                summary += ".\n" + getString(summaryNoKeyResId) + " " + serviceUrl;
                preference.setIntent(new Intent().setAction(ACTION_VIEW).setData(Uri.parse(serviceUrl)));
            } else
                preference.setIntent(null);
            preference.setSummary(summary);
        }

        private void setSubmittedSummary(Preference preference) {
            setSummary(preference, R.string.pref_settings_show_submitted_key,
                    R.string.settings_submitted_summary, true);
        }

        private void setSummary(Preference preference, int preferenceKey,
                                int summaryResId, boolean trim) {
            String value = dataStore.get(preferenceKey, "");
            String summary = getString(summaryResId);
            if (!TextUtils.isEmpty(value))
                summary += getString(R.string.settings_currently) + " " + (trim ? value.trim() : value);
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
//                Preference postponeOcrPreference = getPreference(R.string.pref_settings_ocr_postpone_key);
//                postponeOcrPreference.setEnabled(preference.isChecked());
            } catch (NoPreferenceException e) {
                Log.w(TAG, e.getMessage());
            }
        }

        private boolean isEmailAndPasswordSet() {
            return !TextUtils.isEmpty(dataStore.get(R.string.pref_settings_email_key, "")) &&
                    !TextUtils.isEmpty(dataStore.get(R.string.pref_settings_password_key, ""));
        }
    }
}
