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
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.View.OnTouchListener;



class MyGestureListener extends SimpleOnGestureListener implements
      OnTouchListener
{
   // private static final int SWIPE_MIN_DISTANCE = 100;
   // private static final int SWIPE_MAX_OFF_PATH = 250;
   // private static final int SWIPE_THRESHOLD_VELOCITY = 100;
   private int REL_SWIPE_MIN_DISTANCE;
   private int REL_SWIPE_MAX_OFF_PATH;
   private int REL_SWIPE_THRESHOLD_VELOCITY;
   private GestureDetector mGestureDetector;



   public MyGestureListener(Context context)
   {
      mGestureDetector = new GestureDetector(context, this);
      DisplayMetrics dm = context.getResources().getDisplayMetrics();
      REL_SWIPE_MIN_DISTANCE = (int) dm.widthPixels / 4; // a fourth of the screen
      REL_SWIPE_MAX_OFF_PATH = (int) dm.heightPixels / 5;
      REL_SWIPE_THRESHOLD_VELOCITY = 100; // px/s
   }



   @Override
   public boolean
   onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY)
   {
      if (e1 == null || e2 == null)
         return false;
      if (Math.abs(e1.getY() - e2.getY()) > REL_SWIPE_MAX_OFF_PATH)
         return false;
      if (Math.abs(e1.getX() - e2.getX()) > REL_SWIPE_MIN_DISTANCE &&
          Math.abs(velocityX) > REL_SWIPE_THRESHOLD_VELOCITY)
         return true;
      return false;
   }



   public boolean
   onTouch(View v, MotionEvent event)
   {
      return mGestureDetector.onTouchEvent(event);
   }



   public GestureDetector
   getDetector()
   {
      return mGestureDetector;
   }
}
