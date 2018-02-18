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

import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

import com.marv42.ebt.newnote.R;



public class SoftKeyboard extends InputMethodService implements KeyboardView.OnKeyboardActionListener
{
   private KeyboardView mInputView;
   
   private StringBuilder mComposing = new StringBuilder();
   private int mLastDisplayWidth;
   
   private EbtKeyboard mKeyboard;
   
   
   
   @Override
   public void
   onCreate()
   {
      super.onCreate();
   }
   
   
   
   @Override
   public void
   onInitializeInterface()
   {
      if (mKeyboard != null)
      {
         int displayWidth = getMaxWidth();
         if (displayWidth == mLastDisplayWidth) return;
         mLastDisplayWidth = displayWidth;
      }
      mKeyboard = new EbtKeyboard(this, R.xml.keyboard);
   }
   
   
   
   @Override
   public View
   onCreateInputView()
   {
      mInputView = (KeyboardView)getLayoutInflater().inflate(R.layout.input, null);
      mInputView.setOnKeyboardActionListener(this);
      mInputView.setKeyboard(mKeyboard);
      return mInputView;
   }
   
   
   
   @Override
   public View
   onCreateCandidatesView()
   {
       return null;
   }
   
   
   
   @Override
   public void
   onStartInput(EditorInfo attribute, boolean restarting)
   {
      super.onStartInput(attribute, restarting);
      
      mComposing.setLength(0);
      
      mKeyboard.setImeOptions(getResources(), attribute.imeOptions);
   }
   
   
   
   @Override
   public void
   onFinishInput()
   {
      super.onFinishInput();
      
      mComposing.setLength(0);
      
      if (mInputView != null)
          mInputView.closing();
   }
   
   
   
   @Override
   public void
   onStartInputView(EditorInfo attribute, boolean restarting)
   {
      super.onStartInputView(attribute, restarting);
      mInputView.setKeyboard(mKeyboard);
      mInputView.closing();
   }
   
   
   
   @Override
   public void
   onUpdateSelection(int oldSelStart, int oldSelEnd,
                     int newSelStart, int newSelEnd,
                     int candidatesStart, int candidatesEnd)
   {
      super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd,
            candidatesStart, candidatesEnd);
      if (mComposing.length() > 0 && (newSelStart != candidatesEnd
         || newSelEnd != candidatesEnd))
      {
         mComposing.setLength(0);
         InputConnection ic = getCurrentInputConnection();
         if (ic != null)
            ic.finishComposingText();
      }
   }
   
   
   
   @Override
   public boolean
   onKeyDown(int keyCode, KeyEvent event)
   {
      switch (keyCode)
      {
          case KeyEvent.KEYCODE_BACK:
              if (event.getRepeatCount() == 0 && mInputView != null)
                  if (mInputView.handleBack())
                      return true;
              break;
              
          case KeyEvent.KEYCODE_DEL:
              if (mComposing.length() > 0)
              {
                  onKey(Keyboard.KEYCODE_DELETE, null);
                  return true;
              }
              break;
              
          case KeyEvent.KEYCODE_ENTER:
              return false;
      }
      
      return super.onKeyDown(keyCode, event);
   }
   
   
   
   private void
   commitTyped(InputConnection inputConnection)
   {
      if (mComposing.length() > 0)
      {
          inputConnection.commitText(mComposing, mComposing.length());
          mComposing.setLength(0);
      }
   }
   
   
   
   private void
   keyDownUp(int keyEventCode)
   {
      getCurrentInputConnection().sendKeyEvent(
           new KeyEvent(KeyEvent.ACTION_DOWN, keyEventCode));
      getCurrentInputConnection().sendKeyEvent(
           new KeyEvent(KeyEvent.ACTION_UP, keyEventCode));
   }
   
   
   
   public void
   onKey(int primaryCode, int[] keyCodes)
   {
      if (primaryCode == '\n')
      {
         if (mComposing.length() > 0)
             commitTyped(getCurrentInputConnection());
         keyDownUp(KeyEvent.KEYCODE_ENTER);
     }
     else if (primaryCode == Keyboard.KEYCODE_DELETE)
         handleBackspace();
     else if (primaryCode == Keyboard.KEYCODE_DONE)
         {handleClose();return;}
     else
         handleCharacter(primaryCode, keyCodes);
   }
   
   
   
   public void
   onText(CharSequence text)
   {
      InputConnection ic = getCurrentInputConnection();
      if (ic == null) return;
      ic.beginBatchEdit();
      if (mComposing.length() > 0)
          commitTyped(ic);
      ic.commitText(text, 0);
      ic.endBatchEdit();
   }
   
   
   
   private void
   handleBackspace()
   {
      final int length = mComposing.length();
      if (length > 1)
      {
          mComposing.delete(length - 1, length);
          getCurrentInputConnection().setComposingText(mComposing, 1);
      }
      else if (length > 0)
      {
          mComposing.setLength(0);
          getCurrentInputConnection().commitText("", 0);
      }
      else
          keyDownUp(KeyEvent.KEYCODE_DEL);
   }
   
   
   
   private void
   handleCharacter(int primaryCode, int[] keyCodes)
   {
      getCurrentInputConnection().commitText(String.valueOf((char)primaryCode), 1);
   }
   
   
   
   private void
   handleClose()
   {
      commitTyped(getCurrentInputConnection());
      requestHideSelf(0);
      mInputView.closing();
   }
   
   
   
   public void
   swipeRight()
   {
   }
   
   
   
   public void
   swipeLeft()
   {
      handleBackspace();
   }
   
   
   
   public void
   swipeDown()
   {
      handleClose();
   }
   
   
   
   public void
   swipeUp()
   {
   }
   
   
   
   public void
   onPress(int primaryCode)
   {
   }
   
   
   
   public void
   onRelease(int primaryCode)
   {
   }
}
