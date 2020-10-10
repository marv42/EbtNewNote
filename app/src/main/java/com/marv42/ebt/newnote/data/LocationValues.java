/*
 Copyright (c) 2010 - 2020 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote.data;

public class LocationValues {
    public final String country;
    public final String city;
    public final String postalCode;

    public LocationValues(final String country, final String city, final String postalCode) {
        this.country = country;
        this.city = city;
        this.postalCode = postalCode;
    }

    public LocationValues() {
        this.country = "";
        this.city = "";
        this.postalCode = "";
    }
}