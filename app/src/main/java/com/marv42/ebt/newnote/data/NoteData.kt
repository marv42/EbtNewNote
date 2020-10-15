/*
 Copyright (c) 2010 - 2020 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */
package com.marv42.ebt.newnote.data

class NoteData(country: String, city: String, postalCode: String,
               denomination: String, shortCode: String, serialNumber: String, comment: String) {
    // TODO If we rename these, we have to change the values in shared preferences
    @JvmField
    val mCountry: String = country

    @JvmField
    val mCity: String = city

    @JvmField
    val mPostalCode: String = postalCode

    @JvmField
    val mDenomination: String = denomination

    @JvmField
    val mShortCode: String = shortCode

    @JvmField
    val mSerialNumber: String = serialNumber

    @JvmField
    val mComment: String = comment
}