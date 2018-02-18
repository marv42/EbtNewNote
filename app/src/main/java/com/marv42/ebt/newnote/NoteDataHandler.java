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

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.util.Pair;
import android.text.Html;
import android.text.TextUtils;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static com.marv42.ebt.newnote.scanning.OcrHandler.OCR_NOTIFICATION_ID;


public class NoteDataHandler extends AsyncTask<NoteData, Void, SubmissionResult>
{
   private static final int EBT_NOTIFICATION_ID = 1;

   private final Context mContext;



   public NoteDataHandler(final Context context)
   {
      mContext = context;
   }



   @Override
   protected SubmissionResult
   doInBackground(final NoteData... params)
   {
      return submit(params[0]);
   }



   @Override
   protected void
   onPostExecute(final SubmissionResult result)
   {
      ThisApp app = (ThisApp)mContext.getApplicationContext();
      app.addResult(result);

      final int n = app.getNumberOfResults();
      String contentTitle = String.format(mContext.getResources().getQuantityString(R.plurals.xNotes, n) +
              " " + mContext.getString(R.string.sent), n);
      String tickerText = mContext.getString(R.string.insertion) + " " +
         (result.wasSuccessful() ? mContext.getString(R.string.successful)
                                 : mContext.getString(R.string.failed));

      Intent intent = new Intent(app, ResultRepresentation.class);
      PendingIntent contentIntent = PendingIntent.getActivity(app, 0, intent,
              PendingIntent.FLAG_UPDATE_CURRENT);
      NotificationCompat.Builder builder =
              new NotificationCompat.Builder(app, "miscellaneous")
                      .setSmallIcon(R.drawable.ic_stat_ebt)
                      .setContentTitle(contentTitle)
                      .setContentText(getSummary(app.getSummary()))
                      .setAutoCancel(true)
                      .setContentIntent(contentIntent);
      ((NotificationManager) app.getSystemService(
              Context.NOTIFICATION_SERVICE)).notify(OCR_NOTIFICATION_ID, builder.build());
   }

   private SubmissionResult
   submit(NoteData noteData)
   {
      NoteData submittedNoteData = new NoteData(
            noteData.getCountry(),
            noteData.getCity(),
            noteData.getPostalCode(),
            noteData.getDenomination(),
            noteData.getShortCode(),
            noteData.getSerialNumber(),
            noteData.getComment() +
               PreferenceManager.getDefaultSharedPreferences(mContext).getString(
                  mContext.getString(R.string.pref_settings_comment_key), ""));

      ApiCaller apiCaller = ApiCaller.getInstance();
      if (! apiCaller.callLogin())
         return new SubmissionResult(submittedNoteData, false, apiCaller.getError());

      List<Pair<String, String>> params = new ArrayList<>();
      params.add(new Pair("m", "insertbills"));
      params.add(new Pair("v", "1"));
      params.add(new Pair("PHPSESSID",
         apiCaller.getResult().optString("sessionid")));
      params.add(new Pair("city",       noteData.getCity())        );
      params.add(new Pair("zip",        noteData.getPostalCode())  );
      params.add(new Pair("country",    noteData.getCountry())     );
      params.add(new Pair("serial0",    noteData.getSerialNumber()));
      params.add(new Pair("denomination0",
         noteData.getDenomination().substring(0, noteData.getDenomination().length() - 2)));
      params.add(new Pair("shortcode0", noteData.getShortCode())   );
      params.add(new Pair("comment0",   submittedNoteData.getComment()));

      if (! apiCaller.callInsertBills(params))
         return new SubmissionResult(submittedNoteData, false, apiCaller.getError());

      JSONObject result = apiCaller.getResult();
      int billId = result.optInt("billId");
      int status = result.optInt("status");

      if (status == 0)
         return new SubmissionResult(submittedNoteData, true,
            mContext.getString(R.string.has_been_entered), billId);

      if (status == 1)
         return new SubmissionResult(submittedNoteData, true,
            mContext.getString(R.string.got_hit), billId, true);

      String reply = "";
      if ((status &  64) != 0)
         reply += mContext.getString(R.string.already_entered      ) + "<br>";
      if ((status & 128) != 0)
         reply += mContext.getString(R.string.different_short_code ) + "<br>";
      if ((status &   4) != 0)
         reply += mContext.getString(R.string.invalid_country      ) + "<br>";
      if ((status &  32) != 0)
         reply += mContext.getString(R.string.city_missing         ) + "<br>";
      if ((status &   2) != 0)
         reply += mContext.getString(R.string.invalid_denomination ) + "<br>"; // ;-)
      if ((status &  16) != 0)
         reply += mContext.getString(R.string.invalid_short_code   ) + "<br>";
      if ((status &   8) != 0)
         reply += mContext.getString(R.string.invalid_serial_number) + "<br>";
      if (reply.endsWith("<br>"))
         reply = reply.substring(0, reply.length() - 4);
      if (TextUtils.isEmpty(reply))
         reply = "Someone seems to have to debug something here...";

      return new SubmissionResult(submittedNoteData, false, reply, billId);
   }



   private CharSequence
   getSummary(final ThisApp.ResultSummary summary)
   {
      String s = "";

      int numHits = summary.getHits();
      if ( numHits > 0)
      {
         s = "<font color=\"green\">" + String.format(mContext.getResources().getQuantityString(
            R.plurals.xHits, numHits), numHits) + "</font>";
      }

      int numFailed = summary.getFailed();
      if ( numFailed > 0)
      {
         if (s.length() > 0)
            s += ", ";
         s += "<font color=\"red\">" + Integer.toString(numFailed) + " " +
            mContext.getString(R.string.failed) + "</font>";
      }

      int numSuccessful = summary.getSuccessful();
      if ( numSuccessful > 0)
      {
         if (s.length() > 0)
            s += ", ";
         s += Integer.toString(numSuccessful) + " " +
            mContext.getString(R.string.successful);
      }

      return Html.fromHtml(s);
   }
}
