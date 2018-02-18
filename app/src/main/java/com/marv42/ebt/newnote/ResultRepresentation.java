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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.ExpandableListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
//import android.webkit.WebView;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;



public class ResultRepresentation extends ExpandableListActivity
{
   private static final String EBT_HOST = "http://en.eurobilltracker.com/";
   
   private static final String BUTTON_PLACEHOLDER = "place holder";
//   private static final String DENOMINATION_IMAGE = "denomination_image";
   private static final String DENOMINATION       = "denomination";
   private static final String SERIAL_NUMBER      = "serial number";
   private static final String RESULT             = "result";
   
   private static final String REASON             = "reason";
   private static final String NOTE               = "note";
   private static final String LOCATION           = "location";
   private static final String COMMENT            = "comment";
   
   private static final int MENU_ITEM_EDIT = 0;
   private static final int MENU_ITEM_SHOW = 1;
   
   protected MyGestureListener mGestureListener;
   
   private ArrayList<SubmissionResult> mResults;
   
   
   
   @Override
   protected void
   onCreate(Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.results);
      
      mResults = ((ThisApp) getApplicationContext()).getResults();
      
      mGestureListener = new MyGestureListener(this)
      {
         public boolean
         onTouch(View v, MotionEvent event)
         {
            if (mGestureListener.getDetector().onTouchEvent(event))
            {
               startActivity(new Intent(ResultRepresentation.this, EbtNewNote.class));
               return true;
            }
            else
               return false;
         }
      };
      ((LinearLayout) findViewById(R.id.result_layout)).setOnTouchListener(mGestureListener);
   }
   
   
   
   @Override
   protected void
   onResume()
   {
      super.onResume();
      
      List<     Map<String, String> > groupData = new ArrayList<     Map<String, String> >();
      List<List<Map<String, String>>> childData = new ArrayList<List<Map<String, String>>>();
      
      Map<String, String> groupMap;
      Map<String, String> childMap;
      
      for (SubmissionResult sr : mResults)
      {
         groupMap = new HashMap<String, String>();
         childMap = new HashMap<String, String>();
         
         String denomination      = sr.getNoteData().getDenomination();
//         String denominationImage = EBT_HOST + "img/bills/ebt" +
//                                    denomination.replace(" €", "") + "b.gif";
         String result = getResult(sr);
         
         String sn = sr.getNoteData().getSerialNumber();
         String serialNumber = sn.length() > 0 ? ", " + sn : "";
         
         String sc = sr.getNoteData().getShortCode();
         String shortCode = sc.length() > 0 ? ", " + sc : "";
         
         String reason   = getReason(sr);
         String note     = getString(R.string.note)     + ": " + denomination +
            serialNumber + shortCode;
         
         String comment  = getString(R.string.comment)  + ": " + sr.getNoteData().getComment();
         String location = getString(R.string.location) + ": " + getLocation(sr);
         
         groupMap.put(BUTTON_PLACEHOLDER,              " "              );
//         groupMap.put(DENOMINATION_IMAGE, denominationImage);
         groupMap.put(DENOMINATION,       denomination     );
         groupMap.put(SERIAL_NUMBER,      sn               );
         groupMap.put(RESULT,             result           );
         groupData.add(groupMap);
         
         childMap.put(REASON,   reason  );
         childMap.put(NOTE,     note    );
         childMap.put(COMMENT,  comment );
         childMap.put(LOCATION, location);
         
         List<Map<String, String>> children = new ArrayList<Map<String, String>>();
         children. add(childMap);
         childData.add(children);
      }
      
      String[] groupFrom =
         new String[] { BUTTON_PLACEHOLDER, /*DENOMINATION_IMAGE,*/ DENOMINATION, SERIAL_NUMBER, RESULT };
      
      setListAdapter(new MyExpandableListAdapter(
         this,
         groupData,
         R.layout.list_parents,
         groupFrom,
         new int[] { R.id.list_place_holder,
//                     R.id.list_denomination_image,
                     R.id.list_denomination,
                     R.id.list_serial,
                     R.id.list_result },
         childData,
         R.layout.list_children,
         new String[] { REASON, NOTE, COMMENT, LOCATION },
         new int[] { R.id.list_reason,
                     R.id.list_note,
                     R.id.list_comment,
                     R.id.list_location }));
      
      TableLayout layout = (TableLayout) getLayoutInflater()
         .inflate(R.layout.list_parents, null).findViewById(R.id.list_parent);
      if (layout != null)
         for (int i = 0; i < groupFrom.length; ++i)
            layout.setColumnStretchable(i, groupFrom[i].equals(SERIAL_NUMBER));
      
      registerForContextMenu(getExpandableListView());
   }
   
   
   
   @Override
   public void
   onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
   {
      super.onCreateContextMenu(menu, v, menuInfo);
      
      ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) menuInfo;
      int group = ExpandableListView.getPackedPositionGroup(info.packedPosition);
      
      menu.add(Menu.NONE, MENU_ITEM_EDIT, Menu.NONE, R.string.edit_data);
      if (mResults.get(group).getBillId() > 0)
         menu.add(Menu.NONE, MENU_ITEM_SHOW, Menu.NONE, R.string.show_in_browser);
   }
   
   
   
   @Override
   public boolean
   onContextItemSelected(MenuItem item)
   {
      ExpandableListContextMenuInfo info =
         (ExpandableListContextMenuInfo) item.getMenuInfo();
      
      switch (item.getItemId())
      {
      case MENU_ITEM_EDIT:
         startNewNote(ExpandableListView.getPackedPositionGroup(info.packedPosition));
         return true;
      case MENU_ITEM_SHOW:
         showInBrowser(ExpandableListView.getPackedPositionGroup(info.packedPosition));
         return true;
      default:
         return super.onContextItemSelected(item);
      }
   }
   
   
   
   private String
   getLocation(SubmissionResult result)
   {
      String city    = result.getNoteData().getCity();
      String country = result.getNoteData().getCountry();
      
      String pc = result.getNoteData().getPostalCode();
      String postalCode = pc.length() > 0 ? " (" + pc + ") " : " ";
      
      return city + postalCode + country;
   }
   
   
   
   private String
   getResult(SubmissionResult result)
   {
      return result.wasSuccessful() ?
         getColoredString(result.wasHit() ? getString(R.string.hit) :
                                            getString(R.string.successful), "green") :
         getColoredString(getString(R.string.failed), "red");
   }
   
   
   
   private String
   getReason(SubmissionResult result)
   {
      return result.wasSuccessful() ?
         getColoredString(getString(R.string.insertion) + " " +
                          getString(R.string.successful), "green") :
         getColoredString(result.getReason(), "red");
   }
   
   
   
   private String
   getColoredString(String s, String color)
   {
      return "<font color=\"" + color + "\">" + s + "</font>";
   }
   
   
   
   private void
   startNewNote(int groupPos)
   {
      NoteData noteData = mResults.get(groupPos).getNoteData();
      
      SharedPreferences.Editor editor =
         PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
      
      editor.putString(getString(R.string.pref_country_key      ), noteData.getCountry()     );
      editor.putString(getString(R.string.pref_city_key         ), noteData.getCity()        );
      editor.putString(getString(R.string.pref_postal_code_key  ), noteData.getPostalCode()  );
      editor.putString(getString(R.string.pref_denomination_key ), noteData.getDenomination());
      editor.putString(getString(R.string.pref_short_code_key   ), noteData.getShortCode()   );
      editor.putString(getString(R.string.pref_serial_number_key), noteData.getSerialNumber());
      editor.putString(getString(R.string.pref_comment_key      ), noteData.getComment()     );
      if (! editor.commit())
         Log.e(EbtNewNote.LOG_TARGET, "Editor's commit failed");
      
      startActivity(new Intent(this, EbtNewNote.class));
   }
   
   
   private void
   showInBrowser(int groupPos)
   {
      startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(
         EBT_HOST + "notes/?id=" + Integer.toString(
            mResults.get(groupPos).getBillId()))));
   }
   
   
   
   @Override
   public boolean
   onCreateOptionsMenu(Menu menu)
   {
      getMenuInflater().inflate(R.menu.menu, menu);
      menu.findItem(R.id.settings).setIntent(new Intent(this, Settings.class));
      menu.findItem(R.id.about).setOnMenuItemClickListener(new About(this));
      menu.findItem(R.id.submitted).setEnabled(false);
      menu.findItem(R.id.new_note).setIntent(new Intent(this, EbtNewNote.class));
      return super.onCreateOptionsMenu(menu);
   }
   
   
   
   public class MyExpandableListAdapter extends SimpleExpandableListAdapter
   {
      private List<? extends                Map<String, ?> > mGroupData;
      private String[] mGroupFrom;
      private int[]    mGroupTo;
      
      private List<? extends List<? extends Map<String, ?>>> mChildData;
      private String[] mChildFrom;
      private int[]    mChildTo;
      
      
      
      public MyExpandableListAdapter(Context context,
         List<? extends                Map<String, ?> > groupData,
         int groupLayout,
         String[] groupFrom,
         int[] groupTo,
         List<? extends List<? extends Map<String, ?>>> childData,
         int childLayout,
         String[] childFrom,
         int[] childTo)
      {
         super(context, groupData, groupLayout, groupFrom, groupTo,
                        childData, childLayout, childFrom, childTo);
         
         mGroupData = groupData;
         mGroupFrom = groupFrom;
         mGroupTo   = groupTo;
         
         mChildData = childData;
         mChildFrom = childFrom;
         mChildTo   = childTo;
      }
      
      
      
      @Override
      public View
      getGroupView(int groupPosition, boolean isExpanded,
                   View convertView, ViewGroup parent)
      {
         View v = ((LayoutInflater) getApplicationContext().getSystemService
            (Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.list_parents, null);
         
         bindView(v, mGroupData.get(groupPosition), mGroupFrom, mGroupTo);
         return v;
      }
      
      
      
      @Override
      public View
      getChildView(int groupPosition, int childPosition, boolean isLastChild,
                   View convertView, ViewGroup parent)
      {
         View v = ((LayoutInflater) getApplicationContext().getSystemService
            (Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.list_children, null);
         
         bindView(v, mChildData.get(groupPosition).get(childPosition),
                  mChildFrom, mChildTo);
         return v;
      }
      
      
      
      private void
      bindView(View view, Map<String, ?> data, String[] from, int[] to)
      {
         for (int i = 0; i < to.length; ++i)
         {
            String s = (String) data.get(from[i]);
//            if (from[i].equals(DENOMINATION_IMAGE))
//            {
//               WebView v = (WebView) view.findViewById(to[i]);
//               if (v != null)
////                  v.loadUrl(s);
//                  v.setVisibility(View.GONE);
//            }
//            else
//            {
               TextView v = (TextView) view.findViewById(to[i]);
               if (v != null)
               {
                  v.setText(Html.fromHtml(s));
                  if (TextUtils.isEmpty(s))
                     v.setVisibility(View.GONE);
               }
//            }
         }
      }
   }
}
