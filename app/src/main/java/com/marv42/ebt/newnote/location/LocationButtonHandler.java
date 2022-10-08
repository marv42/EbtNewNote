/*
 Copyright (c) 2010 - 2022 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote.location;

import android.app.Activity;
import android.content.Context;
import android.location.LocationManager;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import com.marv42.ebt.newnote.R;
import com.marv42.ebt.newnote.ThisApp;

import javax.inject.Inject;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.widget.Toast.LENGTH_LONG;
import static androidx.core.content.PermissionChecker.PERMISSION_GRANTED;
import static androidx.core.content.PermissionChecker.checkSelfPermission;
import static com.marv42.ebt.newnote.EbtNewNote.LOCATION_PERMISSION_REQUEST_CODE;

public class LocationButtonHandler {

    private final ThisApp app;
    private final Activity activity;

    @Inject
    public LocationButtonHandler(ThisApp app, Activity activity) {
        this.app = app;
        this.activity = activity;
    }

    public void clicked() {
        if (activity == null)
            throw new IllegalStateException("No activity");
        if (!locationPermissionsGranted())
            return;
        if (!locationIsEnabled())
            return;
        if (checkSelfPermission(app, ACCESS_FINE_LOCATION) != PERMISSION_GRANTED)
            Toast.makeText(activity, R.string.location_no_gps, LENGTH_LONG).show();
        app.startLocationTask();
    }

    private boolean locationPermissionsGranted() {
        if (activity != null &&
                checkSelfPermission(app, ACCESS_COARSE_LOCATION) != PERMISSION_GRANTED &&
                checkSelfPermission(app, ACCESS_FINE_LOCATION) != PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity,
                    new String[]{ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return false;
        }
        return true;
    }

    private boolean locationIsEnabled() {
        LocationManager locationManager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager != null && !locationManager.isLocationEnabled()) {
            Toast.makeText(activity, R.string.location_not_enabled, LENGTH_LONG).show();
            app.startLocationProviderChangedReceiver();
            return false;
        }
        return true;
    }
}
