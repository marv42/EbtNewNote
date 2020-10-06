/*
 Copyright (c) 2010 - 2020 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote.scanning;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.exifinterface.media.ExifInterface;

import com.marv42.ebt.newnote.HttpCaller;
import com.marv42.ebt.newnote.SubmitFragment;
import com.marv42.ebt.newnote.exceptions.CallResponseException;
import com.marv42.ebt.newnote.exceptions.HttpCallException;
import com.marv42.ebt.newnote.exceptions.NoNotificationManagerException;
import com.marv42.ebt.newnote.exceptions.NoPictureException;
import com.marv42.ebt.newnote.exceptions.OcrException;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import okhttp3.FormBody;
import okhttp3.Request;

import static android.provider.MediaStore.MediaColumns.ORIENTATION;
import static androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL;
import static androidx.exifinterface.media.ExifInterface.ORIENTATION_UNDEFINED;
import static androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION;
import static com.marv42.ebt.newnote.exceptions.ErrorMessage.ERROR;

public class OcrHandler extends AsyncTask<Void, Void, String> {
    private static final String OCR_HOST = "api.ocr.space";
    private static final String OCR_URL = "https://" + OCR_HOST + "/parse/image";
    private static final String WRONG_OCR_KEY_MESSAGE = "R.string.http_error " + OCR_HOST + ", R.string.response_code: 403";
    private Callback callback;
    private String photoPath;
    private Uri photoUri;
    private ContentResolver contentResolver;
    private String apiKey;
    public OcrHandler(@NonNull SubmitFragment fragment, String photoPath, Uri photoUri,
                      @NonNull ContentResolver contentResolver, @NonNull String apiKey) {
        callback = fragment;
        this.photoPath = photoPath;
        this.photoUri = photoUri;
        this.contentResolver = contentResolver;
        this.apiKey = apiKey;
    }

    @Override
    protected String doInBackground(Void... voids) {
        try {
            return getResult();
        } catch (HttpCallException e) {
            return getHttpCallErrorMessage(e);
        } catch (CallResponseException | NoPictureException e) {
            return ERROR + e.getMessage();
        } catch (OcrException e) {
            return ERROR + "R.string.ocr_error: " + e.getMessage();
        }
    }

    @NotNull
    private String getHttpCallErrorMessage(HttpCallException e) {
        String errorMessage = e.getMessage();
        if (errorMessage == null)
            errorMessage = "R.string.http_error";
        if (errorMessage.equals(WRONG_OCR_KEY_MESSAGE))
            errorMessage += ".\n\nR.string.ocr_wrong_key.";
        if (!errorMessage.startsWith("R.string.http_error"))
            errorMessage = "R.string.http_error: " + errorMessage;
        return ERROR + errorMessage;
    }

    private String getResult() throws HttpCallException, CallResponseException, OcrException, NoPictureException {
        FormBody formBody = getFormBody();
        Request request = new Request.Builder().url(OCR_URL).post(formBody).build();
        String body = new HttpCaller().call(request);
        return TextProcessor.getOcrResult(body);
    }

    @NotNull
    private FormBody getFormBody() throws NoPictureException {
        String base64Image = new PictureConverter(photoPath, getOrientation()).convert();
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

    private int getOrientation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            return getOrientationFromMediaStore();
        else
            return getOrientationFromExif();
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private int getOrientationFromMediaStore() {
        if (photoUri != null)
            return getOrientationFromCursor();
        return ORIENTATION_UNDEFINED;
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private int getOrientationFromCursor() {
        int orientation = ORIENTATION_UNDEFINED;
        String[] columns = new String[]{ORIENTATION};
        Cursor cursor = contentResolver.query(photoUri, columns, null, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                final int columnIndex = cursor.getColumnIndex(columns[0]);
                if (columnIndex >= 0)
                    orientation = cursor.getInt(columnIndex);
            }
            cursor.close();
        }
        return orientation;
    }

    private int getOrientationFromExif() {
        // this is always 0 :-(
        try {
            ExifInterface exif = new ExifInterface(photoPath);
            return exif.getAttributeInt(TAG_ORIENTATION, ORIENTATION_NORMAL);
        } catch (IOException e) {
            return ORIENTATION_UNDEFINED;
        }
    }

    public interface Callback {
        void onOcrResult(String result) throws NoNotificationManagerException;
    }
}
