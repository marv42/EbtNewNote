package com.marv42.ebt.newnote.test;

import com.marv42.ebt.newnote.EbtNewNote;

import android.test.ActivityInstrumentationTestCase2;
import android.view.KeyEvent;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;



public class EbtNewNoteTest extends ActivityInstrumentationTestCase2<EbtNewNote>
{
   private EbtNewNote mActivity;

   private EditText mCountryText;
   private EditText mCityText;
   private EditText mZipText;
   private EditText mPrinterText;
   private EditText mSerialText;
   private EditText mCommentText;

   private Spinner        mSpinner;
   private SpinnerAdapter mValues;

   public static final int ADAPTER_COUNT    = 7;
   public static final int INITIAL_POSITION = 0;



   public EbtNewNoteTest()
   {
      super("com.marv42.ebt.newnote", EbtNewNote.class);
   }



   @Override
   protected void
   setUp() throws Exception
   {
      super.setUp();

      setActivityInitialTouchMode(false);

      mActivity = getActivity();

      findAllViewsById();
   }



   private void
   findAllViewsById()
   {
      mCountryText = (EditText)mActivity.findViewById(com.marv42.ebt.newnote.R.id.edit_text_country);
      mCityText    = (EditText)mActivity.findViewById(com.marv42.ebt.newnote.R.id.edit_text_city   );
      mZipText     = (EditText)mActivity.findViewById(com.marv42.ebt.newnote.R.id.edit_text_zip    );
      mPrinterText = (EditText)mActivity.findViewById(com.marv42.ebt.newnote.R.id.edit_text_printer);
      mSerialText  = (EditText)mActivity.findViewById(com.marv42.ebt.newnote.R.id.edit_text_serial );
      mCommentText = (EditText)mActivity.findViewById(com.marv42.ebt.newnote.R.id.edit_text_comment);

      mSpinner = (Spinner)mActivity.findViewById(com.marv42.ebt.newnote.R.id.spinner);
      mValues = mSpinner.getAdapter();
   }
   
   
   
   public void
   testPreConditions()
   {
      //assertTrue(mSpinner.getOnItemSelectedListener() != null);
      // We don't have the listeners because they are somewhere else. :-/
      assertTrue(mValues != null);
      assertEquals(mValues.getCount(), ADAPTER_COUNT);
   }



   public void
   testSpinnerUI()
   {
      for (int n = INITIAL_POSITION; n < ADAPTER_COUNT; n++)
      {
         mActivity.runOnUiThread(
            new Runnable()
         {
            public void run()
            {
               mSpinner.requestFocus();
               mSpinner.setSelection(INITIAL_POSITION);
            }
         });

         sendKeys(KeyEvent.KEYCODE_DPAD_CENTER);
         for (int i = 1; i <= n; i++)
         {
            sendKeys(KeyEvent.KEYCODE_DPAD_DOWN);
         }
         sendKeys(KeyEvent.KEYCODE_DPAD_CENTER);

         String selection = (String)mSpinner.getItemAtPosition(mSpinner.getSelectedItemPosition());

         String resultText = (String)mValues.getItem(n);

         assertEquals(resultText,selection);
      }
   }
}
