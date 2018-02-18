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
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
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

import com.marv42.ebt.newnote.scanning.OcrHandler;
import com.marv42.ebt.newnote.scanning.PhotoSaveLocation;

public class EbtNewNote extends AppCompatActivity implements LocationTask.Callback {
   public static final String LOG_TARGET = EbtNewNote.class.getSimpleName();

   private static final int IMAGE_CAPTURE_REQUEST_CODE = 0;

   private static String mOcrResult = "";

   protected MyGestureListener mGestureListener;

   private EditText             mCountryText;
   private EditText             mCityText;
   private EditText             mPostalCodeText;
   private EditText             mShortCodeText;
   private EditText             mSerialText;
   private AutoCompleteTextView mCommentText;
   private Spinner              mSpinner;

   private LocationTextWatcher mLocationTextWatcher;

//   private CommentTextWatcher  mCommentTextWatcher;
//   private CommentAdapter      mCommentAdapter;

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.submit);

      ApiCaller.create(this);

      findAllViewsById();

      mGestureListener = new MyGestureListener(this) {
         public boolean
         onTouch(View v, MotionEvent event) {
            if (mGestureListener.getDetector().onTouchEvent(event)) {
               startActivity(new Intent(EbtNewNote.this,
                       ResultRepresentation.class));
               return true;
            }
            else
               return false;
         }
      };

      (findViewById(R.id.submit_layout)).setOnTouchListener(mGestureListener);
      (findViewById(R.id.location_button)).setOnClickListener(new View.OnClickListener()
              {
                 public void
                 onClick(View v)
                 {
                    requestLocation();
                 }
              });
      (findViewById(R.id.submit_button)).setOnClickListener(new View.OnClickListener()
              {
                 public void
                 onClick(View v)
                 {
                    submitValues();
                 }
              });
      // avoid keyboard pop-up
      // TODO Doesn't work on the device
      if (! findViewById(R.id.submit_button).requestFocus())
         Log.e(LOG_TARGET, "Button didn't take focus. -> Why?");

      (findViewById(R.id.photo_button)).setOnClickListener(new View.OnClickListener()
              {
                 public void
                 onClick(View v)
                 {
                    acquireNumberFromPhoto();
                 }
              });
      resetPreferences();
   }

   @Override
   protected void onResume() {
      super.onResume();

      String loginChangedKey = getString(R.string.pref_login_changed_key);
      SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
      if (prefs.getBoolean(loginChangedKey, true) &&
              ! CallManager.weAreCalling(R.string.pref_calling_login_key, this))
      {
         if (! prefs.edit().putBoolean(loginChangedKey, false).commit())
            Log.e(EbtNewNote.LOG_TARGET, "Editor's commit failed");
         Log.d(EbtNewNote.LOG_TARGET, loginChangedKey + ": " + prefs.getBoolean(loginChangedKey, false));

         new LoginChecker(this).execute();
      }

      Log.d(EbtNewNote.LOG_TARGET, "check mOcrResult");
      if (mOcrResult.length() > 0)
         showOcrDialog();
      Log.d(EbtNewNote.LOG_TARGET, "-> empty");

      mLocationTextWatcher = new LocationTextWatcher();

      loadPreferences();
      loadLocationValues();

      if (mCountryText    != null)
         mCountryText   .addTextChangedListener(mLocationTextWatcher);
      if (mCityText       != null)
         mCityText      .addTextChangedListener(mLocationTextWatcher);
      if (mPostalCodeText != null)
         mPostalCodeText.addTextChangedListener(mLocationTextWatcher);

      if (mCountryText   .getText() == null ||
              mCityText      .getText() == null ||
              mPostalCodeText.getText() == null)
         return;

      if (! CallManager.weAreCalling(R.string.pref_calling_my_comments_key, this))
         new CommentSuggestion(this).execute(new LocationValues(
                 mCountryText   .getText().toString(),
                 mCityText      .getText().toString(),
                 mPostalCodeText.getText().toString()));
   }

   @Override
   protected void onPause() {
      if (mCountryText   .getText() == null ||
              mCityText      .getText() == null ||
              mPostalCodeText.getText() == null)
         return;

      savePreferences(new NoteData(mCountryText    .getText().toString(),
              mCityText       .getText().toString(),
              mPostalCodeText .getText().toString(),
              mSpinner.getSelectedItem().toString(),
              mShortCodeText  .getText().toString(),
              mSerialText     .getText().toString(),
              mCommentText    .getText().toString()));

      mLocationTextWatcher = null;

      super.onPause();
   }

   @Override
   public boolean onCreateOptionsMenu(Menu menu) {
      getMenuInflater().inflate(R.menu.menu, menu);
      menu.findItem(R.id.settings) .setIntent(new Intent(this, Settings.class));
      menu.findItem(R.id.about)    .setOnMenuItemClickListener(new About(this));
      menu.findItem(R.id.submitted).setIntent(new Intent(this, ResultRepresentation.class));
      menu.findItem(R.id.new_note) .setEnabled(false);
      return super.onCreateOptionsMenu(menu);
   }

   private void submitValues() {
      if (mCountryText   .getText()  == null ||
              mCityText      .getText()  == null ||
              mPostalCodeText.getText()  == null ||
              mShortCodeText .getText()  == null ||
              mSerialText    .getText()  == null ||
              mCommentText   .getText()  == null ||
              mSpinner.getSelectedItem() == null)
         return;

      Toast.makeText(this, getString(R.string.submitting), Toast.LENGTH_LONG).show();

      new NoteDataHandler(this).execute(new NoteData(
              mCountryText    .getText().toString(),
              mCityText       .getText().toString(),
              mPostalCodeText .getText().toString(),
              mSpinner.getSelectedItem().toString(),
              mShortCodeText  .getText().toString(),
              mSerialText     .getText().toString(),
              mCommentText    .getText().toString()));

      mShortCodeText.setText("");
      if (mSerialText.length() > 1)
         mSerialText.setText(mSerialText.getText().delete(1, mSerialText.length()));
   }

   private void requestLocation() {
      if (! CallManager.weAreCalling(R.string.pref_getting_location_key, this)) {
         // Toast.makeText(this, getString(R.string.getting_location), Toast.LENGTH_LONG).show();
         new LocationTask(getApplicationContext(), this).execute();
      }
   }

   public void setLocationValues(LocationValues l) {
      // only set complete locations
      if (TextUtils.isEmpty(l.getCountry()) ||
              TextUtils.isEmpty(l.getCity()   ) ||
              TextUtils.isEmpty(l.getPostalCode()))
         return;

      if (! l.canOverwrite() &&
              (mCountryText   .getText() == null ||
                      mCityText      .getText() == null ||
                      mPostalCodeText.getText() == null ||
                      ! TextUtils.isEmpty(mCountryText   .getText().toString()) ||
                      ! TextUtils.isEmpty(mCityText      .getText().toString()) ||
                      ! TextUtils.isEmpty(mPostalCodeText.getText().toString())))
         return;

      mCountryText   .setText(l.getCountry()   );
      mCityText      .setText(l.getCity()      );
      mPostalCodeText.setText(l.getPostalCode());
   }

   private void findAllViewsById()
   {
      mCountryText    = (EditText) findViewById(R.id.edit_text_country);
      mCityText       = (EditText) findViewById(R.id.edit_text_city   );
      mPostalCodeText = (EditText) findViewById(R.id.edit_text_zip    );
      mShortCodeText  = (EditText) findViewById(R.id.edit_text_printer);
      mSerialText     = (EditText) findViewById(R.id.edit_text_serial );
      mSpinner        = (Spinner)  findViewById(R.id.spinner);
      mCommentText    = (AutoCompleteTextView) findViewById(R.id.edit_text_comment);
      mCommentText.setThreshold(0);

      ArrayAdapter<CharSequence> denominationAdapter = ArrayAdapter.createFromResource(
              this, R.array.values, android.R.layout.simple_spinner_item);
      denominationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
      mSpinner.setAdapter(denominationAdapter);
   }

   private void resetPreferences()
   {
      SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
      Editor editor = prefs.edit();

      String callingLoginKey      = getString(R.string.pref_calling_login_key      );
      String callingMyCommentsKey = getString(R.string.pref_calling_my_comments_key);
      String gettingLocationKey   = getString(R.string.pref_getting_location_key);

      editor.putBoolean(callingLoginKey,      false);
      editor.putBoolean(callingMyCommentsKey, false);
      editor.putBoolean(gettingLocationKey,   false);
      if (! editor.commit())
         Log.e(LOG_TARGET, "Editor's commit failed");
      Log.d(LOG_TARGET, callingLoginKey + ": " + prefs.getBoolean(callingLoginKey, false));
   }

   public void savePreferences(final NoteData noteData) {
      SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
      editor.putString(getString(R.string.pref_country_key),       noteData.getCountry()     );
      editor.putString(getString(R.string.pref_city_key),          noteData.getCity()        );
      editor.putString(getString(R.string.pref_postal_code_key),   noteData.getPostalCode()  );
      editor.putString(getString(R.string.pref_denomination_key),  noteData.getDenomination());
      editor.putString(getString(R.string.pref_short_code_key),    noteData.getShortCode()   );
      editor.putString(getString(R.string.pref_serial_number_key), noteData.getSerialNumber());
      editor.putString(getString(R.string.pref_comment_key),       noteData.getComment()     );
      if (! editor.commit())
         Log.e(LOG_TARGET, "Editor's commit failed");
   }

   private void loadPreferences() {
      SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
      mCountryText   .setText(prefs.getString(getString(R.string.pref_country_key),       ""));
      mCityText      .setText(prefs.getString(getString(R.string.pref_city_key),          ""));
      mPostalCodeText.setText(prefs.getString(getString(R.string.pref_postal_code_key),   ""));
      mShortCodeText .setText(prefs.getString(getString(R.string.pref_short_code_key),    ""));
      mSerialText    .setText(prefs.getString(getString(R.string.pref_serial_number_key), ""));
      mCommentText   .setText(prefs.getString(getString(R.string.pref_comment_key),       ""));
      mSpinner.setSelection(getIndexOfDenomination(prefs.getString(getString(R.string.pref_denomination_key), "5 €")));

      String additionalComment = prefs.getString(getString(R.string.pref_settings_comment_key), "");
      if (mCommentText.getText().toString().endsWith(additionalComment))
         mCommentText.setText(mCommentText.getText().toString().substring(0,
                 mCommentText.getText().toString().length() - additionalComment.length()));
   }

   public void loadLocationValues() {
      setLocationValues(((ThisApp) getApplicationContext()).getLocationValues());
   }

   private int getIndexOfDenomination(final String denomination) {
      for (int i = 0; i < mSpinner.getAdapter().getCount(); ++i)
         if (mSpinner.getItemAtPosition(i).equals(denomination))
            return i;

      return -1;
   }

   private void acquireNumberFromPhoto() {
      Intent i = null;

//      if (Build.DEVICE.startsWith("generic")) // emulator
//         i = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE, null,
//                        this, CameraPreview.class);
//      else
      i = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);

      i.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(PhotoSaveLocation.getPath(this)));

      startActivityForResult(i, IMAGE_CAPTURE_REQUEST_CODE);
   }

   @Override
   protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
      super.onActivityResult(requestCode, resultCode, intent);

      if (requestCode != IMAGE_CAPTURE_REQUEST_CODE ||
              resultCode  != Activity.RESULT_OK)
         return;

      Toast.makeText(this, getString(R.string.processing), Toast.LENGTH_LONG).show();
      new OcrHandler(this).execute(Uri.fromFile(PhotoSaveLocation.getPath(this)));
   }

   public void setOcrResult(final String ocrResult) {
      Log.d(EbtNewNote.LOG_TARGET, "set mOcrResult");
      mOcrResult = ocrResult;
   }

   public void setCommentSuggestions(String[] suggestions) {
      ArrayAdapter<String> commentAdapter = new ArrayAdapter<String>(this,
              android.R.layout.simple_dropdown_item_1line, suggestions);
      mCommentText.setAdapter(commentAdapter);
   }

   private void showOcrDialog() {
      OcrDialogButtonHandler handler = new OcrDialogButtonHandler();

      if (mOcrResult.startsWith("Error: "))
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

   @Override
   public void onNewLocation(LocationValues locationValues) {
      setLocationValues(locationValues);
   }

   private class OcrDialogButtonHandler implements OnClickListener {
      public void
      onClick(DialogInterface dialog, int button)
      {
         if (button == DialogInterface.BUTTON_POSITIVE)
            mSerialText.setText(mOcrResult);

         Log.d(EbtNewNote.LOG_TARGET, "reset mOcrResult");
         mOcrResult = "";
         dialog.dismiss();
      }
   }

   private class LocationTextWatcher implements TextWatcher {
      public void afterTextChanged(Editable s) {
         if (mCountryText   .getText() == null ||
                 mCityText      .getText() == null ||
                 mPostalCodeText.getText() == null)
            return;

         if (! CallManager.weAreCalling(R.string.pref_calling_my_comments_key, EbtNewNote.this))
            new CommentSuggestion(EbtNewNote.this).execute(new LocationValues(
                    mCountryText   .getText().toString(),
                    mCityText      .getText().toString(),
                    mPostalCodeText.getText().toString()));
      }

      public void beforeTextChanged(CharSequence s, int start, int count, int after) {
      }

      public void onTextChanged(CharSequence s, int start, int before, int count) {
      }
   }
}
