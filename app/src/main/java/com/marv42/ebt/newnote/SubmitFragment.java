/*
 Copyright (c) 2010 - 2022 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;

import com.marv42.ebt.newnote.data.LocationValues;
import com.marv42.ebt.newnote.data.NoteData;
import com.marv42.ebt.newnote.databinding.SubmitBinding;
import com.marv42.ebt.newnote.exceptions.ErrorMessage;
import com.marv42.ebt.newnote.exceptions.NoClipboardManagerException;
import com.marv42.ebt.newnote.exceptions.NoPictureException;
import com.marv42.ebt.newnote.location.LocationButtonHandler;
import com.marv42.ebt.newnote.preferences.EncryptedPreferenceDataStore;
import com.marv42.ebt.newnote.preferences.MySharedPreferencesListener;
import com.marv42.ebt.newnote.preferences.SavePreferencesTextWatcher;
import com.marv42.ebt.newnote.preferences.SettingsActivity;
import com.marv42.ebt.newnote.preferences.SharedPreferencesHandler;
import com.marv42.ebt.newnote.scanning.CameraStarter;
import com.marv42.ebt.newnote.ui.SubmitViewModel;

import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;

import dagger.android.support.DaggerFragment;

import static android.content.Context.CLIPBOARD_SERVICE;
import static android.widget.Toast.LENGTH_LONG;
import static android.widget.Toast.LENGTH_SHORT;
import static androidx.appcompat.widget.TooltipCompat.setTooltipText;

public class SubmitFragment extends DaggerFragment implements LifecycleOwner {

    private static final String TAG = SubmitFragment.class.getSimpleName();
    private static final CharSequence CLIPBOARD_LABEL = "overwritten EBT data";
    @Inject
    ThisApp app;
    @Inject
    ApiCaller apiCaller;
    @Inject
    LocationButtonHandler locationButtonHandler;
    @Inject
    SubmissionResultHandler submissionResultHandler;
    @Inject
    SharedPreferencesHandler sharedPreferencesHandler;
    @Inject
    EncryptedPreferenceDataStore dataStore;
    @Inject
    MySharedPreferencesListener sharedPreferencesListener;
    @Inject
    ViewModelProvider viewModelProvider;
    private SubmitBinding binding;
    private boolean radioChangingDone;
    private LocationTextWatcher locationTextWatcher;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = SubmitBinding.inflate(inflater, container, false);
        setOnClickListeners();
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupViewModel();
        setOnCheckedChangeListener();
        addTextChangedListeners();
        setTooltipText(binding.locationButton, getString(R.string.get_location));
        setTooltipText(binding.photoButton, getString(R.string.acquire));

        FragmentActivity activity = requireActivity();
        LoginChecker.checkLoginInfo(activity);
        ((Callback) activity).onSubmitFragmentAdded();
    }

    @Override
    public void onResume() {
        super.onResume();
        setViewValuesFromPreferences();
        executeCommentSuggestion();
    }

    @Override
    public void onDestroyView() {
        removeTextChangedListeners();
        binding = null;
        super.onDestroyView();
    }

    private void setOnClickListeners() {
        binding.locationButton.setOnClickListener(v -> locationButtonClicked());
        binding.photoButton.setOnClickListener(v -> takePhoto());
        binding.submitButton.setOnClickListener(v -> submitButtonClicked());
    }

    void locationButtonClicked() {
        locationButtonHandler.clicked();
    }

    void takePhoto() {
        Activity activity = getActivity();
        CameraStarter cameraStarter = new CameraStarter(activity);
        final String ocrKey = dataStore.get(R.string.pref_settings_ocr_service_key, "");
        final boolean isOnlineOcr = dataStore.get(R.string.pref_settings_ocr_online_key, false);
        if (!cameraStarter.canTakePhoto(isOnlineOcr && TextUtils.isEmpty(ocrKey)))
            return;
        try {
            cameraStarter.startCameraActivity(sharedPreferencesHandler);
        } catch (NoPictureException e) {
            Toast.makeText(activity, new ErrorMessage(activity).getErrorMessage(e.getMessage()), LENGTH_LONG).show();
        }
    }

    void submitButtonClicked() {
        Toast.makeText(getActivity(), R.string.submitting, LENGTH_LONG).show();
        submitNoteData();
        binding.editTextShortCode.setText("");
        binding.editTextSerialNumber.setText("");
    }

    private void submitNoteData() {
        NoteData noteData = new NoteData(
                getCountry(),
                getCity(),
                getPostalCode(),
                getDenomination(),
                getFixedShortCode().toUpperCase(),
                getSerialNumber().toUpperCase(),
                binding.editTextComment.getText().toString());
        noteData = getNoteDataWithAdditionalComment(noteData);
        new NoteDataSubmitter(app, apiCaller, submissionResultHandler).execute(noteData);
    }

    @NotNull
    private NoteData getNoteDataWithAdditionalComment(NoteData noteData) {
        return new NoteData(
                noteData.mCountry,
                noteData.mCity,
                noteData.mPostalCode,
                noteData.mDenomination,
                noteData.mShortCode,
                noteData.mSerialNumber,
                noteData.mComment + dataStore.get(R.string.pref_settings_comment_key, ""));
    }

    @NotNull
    private String getCountry() {
        return binding.editTextCountry.getText().toString();
    }

    @NotNull
    private String getCity() {
        return binding.editTextCity.getText().toString();
    }

    @NotNull
    private String getPostalCode() {
        return binding.editTextPostalCode.getText().toString();
    }

    @NonNull
    private String getDenomination() {
        if (binding.radio5.isChecked())
            return getString(R.string.eur5);
        if (binding.radio10.isChecked())
            return getString(R.string.eur10);
        if (binding.radio20.isChecked())
            return getString(R.string.eur20);
        if (binding.radio50.isChecked())
            return getString(R.string.eur50);
        if (binding.radio100.isChecked())
            return getString(R.string.eur100);
        if (binding.radio200.isChecked())
            return getString(R.string.eur200);
        if (binding.radio500.isChecked())
            return getString(R.string.eur500);
        return "";
    }

    private void setDenomination(String denomination) {
        if (denomination.equals(getString(R.string.eur5)))
            binding.radio5.setChecked(true);
        if (denomination.equals(getString(R.string.eur10)))
            binding.radio10.setChecked(true);
        if (denomination.equals(getString(R.string.eur20)))
            binding.radio20.setChecked(true);
        if (denomination.equals(getString(R.string.eur50)))
            binding.radio50.setChecked(true);
        if (denomination.equals(getString(R.string.eur100)))
            binding.radio100.setChecked(true);
        if (denomination.equals(getString(R.string.eur200)))
            binding.radio200.setChecked(true);
        if (denomination.equals(getString(R.string.eur500)))
            binding.radio500.setChecked(true);
    }

    private String getFixedShortCode() {
        String shortCode = binding.editTextShortCode.getText().toString();
        shortCode = removeNonWordCharacters(shortCode);
        shortCode = fixLeadingZeros(shortCode);
        return shortCode;
    }

    @NotNull
    private String getSerialNumber() {
        return removeNonWordCharacters(binding.editTextSerialNumber.getText().toString());
    }

    @NotNull
    private String removeNonWordCharacters(String s) {
        return s.replaceAll("\\W+", "");
    }

    @NotNull
    private String fixLeadingZeros(String shortCode) {
        if (shortCode.length() == 4)
            shortCode = shortCode.charAt(0) + "00" + shortCode.substring(1);
        else if (shortCode.length() == 5)
            shortCode = shortCode.charAt(0) + "0" + shortCode.substring(1);
        return shortCode;
    }

    void checkClipboardManager(boolean serialNumberOrShortCode) {
        try {
            putToClipboard(serialNumberOrShortCode);
        } catch (NoClipboardManagerException e) {
            Log.w(TAG, e.getMessage());
        }
    }

    private void putToClipboard(boolean serialNumberOrShortCode) throws NoClipboardManagerException {
        if (binding == null)
            return; // TODO save (and replace) the text later
        EditText editText = serialNumberOrShortCode ? binding.editTextSerialNumber : binding.editTextShortCode;
        ClipboardManager manager = (ClipboardManager) app.getSystemService(CLIPBOARD_SERVICE);
        if (manager == null)
            throw new NoClipboardManagerException();
        String text = editText.getText().toString();
        if (!text.isEmpty()) {
            ClipData data = ClipData.newPlainText(CLIPBOARD_LABEL, text);
            manager.setPrimaryClip(data);
            Toast.makeText(getActivity(), R.string.content_in_clipboard, LENGTH_LONG).show();
        }
    }

    private void setupViewModel() {
        final LifecycleOwner lifecycleOwner = getViewLifecycleOwner();
        SubmitViewModel viewModel = viewModelProvider.get(SubmitViewModel.class);
        viewModel.getCountry().observe(lifecycleOwner, observer -> setTextIfNotEqual(binding.editTextCountry, observer));
        viewModel.getCity().observe(lifecycleOwner, observer -> setTextIfNotEqual(binding.editTextCity, observer));
        viewModel.getPostalCode().observe(lifecycleOwner, observer -> setTextIfNotEqual(binding.editTextPostalCode, observer));
        viewModel.getDenomination().observe(lifecycleOwner, this::setDenomination);
        viewModel.getShortCode().observe(lifecycleOwner, observer -> setTextIfNotEqual(binding.editTextShortCode, observer));
        viewModel.getSerialNumber().observe(lifecycleOwner, observer -> setTextIfNotEqual(binding.editTextSerialNumber, observer));
        viewModel.getComment().observe(lifecycleOwner, observer -> setTextIfNotEqual(binding.editTextComment, observer));
    }

    private void setTextIfNotEqual(EditText editText, String observer) {
        if (!TextUtils.isEmpty(observer) && !TextUtils.equals(observer, editText.getText()))
            editText.setText(observer);
    }

    private void setOnCheckedChangeListener() {
        setRadioGroupListener(binding.radioGroup1, binding.radioGroup2);
        setRadioGroupListener(binding.radioGroup2, binding.radioGroup1);
    }

    private void setRadioGroupListener(RadioGroup group, RadioGroup otherGroup) {
        group.setOnCheckedChangeListener((dummy, checkedId) -> {
            if (checkedId != -1 && radioChangingDone) {
                radioChangingDone = false;
                otherGroup.clearCheck();
                sharedPreferencesHandler.set(R.string.pref_denomination_key, getDenomination());
            }
            radioChangingDone = true;
        });
    }

    void setViewValuesFromPreferences() {
        sharedPreferencesListener.setLocation();
        setDenominationFromPreferences();
        setShortCodeFromPreferences();
        setSerialNumberFromPreferences();
        setCommentFromPreferences();
    }

    private void setDenominationFromPreferences() {
        String denomination = sharedPreferencesHandler.get(R.string.pref_denomination_key, getString(R.string.eur5));
        SubmitViewModel viewModel = viewModelProvider.get(SubmitViewModel.class);
        viewModel.setDenomination(denomination);
    }

    private void setShortCodeFromPreferences() {
        String shortCode = sharedPreferencesHandler.get(R.string.pref_short_code_key, "");
        SubmitViewModel viewModel = viewModelProvider.get(SubmitViewModel.class);
        viewModel.setShortCode(shortCode);
    }

    private void setSerialNumberFromPreferences() {
        String serialNumber = sharedPreferencesHandler.get(R.string.pref_serial_number_key, "");
        SubmitViewModel viewModel = viewModelProvider.get(SubmitViewModel.class);
        viewModel.setSerialNumber(serialNumber);
    }

    private void setCommentFromPreferences() {
        String comment = sharedPreferencesHandler.get(R.string.pref_comment_key, "");
        String additionalComment = dataStore.get(R.string.pref_settings_comment_key, "");
        if (comment.endsWith(additionalComment))
            comment = comment.substring(0, comment.length() - additionalComment.length());
        SubmitViewModel viewModel = viewModelProvider.get(SubmitViewModel.class);
        viewModel.setComment(comment);
    }

    private void addTextChangedListeners() {
        locationTextWatcher = new LocationTextWatcher();
        binding.editTextCountry.addTextChangedListener(locationTextWatcher);
        binding.editTextCity.addTextChangedListener(locationTextWatcher);
        binding.editTextPostalCode.addTextChangedListener(locationTextWatcher);
        binding.editTextCountry.addTextChangedListener(
                new SavePreferencesTextWatcher(sharedPreferencesHandler, getString(R.string.pref_country_key)));
        binding.editTextCity.addTextChangedListener(
                new SavePreferencesTextWatcher(sharedPreferencesHandler, getString(R.string.pref_city_key)));
        binding.editTextPostalCode.addTextChangedListener(
                new SavePreferencesTextWatcher(sharedPreferencesHandler, getString(R.string.pref_postal_code_key)));
        binding.editTextShortCode.addTextChangedListener(
                new SavePreferencesTextWatcher(sharedPreferencesHandler, getString(R.string.pref_short_code_key)));
        binding.editTextSerialNumber.addTextChangedListener(
                new SavePreferencesTextWatcher(sharedPreferencesHandler, getString(R.string.pref_serial_number_key)));
        binding.editTextComment.addTextChangedListener(
                new SavePreferencesTextWatcher(sharedPreferencesHandler, getString(R.string.pref_comment_key)));
    }

    private void executeCommentSuggestion() {
        if (!sharedPreferencesHandler.get(R.string.pref_login_values_ok_key, false))
            return;
        new CommentSuggestion(apiCaller, (EbtNewNote) requireActivity(), dataStore)
                .execute(new LocationValues(
                        getCountry(),
                        getCity(),
                        getPostalCode()));
    }

    private void removeTextChangedListeners() {
        binding.editTextCountry.removeTextChangedListener(locationTextWatcher);
        binding.editTextCity.removeTextChangedListener(locationTextWatcher);
        binding.editTextPostalCode.removeTextChangedListener(locationTextWatcher);
        locationTextWatcher = null;
    }

    void setCommentsAdapter(String[] suggestions) {
        Activity activity = getActivity();
        if (activity != null) {
            binding.editTextComment.setAdapter(new ArrayAdapter<>(activity,
                    android.R.layout.simple_dropdown_item_1line, suggestions));
            Toast.makeText(activity, R.string.comment_suggestions_set, LENGTH_SHORT).show();
        }
    }

    public interface Callback {
        void onSubmitFragmentAdded();
    }

    private class LocationTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            binding.editTextComment.setText("");
            executeCommentSuggestion();
        }
    }
}
