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



public class SubmissionResult
{
   private final NoteData mNoteData;
   private final boolean  mSuccessful;
   private final String   mReason;
   private final int      mBillId;
   private final boolean  mHit;
   
   
   
   public SubmissionResult(final NoteData noteData,
                           final boolean  successful,
                           final String   reason)
   {
      this(noteData, successful, reason, -1, false);
   }
   
   
   
   public SubmissionResult(final NoteData noteData,
                           final boolean  successful,
                           final String   reason,
                           final int      billId)
   {
      this(noteData, successful, reason, billId, false);
   }
   
   
   
   public SubmissionResult(final NoteData noteData,
                           final boolean  successful,
                           final String   reason,
                           final int      billId,
                           final boolean  hit)
   {
      mNoteData   = noteData;
      mSuccessful = successful;
      mReason     = reason;
      mBillId     = billId;
      mHit        = hit;
   }
   
   
   
   public NoteData
   getNoteData()
   {
      return mNoteData;
   }
   
   
   
   public boolean
   wasSuccessful()
   {
      return mSuccessful;
   }
   
   
   
   public String
   getReason()
   {
      return mReason;
   }
   
   
   
   public int
   getBillId()
   {
      return mBillId;
   }
   
   
   
   public boolean
   wasHit()
   {
      return mHit;
   }
}
