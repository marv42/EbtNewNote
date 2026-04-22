/*
 Copyright (c) 2010 - 2026 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote;

import android.content.Context;
import android.text.TextUtils;

import com.marv42.ebt.newnote.data.NoteData;

import java.util.Comparator;

import static androidx.core.content.ContextCompat.getColor;
import static com.marv42.ebt.newnote.Utils.getColoredString;

public class SubmissionResult {

    // If we rename these, we have to change the values in shared preferences
    final NoteData mNoteData;
    final String mReason;
    final boolean mSuccessful;
    final int mBillId;
    boolean mRemovable;

    SubmissionResult(final NoteData noteData, final String reason) {
        this(noteData, reason, false, -1);
    }

    SubmissionResult(final NoteData noteData, final String reason, final boolean successful, final int billId) {
        mNoteData = noteData;
        mReason = reason;
        mSuccessful = successful;
        mBillId = billId;
        mRemovable = true;
    }

    boolean isAHit(Context context) {
        if (context == null)
            throw new IllegalStateException("No activity");
        return TextUtils.equals(mReason, context.getString(R.string.got_hit));
    }

    boolean isSuccessful() {
        return mSuccessful;
    }

    String getResult(Context context) {
        if (context == null)
            throw new IllegalStateException("No activity");
        return isSuccessful() ?
                getColoredString(isAHit(context) ?
                        context.getString(R.string.hit) : context.getString(R.string.successful),
                        getColor(context, R.color.success)) :
                getColoredString(context.getString(R.string.failed),
                        getColor(context, R.color.failed));
    }

    static class SubmissionComparator implements Comparator<SubmissionResult> {
        @Override
        public int compare(SubmissionResult sr1, SubmissionResult sr2) {
            if (sr1 == null && sr2 == null)
                return 0;
            if (sr1 == null)
                return -1;
            if (sr2 == null)
                return 1;
            return sr1.mBillId - sr2.mBillId;
        }
    }
}
