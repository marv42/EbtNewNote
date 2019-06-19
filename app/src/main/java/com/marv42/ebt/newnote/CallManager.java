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

import static android.preference.PreferenceManager.getDefaultSharedPreferences;

class CallManager {
   static synchronized boolean weAreCalling(int key, Context context) {
      SharedPreferences prefs = getDefaultSharedPreferences(context);
      String callingKey = context.getString(key);
      if (! prefs.getBoolean(callingKey, false)) {
         prefs.edit().putBoolean(callingKey, true).apply();
         return false;
      }
      return true;
   }
}
