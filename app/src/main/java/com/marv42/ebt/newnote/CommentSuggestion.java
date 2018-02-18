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
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.util.Pair;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;

import static com.marv42.ebt.newnote.EbtNewNote.LOG_TAG;


public class CommentSuggestion extends AsyncTask<LocationValues, Void, String[]>
{
   public static final int MAX_NUMBER_SUGGESTIONS = 50;

   private final Context mContext;



   public CommentSuggestion (final Context context)
   {
      mContext = context;
   }



   @Override
   protected String[]
   doInBackground(LocationValues... params)
   {
      return getSuggestion(params[0]);
   }



   @Override
   protected void
   onPostExecute(String[] s)
   {
      if (s != null && s.length > 0)
         ((EbtNewNote) mContext).setCommentSuggestions(s);

      if (! PreferenceManager.getDefaultSharedPreferences(mContext).edit()
            .putBoolean(mContext.getString(R.string.pref_calling_my_comments_key), false)
            .commit())
         Log.e(LOG_TAG, "editor's commit failed");
   }



   String[]
   getSuggestion(LocationValues l)
   {
      ApiCaller apiCaller = ApiCaller.getInstance();

      if (! apiCaller.callLogin())
      {
         apiCaller.getError();
         return null;
      }

      List<Pair<String, String>> params = new ArrayList<>();
      params.add(new Pair("m", "mycomments"));
      params.add(new Pair("v", "1"));
      params.add(new Pair("PHPSESSID",
            apiCaller.getResult().optString("sessionid")));
      params.add(new Pair("city",    l.getCity()      ));
      params.add(new Pair("country", l.getCountry()   ));
      params.add(new Pair("zip",     l.getPostalCode()));

      if (! apiCaller.callMyComments(params))
      {
         apiCaller.getError();
         return null;
      }

      JSONArray allComments = apiCaller.getResult().optJSONArray("data");

      List<JSONObject> list = new ArrayList<JSONObject>();
      for (int i = 0; i < allComments.length(); ++i)
         list.add(allComments.optJSONObject(i));

      Collections.sort(list, new Comparator<JSONObject>()
         {
            public int
            compare(JSONObject j1, JSONObject j2)
            {
               return j2.optInt("amount") - j1.optInt("amount");
            }
         });

      String additionalComment =
            PreferenceManager.getDefaultSharedPreferences(mContext).getString(
                  mContext.getString(R.string.pref_settings_comment_key), "");

      // unique wrt additionalComment
      List<String> uniqueList = new ArrayList<String>();
      for (int i = 0; i < list.size(); ++i)
      {
         String value = list.get(i).optString("comment");
         Log.d(LOG_TAG, value + " (" + list.get(i).optString("amount") + ")");
         if (value.endsWith(additionalComment))
            value = value.substring(0, value.length() - additionalComment.length());
         uniqueList.add(value);
      }
      uniqueList = new ArrayList<String>(new LinkedHashSet<String>(uniqueList));

      int numSuggestions = Math.min(uniqueList.size(), MAX_NUMBER_SUGGESTIONS);

      String[] s = new String[numSuggestions];
      for (int i = 0; i < numSuggestions; ++i)
         s[i] = uniqueList.get(i);

      return s;
   }
}
