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

import com.marv42.ebt.newnote.ApiCaller;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static com.marv42.ebt.newnote.JsonHelper.getJsonObject;
import static com.marv42.ebt.newnote.scanning.Keys.OCR_SERVICE;
import static com.marv42.ebt.newnote.scanning.PictureConverter.convert;

public class OcrHandler extends AsyncTask<Void, Void, String> {
    public interface Callback {
        void onOcrResult(String result);
        String getPhotoPath();
    }

    private static final String OCR_HOST = "https://api.ocr.space/parse/image";

    private Callback mCallback;

    public OcrHandler(Callback callback) {
        mCallback = callback;
    }

    @Override
    protected String doInBackground(Void... voids) {
        String base64Image = convert(mCallback.getPhotoPath());
        FormBody.Builder formBodyBuilder = new FormBody.Builder();
        formBodyBuilder.add("apikey", OCR_SERVICE);
        formBodyBuilder.add("base64Image", "data:image/jpeg;base64," + base64Image);
        FormBody formBody = formBodyBuilder.build();

        Request request = new Request.Builder().url(OCR_HOST).post(formBody).build();
        Call call = new OkHttpClient().newCall(request);
        try (Response response = call.execute()) {
            if (! response.isSuccessful())
                return null;
            String body = response.body().string();
            JSONObject json = getJsonObject(body);
            if (json == null || json.has("error"))
                return getJsonObject(ApiCaller.ERROR, body).toString();
            return json.toString();
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    protected void onPostExecute(String result) {
        mCallback.onOcrResult(TextProcessor.getOcrResult(result));
    }
}
