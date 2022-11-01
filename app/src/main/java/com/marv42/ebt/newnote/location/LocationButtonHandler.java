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
import static android.widget.Toast.LENGTH_SHORT;
import static androidx.core.content.PermissionChecker.PERMISSION_GRANTED;
import static androidx.core.content.PermissionChecker.checkSelfPermission;
import static com.marv42.ebt.newnote.EbtNewNote.LOCATION_PERMISSION_REQUEST_CODE;

import java.util.Objects;

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
        if (!isLocationEnabled()) {
            Toast.makeText(activity, R.string.location_not_enabled, LENGTH_SHORT).show();
            Toast.makeText(activity, R.string.location_please_enable, LENGTH_LONG).show();
            app.startLocationProviderChangedReceiver();
            return;
        }
        final int coarseLocationPermission = checkSelfPermission(app, ACCESS_COARSE_LOCATION);
        if (coarseLocationPermission != PERMISSION_GRANTED) {
            requestLocationPermissions();
            return;
        }
        final int fineLocationPermission = checkSelfPermission(app, ACCESS_FINE_LOCATION);
        if (fineLocationPermission != PERMISSION_GRANTED)
            Toast.makeText(activity, R.string.location_no_gps, LENGTH_LONG).show();
        app.startLocationTask();
    }

    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
        return locationManager == null || locationManager.isLocationEnabled();
    }

    private void requestLocationPermissions() {
        ActivityCompat.requestPermissions(Objects.requireNonNull(activity),
                new String[]{ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION},
                LOCATION_PERMISSION_REQUEST_CODE);
    }
}
