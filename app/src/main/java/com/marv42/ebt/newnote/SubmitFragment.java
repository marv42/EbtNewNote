/*
 Copyright (c) 2010 - 2020 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LifecycleOwner;

import com.marv42.ebt.newnote.exceptions.ErrorMessage;
import com.marv42.ebt.newnote.exceptions.NoClipboardManagerException;
import com.marv42.ebt.newnote.exceptions.NoNotificationManagerException;
import com.marv42.ebt.newnote.scanning.OcrHandler;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import dagger.android.support.DaggerFragment;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.CAMERA;
import static android.content.Context.NOTIFICATION_SERVICE;
import static android.content.Context.VIBRATOR_SERVICE;
import static android.os.VibrationEffect.DEFAULT_AMPLITUDE;
import static android.provider.MediaStore.ACTION_IMAGE_CAPTURE;
import static android.widget.Toast.LENGTH_LONG;
import static androidx.core.content.FileProvider.getUriForFile;
import static androidx.core.content.PermissionChecker.PERMISSION_GRANTED;
import static androidx.core.content.PermissionChecker.checkSelfPermission;
import static com.marv42.ebt.newnote.EbtNewNote.CAMERA_PERMISSION_REQUEST_CODE;
import static com.marv42.ebt.newnote.EbtNewNote.FRAGMENT_TYPE;
import static com.marv42.ebt.newnote.EbtNewNote.IMAGE_CAPTURE_REQUEST_CODE;
import static com.marv42.ebt.newnote.EbtNewNote.LOCATION_PERMISSION_REQUEST_CODE;
import static com.marv42.ebt.newnote.EbtNewNote.OCR_NOTIFICATION_ID;
import static com.marv42.ebt.newnote.Notifications.OCR_CHANNEL_ID;
import static com.marv42.ebt.newnote.Notifications.OCR_CHANNEL_NAME;
import static com.marv42.ebt.newnote.Notifications.createBuilder;
import static com.marv42.ebt.newnote.Notifications.getNotificationChannel;
import static com.marv42.ebt.newnote.Utils.DAYS_IN_MS;
import static com.marv42.ebt.newnote.exceptions.ErrorMessage.ERROR;
import static com.marv42.ebt.newnote.scanning.Corrections.LENGTH_THRESHOLD_SHORT_CODE_SERIAL_NUMBER;
import static java.io.File.createTempFile;

public class SubmitFragment extends DaggerFragment implements OcrHandler.Callback,
        SharedPreferences.OnSharedPreferenceChangeListener, LifecycleOwner {

    public interface Callback {
        void onSubmitFragmentAdded();
    }

    @Inject
    ThisApp app;
    @Inject
    SharedPreferences sharedPreferences;
    @Inject
    ApiCaller apiCaller;
    @Inject
    SubmissionResults submissionResults;
    @Inject
    SharedPreferencesHandler sharedPreferencesHandler;
    @Inject
    EncryptedPreferenceDataStore dataStore;

    private static final int TIME_THRESHOLD_DELETE_OLD_PICS_MS = DAYS_IN_MS;
    private static final CharSequence CLIPBOARD_LABEL = "overwritten EBT data";
    private static final int VIBRATION_MS = 150;

    private String currentPhotoPath;

    private Unbinder unbinder;
    @BindView(R.id.edit_text_country) EditText countryText;
    @BindView(R.id.edit_text_city) EditText cityText;
    @BindView(R.id.edit_text_zip) EditText postalCodeText;
    @BindView(R.id.radio_group_1) RadioGroup radioGroup1;
    @BindView(R.id.radio_group_2) RadioGroup radioGroup2;
    private boolean radioChangingDone;
    @BindView(R.id.radio_5) RadioButton eur5Radio;
    @BindView(R.id.radio_10) RadioButton eur10Radio;
    @BindView(R.id.radio_20) RadioButton eur20Radio;
    @BindView(R.id.radio_50) RadioButton eur50Radio;
    @BindView(R.id.radio_100) RadioButton eur100Radio;
    @BindView(R.id.radio_200) RadioButton eur200Radio;
    @BindView(R.id.radio_500) RadioButton eur500Radio;
    @BindView(R.id.edit_text_printer) EditText shortCodeText;
    @BindView(R.id.edit_text_serial) EditText serialText;
    @BindView(R.id.edit_text_comment) AutoCompleteTextView commentText;

    private LocationTextWatcher locationTextWatcher;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.submit, container, false);
        unbinder = ButterKnife.bind(this, view);
        return view;
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

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setOnCheckedChangeListener();
    }

    @Override
    public void onResume() {
        super.onResume();
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        setViewValuesFromPreferences();
        addTextChangedListeners();
        executeCommentSuggestion();
    }

    @Override
    public void onPause() {
        removeTextChangedListeners();
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        unbinder.unbind();
        sharedPreferencesHandler = null;
        super.onDestroyView();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (sharedPreferences == this.sharedPreferences)
            setEditTextFromSharedPreferences(key);
    }

    @Override
    public void onOcrResult(String result) throws NoNotificationManagerException {
        currentPhotoPath = "";
        if (isVisible()) // des is immer visible, mir soll's recht sei
            presentOcrResult(result);
        else {
            sharedPreferencesHandler.set(R.string.pref_ocr_result, result);
            showNotification();
        }
    }

    private void setOnCheckedChangeListener() {
        setRadioGroupListener(radioGroup1, radioGroup2);
        setRadioGroupListener(radioGroup2, radioGroup1);
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
        setIfNotEqual(countryText, R.string.pref_country_key);
        setIfNotEqual(cityText, R.string.pref_city_key);
        setIfNotEqual(postalCodeText, R.string.pref_postal_code_key);
        setRadioButtons();
        setEditText(shortCodeText, R.string.pref_short_code_key);
        setEditText(serialText, R.string.pref_serial_number_key);
        setComment();
    }

    private void setIfNotEqual(EditText editText, int keyId) {
        String value = sharedPreferencesHandler.get(keyId, "");
        if (!TextUtils.equals(value, editText.getText()))
            editText.setText(value);
    }

    private void setRadioButtons() {
        setDenomination(sharedPreferencesHandler.get(R.string.pref_denomination_key, getString(R.string.eur5)));
    }

    private void setEditText(EditText editText, int keyId) {
        editText.setText(sharedPreferencesHandler.get(keyId, ""));

    }

    private void setComment() {
        String comment = sharedPreferencesHandler.get(R.string.pref_comment_key, "");
        String additionalComment = dataStore.get(R.string.pref_settings_comment_key, "");
        if (comment.endsWith(additionalComment))
            commentText.setText(comment.substring(0, comment.length() - additionalComment.length()));
        else
            commentText.setText(comment);
    }

    private void addTextChangedListeners() {
        locationTextWatcher = new LocationTextWatcher();
        countryText.addTextChangedListener(locationTextWatcher);
        cityText.addTextChangedListener(locationTextWatcher);
        postalCodeText.addTextChangedListener(locationTextWatcher);
        countryText.addTextChangedListener(new SavePreferencesTextWatcher(getString(R.string.pref_country_key)));
        cityText.addTextChangedListener(new SavePreferencesTextWatcher(getString(R.string.pref_city_key)));
        postalCodeText.addTextChangedListener(new SavePreferencesTextWatcher(getString(R.string.pref_postal_code_key)));
        shortCodeText.addTextChangedListener(new SavePreferencesTextWatcher(getString(R.string.pref_short_code_key)));
        serialText.addTextChangedListener(new SavePreferencesTextWatcher(getString(R.string.pref_serial_number_key)));
        commentText.addTextChangedListener(new SavePreferencesTextWatcher(getString(R.string.pref_comment_key)));
    }

    private void removeTextChangedListeners() {
        countryText.removeTextChangedListener(locationTextWatcher);
        cityText.removeTextChangedListener(locationTextWatcher);
        postalCodeText.removeTextChangedListener(locationTextWatcher);
        locationTextWatcher = null;
    }

    @OnClick(R.id.submit_button)
    void submitValues() {
        Toast.makeText(getActivity(), getString(R.string.submitting), LENGTH_LONG).show();
        doSubmit();
        shortCodeText.setText("");
        serialText.setText("");
    }

    private void doSubmit() {
        final NoteData noteData = new NoteData(
                getCountry(),
                getCity(),
                getPostalCode(),
                getDenomination(),
                getFixedShortCode().toUpperCase(),
                getSerialNumber().toUpperCase(),
                commentText.getText().toString());
        new NoteDataSubmitter(app, apiCaller, submissionResults, dataStore).execute(noteData);
    }

    @NotNull
    private String getCountry() {
        return countryText.getText().toString();
    }

    @NotNull
    private String getCity() {
        return cityText.getText().toString();
    }

    @NotNull
    private String getPostalCode() {
        return postalCodeText.getText().toString();
    }

    private String getFixedShortCode() {
        String shortCode = shortCodeText.getText().toString();
        shortCode = removeNonWordCharacters(shortCode);
        shortCode = fixLeadingZeros(shortCode);
        return shortCode;
    }

    @NotNull
    private String getSerialNumber() {
        return removeNonWordCharacters(serialText.getText().toString());
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

    @OnClick(R.id.location_button)
    void checkLocationSetting() {
        Activity activity = getActivity();
        if (checkSelfPermission(app, ACCESS_COARSE_LOCATION) != PERMISSION_GRANTED &&
                checkSelfPermission(app, ACCESS_FINE_LOCATION) != PERMISSION_GRANTED &&
                activity != null) {
            ActivityCompat.requestPermissions(activity,
                    new String[] { ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION },
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }
        if (activity == null)
            throw new IllegalStateException("No activity");
        LocationManager locationManager = (LocationManager)
                activity.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager != null && !locationManager.isLocationEnabled()) {
            Toast.makeText(activity, getString(R.string.location_not_enabled), LENGTH_LONG).show();
            app.startLocationProviderChangedReceiver();
            return;
        }
        if (checkSelfPermission(app, ACCESS_FINE_LOCATION) != PERMISSION_GRANTED)
            Toast.makeText(activity, getString(R.string.location_no_gps), LENGTH_LONG).show();
        app.startLocationTask();
    }

    private void setEditTextFromSharedPreferences(String key) {
        String newValue = sharedPreferences.getString(key, "");
        EditText editText = getEditText(key);
        if (editText != null && !TextUtils.isEmpty(newValue) &&
                newValue != null && !newValue.equals(editText.getText().toString()))
            editText.setText(newValue);
    }

    private EditText getEditText(String prefRes) {
        if (prefRes.equals(app.getString(R.string.pref_country_key)))
            return countryText;
        if (prefRes.equals(app.getString(R.string.pref_city_key)))
            return cityText;
        if (prefRes.equals(app.getString(R.string.pref_postal_code_key)))
            return postalCodeText;
        if (prefRes.equals(app.getString(R.string.pref_short_code_key)))
            return shortCodeText;
        if (prefRes.equals(app.getString(R.string.pref_serial_number_key)))
            return serialText;
        if (prefRes.equals(app.getString(R.string.pref_comment_key)))
            return commentText;
        return null;
    }

    @NonNull
    private String getDenomination() {
        if (eur5Radio.isChecked())
            return getString(R.string.eur5);
        if (eur10Radio.isChecked())
            return getString(R.string.eur10);
        if (eur20Radio.isChecked())
            return getString(R.string.eur20);
        if (eur50Radio.isChecked())
            return getString(R.string.eur50);
        if (eur100Radio.isChecked())
            return getString(R.string.eur100);
        if (eur200Radio.isChecked())
            return getString(R.string.eur200);
        if (eur500Radio.isChecked())
            return getString(R.string.eur500);
        return "";
    }

    private void setDenomination(String denomination) {
        if (denomination.equals(getString(R.string.eur5)))
            eur5Radio.setChecked(true);
        if (denomination.equals(getString(R.string.eur10)))
            eur10Radio.setChecked(true);
        if (denomination.equals(getString(R.string.eur20)))
            eur20Radio.setChecked(true);
        if (denomination.equals(getString(R.string.eur50)))
            eur50Radio.setChecked(true);
        if (denomination.equals(getString(R.string.eur100)))
            eur100Radio.setChecked(true);
        if (denomination.equals(getString(R.string.eur200)))
            eur200Radio.setChecked(true);
        if (denomination.equals(getString(R.string.eur500)))
            eur500Radio.setChecked(true);
    }

    @OnClick(R.id.photo_button)
    void takePhoto() {
        Activity activity = getActivity();
        if (activity == null)
            throw new IllegalStateException("No activity");
        Intent intent = new Intent(ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(activity.getPackageManager()) == null) {
            Toast.makeText(activity, getString(R.string.no_camera_activity), LENGTH_LONG).show();
            return;
        }
        if (checkSelfPermission(app, CAMERA) != PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[] { CAMERA }, CAMERA_PERMISSION_REQUEST_CODE);
            return;
        }
        if (!TextUtils.isEmpty(currentPhotoPath)) {
            // TODO enable multiple OCR runs at the same time
            Toast.makeText(activity, getString(R.string.ocr_executing), LENGTH_LONG).show();
            return;
        }
        if (TextUtils.isEmpty(dataStore.get(R.string.pref_settings_ocr_key, ""))) {
            showDialogNoOcrServiceKey(activity);
            return;
        }
        File photoFile;
        try {
            photoFile = createImageFile();
        } catch (IOException e) {
            Toast.makeText(activity, getString(R.string.error_creating_file) + ": "
                    + e.getMessage(), LENGTH_LONG).show();
            return;
        }
        currentPhotoPath = photoFile.getAbsolutePath();
        Uri photoUri = getUriForFile(activity, activity.getPackageName(), photoFile);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
        activity.startActivityForResult(intent, IMAGE_CAPTURE_REQUEST_CODE);
    }

    private void showDialogNoOcrServiceKey(Activity activity) {
        new AlertDialog.Builder(activity)
                .setTitle(R.string.ocr_no_service_key)
                .setMessage(app.getString(R.string.settings_ocr_summary) + " " +
                        app.getString(R.string.get_ocr_key))
                .setPositiveButton(getString(R.string.ok),
                        (dialog, which) -> {
                            startActivity(new Intent(getActivity().getApplicationContext(),
                                    SettingsActivity.class));
                            dialog.dismiss(); })
                .show();
    }

    private File createImageFile() throws IOException {
        Activity activity = getActivity();
        if (activity == null)
            throw new IllegalStateException("No activity");
        File tempFolder = activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
//        if (tempFolder == null || tempFolder.listFiles() == null)
//            throw new IOException("Error getting picture directory");
        for (File file: tempFolder.listFiles())
            if (Calendar.getInstance().getTimeInMillis() - file.lastModified()
                    > TIME_THRESHOLD_DELETE_OLD_PICS_MS)
                file.delete();
        return createTempFile("bill_", ".png", tempFolder);
    }

    private void showNotification() throws NoNotificationManagerException {
        NotificationManager notificationManager =
                (NotificationManager) app.getSystemService(NOTIFICATION_SERVICE);
        if (notificationManager == null)
            throw new NoNotificationManagerException();
        NotificationChannel notificationChannel = getNotificationChannel(
                OCR_CHANNEL_ID, OCR_CHANNEL_NAME);
        notificationManager.createNotificationChannel(notificationChannel);
        Intent intent = new Intent(app, EbtNewNote.class);
        intent.putExtra(FRAGMENT_TYPE, SubmitFragment.class.getSimpleName());
        NotificationCompat.Builder builder = getNotificationBuilder(intent);
        notificationManager.notify(OCR_NOTIFICATION_ID, builder.build());
    }

    private NotificationCompat.Builder getNotificationBuilder(Intent intent) {
        final String title = app.getString(R.string.ocr_result);
        final String content = app.getString(R.string.ocr_result_description);
        PendingIntent contentIntent = PendingIntent.getActivity(app, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        return createBuilder(app, OCR_CHANNEL_ID, title, content, contentIntent);
    }

    @NonNull
    public String getPhotoPath() {
        return currentPhotoPath;
    }

    void resetPhotoPath() {
        currentPhotoPath = "";
    }

    void setCommentsAdapter(String[] suggestions) {
        Activity activity = getActivity();
        if (activity != null)
            commentText.setAdapter(new ArrayAdapter<>(activity,
                    android.R.layout.simple_dropdown_item_1line, suggestions));
    }

    private void checkOcrResult() {
        String ocrResult = sharedPreferencesHandler.get(R.string.pref_ocr_result, "");
        if (!TextUtils.isEmpty(ocrResult))
            presentOcrResult(ocrResult);
    }

    private void presentOcrResult(String ocrResult) {
        vibrate();
        showOcrResult(ocrResult);
        sharedPreferencesHandler.set(R.string.pref_ocr_result, "");
    }

    private void vibrate() {
        Vibrator v = (Vibrator) app.getSystemService(VIBRATOR_SERVICE);
        if (v != null)
            v.vibrate(VibrationEffect.createOneShot(VIBRATION_MS, DEFAULT_AMPLITUDE));
    }

    private void showOcrResult(String ocrResult) {
        Activity activity = getActivity();
        if (ocrResult.isEmpty())
            showDialog(activity, getString(R.string.ocr_dialog_empty));
        else if (ocrResult.startsWith(ERROR))
            showDialog(activity, new ErrorMessage(activity).getErrorMessage(ocrResult));
        else
            replaceShortCodeOrSerialNumber(ocrResult);
    }

    private static void showDialog(Activity activity, String message) {
        new AlertDialog.Builder(activity)
                .setTitle(R.string.ocr_dialog_title)
                .setMessage(message)
                .show();
    }

    private void replaceShortCodeOrSerialNumber(String ocrResult) {
        if (ocrResult.length() < LENGTH_THRESHOLD_SHORT_CODE_SERIAL_NUMBER)
            replaceText(ocrResult, shortCodeText);
        else
            replaceText(ocrResult, serialText);
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

    private void putToClipboard(Editable text) throws NoClipboardManagerException {
        ClipboardManager manager = (ClipboardManager) app.getSystemService(Context.CLIPBOARD_SERVICE);
        if (manager == null)
            throw new NoClipboardManagerException();
        String s = text.toString();
        if (! s.isEmpty()) {
            ClipData data = ClipData.newPlainText(CLIPBOARD_LABEL, s);
            manager.setPrimaryClip(data);
        }
    }

    private class SavePreferencesTextWatcher implements TextWatcher {
        private String preferenceKey;

        SavePreferencesTextWatcher(String preference) {
            preferenceKey = preference;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) { }

        @Override
        public void afterTextChanged(Editable s) {
            sharedPreferencesHandler.set(preferenceKey, s.toString());
        }
    }

    private class LocationTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) { }

        @Override
        public void afterTextChanged(Editable s) {
            commentText.setText("");
            executeCommentSuggestion();
        }
    }

    private void executeCommentSuggestion() {
        if (! sharedPreferencesHandler.get(R.string.pref_login_values_ok_key, false))
            return;
        new CommentSuggestion(apiCaller, (EbtNewNote) getActivity(), dataStore)
                .execute(new LocationValues(
                        getCountry(),
                        getCity(),
                        getPostalCode()));
    }
}
