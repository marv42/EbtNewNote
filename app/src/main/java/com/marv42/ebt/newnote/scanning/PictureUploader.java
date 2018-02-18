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

import android.net.Uri;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


class PictureUploader
{
   private static final String SHORT_TERM_STORAGE_HOST = "http://ws.webservius.com/sts/v1/20mb/15m";
   private static final MediaType JSON = MediaType.parse("image/jpeg; charset=UTF-8");

   static String
   upload(final Uri uri)
   {
      OkHttpClient client = new OkHttpClient();
      String json = "{'wsvKey':'" + Keys.WSV_SHORT_TERM_STORAGE + "'}";
      RequestBody body = RequestBody.create(JSON, json);
      Request request = new Request.Builder()
              .url(SHORT_TERM_STORAGE_HOST)
              .post(body)
              .build();
      Response response = null;
      try {
         response = client.newCall(request).execute();
         return response.body().string();
      } catch (IOException e) {
         e.printStackTrace();
      }
      return null;
   }
}
