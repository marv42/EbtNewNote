
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
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.marv42.ebt.newnote.scanning.OcrHandler;
import com.marv42.ebt.newnote.scanning.TextProcessor;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Named;

import dagger.android.support.DaggerFragment;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.CAMERA;
import static android.provider.MediaStore.ACTION_IMAGE_CAPTURE;
import static android.support.v4.content.FileProvider.getUriForFile;
import static android.support.v4.content.PermissionChecker.PERMISSION_GRANTED;
import static android.widget.Toast.LENGTH_LONG;
import static com.google.android.gms.location.LocationServices.getFusedLocationProviderClient;
import static java.io.File.createTempFile;

public class SubmitFragment extends DaggerFragment implements OcrHandler.Callback,
        CommentSuggestion.Callback /*, LifecycleOwner*/ {

    public interface Callback {
        void onSwitchToSubmitted();
    }

    @Inject
    Context mAppContext;
    @Inject
    @Named("Activity")
    Context mActivityContext;
    @Inject
    SharedPreferences mSharedPreferences;
    @Inject
    ApiCaller mApiCaller;

    public static final String LOG_TAG = SubmitFragment.class.getSimpleName();
    static final float VERTICAL_FLING_VELOCITY_THRESHOLD = 200;

    private static final int IMAGE_CAPTURE_REQUEST_CODE = 1;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 2;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 3;
    private static final int NUMBER_ADDRESSES = 5;
    private static final int REQUEST_CHECK_SETTINGS = 0x1;

    private String mCurrentPhotoPath;
    private static String mOcrResult = "";

    private GestureDetector mDetector;

    private EditText mCountryText;
    private EditText mCityText;
    private EditText mPostalCodeText;
    private RadioButton m5EurRadio;
    private RadioButton m10EurRadio;
    private RadioButton m20EurRadio;
    private RadioButton m50EurRadio;
    private RadioButton m100EurRadio;
    private RadioButton m200EurRadio;
    private RadioButton m500EurRadio;
    private EditText mShortCodeText;
    private EditText mSerialText;
    private AutoCompleteTextView mCommentText;

    private LocationTextWatcher mLocationTextWatcher;
    private FusedLocationProviderClient mFusedLocationClient;

//    @Override
//    public void onAttach(Context context) {
//        super.onAttach(context);
//    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.submit, parent, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        findAllViewsById(view);

        (view.findViewById(R.id.submit_layout)).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                v.performClick();
                return mDetector.onTouchEvent(event);
            }
        });
        (view.findViewById(R.id.location_button)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                requestLocation();
            }
        });
        (view.findViewById(R.id.submit_button)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                submitValues();
            }
        });
        (view.findViewById(R.id.photo_button)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                acquireNumberFromPhoto();
            }
        });
        if (!view.findViewById(R.id.edit_text_printer).requestFocus())
            Log.e(LOG_TAG, "Button didn't take focus. -> Why?");
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mFusedLocationClient = getFusedLocationProviderClient(mActivityContext);

        mDetector = new GestureDetector(mActivityContext, new MyGestureListener());

        resetPreferences();
    }

    @Override
    public void onResume() {
        Log.d(LOG_TAG, "onResume");
        super.onResume();

        loadPreferences();
        loadLocationValues();
        mLocationTextWatcher = new LocationTextWatcher();
        mCountryText.addTextChangedListener(mLocationTextWatcher);
        mCityText.addTextChangedListener(mLocationTextWatcher);
        mPostalCodeText.addTextChangedListener(mLocationTextWatcher);

        executeCommentSuggestion();
    }

    @Override
    public void onPause() {
        mSharedPreferences.edit().putString(getString(R.string.pref_country_key), mCountryText.getText().toString())
                .putString(getString(R.string.pref_city_key), mCityText.getText().toString())
                .putString(getString(R.string.pref_postal_code_key), mPostalCodeText.getText().toString())
                .putString(getString(R.string.pref_denomination_key), getDenomination())
                .putString(getString(R.string.pref_short_code_key), mShortCodeText.getText().toString())
                .putString(getString(R.string.pref_serial_number_key), mSerialText.getText().toString())
                .putString(getString(R.string.pref_comment_key), mCommentText.getText().toString()).apply();
        mLocationTextWatcher = null;
        super.onPause();
    }

    class MyGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDown(MotionEvent event) {
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            Log.d(LOG_TAG, "velocityY: " + velocityY);
            if (Math.abs(velocityY) > VERTICAL_FLING_VELOCITY_THRESHOLD)
                return false;
            switchToResults();
            return true;
        }
    }

    private void switchToResults() {
        ((Callback) mActivityContext).onSwitchToSubmitted();
    }

    private void submitValues() {
        Toast.makeText(mActivityContext, getString(R.string.submitting), LENGTH_LONG).show();
        new NoteDataHandler(mActivityContext, mApiCaller).execute(new NoteData(
                mCountryText.getText().toString(),
                mCityText.getText().toString(),
                mPostalCodeText.getText().toString(),
                getDenomination(),
                mShortCodeText.getText().toString().replaceAll("\\s+", ""),
                mSerialText.getText().toString().replaceAll("\\s+", ""),
                mCommentText.getText().toString()));
        mShortCodeText.setText("");
        mSerialText.setText("");
    }

    private void requestLocation() {
        if (ContextCompat.checkSelfPermission(mActivityContext, ACCESS_COARSE_LOCATION) != PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(mActivityContext, ACCESS_FINE_LOCATION) != PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }
        Toast.makeText(mActivityContext, getString(R.string.location_getting), LENGTH_LONG).show();
        mFusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    setLocationValues(location);
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (e instanceof ResolvableApiException) {
                    try {
                        ResolvableApiException resolvable = (ResolvableApiException) e;
                        resolvable.startResolutionForResult((Activity) mActivityContext, REQUEST_CHECK_SETTINGS);
                    } catch (IntentSender.SendIntentException ex) {
                        // ignore this
                    }
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length == 0 || grantResults[0] != PERMISSION_GRANTED) {
            Toast.makeText(mActivityContext, getString(R.string.no_permission), LENGTH_LONG).show();
            return;
        }
        Toast.makeText(mActivityContext, getString(R.string.permission), LENGTH_LONG).show();
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            requestLocation();
        } else if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            acquireNumberFromPhoto();
        }
    }

    void setLocationValues(Location l) {
        Log.d(LOG_TAG, "location: " + l.getLatitude() + ", " + l.getLongitude());
        try {
            final Geocoder gc = new Geocoder(mActivityContext, Locale.US);
            List<Address> addresses = gc.getFromLocation(l.getLatitude(), l.getLongitude(), NUMBER_ADDRESSES);
            Log.d(LOG_TAG, "Geocoder got " + addresses.size() + " address(es)");

            if (addresses.size() == 0)
                Toast.makeText(mActivityContext, mActivityContext.getString(R.string.location_no_address) + ": " + l.getLatitude() + ", " + l.getLongitude() + ".", LENGTH_LONG).show();

            for (Address a : addresses) {
                if (a == null)
                    continue;
                setLocationValues(
                        new LocationValues(a.getCountryName(), a.getLocality(), a.getPostalCode(), true));
            }
        } catch (IOException e) {
            Log.e(LOG_TAG, "Geocoder IOException: " + e);
            Toast.makeText(mActivityContext, mActivityContext.getString(R.string.location_geocoder_exception) + ": " + e.getMessage() + ".", LENGTH_LONG).show();
        }
    }

    public void setLocationValues(LocationValues l) {
        // only set complete locations
        if (TextUtils.isEmpty(l.getCountry()) ||
                TextUtils.isEmpty(l.getCity()   ) ||
                TextUtils.isEmpty(l.getPostalCode()))
            return;
        if (! l.canOverwrite() && (
                mCountryText.getText() == null || mCityText.getText() == null || mPostalCodeText.getText() == null ||
                ! TextUtils.isEmpty(mCountryText   .getText().toString()) ||
                ! TextUtils.isEmpty(mCityText      .getText().toString()) ||
                ! TextUtils.isEmpty(mPostalCodeText.getText().toString())))
            return;
        mCountryText   .setText(l.getCountry()   );
        mCityText      .setText(l.getCity()      );
        mPostalCodeText.setText(l.getPostalCode());
    }

    private void findAllViewsById(View view) {
        mCountryText    = view.findViewById(R.id.edit_text_country);
        mCityText       = view.findViewById(R.id.edit_text_city);
        mPostalCodeText = view.findViewById(R.id.edit_text_zip);
        m5EurRadio = view.findViewById(R.id.radio_5);
        m10EurRadio = view.findViewById(R.id.radio_10);
        m20EurRadio = view.findViewById(R.id.radio_20);
        m50EurRadio = view.findViewById(R.id.radio_50);
        m100EurRadio = view.findViewById(R.id.radio_100);
        m200EurRadio = view.findViewById(R.id.radio_200);
        m500EurRadio = view.findViewById(R.id.radio_500);
        mShortCodeText  = view.findViewById(R.id.edit_text_printer);
        mSerialText     = view.findViewById(R.id.edit_text_serial);
        mCommentText    = view.findViewById(R.id.edit_text_comment);
        mCommentText.setThreshold(0);
    }

    private void resetPreferences() {
        String callingLoginKey      = getString(R.string.pref_calling_login_key      );
        String callingMyCommentsKey = getString(R.string.pref_calling_my_comments_key);
        String gettingLocationKey   = getString(R.string.pref_getting_location_key);
        mSharedPreferences.edit().putBoolean(callingLoginKey, false)
                .putBoolean(callingMyCommentsKey, false)
                .putBoolean(gettingLocationKey, false).apply();
        Log.d(LOG_TAG, callingLoginKey + ": " + mSharedPreferences.getBoolean(callingLoginKey, false));
    }

    private void loadPreferences() {
        mCountryText   .setText(mSharedPreferences.getString(getString(R.string.pref_country_key),       ""));
        mCityText      .setText(mSharedPreferences.getString(getString(R.string.pref_city_key),          ""));
        mPostalCodeText.setText(mSharedPreferences.getString(getString(R.string.pref_postal_code_key),   ""));
        mShortCodeText .setText(mSharedPreferences.getString(getString(R.string.pref_short_code_key),    ""));
        mSerialText    .setText(mSharedPreferences.getString(getString(R.string.pref_serial_number_key), ""));
        mCommentText   .setText(mSharedPreferences.getString(getString(R.string.pref_comment_key),       ""));
        setDenomination(mSharedPreferences.getString(getString(R.string.pref_denomination_key), "5 €"));

        String additionalComment = mSharedPreferences.getString(getString(R.string.pref_settings_comment_key), "");
        if (mCommentText.getText().toString().endsWith(additionalComment))
            mCommentText.setText(mCommentText.getText().toString().substring(0,
                    mCommentText.getText().toString().length() - additionalComment.length()));
    }

    public void loadLocationValues() {
        setLocationValues(((ThisApp) mAppContext).getLocationValues());
    }

    private String getDenomination() {
        if (m5EurRadio.isChecked())
            return "5 €";
        if (m10EurRadio.isChecked())
            return "10 €";
        if (m20EurRadio.isChecked())
            return "20 €";
        if (m50EurRadio.isChecked())
            return "50 €";
        if (m100EurRadio.isChecked())
            return "100 €";
        if (m200EurRadio.isChecked())
            return "200 €";
        if (m500EurRadio.isChecked())
            return "500 €";
        return "";
    }

    private void setDenomination(String denomination) {
        if (denomination.equals("5 €"))
            m5EurRadio.setChecked(true);
        if (denomination.equals("10 €"))
            m10EurRadio.setChecked(true);
        if (denomination.equals("20 €"))
            m20EurRadio.setChecked(true);
        if (denomination.equals("50 €"))
            m50EurRadio.setChecked(true);
        if (denomination.equals("100 €"))
            m100EurRadio.setChecked(true);
        if (denomination.equals("200 €"))
            m200EurRadio.setChecked(true);
        if (denomination.equals("500 €"))
            m500EurRadio.setChecked(true);
    }

    private void acquireNumberFromPhoto() {
        Intent intent = new Intent(ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(mActivityContext.getPackageManager()) == null) {
            Toast.makeText(mActivityContext, getString(R.string.no_camera_activity), LENGTH_LONG).show();
            return;
        }
        if (ContextCompat.checkSelfPermission(mActivityContext, CAMERA) != PERMISSION_GRANTED) {
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
            Toast.makeText(mActivityContext, getString(R.string.error_creating_file), LENGTH_LONG).show();
            return;
        }
        Uri photoURI = getUriForFile(mActivityContext, mActivityContext.getPackageName(), photoFile);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);

        startActivityForResult(intent, IMAGE_CAPTURE_REQUEST_CODE);
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.GERMANY).format(new Date());
        File image = createTempFile("EBT_" + timeStamp + "_", ".png",
                mActivityContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES));
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    public void onOcrResult(String result) {
        Log.d(LOG_TAG, "set mOcrResult: " + result);
        mOcrResult = result;
        showOcrDialog();
        if (! new File(mCurrentPhotoPath).delete()) {
            Log.e(LOG_TAG, "Error deleting image file");
        }
    }

    @Override
    public String getPhotoPath() {
        return mCurrentPhotoPath;
    }

    @Override
    public void onSuggestions(String[] suggestions) {
        ArrayAdapter<String> commentAdapter = new ArrayAdapter<>(mActivityContext,
                android.R.layout.simple_dropdown_item_1line, suggestions);
        mCommentText.setAdapter(commentAdapter);
    }

    private void showOcrDialog() {
        if (mOcrResult.equals(TextProcessor.EMPTY))
            new AlertDialog.Builder(mActivityContext).setTitle(R.string.ocr_dialog_title)
                    .setMessage(getString(R.string.ocr_dialog_empty))
                    .show();
        else if (mOcrResult.startsWith("Error: "))
            new AlertDialog.Builder(mActivityContext).setTitle(R.string.ocr_dialog_title)
                    .setMessage(mOcrResult.substring(7, mOcrResult.length()))
                    .show();
        else {
            if (mOcrResult.length() < 9)
                mShortCodeText.setText(mOcrResult);
            else
                mSerialText.setText(mOcrResult);
            Toast.makeText(mActivityContext, getString(R.string.ocr_return), LENGTH_LONG).show();
            Toast.makeText(mActivityContext, getString(R.string.ocr_paste), LENGTH_LONG).show();
        }
        mOcrResult = "";
    }

    private class LocationTextWatcher implements TextWatcher {
        public void afterTextChanged(Editable s) {
            executeCommentSuggestion();
            mCommentText.setText("");
        }

        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
    }

    private void executeCommentSuggestion() {
        if (! CallManager.weAreCalling(R.string.pref_calling_my_comments_key, mActivityContext.getApplicationContext()))
            new CommentSuggestion(this, mActivityContext.getApplicationContext(), mApiCaller, mSharedPreferences)
                    .execute(new LocationValues(
                            mCountryText.getText().toString(),
                            mCityText.getText().toString(),
                            mPostalCodeText.getText().toString()));
    }
}
