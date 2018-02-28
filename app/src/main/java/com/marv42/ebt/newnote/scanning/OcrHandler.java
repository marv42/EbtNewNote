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

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Iterator;

import javax.net.ssl.HttpsURLConnection;

import static com.marv42.ebt.newnote.EbtNewNote.LOG_TAG;
import static com.marv42.ebt.newnote.scanning.Keys.OCR_SERVICE;

public class OcrHandler extends AsyncTask<Void, Void, String> {
    public interface Callback {
        void onOcrResult(String result);
    }
    private static final String OCR_HOST = "https://api.ocr.space/parse/image";

    private Callback mCallback;
    private String mBase64Image;

    public OcrHandler(Callback callback, String base64Image) {
        mCallback = callback;
        mBase64Image = base64Image;
    }

    @Override
    protected String doInBackground(Void... voids) {
        return doOcr();
    }

    @Override
    protected void onPostExecute(String result) {
        mCallback.onOcrResult(TextProcessor.getOcrResult(result));
    }

    private String doOcr() {
        try {
            URL url = new URL(OCR_HOST);
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            //connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            //connection.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
            //connection.setRequestProperty("Content-Language", "en-US");

            JSONObject postDataParams = new JSONObject();
            postDataParams.put("apikey", OCR_SERVICE);
            //postDataParams.put("url", );
//            postDataParams.put("filetype", "JPG");
            postDataParams.put("base64Image", "data:image/jpeg;base64," + mBase64Image);

            Log.d(LOG_TAG, "new DataOutputStream");
            DataOutputStream dataOutputStream = new DataOutputStream(connection.getOutputStream());
            dataOutputStream.writeBytes(getPostDataString(postDataParams));
            dataOutputStream.flush();
            dataOutputStream.close();

            Log.d(LOG_TAG, "new InputStreamReader");
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();
            while ((inputLine = bufferedReader.readLine()) != null) {
                response.append(inputLine);
            }
            bufferedReader.close();
            Log.d(LOG_TAG, "return value of response");
            return String.valueOf(response);
        } catch (Exception e) {
            Log.e(LOG_TAG, "error creating/sending OCR http request: " + e);
        }
        return null;
    }

    private String getPostDataString(JSONObject params) throws Exception {
        StringBuilder result = new StringBuilder();
        boolean first = true;
        Iterator<String> itr = params.keys();
        while (itr.hasNext()) {
            String key = itr.next();
            Object value = params.get(key);
            if (first)
                first = false;
            else
                result.append("&");
            result.append(URLEncoder.encode(key, "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(value.toString(), "UTF-8"));
        }
        return result.toString();
    }
}
