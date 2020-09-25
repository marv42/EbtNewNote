/*
 Copyright (c) 2010 - 2020 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote.scanning;

import android.os.AsyncTask;

import androidx.annotation.NonNull;

import com.marv42.ebt.newnote.HttpCaller;
import com.marv42.ebt.newnote.exceptions.CallResponseException;
import com.marv42.ebt.newnote.exceptions.HttpCallException;
import com.marv42.ebt.newnote.exceptions.NoNotificationManagerException;
import com.marv42.ebt.newnote.exceptions.OcrException;

import org.jetbrains.annotations.NotNull;

import okhttp3.FormBody;
import okhttp3.Request;

import static com.marv42.ebt.newnote.exceptions.ErrorMessage.ERROR;
import static com.marv42.ebt.newnote.scanning.PictureConverter.convert;

public class OcrHandler extends AsyncTask<Void, Void, String> {
    public interface Callback {
        void onOcrResult(String result) throws NoNotificationManagerException;
    }

    private static final String OCR_HOST = "api.ocr.space";
    private static final String OCR_URL = "https://" + OCR_HOST + "/parse/image";

    private Callback callback;
    private String photoPath;
    private String apiKey;

    public OcrHandler(@NonNull Callback callback, @NonNull String photoPath, @NonNull String apiKey) {
        this.callback = callback;
        this.photoPath = photoPath;
        this.apiKey = apiKey;
    }

    @Override
    protected String doInBackground(Void... voids) {
        try {
            return getResult();
        } catch (HttpCallException e) {
            return ERROR + "R.string.http_error: " + e.getMessage();
        } catch (CallResponseException e) {
            return ERROR + "R.string.server_error: " + e.getMessage();
        } catch (OcrException e) {
            return ERROR + "R.string.ocr_error: " + e.getMessage();
        }
    }

    private String getResult() throws HttpCallException, CallResponseException, OcrException {
        FormBody formBody = getFormBody();
        Request request = new Request.Builder().url(OCR_URL).post(formBody).build();
        String body = new HttpCaller().call(request);
        return TextProcessor.getOcrResult(body);
    }

    @NotNull
    private FormBody getFormBody() {
        String base64Image = convert(photoPath);
        FormBody.Builder formBodyBuilder = new FormBody.Builder();
        formBodyBuilder.add("apikey", apiKey);
        formBodyBuilder.add("base64Image", "data:image/jpeg;base64," + base64Image);
        return formBodyBuilder.build();
    }

    @Override
    protected void onPostExecute(String result) {
        try {
            callback.onOcrResult(result);
        } catch (NoNotificationManagerException e) {
            e.printStackTrace();
        }
    }
}
