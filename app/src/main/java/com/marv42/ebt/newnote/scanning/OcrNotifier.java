/*
 Copyright (c) 2010 - 2022 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote.scanning;

import android.app.Activity;

import androidx.appcompat.app.AlertDialog;

import com.marv42.ebt.newnote.R;

public class OcrNotifier {

    public static void showDialog(Activity activity, String message) {
        new AlertDialog.Builder(activity)
                .setTitle(R.string.ocr_dialog_title)
                .setMessage(message)
                .show();
    }
}
