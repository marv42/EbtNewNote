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

import android.content.Context;

class LocationValues {
   private final String mCountry;
   private final String mCity;
   private final String mPostalCode;
   
   private final boolean mOverwrite;

   LocationValues() {
      this("", "", "", false, null);
   }

   LocationValues(final String country, final String city, final String postalCode) {
      this(country, city, postalCode, false, null);
   }

   LocationValues(final String country, final String city, final String postalCode, final boolean overwrite) {
      this(country, city, postalCode, overwrite, null);
   }

   LocationValues(final String country, final String city, final String postalCode,
                  final boolean overwrite, final Context context) {
      mCountry        = country;
      mCity           = city;
      mPostalCode     = postalCode;
      mOverwrite      = overwrite;
      if (context != null)
         ((ThisApp) context.getApplicationContext())
            .setLocationValues(new LocationValues(country, city, postalCode, overwrite));
   }

   String getCountry() {
      return mCountry;
   }
   
   String getCity() {
      return mCity;
   }
   
   String getPostalCode() {
      return mPostalCode;
   }

   boolean canOverwrite() {
      return mOverwrite;
   }
}
