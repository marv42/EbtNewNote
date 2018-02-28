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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;

import static android.graphics.Bitmap.CompressFormat.JPEG;
import static android.util.Base64.NO_WRAP;

public class PictureConverter {
    public static String getBase64(Context context, String path) {
        try {
            File file = new File(path);
            Uri uri = Uri.fromFile(file);
            final InputStream inputStream = context.getContentResolver().openInputStream(uri);
            final Bitmap selectedImage = BitmapFactory.decodeStream(inputStream);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            selectedImage.compress(JPEG, 100, byteArrayOutputStream);
            byte[] b = byteArrayOutputStream.toByteArray();
            return Base64.encodeToString(b, NO_WRAP);
        } catch (FileNotFoundException e) {
            // this should not happen
        }
        return null;
    }
}
