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

package com.marv42.ebt.newnote.scanning;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.text.TextUtils;
import android.util.Log;

import com.marv42.ebt.newnote.EbtNewNote;



public class TextProcessor
{
   public static String
   getOcrResult(String s)
   {
      String serialNumber = extractEssentials(s);
      Log.d(EbtNewNote.LOG_TARGET, "serialNumber: " + serialNumber);
      
      return correct(serialNumber);
   }
   
   
   
   private static String
   extractEssentials(String s)
   {
      // accept a lot because we may correct it later
      // -> accept *everything* and let the user decide!
//      Pattern serialNumberPattern = Pattern.compile("[\\S]{12,14}");
//      Pattern shortCodePattern    = Pattern.compile("[\\S]{6,8}");
//      
//      Matcher serialNumberMatcher = serialNumberPattern.matcher(s);
//      Matcher shortCodeMatcher    =    shortCodePattern.matcher(s);
//      
//      if (serialNumberMatcher.matches())
//         return serialNumberMatcher.group();
//      if (shortCodeMatcher.matches())
//         return shortCodeMatcher.group();
      
      // reduce white spaces
      s = s.replaceAll("\\s+", "");
      if (TextUtils.isEmpty(s.trim()))
         s = "<empty>";
      
      return s;
   }
   
   
   
   private static String
   correct(String s)
   {
      // when we don't know whether the result must be a letter or a digit
      Map<String, String> char2unambiguous = new HashMap<String, String>();
      // char2unambiguous.put("$", "S");
      // char2unambiguous.put("$", "5");
      char2unambiguous.put("W", "U");
      char2unambiguous.put("K", "X");
      char2unambiguous.put("%", "X");
      char2unambiguous.put("@", "0");
      char2unambiguous.put("i", "1");
      char2unambiguous.put("I", "1");
      char2unambiguous.put("t", "1");
      char2unambiguous.put("#", "4");
      char2unambiguous.put("s", "5");
      char2unambiguous.put("*", "5");
      char2unambiguous.put("?", "7");
      char2unambiguous.put("f", "7");
      char2unambiguous.put("a", "8");
      char2unambiguous.put("&", "8");
      
      // when we know the result must be a letter
      Map<String, String> char2letter = new HashMap<String, String>();
      char2letter.put("4", "N");
      char2letter.put("0", "O");
      char2letter.put("W", "U");
      char2letter.put("K", "X");
      char2letter.put("%", "X");
      
      // when we know the result must be a digit
      Map<String, String> char2digit = new HashMap<String, String>();
      char2digit.put("D", "0");
      char2digit.put("O", "0");
      char2digit.put("o", "0");
      char2digit.put("@", "0");
      char2digit.put("i", "1");
      char2digit.put("I", "1");
      char2digit.put("t", "1");
      char2digit.put("Z", "2");
      //char2digit.put("s", "3");
      //char2digit.put("s", "5");
      char2digit.put("S", "5");
      char2digit.put("$", "5");
      char2digit.put("*", "5");
      char2digit.put("?", "7");
      char2digit.put("f", "7");
      char2digit.put("a", "8");
      char2digit.put("A", "8");
      char2digit.put("B", "8");
      
      List<Integer> letterIndices = new ArrayList<Integer>();
      
      letterIndices.add(0);
      
      for (int i = 0; i < s.length(); ++i)
      {
         s = s.substring(0, i) + correctCharacter(s.charAt(i), char2unambiguous) + s.substring(i+1);
         
         if (letterIndices.contains(i))
         {
            if (! s.substring(i, i+1).matches("\\w"))
               s = s.substring(0, i) + correctCharacter(s.charAt(i), char2letter) + s.substring(i+1);
         }
         else
            if (! s.substring(i, i+1).matches("\\d"))
               s = s.substring(0, i) + correctCharacter(s.charAt(i), char2digit) + s.substring(i+1);
      }
      
      Pattern pattern = Pattern.compile("\\w\\d{11}", Pattern.CASE_INSENSITIVE);
      Matcher matcher = pattern.matcher(s);
      if (matcher.find())
      {
         s = s.substring(matcher.start(), matcher.end());
         Log.d(EbtNewNote.LOG_TARGET, "cutting out " + s);
      }
      
      return s;
   }
   
   
   
   private static String
   correctCharacter(char c, Map<String, String> char2char)
   {
      String sC = Character.toString(c);
      if (char2char.containsKey(sC))
      {
         String replacement = char2char.get(sC);
         Log.d(EbtNewNote.LOG_TARGET, "replacing " + c + " with " + replacement);
         return replacement;
      }
      
      Log.d(EbtNewNote.LOG_TARGET, "didn't replace " + c);
      return sC;
   }
}
