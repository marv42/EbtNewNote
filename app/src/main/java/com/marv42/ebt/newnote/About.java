/*
 Copyright (c) 2010 - 2024 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote;

import android.app.Dialog;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.widget.TextView;

import androidx.annotation.NonNull;

public class About implements OnMenuItemClickListener {

    private final Context context;

    About(Context context) {
        this.context = context;
    }

    public boolean onMenuItemClick(@NonNull MenuItem item) {
        final Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.about);
        dialog.setTitle(R.string.app_name);
        String versionNumber;
        final PackageManager packageManager = context.getPackageManager();
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                versionNumber = packageManager.getPackageInfo(
                        context.getPackageName(), PackageManager.PackageInfoFlags.of(0)).versionName;
            else
                versionNumber = packageManager.getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (NameNotFoundException ex) {
            versionNumber = "0.0.0";
        }
        boolean isDebug = (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        if (isDebug) {
            versionNumber += "-DEBUG";
        }
        final TextView versionTextView = dialog.findViewById(R.id.about_version);
        versionTextView.setText(String.format(context.getString(R.string.about_version), versionNumber));
        final TextView descriptionTextView = dialog.findViewById(R.id.about_text);
        descriptionTextView.setText(context.getString(R.string.about_text));
        dialog.show();
        return true;
    }
}
