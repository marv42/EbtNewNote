/*
 Copyright (c) 2010 - 2026 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */
package com.marv42.ebt.newnote.scanning

import android.content.ContentResolver
import android.net.Uri

class OcrHandlerOnline(callback: Callback, photoPath: String, photoUri: Uri?,
                       contentResolver: ContentResolver, private val onlineOcr: OnlineOcr) :
    OcrHandlerTask(callback, photoPath, photoUri, contentResolver) {

    override fun scan(): OcrResult =
        onlineOcr.scan(photoPath, photoUri, contentResolver)
}
