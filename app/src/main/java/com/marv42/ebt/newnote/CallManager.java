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
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import static com.marv42.ebt.newnote.EbtNewNote.LOG_TAG;

class CallManager {
   static synchronized boolean weAreCalling(int key, Context context) {
      SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
      String callingKey = context.getString(key);
      if (! prefs.getBoolean(callingKey, false)) {
         if (! prefs.edit().putBoolean(callingKey, true).commit())
            Log.e(LOG_TAG, "Editor's commit failed");
         Log.d(LOG_TAG, callingKey + ": " + prefs.getBoolean(callingKey, false));
         return false;
      }
      return true;
   }
}
