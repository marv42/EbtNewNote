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

package com.marv42.ebt.newnote;

import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.widget.ImageView;
import android.widget.TextView;

public class About implements OnMenuItemClickListener {
    private final Context mContext;

    About(Context context) {
        mContext = context;
    }

    public boolean onMenuItemClick(MenuItem item) {
        final Dialog dialog = new Dialog(mContext);
        dialog.setContentView(R.layout.about);
        dialog.setTitle(R.string.app_name);

        String versionNumber = "0.0";
        try {
            versionNumber = mContext.getPackageManager().getPackageInfo(
                    mContext.getPackageName(), 0).versionName;
        } catch (NameNotFoundException ignored) {
        }

        ((TextView) dialog.findViewById(R.id.about_version)).setText(
                String.format(mContext.getString(R.string.version), versionNumber));
        ((TextView) dialog.findViewById(R.id.about_text)).setText(
                String.format("%1$s %2$s", mContext.getString(R.string.app_name),
                        mContext.getString(R.string.about_text)));
        dialog.show();
        return true;
    }
}
