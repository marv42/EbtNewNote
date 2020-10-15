/*
 Copyright (c) 2010 - 2020 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */
package com.marv42.ebt.newnote.data

class LocationValues {
    @JvmField
    val country: String
    @JvmField
    val city: String
    @JvmField
    val postalCode: String

    constructor(country: String, city: String, postalCode: String) {
        this.country = country
        this.city = city
        this.postalCode = postalCode
    }

    constructor() {
        country = ""
        city = ""
        postalCode = ""
    }
}