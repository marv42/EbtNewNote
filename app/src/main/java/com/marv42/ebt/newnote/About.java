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

import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.MenuItem.OnMenuItemClickListener;
import android.widget.ImageView;
import android.widget.TextView;

import static com.marv42.ebt.newnote.EbtNewNote.LOG_TAG;

public class About implements OnMenuItemClickListener {
   private final Context mContext;
   
   About(Context context)
   {
      mContext = context;
   }
   
   public boolean onMenuItemClick(MenuItem item) {
      final Dialog dialog = new Dialog(mContext);
      
      dialog.setContentView(R.layout.about);
      dialog.setTitle(R.string.app_name);
      
      String versionNumber = "x.y.z";
      
      try {
         versionNumber = mContext.getPackageManager().getPackageInfo(
               mContext.getPackageName(), 0).versionName;
      } catch (NameNotFoundException e) {
         Log.e(LOG_TAG, "Package info not found", e);
      }
      
      ((TextView)dialog.findViewById(R.id.about_version)).setText(
            mContext.getString(R.string.version) + " " + versionNumber);
      
      ((ImageView)dialog.findViewById(R.id.about_image)).setImageResource(R.drawable.ic_ebt);
      
      ((TextView)dialog.findViewById(R.id.about_text)).setText(
            mContext.getString(R.string.app_name) + " " + mContext.getString(R.string.about_text));
      
      dialog.findViewById(R.id.about_ok_button).setOnClickListener(
            new View.OnClickListener()
      {
         public void
         onClick(View v)
         {
            dialog.dismiss();
         }
      });
      
      dialog.show();
      
      return true;
   }
}
