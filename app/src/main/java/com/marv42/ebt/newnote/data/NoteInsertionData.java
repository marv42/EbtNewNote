/*
 Copyright (c) 2010 - 2020 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote.data;

public class NoteInsertionData {
    public final int billId;
    public final int status;

    public NoteInsertionData(final int billId, final int status) {
        this.billId = billId;
        this.status = status;
    }
}
