/*
 Copyright (c) 2010 - 2020 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote.data;

public class NoteData {
    // TODO If we rename these, we have to change the values in shared preferences
    public final String mCountry;
    public final String mCity;
    public final String mPostalCode;
    public final String mDenomination;
    public final String mShortCode;
    public final String mSerialNumber;
    public final String mComment;

    public NoteData(final String country, final String city, final String postalCode,
                    final String denomination, final String shortCode, final String serialNumber,
                    final String comment) {
        mCountry = country;
        mCity = city;
        mPostalCode = postalCode;
        mDenomination = denomination;
        mShortCode = shortCode;
        mSerialNumber = serialNumber;
        mComment = comment;
    }
}
