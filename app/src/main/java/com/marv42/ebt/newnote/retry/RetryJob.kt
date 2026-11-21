/*
 Copyright (c) 2010 - 2026 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */
package com.marv42.ebt.newnote.retry

import com.marv42.ebt.newnote.data.NoteData

class RetryJob {

    var id: String = ""
    var kind: String = KIND_SUBMISSION
    var noteData: NoteData? = null
    var photoPath: String? = null
    var photoUri: String? = null
    var attempts: Int = 0
    var nextAttemptAt: Long = 0L

    companion object {
        const val KIND_SUBMISSION = "submission"
        const val KIND_OCR = "ocr"
    }
}
