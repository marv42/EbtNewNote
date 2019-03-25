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

import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;

class SubmissionResult {
    final NoteData mNoteData;
    final boolean mSuccessful;
    final String mReason;
    final int mBillId;
    final boolean mHit;
    final Date mTime;

    SubmissionResult(final NoteData noteData, final boolean successful, final String reason) {
        this(noteData, successful, reason, -1, false);
    }

    SubmissionResult(final NoteData noteData, final boolean successful, final String reason, final int billId) {
        this(noteData, successful, reason, billId, false);
    }

    SubmissionResult(final NoteData noteData, final boolean successful, final String reason,
                     final int billId, final boolean hit) {
        mNoteData = noteData;
        mSuccessful = successful;
        mReason = reason;
        mBillId = billId;
        mHit = hit;
        mTime = Calendar.getInstance().getTime();
    }

    static class TimeComparator implements Comparator<SubmissionResult> {
        private boolean mConsiderSuccessful;

        TimeComparator(boolean considerSuccessful) {
            mConsiderSuccessful = considerSuccessful;
        }

        @Override
        public int compare(SubmissionResult sr1, SubmissionResult sr2) {
            if (mConsiderSuccessful && (sr1.mSuccessful ^ sr2.mSuccessful))
                return sr1.mSuccessful ? 1 : -1;
            if (sr1.mTime == sr2.mTime)
                return 0;
            return sr1.mTime.after(sr2.mTime) ? 1 : -1;
        }
    }
}
