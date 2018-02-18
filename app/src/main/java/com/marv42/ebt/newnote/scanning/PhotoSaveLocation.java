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

package com.marv42.ebt.newnote.scanning;

import java.io.File;
import java.io.IOException;

import com.marv42.ebt.newnote.EbtNewNote;
import com.marv42.ebt.newnote.R;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;



public class PhotoSaveLocation
{
   public static final String PICTURE_FILE = "Picture.jpg";
   
   
   
   public static final File
   getPath(Context context)
   {
      File path = new File(Environment.getExternalStorageDirectory(), context.getPackageName());
      File file = new File(path, PICTURE_FILE);
      
      if (! file.getParentFile().mkdirs())
         Log.w(EbtNewNote.LOG_TARGET, "Didn't create directory for " + PICTURE_FILE );
      
      try
      {
         if (! file.createNewFile())
            Log.w(EbtNewNote.LOG_TARGET, "Didn't create " + PICTURE_FILE );
      }
      catch (IOException e)
      {
         Log.e(EbtNewNote.LOG_TARGET, e.getMessage());
         Toast.makeText(context, context.getString(R.string.error_creating_file),
                        Toast.LENGTH_LONG).show();
//         return null;
      }
      
//      file.deleteOnExit();
      
      Log.d(EbtNewNote.LOG_TARGET, "getPath: " + file.getAbsolutePath());
      
      return file;
   }
}
