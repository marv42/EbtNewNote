/*
 Copyright (c) 2010 - 2022 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote.scanning;

import android.app.Activity;
import android.content.Intent;
import android.icu.util.Calendar;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.marv42.ebt.newnote.R;
import com.marv42.ebt.newnote.preferences.SettingsActivity;
import com.marv42.ebt.newnote.preferences.SharedPreferencesHandler;
import com.marv42.ebt.newnote.exceptions.NoPictureException;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import static android.Manifest.permission.CAMERA;
import static android.os.Environment.DIRECTORY_PICTURES;
import static android.provider.MediaStore.ACTION_IMAGE_CAPTURE;
import static android.provider.MediaStore.EXTRA_OUTPUT;
import static android.widget.Toast.LENGTH_LONG;
import static androidx.core.app.ActivityCompat.requestPermissions;
import static androidx.core.content.FileProvider.getUriForFile;
import static androidx.core.content.PermissionChecker.PERMISSION_GRANTED;
import static androidx.core.content.PermissionChecker.checkSelfPermission;
import static com.marv42.ebt.newnote.EbtNewNote.CAMERA_PERMISSION_REQUEST_CODE;
import static com.marv42.ebt.newnote.EbtNewNote.IMAGE_CAPTURE_REQUEST_CODE;
import static com.marv42.ebt.newnote.Utils.DAYS_IN_MS;
import static java.io.File.createTempFile;

public class CameraStarter {

    private static final int TIME_THRESHOLD_DELETE_OLD_PICS_MS = DAYS_IN_MS;
    private static final String TAG = CameraStarter.class.getSimpleName();
    private final Activity activity;

    public CameraStarter(Activity activity) {
        this.activity = activity;
    }

    public boolean canTakePhoto(boolean showNoOnlineOcrServiceKeyDialog) {
        if (activity == null)
            throw new IllegalStateException("No activity");
        Intent intent = new Intent(ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(activity.getPackageManager()) == null) {
            Toast.makeText(activity, R.string.no_camera_activity, LENGTH_LONG).show();
            return false;
        }
        if (checkSelfPermission(activity, CAMERA) != PERMISSION_GRANTED) {
            requestPermissions(activity, new String[]{CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
            return false;
        }
        if (showNoOnlineOcrServiceKeyDialog) {
            showDialogNoOcrServiceKey(activity);
            return false;
        }
        return true;
    }

    private void showDialogNoOcrServiceKey(Activity activity) {
        new AlertDialog.Builder(activity)
                .setTitle(R.string.ocr_no_service_key)
                .setMessage(activity.getString(R.string.settings_ocr_summary) + "." + activity.getString(R.string.get_ocr_key))
                .setPositiveButton(activity.getString(android.R.string.ok),
                        (dialog, which) -> {
                            activity.startActivity(new Intent(activity.getApplicationContext(),
                                    SettingsActivity.class));
                            dialog.dismiss();
                        })
                .show();
    }

    public void startCameraActivity(SharedPreferencesHandler sharedPreferencesHandler) throws NoPictureException {
        if (activity == null)
            throw new IllegalStateException("No activity");
        File photoFile = createImageFile();
        String photoPath = photoFile.getAbsolutePath();
        Uri photoUri = getUriForFile(activity, activity.getPackageName(), photoFile);
        sharedPreferencesHandler.set(R.string.pref_photo_path_key, photoPath);
        sharedPreferencesHandler.set(R.string.pref_photo_uri_key, photoUri.toString());
        Intent intent = new Intent(ACTION_IMAGE_CAPTURE);
        intent.putExtra(EXTRA_OUTPUT, photoUri);
        activity.startActivityForResult(intent, IMAGE_CAPTURE_REQUEST_CODE);
    }

    private File createImageFile() throws NoPictureException {
        if (activity == null)
            throw new IllegalStateException("No activity");
        File tempFolder = activity.getExternalFilesDir(DIRECTORY_PICTURES);
        if (tempFolder == null)
            throw new NoPictureException("R.string.error_creating_file: Error getting picture directory");
        deleteOldPhotos(tempFolder);
        return createTempPhotoFile(tempFolder);
    }

    private void deleteOldPhotos(@NonNull File tempFolder) {
        for (File file : Objects.requireNonNull(tempFolder.listFiles()))
            if (fileIsOld(file))
                if (!file.delete())
                    Log.w(TAG, "deleteOldPhotos: Could not delete file " + file.getAbsolutePath());
    }

    private boolean fileIsOld(File file) {
        return Calendar.getInstance().getTimeInMillis() - file.lastModified() > TIME_THRESHOLD_DELETE_OLD_PICS_MS;
    }

    @NotNull
    private File createTempPhotoFile(File tempFolder) throws NoPictureException {
        try {
            return createTempFile("bill_", ".jpg", tempFolder);
        } catch (IOException e) {
            throw new NoPictureException("R.string.error_creating_file: " + e.getMessage());
        }
    }
}
