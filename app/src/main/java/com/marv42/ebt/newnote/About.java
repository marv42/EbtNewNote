/*
 Copyright (c) 2010 - 2022 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote;

import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.widget.TextView;

public class About implements OnMenuItemClickListener {

    private final Context context;

    About(Context context) {
        this.context = context;
    }

    public boolean onMenuItemClick(MenuItem item) {
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

        ((TextView) dialog.findViewById(R.id.about_version)).setText(
                String.format(context.getString(R.string.version), versionNumber));
        ((TextView) dialog.findViewById(R.id.about_text)).setText(
                String.format("'%1$s' %2$s", context.getString(R.string.app_name),
                        context.getString(R.string.about_text,
                                context.getString(R.string.www_ebt),
                                context.getString(R.string.developer_email),
                                context.getString(R.string.contributors))));
        dialog.show();
        return true;
    }
}
