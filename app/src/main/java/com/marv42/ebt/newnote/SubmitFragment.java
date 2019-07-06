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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
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
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.tasks.Task;
import com.marv42.ebt.newnote.scanning.OcrHandler;
import com.marv42.ebt.newnote.scanning.TextProcessor;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import dagger.android.support.DaggerFragment;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.CAMERA;
import static android.provider.MediaStore.ACTION_IMAGE_CAPTURE;
import static android.widget.Toast.LENGTH_LONG;
import static androidx.core.content.FileProvider.getUriForFile;
import static androidx.core.content.PermissionChecker.PERMISSION_GRANTED;
import static com.google.android.gms.location.LocationServices.getFusedLocationProviderClient;
import static com.marv42.ebt.newnote.EbtNewNote.CHECK_LOCATION_SETTINGS_REQUEST_CODE;
import static com.marv42.ebt.newnote.EbtNewNote.IMAGE_CAPTURE_REQUEST_CODE;
import static java.io.File.createTempFile;

public class SubmitFragment extends DaggerFragment implements OcrHandler.Callback,
        CommentSuggestion.Callback /*, LifecycleOwner*/ {
    @Inject
    ThisApp mApp;
    @Inject
    SharedPreferences mSharedPreferences;
    @Inject
    ApiCaller mApiCaller;
    @Inject
    SubmissionResults mSubmissionResults;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 3;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 4;
    private static final int NUMBER_ADDRESSES = 5;
    private static final CharSequence CLIPBOARD_LABEL = "overwritten EBT data";
    private static final long LOCATION_MAX_WAIT_TIME_MS = 30 * 1000;
    private static final long TOAST_DELAY_MS = 3 * 1000;
    private static final int DELAY_NO_ACTIVITY_ON_COMMENT_SUGGESTIONS_MS = 2 * 1000;

    private String mCurrentPhotoPath;
    private static String mOcrResult = "";

    private Unbinder mUnbinder;
    @BindView(R.id.edit_text_country) EditText mCountryText;
    @BindView(R.id.edit_text_city) EditText mCityText;
    @BindView(R.id.edit_text_zip) EditText mPostalCodeText;
    @BindView(R.id.radio_group_1) RadioGroup mRadioGroup1;
    @BindView(R.id.radio_group_2) RadioGroup mRadioGroup2;
    private boolean mRadioChangingDone;
    @BindView(R.id.radio_5) RadioButton m5EurRadio;
    @BindView(R.id.radio_10) RadioButton m10EurRadio;
    @BindView(R.id.radio_20) RadioButton m20EurRadio;
    @BindView(R.id.radio_50) RadioButton m50EurRadio;
    @BindView(R.id.radio_100) RadioButton m100EurRadio;
    @BindView(R.id.radio_200) RadioButton m200EurRadio;
    @BindView(R.id.radio_500) RadioButton m500EurRadio;
    @BindView(R.id.edit_text_printer) EditText mShortCodeText;
    @BindView(R.id.edit_text_serial) EditText mSerialText;
    @BindView(R.id.edit_text_comment) AutoCompleteTextView mCommentText;

    private LocationTextWatcher mLocationTextWatcher;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationCallback mLocationCallback;
    private LocationRequest mLocationRequest;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.submit, container, false);
        mUnbinder = ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mRadioGroup1.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId != -1 && mRadioChangingDone) {
                mRadioChangingDone = false;
                mRadioGroup2.clearCheck();
                mSharedPreferences.edit().putString(getString(R.string.pref_denomination_key),
                        getDenomination()).apply();
            }
            mRadioChangingDone = true;
        });
        mRadioGroup2.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId != -1 && mRadioChangingDone) {
                mRadioChangingDone = false;
                mRadioGroup1.clearCheck();
                mSharedPreferences.edit().putString(getString(R.string.pref_denomination_key),
                        getDenomination()).apply();
            }
            mRadioChangingDone = true;
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        setViewValuesFromPreferences();
        mLocationTextWatcher = new LocationTextWatcher();
        mCountryText.addTextChangedListener(mLocationTextWatcher);
        mCityText.addTextChangedListener(mLocationTextWatcher);
        mPostalCodeText.addTextChangedListener(mLocationTextWatcher);
        mCountryText.addTextChangedListener(new SavePreferencesTextWatcher(getString(R.string.pref_country_key)));
        mCityText.addTextChangedListener(new SavePreferencesTextWatcher(getString(R.string.pref_city_key)));
        mPostalCodeText.addTextChangedListener(new SavePreferencesTextWatcher(getString(R.string.pref_postal_code_key)));
        mShortCodeText.addTextChangedListener(new SavePreferencesTextWatcher(getString(R.string.pref_short_code_key)));
        mSerialText.addTextChangedListener(new SavePreferencesTextWatcher(getString(R.string.pref_serial_number_key)));
        mCommentText.addTextChangedListener(new SavePreferencesTextWatcher(getString(R.string.pref_comment_key)));
        executeCommentSuggestion();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (!mSharedPreferences.getBoolean(getString(R.string.pref_login_values_ok_key), false)) {
            new AlertDialog.Builder(getActivity()).setTitle(getString(R.string.info))
                    .setMessage(getString(R.string.wrong_login_info) + getString(R.string.change_login_info))
                    .setPositiveButton(getString(R.string.yes),
                            (dialog, which) -> {
                                startActivity(new Intent(getActivity().getApplicationContext(),
                                        SettingsActivity.class));
                                dialog.dismiss();
                            })
                    .setNegativeButton(getString(R.string.no), (dialog, which) -> dialog.dismiss())
                    .show();
        }
    }

    @Override
    public void onPause() {
        mCountryText.removeTextChangedListener(mLocationTextWatcher);
        mCityText.removeTextChangedListener(mLocationTextWatcher);
        mPostalCodeText.removeTextChangedListener(mLocationTextWatcher);
        mLocationTextWatcher = null;
//        if (mFusedLocationClient != null)
//            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        mUnbinder.unbind();
        super.onDestroyView();
    }

    @OnClick(R.id.submit_button)
    void submitValues() {
        Toast.makeText(getActivity(), getString(R.string.submitting), LENGTH_LONG).show();
        new NoteDataSubmitter(mApp, mApiCaller, mSubmissionResults).execute(new NoteData(
                mCountryText.getText().toString(),
                mCityText.getText().toString(),
                mPostalCodeText.getText().toString(),
                getDenomination(),
                getFixedShortCode().toUpperCase(),
                mSerialText.getText().toString().replaceAll("\\s+", "").toUpperCase(),
                mCommentText.getText().toString()));
        mShortCodeText.setText("");
        mSerialText.setText("");
    }

    private String getFixedShortCode() {
        String fixedShortCode = mShortCodeText.getText().toString().replaceAll("\\s+", "");
        if (fixedShortCode.length() == 4)
            return fixedShortCode.substring(0, 1) + "00" + fixedShortCode.substring(1);
        else if (fixedShortCode.length() == 5)
            return fixedShortCode.substring(0, 1) + "0" + fixedShortCode.substring(1);
        return fixedShortCode;
    }

    @OnClick(R.id.location_button)
    void checkLocationSetting() {
        if (ContextCompat.checkSelfPermission(mApp, ACCESS_COARSE_LOCATION) != PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(mApp, ACCESS_FINE_LOCATION) != PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }
        Toast.makeText(getActivity(), getString(R.string.location_getting), LENGTH_LONG).show();

        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setNumUpdates(1)
                .setMaxWaitTime(LOCATION_MAX_WAIT_TIME_MS)
                .setExpirationDuration(LOCATION_MAX_WAIT_TIME_MS);

        // https://developers.google.com/android/reference/com/google/android/gms/location/SettingsClient
        LocationSettingsRequest locationSettingsRequest =
                new LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest).build();
        Task<LocationSettingsResponse> response =
                LocationServices.getSettingsClient(getActivity()).checkLocationSettings(locationSettingsRequest);
        response.addOnCompleteListener(task -> {
            try {
                /*LocationSettingsResponse response =*/ task.getResult(ApiException.class);
                requestLocation();
            } catch (ApiException exception) {
                switch (exception.getStatusCode()) {
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        try {
                            ResolvableApiException resolvable = (ResolvableApiException) exception;
                            resolvable.startResolutionForResult(
                                    getActivity(), CHECK_LOCATION_SETTINGS_REQUEST_CODE);
                        } catch (IntentSender.SendIntentException e) {
                            // ignore
                        } catch (ClassCastException e) {
                            // ignore
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        break;
                }
            }
        });
    }

    @SuppressLint("MissingPermission")
    void requestLocation() {
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null)
                    return;
                for (Location location : locationResult.getLocations())
                    if (location != null)
                        setLocation(location);
            }
        };
        if (mFusedLocationClient == null)
            mFusedLocationClient = getFusedLocationProviderClient(getActivity());
        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null);
        mFusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null)
                setLocation(location);
        });
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length == 0 || grantResults[0] != PERMISSION_GRANTED) {
            Toast.makeText(getActivity(), getString(R.string.no_permission), LENGTH_LONG).show();
            return;
        }
        Toast.makeText(getActivity(), getString(R.string.permission), LENGTH_LONG).show();
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            checkLocationSetting();
        } else if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            takePhoto();
        }
    }

    private void setLocation(Location l) {
        try {
            final Geocoder gc = new Geocoder(getActivity(), Locale.US);
            List<Address> addresses = gc.getFromLocation(l.getLatitude(), l.getLongitude(), NUMBER_ADDRESSES);
            if (addresses.size() == 0)
                Toast.makeText(getActivity(), getActivity().getString(R.string.location_no_address)
                        + ": " + l.getLatitude() + ", " + l.getLongitude() + ".", LENGTH_LONG).show();
            String[] previousLocation = new String[3];
            for (Address a : addresses) {
                if (a == null)
                    continue;
                String countryName = a.getCountryName();
                String locality = a.getLocality();
                String postalCode = a.getPostalCode();
                if ((!TextUtils.isEmpty(countryName) && !countryName.equals(previousLocation[0])) ||
                        (!TextUtils.isEmpty(locality) && !locality.equals(previousLocation[1])) ||
                        (!TextUtils.isEmpty(postalCode) && !postalCode.equals(previousLocation[2]))) {
                    previousLocation = new String[]{countryName, locality, postalCode};
                    setLocationValues(new LocationValues(countryName, locality, postalCode));
                }
            }
        } catch (IOException e) {
            Toast.makeText(getActivity(), getActivity().getString(R.string.location_geocoder_exception)
                    + ": " + e.getMessage() + ".", LENGTH_LONG).show();
        }
    }

    private void setLocationValues(LocationValues l) {
        // only set complete locations
        if (TextUtils.isEmpty(l.mCountry) || TextUtils.isEmpty(l.mCity) || TextUtils.isEmpty(l.mPostalCode))
            return;
        mCountryText.setText(l.mCountry);
        mCityText.setText(l.mCity);
        mPostalCodeText.setText(l.mPostalCode);
    }

    void setViewValuesFromPreferences() {
        String country = mSharedPreferences.getString(getString(R.string.pref_country_key), "");
        if (! TextUtils.equals(country, mCountryText.getText()))
            mCountryText.setText(country);
        String city = mSharedPreferences.getString(getString(R.string.pref_city_key), "");
        if (! TextUtils.equals(city, mCityText.getText()))
            mCityText.setText(city);
        String postalCode = mSharedPreferences.getString(getString(R.string.pref_postal_code_key), "");
        if (! TextUtils.equals(postalCode, mPostalCodeText.getText()))
            mPostalCodeText.setText(postalCode);
        setDenomination(mSharedPreferences.getString(getString(R.string.pref_denomination_key),
                getString(R.string.eur5)));
        mShortCodeText.setText(mSharedPreferences.getString(getString(R.string.pref_short_code_key), ""));
        mSerialText.setText(mSharedPreferences.getString(getString(R.string.pref_serial_number_key), ""));
        String comment = mSharedPreferences.getString(getString(R.string.pref_comment_key), "");
        String additionalComment =
                mSharedPreferences.getString(getString(R.string.pref_settings_comment_key), "");
        if (comment.endsWith(additionalComment))
            mCommentText.setText(comment.substring(0, comment.length() - additionalComment.length()));
        else
            mCommentText.setText(comment);
    }

    @NonNull
    private String getDenomination() {
        if (m5EurRadio.isChecked())
            return getString(R.string.eur5);
        if (m10EurRadio.isChecked())
            return getString(R.string.eur10);
        if (m20EurRadio.isChecked())
            return getString(R.string.eur20);
        if (m50EurRadio.isChecked())
            return getString(R.string.eur50);
        if (m100EurRadio.isChecked())
            return getString(R.string.eur100);
        if (m200EurRadio.isChecked())
            return getString(R.string.eur200);
        if (m500EurRadio.isChecked())
            return getString(R.string.eur500);
        return "";
    }

    private void setDenomination(String denomination) {
        if (denomination.equals(getString(R.string.eur5)))
            m5EurRadio.setChecked(true);
        if (denomination.equals(getString(R.string.eur10)))
            m10EurRadio.setChecked(true);
        if (denomination.equals(getString(R.string.eur20)))
            m20EurRadio.setChecked(true);
        if (denomination.equals(getString(R.string.eur50)))
            m50EurRadio.setChecked(true);
        if (denomination.equals(getString(R.string.eur100)))
            m100EurRadio.setChecked(true);
        if (denomination.equals(getString(R.string.eur200)))
            m200EurRadio.setChecked(true);
        if (denomination.equals(getString(R.string.eur500)))
            m500EurRadio.setChecked(true);
    }

    @OnClick(R.id.photo_button)
    void takePhoto() {
        Intent intent = new Intent(ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getActivity().getPackageManager()) == null) {
            Toast.makeText(getActivity(), getString(R.string.no_camera_activity), LENGTH_LONG).show();
            return;
        }
        if (ContextCompat.checkSelfPermission(mApp, CAMERA) != PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{CAMERA},
                    CAMERA_PERMISSION_REQUEST_CODE);
            return;
        }
        File photoFile = null;
        try {
            photoFile = createImageFile();
        } catch (IOException ex) {
            // photoFile will be null
        }
        if (photoFile == null) {
            Toast.makeText(getActivity(), getString(R.string.error_creating_file), LENGTH_LONG).show();
            return;
        }
        Uri photoUri = getUriForFile(getActivity(), getActivity().getPackageName(), photoFile);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
        getActivity().startActivityForResult(intent, IMAGE_CAPTURE_REQUEST_CODE);
    }

    private File createImageFile() throws IOException {
        File tempFolder = getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        for (File file: tempFolder.listFiles())
            file.delete();
        File image = createTempFile("bill_", ".png", tempFolder);
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    public void onOcrResult(String result) {
        mOcrResult = result;
        showOcrDialog();
    }

    @Override
    public String getPhotoPath() {
        return mCurrentPhotoPath;
    }

    @Override
    public void onSuggestions(String[] suggestions) {
        Activity activity = getActivity();
        if (activity == null) {
            // TODO können wir nicht aufrufen, weil das Fragment nicht attached ist
            // new Handler().postDelayed(this::executeCommentSuggestion, DELAY_NO_ACTIVITY_ON_COMMENT_SUGGESTIONS_MS);
            return;
        }
        ArrayAdapter<String> commentAdapter = new ArrayAdapter<>(activity,
                android.R.layout.simple_dropdown_item_1line, suggestions);
        mCommentText.setAdapter(commentAdapter);
    }

    private void showOcrDialog() {
        Vibrator v = (Vibrator) mApp.getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null) {
            v.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE));
        }
        Activity activity = getActivity();
        if (mOcrResult.equals(TextProcessor.EMPTY)) {
            if (activity != null) {
                new AlertDialog.Builder(activity)
                        .setTitle(R.string.ocr_dialog_title)
                        .setMessage(getString(R.string.ocr_dialog_empty))
                        .show();
            }
        }
        else if (mOcrResult.startsWith("Error: ")) {
            if (activity != null) {
                new AlertDialog.Builder(activity)
                        .setTitle(R.string.ocr_dialog_title)
                        .setMessage(mOcrResult.substring(7))
                        .show();
            }
        }
        else {
            if (mOcrResult.length() < 9) {
                putToClipboard(mShortCodeText.getText());
                mShortCodeText.setText(mOcrResult);
            } else {
                putToClipboard(mSerialText.getText());
                mSerialText.setText(mOcrResult);
            }
            Toast.makeText(getActivity(), getString(R.string.ocr_return), LENGTH_LONG).show();
            toastAfterToast(getActivity(), getString(R.string.ocr_paste), TOAST_DELAY_MS);
            toastAfterToast(getActivity(), getString(R.string.ocr_clipboard), 2 * TOAST_DELAY_MS);
        }
        mOcrResult = "";
    }

    private void toastAfterToast(final Context context, final CharSequence text, long delay) {
        Handler handler = new Handler();
        handler.postDelayed(() -> Toast.makeText(context, text, LENGTH_LONG).show(), delay);
    }

    private void putToClipboard(Editable text) {
        ClipboardManager manager = (ClipboardManager) mApp.getSystemService(Context.CLIPBOARD_SERVICE);
        if (manager == null)
            return;
        String s = text.toString();
        if (! s.isEmpty()) {
            ClipData data = ClipData.newPlainText(CLIPBOARD_LABEL, s);
            manager.setPrimaryClip(data);
        }
    }

    private class SavePreferencesTextWatcher implements TextWatcher {
        private String mPreferenceKey;

        SavePreferencesTextWatcher(String preference) {
            mPreferenceKey = preference;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            mSharedPreferences.edit().putString(mPreferenceKey, s.toString()).apply();
        }
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
            mCommentText.setText("");
            executeCommentSuggestion();
        }
    }

    private void executeCommentSuggestion() {
        if (! mSharedPreferences.getBoolean(getString(R.string.pref_login_values_ok_key), false))
            return;
        CommentSuggestion suggestion = new CommentSuggestion(mApiCaller, mSharedPreferences);
        suggestion.init(this, mApp.getString(R.string.pref_settings_comment_key));
        suggestion.execute(new LocationValues(
                mCountryText.getText().toString(),
                mCityText.getText().toString(),
                mPostalCodeText.getText().toString()));
    }
}
