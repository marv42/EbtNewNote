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

import androidx.annotation.NonNull;

import com.marv42.ebt.newnote.R;
import com.marv42.ebt.newnote.ThisApp;

import org.json.JSONObject;

import java.io.IOException;
import java.net.SocketTimeoutException;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static android.preference.PreferenceManager.getDefaultSharedPreferences;
import static com.marv42.ebt.newnote.ApiCaller.ERROR;
import static com.marv42.ebt.newnote.JsonHelper.getJsonObject;
import static com.marv42.ebt.newnote.scanning.PictureConverter.convert;

public class OcrHandler extends AsyncTask<Void, Void, String> {
    public interface Callback {
        void onOcrResult(String result);
    }

    private static final String OCR_HOST = "https://api.ocr.space/parse/image";

    private ThisApp mApp;
    private Callback mCallback;
    private String mPhotoPath;

    // TODO @Inject ?
    public OcrHandler(@NonNull ThisApp app, @NonNull Callback callback, @NonNull String photoPath) {
        mApp = app;
        mCallback = callback;
        mPhotoPath = photoPath;
    }

    @Override
    protected String doInBackground(Void... voids) {
        String base64Image = convert(mPhotoPath);
        FormBody.Builder formBodyBuilder = new FormBody.Builder();
        formBodyBuilder.add("apikey", getDefaultSharedPreferences(mApp).getString(
                mApp.getString(R.string.pref_settings_ocr_key), ""));
        formBodyBuilder.add("base64Image", "data:image/jpeg;base64," + base64Image);
        FormBody formBody = formBodyBuilder.build();

        Request request = new Request.Builder().url(OCR_HOST).post(formBody).build();
        Call call = new OkHttpClient().newCall(request);
        try (Response response = call.execute()) {
            if (!response.isSuccessful())
                return getJsonObject(ERROR, mApp.getString(R.string.http_error)
                        + " " + response.code()).toString();
            String body = response.body().string();
            JSONObject json = getJsonObject(body);
            if (json == null || json.has("error"))
                return getJsonObject(ERROR, body).toString();
            return json.toString();
        } catch (SocketTimeoutException e) {
            return getJsonObject(ERROR, mApp.getString(R.string.error_no_connection)).toString();
        } catch (IOException e) {
            return getJsonObject(ERROR, mApp.getString(R.string.internal_error)).toString();
        }
    }

    @Override
    protected void onPostExecute(String result) {
        mCallback.onOcrResult(TextProcessor.getOcrResult(result, mApp));
    }
}
