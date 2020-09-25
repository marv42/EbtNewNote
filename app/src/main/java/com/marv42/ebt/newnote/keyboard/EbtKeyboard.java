/*
 Copyright (c) 2010 - 2020 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote.keyboard;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.inputmethodservice.Keyboard;
import android.view.inputmethod.EditorInfo;

import com.marv42.ebt.newnote.R;

import static android.view.inputmethod.EditorInfo.IME_ACTION_NEXT;
import static android.view.inputmethod.EditorInfo.IME_FLAG_NO_ENTER_ACTION;
import static android.view.inputmethod.EditorInfo.IME_MASK_ACTION;

public class EbtKeyboard extends Keyboard {
   private static final int UNICODE_LINE_FEED = 0xA;
   
   private Key enterKey;

   EbtKeyboard(Context context, int xmlLayoutResId) {
       super(context, xmlLayoutResId);
   }

   @Override
   protected Key createKeyFromXml(Resources res, Row parent, int x, int y, XmlResourceParser parser) {
      Key key = new Keyboard.Key(res, parent, x, y, parser);
      if (key.codes[0] == UNICODE_LINE_FEED)
         enterKey = key;
      return key;
   }

   void setImeOptions(Resources res, int options) {
      if (enterKey == null)
         return;
      enterKey.iconPreview = null;
      enterKey.icon = null;
      if ((options & (IME_MASK_ACTION | IME_FLAG_NO_ENTER_ACTION)) == IME_ACTION_NEXT)
         enterKey.label = res.getText(R.string.label_next_key);
      else
         enterKey.label = res.getText(R.string.label_done_key);
   }
}
