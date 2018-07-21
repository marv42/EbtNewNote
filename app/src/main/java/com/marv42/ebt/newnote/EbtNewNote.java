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
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
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
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Spinner;
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

import dagger.android.support.DaggerAppCompatActivity;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.CAMERA;
import static android.provider.MediaStore.ACTION_IMAGE_CAPTURE;
import static android.support.v4.content.FileProvider.getUriForFile;
import static android.support.v4.content.PermissionChecker.PERMISSION_GRANTED;
import static android.widget.Toast.LENGTH_LONG;
import static com.google.android.gms.location.LocationServices.getFusedLocationProviderClient;
import static java.io.File.createTempFile;

public class EbtNewNote extends DaggerAppCompatActivity implements OcrHandler.Callback, CommentSuggestion.Callback /*, LifecycleOwner*/ {
    @Inject
    Context mContext;
    @Inject
    SharedPreferences mSharedPreferences;
    @Inject
    ApiCaller mApiCaller;

    public static final String LOG_TAG = EbtNewNote.class.getSimpleName();
    static final int EBT_NOTIFICATION_ID = 1;

    private static final int IMAGE_CAPTURE_REQUEST_CODE = 1;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 2;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 3;
    private static final int NUMBER_ADDRESSES = 5;
    private static final int REQUEST_CHECK_SETTINGS = 0x1;


    private String mCurrentPhotoPath;
    private static String mOcrResult = "";

    protected MyGestureListener mGestureListener;

    private EditText mCountryText;
    private EditText mCityText;
    private EditText mPostalCodeText;
    private EditText mShortCodeText;
    private EditText mSerialText;
    private AutoCompleteTextView mCommentText;
    private Spinner mSpinner;

    private LocationTextWatcher mLocationTextWatcher;
    private FusedLocationProviderClient mFusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.submit);
        findAllViewsById();

        mFusedLocationClient = getFusedLocationProviderClient(this);

        mGestureListener = new MyGestureListener(this) {
            public boolean onTouch(View v, MotionEvent event) {
                if (mGestureListener.getDetector().onTouchEvent(event)) {
                    startActivity(new Intent(EbtNewNote.this, ResultRepresentation.class));
                    return true;
                } else
                    return false;
            }
        };

        (findViewById(R.id.submit_layout)).setOnTouchListener(mGestureListener);
        (findViewById(R.id.location_button)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                requestLocation();
            }
        });
        (findViewById(R.id.submit_button)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                submitValues();
            }
        });
        (findViewById(R.id.photo_button)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                acquireNumberFromPhoto();
            }
        });
        if (!findViewById(R.id.edit_text_printer).requestFocus())
            Log.e(LOG_TAG, "Button didn't take focus. -> Why?");

        resetPreferences();
    }

    @Override
    protected void onResume() {
        Log.d(LOG_TAG, "onResume");
        super.onResume();

        String loginChangedKey = getString(R.string.pref_login_changed_key);
        if (mSharedPreferences.getBoolean(loginChangedKey, true) &&
                !CallManager.weAreCalling(R.string.pref_calling_login_key, this)) {
            if (!mSharedPreferences.edit().putBoolean(loginChangedKey, false).commit())
                Log.e(LOG_TAG, "Editor's commit failed");
            Log.d(LOG_TAG, loginChangedKey + ": " + mSharedPreferences.getBoolean(loginChangedKey, false));
            new LoginChecker(this, mApiCaller).execute();
        }

        mLocationTextWatcher = new LocationTextWatcher();

        loadPreferences();
        loadLocationValues();

        if (mCountryText != null)
            mCountryText.addTextChangedListener(mLocationTextWatcher);
        if (mCityText != null)
            mCityText.addTextChangedListener(mLocationTextWatcher);
        if (mPostalCodeText != null)
            mPostalCodeText.addTextChangedListener(mLocationTextWatcher);

        executeCommentSuggestion();
    }

    @Override
    protected void onPause() {
        if (mCountryText.getText() == null || mCityText.getText() == null || mPostalCodeText.getText() == null)
            return;
        savePreferences(new NoteData(
                mCountryText.getText().toString(),
                mCityText.getText().toString(),
                mPostalCodeText.getText().toString(),
                mSpinner.getSelectedItem().toString(),
                mShortCodeText.getText().toString(),
                mSerialText.getText().toString(),
                mCommentText.getText().toString()));
        mLocationTextWatcher = null;
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        menu.findItem(R.id.settings).setIntent(new Intent(this, Settings.class));
        menu.findItem(R.id.about).setOnMenuItemClickListener(new About(this));
        menu.findItem(R.id.submitted).setIntent(new Intent(this, ResultRepresentation.class));
        menu.findItem(R.id.new_note).setEnabled(false);
        return super.onCreateOptionsMenu(menu);
    }

    private void submitValues() {
        if (mCountryText.getText() == null ||
                mCityText.getText() == null ||
                mPostalCodeText.getText() == null ||
                mShortCodeText.getText() == null ||
                mSerialText.getText() == null ||
                mCommentText.getText() == null ||
                mSpinner.getSelectedItem() == null)
            return;
        Toast.makeText(this, getString(R.string.submitting), LENGTH_LONG).show();
        new NoteDataHandler(mContext, mApiCaller).execute(new NoteData(
                mCountryText.getText().toString(),
                mCityText.getText().toString(),
                mPostalCodeText.getText().toString(),
                mSpinner.getSelectedItem().toString(),
                mShortCodeText.getText().toString().replaceAll("\\s+", ""),
                mSerialText.getText().toString().replaceAll("\\s+", ""),
                mCommentText.getText().toString()));
        mShortCodeText.setText("");
        mSerialText.setText("");
    }

    private void requestLocation() {
        if (ActivityCompat.checkSelfPermission(this, ACCESS_COARSE_LOCATION) != PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) != PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }
        Toast.makeText(this, getString(R.string.location_getting), LENGTH_LONG).show();
        mFusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    setLocationValues(location);
                }
            }
        }).addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (e instanceof ResolvableApiException) {
                    // Location settings are not satisfied, but this can be fixed
                    // by showing the user a dialog.
                    try {
                        // Show the dialog by calling startResolutionForResult(),
                        // and check the result in onActivityResult().
                        ResolvableApiException resolvable = (ResolvableApiException) e;
                        resolvable.startResolutionForResult(EbtNewNote.this, REQUEST_CHECK_SETTINGS);
                    } catch (IntentSender.SendIntentException sendEx) {
                        // Ignore the error.
                    }
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case LOCATION_PERMISSION_REQUEST_CODE: {
                if (grantResults.length <= 0 || grantResults[0] != PERMISSION_GRANTED) {
                    Toast.makeText(this, getString(R.string.location_no_permission), LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, getString(R.string.location_permission), LENGTH_LONG).show();
                }
                break;
            }
            case CAMERA_PERMISSION_REQUEST_CODE: {
                if (grantResults.length <= 0 || grantResults[0] != PERMISSION_GRANTED) {
                    Toast.makeText(this, getString(R.string.no_camera_permission), LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, getString(R.string.camera_permission), LENGTH_LONG).show();
                }
            }
        }
    }

    void setLocationValues(Location l) {
        Log.d(LOG_TAG, "location: " + l.getLatitude() + ", " + l.getLongitude());
        try {
            final Geocoder gc = new Geocoder(mContext);
            List<Address> addresses = gc.getFromLocation(l.getLatitude(), l.getLongitude(), NUMBER_ADDRESSES);
            Log.d(LOG_TAG, "Geocoder got " + addresses.size() + " address(es)");

            if (addresses.size() == 0)
                Toast.makeText(mContext, mContext.getString(R.string.location_no_address) + ": " + l.getLatitude() + ", " + l.getLongitude() + ".", LENGTH_LONG).show();

            for (Address a : addresses) {
                if (a == null)
                    continue;
                setLocationValues(
                        new LocationValues(a.getCountryName(), a.getLocality(), a.getPostalCode(), true));
            }
        } catch (IOException e) {
            Log.e(LOG_TAG, "Geocoder IOException: " + e);
            Toast.makeText(mContext, mContext.getString(R.string.location_geocoder_exception) + ": " + e.getMessage() + ".", LENGTH_LONG).show();
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

    private void findAllViewsById() {
        mCountryText    = findViewById(R.id.edit_text_country);
        mCityText       = findViewById(R.id.edit_text_city);
        mPostalCodeText = findViewById(R.id.edit_text_zip);
        mShortCodeText  = findViewById(R.id.edit_text_printer);
        mSerialText     = findViewById(R.id.edit_text_serial);
        mSpinner        = findViewById(R.id.spinner);
        mCommentText    = findViewById(R.id.edit_text_comment);
        mCommentText.setThreshold(0);

        ArrayAdapter<CharSequence> denominationAdapter = ArrayAdapter.createFromResource(
                this, R.array.values, android.R.layout.simple_spinner_item);
        denominationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinner.setAdapter(denominationAdapter);
    }

    private void resetPreferences() {
        SharedPreferences.Editor editor = mSharedPreferences.edit();

        String callingLoginKey      = getString(R.string.pref_calling_login_key      );
        String callingMyCommentsKey = getString(R.string.pref_calling_my_comments_key);
        String gettingLocationKey   = getString(R.string.pref_getting_location_key);

        editor.putBoolean(callingLoginKey,      false);
        editor.putBoolean(callingMyCommentsKey, false);
        editor.putBoolean(gettingLocationKey,   false);
        if (! editor.commit())
            Log.e(LOG_TAG, "Editor's commit failed");
        Log.d(LOG_TAG, callingLoginKey + ": " + mSharedPreferences.getBoolean(callingLoginKey, false));
    }

    public void savePreferences(final NoteData noteData) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putString(getString(R.string.pref_country_key),       noteData.getCountry()     );
        editor.putString(getString(R.string.pref_city_key),          noteData.getCity()        );
        editor.putString(getString(R.string.pref_postal_code_key),   noteData.getPostalCode()  );
        editor.putString(getString(R.string.pref_denomination_key),  noteData.getDenomination());
        editor.putString(getString(R.string.pref_short_code_key),    noteData.getShortCode()   );
        editor.putString(getString(R.string.pref_serial_number_key), noteData.getSerialNumber());
        editor.putString(getString(R.string.pref_comment_key),       noteData.getComment()     );
        if (! editor.commit())
            Log.e(LOG_TAG, "Editor's commit failed");
    }

    private void loadPreferences() {
        mCountryText   .setText(mSharedPreferences.getString(getString(R.string.pref_country_key),       ""));
        mCityText      .setText(mSharedPreferences.getString(getString(R.string.pref_city_key),          ""));
        mPostalCodeText.setText(mSharedPreferences.getString(getString(R.string.pref_postal_code_key),   ""));
        mShortCodeText .setText(mSharedPreferences.getString(getString(R.string.pref_short_code_key),    ""));
        mSerialText    .setText(mSharedPreferences.getString(getString(R.string.pref_serial_number_key), ""));
        mCommentText   .setText(mSharedPreferences.getString(getString(R.string.pref_comment_key),       ""));
        mSpinner.setSelection(getIndexOfDenomination(mSharedPreferences.getString(getString(R.string.pref_denomination_key), "5 €")));

        String additionalComment = mSharedPreferences.getString(getString(R.string.pref_settings_comment_key), "");
        if (mCommentText.getText().toString().endsWith(additionalComment))
            mCommentText.setText(mCommentText.getText().toString().substring(0,
                    mCommentText.getText().toString().length() - additionalComment.length()));
    }

    public void loadLocationValues() {
        setLocationValues(((ThisApp) mContext).getLocationValues());
    }

    private int getIndexOfDenomination(final String denomination) {
        for (int i = 0; i < mSpinner.getAdapter().getCount(); ++i)
            if (mSpinner.getItemAtPosition(i).equals(denomination))
                return i;

        return -1;
    }

    private void acquireNumberFromPhoto() {
        Intent intent = new Intent(ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) == null) {
            Toast.makeText(this, getString(R.string.no_camera_activity), LENGTH_LONG).show();
            return;
        }
        File photoFile = null;
        try {
            photoFile = createImageFile();
        } catch (IOException ex) {
            // photoFile will be null
        }
        if (photoFile == null) {
            Toast.makeText(this, getString(R.string.error_creating_file), LENGTH_LONG).show();
            return;
        }
        Uri photoURI = getUriForFile(this, getPackageName(), photoFile);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);

        if (ContextCompat.checkSelfPermission(this, CAMERA) != PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
            return;
        }
        startActivityForResult(intent, IMAGE_CAPTURE_REQUEST_CODE);
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.GERMANY).format(new Date());
        File image = createTempFile("EBT_" + timeStamp + "_", ".png",
                getExternalFilesDir(Environment.DIRECTORY_PICTURES));
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (resultCode == RESULT_OK) {
            if (requestCode == IMAGE_CAPTURE_REQUEST_CODE) {
                Toast.makeText(this, getString(R.string.processing), LENGTH_LONG).show();
                new OcrHandler(this, mCurrentPhotoPath).execute();
            }
        }
    }

    @Override
    public void onOcrResult(String result) {
        Log.d(LOG_TAG, "set mOcrResult: " + result);
        mOcrResult = result;
        showOcrDialog();
    }

    @Override
    public void onSuggestions(String[] suggestions) {
        ArrayAdapter<String> commentAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, suggestions);
        mCommentText.setAdapter(commentAdapter);
    }

    private void showOcrDialog() {
        Log.d(LOG_TAG, "showOcrDialog");
        OcrDialogButtonHandler handler = new OcrDialogButtonHandler();

        if (mOcrResult.equals(TextProcessor.EMPTY))
            new AlertDialog.Builder(this).setTitle(R.string.ocr_dialog_title)
                    .setMessage(getString(R.string.ocr_dialog_text) + "\n\n"
                            + getString(R.string.ocr_dialog_empty))
                    .setNeutralButton(R.string.ok, handler)
                    .show();
        else if (mOcrResult.startsWith("Error: "))
            new AlertDialog.Builder(this).setTitle(R.string.ocr_dialog_title)
                    .setMessage(mOcrResult.substring(7, mOcrResult.length()))
                    .setNeutralButton(R.string.ok, handler)
                    .show();
        else
            new AlertDialog.Builder(this).setTitle(R.string.ocr_dialog_title)
                    .setMessage(getString(R.string.ocr_dialog_text) + "\n\n   "
                            + mOcrResult + "\n\n" + getString(R.string.ocr_dialog_question))
                    .setPositiveButton(R.string.yes, handler)
                    .setNegativeButton(R.string.no, handler)
                    .show();
    }

    private class OcrDialogButtonHandler implements OnClickListener {
        public void
        onClick(DialogInterface dialog, int button)
        {
            if (button == DialogInterface.BUTTON_POSITIVE)
                mSerialText.setText(mOcrResult);

            Log.d(LOG_TAG, "reset mOcrResult");
            mOcrResult = "";
            dialog.dismiss();
        }
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
        if (! CallManager.weAreCalling(R.string.pref_calling_my_comments_key, getApplicationContext()) &&
                mCountryText.getText() != null && mCityText.getText() != null && mPostalCodeText.getText() != null)
            new CommentSuggestion(this, getApplicationContext(), mApiCaller, mSharedPreferences)
                    .execute(new LocationValues(
                            mCountryText.getText().toString(),
                            mCityText.getText().toString(),
                            mPostalCodeText.getText().toString()));
    }
}
