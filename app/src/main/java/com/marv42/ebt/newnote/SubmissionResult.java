/*
 Copyright (c) 2010 - 2020 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote;

import android.content.Context;

import com.marv42.ebt.newnote.data.NoteData;

import java.util.Comparator;

import static androidx.core.content.ContextCompat.getColor;
import static com.marv42.ebt.newnote.Utils.getColoredString;

class SubmissionResult {
    // TODO If we rename these, we have to change the values in shared preferences
    final NoteData mNoteData;
    final String mReason;
    final int mBillId;

    SubmissionResult(final NoteData noteData, final String reason) {
        this(noteData, reason, -1);
    }

    SubmissionResult(final NoteData noteData, final String reason, final int billId) {
        mNoteData = noteData;
        mReason = reason;
        mBillId = billId;
    }

    boolean isAHit(Context context) {
        return mReason.equals(context.getString(R.string.got_hit));
    }

    boolean isSuccessful(Context context) {
        return mReason.equals(context.getString(R.string.has_been_entered)) ||
                mReason.equals(context.getString(R.string.got_hit));
    }

    String getResult(Context context) {
        return isSuccessful(context) ?
                getColoredString(isAHit(context) ?
                        context.getString(R.string.hit) : context.getString(R.string.successful),
                        getColor(context, R.color.success)) :
                getColoredString(context.getString(R.string.failed),
                        getColor(context, R.color.failed));
    }

    static class SubmissionComparator implements Comparator<SubmissionResult> {
        @Override
        public int compare(SubmissionResult sr1, SubmissionResult sr2) {
            return sr1.mBillId - sr2.mBillId;
        }
    }
}
