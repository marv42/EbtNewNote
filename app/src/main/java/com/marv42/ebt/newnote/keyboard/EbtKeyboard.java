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

package com.marv42.ebt.newnote.keyboard;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.inputmethodservice.Keyboard;
import android.view.inputmethod.EditorInfo;

import com.marv42.ebt.newnote.R;

public class EbtKeyboard extends Keyboard {
   private static final int UNICODE_LINE_FEED = 0xA;
   
   private Key mEnterKey;

  EbtKeyboard(Context context, int xmlLayoutResId) {
       super(context, xmlLayoutResId);
   }

   public EbtKeyboard(Context context, int layoutTemplateResId,
      CharSequence characters, int columns, int horizontalPadding) {
       super(context, layoutTemplateResId, characters, columns, horizontalPadding);
   }

   @Override
   protected Key createKeyFromXml(Resources res, Row parent, int x, int y, XmlResourceParser parser) {
      Key key = new Keyboard.Key(res, parent, x, y, parser);
      
      if (key.codes[0] == UNICODE_LINE_FEED)
         mEnterKey = key;
      
      return key;
   }

   void setImeOptions(Resources res, int options) {
      if (mEnterKey == null)
         return;
      
      mEnterKey.iconPreview = null;
      mEnterKey.icon = null;

      if ((options & (EditorInfo.IME_MASK_ACTION | EditorInfo.IME_FLAG_NO_ENTER_ACTION))
              == EditorInfo.IME_ACTION_NEXT) {
         mEnterKey.label = res.getText(R.string.label_next_key);
      } else {
         mEnterKey.label = res.getText(R.string.label_done_key);
      }
   }
}
