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

class LocationValues {
    final String mCountry;
    final String mCity;
    final String mPostalCode;

    LocationValues(final String country, final String city, final String postalCode) {
        mCountry = country;
        mCity = city;
        mPostalCode = postalCode;
    }
}
