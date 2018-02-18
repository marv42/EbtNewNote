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



public class LocationValues
{
   private final String mCountry;
   private final String mCity;
   private final String mPostalCode;
   
   private final boolean mOverwrite;
   
   
   
   public LocationValues()
   {
      this("", "", "", false, null);
   }
   
   

   public LocationValues(final String  country,
                         final String  city,
                         final String  postalCode)
   {
      this(country, city, postalCode, false, null);
   }
   
   
   
   public LocationValues(final String  country,
                         final String  city,
                         final String  postalCode,
                         final boolean overwrite)
   {
      this(country, city, postalCode, overwrite, null);
   }
   
   
   
   public LocationValues(final String  country,
                         final String  city,
                         final String  postalCode,
                         final boolean overwrite,
                         final Context context)
   {
      mCountry        = country;
      mCity           = city;
      mPostalCode     = postalCode;
      mOverwrite      = overwrite;
      
      if (context != null)
         ((ThisApp) context.getApplicationContext())
            .setLocationValues(new LocationValues(country,
                                                  city,
                                                  postalCode,
                                                  overwrite));
   }
   
   
   
   public String
   getCountry()
   {
      return mCountry;
   }
   
   
   
   public String
   getCity()
   {
      return mCity;
   }
   
   
   
   public String
   getPostalCode()
   {
      return mPostalCode;
   }
   
   
   
   public boolean
   canOverwrite()
   {
      return mOverwrite;
   }
}
