/*
 Copyright (c) 2010 - 2020 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
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
import com.marv42.ebt.newnote.exceptions.NoNotificationManagerException;
import com.marv42.ebt.newnote.exceptions.NoPictureException;
import com.marv42.ebt.newnote.location.LocationButtonHandler;
import com.marv42.ebt.newnote.scanning.CameraStarter;
import com.marv42.ebt.newnote.scanning.OcrHandler;
import com.marv42.ebt.newnote.scanning.OcrNotifier;

import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;

import dagger.android.support.DaggerFragment;

import static android.content.Context.VIBRATOR_SERVICE;
import static android.os.VibrationEffect.DEFAULT_AMPLITUDE;
import static android.widget.Toast.LENGTH_LONG;
import static androidx.appcompat.widget.TooltipCompat.setTooltipText;
import static com.marv42.ebt.newnote.exceptions.ErrorMessage.ERROR;
import static com.marv42.ebt.newnote.scanning.Corrections.LENGTH_THRESHOLD_SERIAL_NUMBER;

public class SubmitFragment extends DaggerFragment implements OcrHandler.Callback, LifecycleOwner {

    private static final CharSequence CLIPBOARD_LABEL = "overwritten EBT data";
    private static final int VIBRATION_MS = 150;
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
        final String ocrKey = dataStore.get(R.string.pref_settings_ocr_key, "");
        if (!cameraStarter.canTakePhoto(ocrKey))
            return;
        try {
            cameraStarter.startCameraActivity(sharedPreferencesHandler);
        } catch (NoPictureException e) {
            Toast.makeText(activity, new ErrorMessage(activity).getErrorMessage(e.getMessage()), LENGTH_LONG).show();
        }
    }

    void submitButtonClicked() {
        Toast.makeText(getActivity(), getString(R.string.submitting), LENGTH_LONG).show();
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
            shortCode = shortCode.substring(0, 1) + "00" + shortCode.substring(1);
        else if (shortCode.length() == 5)
            shortCode = shortCode.substring(0, 1) + "0" + shortCode.substring(1);
        return shortCode;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        FragmentActivity activity = getActivity();
        if (activity == null)
            throw new IllegalStateException("No activity");
        LoginChecker.checkLoginInfo(activity);
        checkOcrResult();
        ((Callback) activity).onSubmitFragmentAdded();
    }

    private void checkOcrResult() {
        String ocrResult = sharedPreferencesHandler.get(R.string.pref_ocr_result_key, "");
        if (!TextUtils.isEmpty(ocrResult))
            presentOcrResult(ocrResult);
    }

    private void presentOcrResult(String ocrResult) {
        vibrate();
        showOcrResult(ocrResult);
        sharedPreferencesHandler.set(R.string.pref_ocr_result_key, "");
    }

    private void vibrate() {
        Vibrator v = (Vibrator) app.getSystemService(VIBRATOR_SERVICE);
        if (v != null)
            v.vibrate(VibrationEffect.createOneShot(VIBRATION_MS, DEFAULT_AMPLITUDE));
    }

    private void showOcrResult(String ocrResult) {
        Activity activity = getActivity();
        if (ocrResult.isEmpty())
            OcrNotifier.showDialog(activity, getString(R.string.ocr_dialog_empty));
        else if (ocrResult.startsWith(ERROR))
            OcrNotifier.showDialog(activity, new ErrorMessage(activity).getErrorMessage(ocrResult));
        else
            replaceShortCodeOrSerialNumber(ocrResult);
    }

    private void replaceShortCodeOrSerialNumber(String ocrResult) {
        if (ocrResult.length() >= LENGTH_THRESHOLD_SERIAL_NUMBER)
            replaceText(ocrResult, binding.editTextSerialNumber);
        else
            replaceText(ocrResult, binding.editTextShortCode);
        Toast.makeText(getActivity(), getString(R.string.ocr_return), LENGTH_LONG).show();
    }

    private void replaceText(String ocrResult, EditText editText) {
        checkClipboardManager(editText);
        editText.setText(ocrResult);
    }

    private void checkClipboardManager(EditText editText) {
        try {
            putToClipboard(editText.getText());
        } catch (NoClipboardManagerException e) {
            e.printStackTrace();
        }
    }

    private void putToClipboard(Editable editable) throws NoClipboardManagerException {
        ClipboardManager manager = (ClipboardManager) app.getSystemService(Context.CLIPBOARD_SERVICE);
        if (manager == null)
            throw new NoClipboardManagerException();
        String text = editable.toString();
        if (!text.isEmpty()) {
            ClipData data = ClipData.newPlainText(CLIPBOARD_LABEL, text);
            manager.setPrimaryClip(data);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupViewModel();
        setOnCheckedChangeListener();
        setTooltipText(binding.locationButton, getString(R.string.get_location));
        setTooltipText(binding.photoButton, getString(R.string.acquire));
    }

    private void setupViewModel() {
        final LifecycleOwner lifecycleOwner = getViewLifecycleOwner();
        SubmitViewModel viewModel = viewModelProvider.get(SubmitViewModel.class);
        viewModel.getCountry().observe(lifecycleOwner, observer -> binding.editTextCountry.setText(observer));
        viewModel.getCity().observe(lifecycleOwner, observer -> binding.editTextCity.setText(observer));
        viewModel.getPostalCode().observe(lifecycleOwner, observer -> binding.editTextPostalCode.setText(observer));
        viewModel.getDenomination().observe(lifecycleOwner, this::setDenomination);
        viewModel.getShortCode().observe(lifecycleOwner, observer -> binding.editTextShortCode.setText(observer));
        viewModel.getSerialNumber().observe(lifecycleOwner, observer -> binding.editTextSerialNumber.setText(observer));
        viewModel.getComment().observe(lifecycleOwner, this::setComment);
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

    @Override
    public void onResume() {
        super.onResume();
        addTextChangedListeners();
        executeCommentSuggestion();
    }

    private void setComment(String comment) {
        String additionalComment = dataStore.get(R.string.pref_settings_comment_key, "");
        if (comment.endsWith(additionalComment))
            comment = comment.substring(0, comment.length() - additionalComment.length());
        binding.editTextComment.setText(comment);
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
        new CommentSuggestion(apiCaller, (EbtNewNote) getActivity(), dataStore)
                .execute(new LocationValues(
                        getCountry(),
                        getCity(),
                        getPostalCode()));
    }

    @Override
    public void onPause() {
        removeTextChangedListeners();
        super.onPause();
    }

    private void removeTextChangedListeners() {
        binding.editTextCountry.removeTextChangedListener(locationTextWatcher);
        binding.editTextCity.removeTextChangedListener(locationTextWatcher);
        binding.editTextPostalCode.removeTextChangedListener(locationTextWatcher);
        locationTextWatcher = null;
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }

    @Override
    public void onOcrResult(String result) throws NoNotificationManagerException {
        if (isVisible())
            presentOcrResult(result);
        else {
            sharedPreferencesHandler.set(R.string.pref_ocr_result_key, result);
            new OcrNotifier().showNotification(app);
        }
    }

    void setCommentsAdapter(String[] suggestions) {
        Activity activity = getActivity();
        if (activity != null)
            binding.editTextComment.setAdapter(new ArrayAdapter<>(activity,
                    android.R.layout.simple_dropdown_item_1line, suggestions));
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
