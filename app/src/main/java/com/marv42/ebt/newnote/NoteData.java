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



public class NoteData
{
   private final String mCountry;
   private final String mCity;
   private final String mPostalCode;
   private final String mDenomination;
   private final String mShortCode;
   private final String mSerialNumber;
   private final String mComment;
   
   
   
   public NoteData(final String country,
                   final String city,
                   final String postalCode,
                   final String denomination,
                   final String shortCode,
                   final String serialNumber,
                   final String comment)
   {
      mCountry      = country;
      mCity         = city;
      mPostalCode   = postalCode;
      mDenomination = denomination;
      mShortCode    = shortCode;
      mSerialNumber = serialNumber;
      mComment      = comment;
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
   
   
   
   public String
   getDenomination()
   {
      return mDenomination;
   }
   
   
   
   public String
   getShortCode()
   {
      return mShortCode;
   }
   
   
   
   public String
   getSerialNumber()
   {
      return mSerialNumber;
   }
   
   
   
   public String
   getComment()
   {
      return mComment;
   }
}
