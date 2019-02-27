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
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import dagger.android.support.DaggerFragment;

import static android.content.Intent.ACTION_VIEW;
import static android.preference.PreferenceManager.getDefaultSharedPreferences;
import static android.text.Html.FROM_HTML_MODE_COMPACT;
import static android.text.Html.fromHtml;
import static android.view.View.GONE;
import static com.marv42.ebt.newnote.EbtNewNote.SUBMIT_FRAGMENT_INDEX;

public class SubmittedFragment extends DaggerFragment {
    @Inject
    ThisApp mApp;

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

    private ExpandableListView mListView;
    private ArrayList<SubmissionResult> mResults;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.results, container, false);
        mListView = rootView.findViewById(R.id.list);
        mResults = mApp.getResults();
        prepareListData();

        return rootView;
    }

    private void prepareListData() {
        List<     Map<String, String> > groupData = new ArrayList<>();
        List<List<Map<String, String>>> childData = new ArrayList<>();
        Map<String, String> groupMap;
        Map<String, String> childMap;

        for (SubmissionResult sr : mResults) {
            groupMap = new HashMap<>();
            childMap = new HashMap<>();

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

            groupMap.put(BUTTON_PLACEHOLDER,              " ");
//         groupMap.put(DENOMINATION_IMAGE, denominationImage);
            groupMap.put(DENOMINATION,       denomination    );
            groupMap.put(SERIAL_NUMBER,      sn              );
            groupMap.put(RESULT,             result          );
            groupData.add(groupMap);

            childMap.put(REASON,   reason  );
            childMap.put(NOTE,     note    );
            childMap.put(COMMENT,  comment );
            childMap.put(LOCATION, location);

            List<Map<String, String>> children = new ArrayList<>();
            children. add(childMap);
            childData.add(children);
        }

        String[] groupFrom =
                new String[] {BUTTON_PLACEHOLDER, /*DENOMINATION_IMAGE,*/ DENOMINATION, SERIAL_NUMBER, RESULT};

        mListView.setAdapter(new MyExpandableListAdapter(
                getContext(),
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

//        TableLayout layout = getLayoutInflater().inflate(R.layout.list_parents, null)
//                .findViewById(R.id.list_parent);
//        if (layout != null)
//            for (int i = 0; i < groupFrom.length; ++i)
//                layout.setColumnStretchable(i, groupFrom[i].equals(SERIAL_NUMBER));
        registerForContextMenu(mListView);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) menuInfo;
        int group = ExpandableListView.getPackedPositionGroup(info.packedPosition);

        menu.add(Menu.NONE, MENU_ITEM_EDIT, Menu.NONE, R.string.edit_data);
        if (mResults.get(group).getBillId() > 0)
            menu.add(Menu.NONE, MENU_ITEM_SHOW, Menu.NONE, R.string.show_in_browser);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        ExpandableListContextMenuInfo info =
                (ExpandableListContextMenuInfo) item.getMenuInfo();

        switch (item.getItemId()) {
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

    private String getLocation(SubmissionResult result) {
        String city    = result.getNoteData().getCity();
        String country = result.getNoteData().getCountry();

        String pc = result.getNoteData().getPostalCode();
        String postalCode = pc.length() > 0 ? " (" + pc + ") " : " ";

        return city + postalCode + country;
    }

    private String getResult(SubmissionResult result) {
        return result.wasSuccessful() ?
                getColoredString(result.wasHit() ? getString(R.string.hit) :
                        getString(R.string.successful), "green") :
                getColoredString(getString(R.string.failed), "red");
    }

    private String getReason(SubmissionResult result) {
        return result.wasSuccessful() ?
                getColoredString(getString(R.string.insertion) + " " +
                        getString(R.string.successful), "green") :
                getColoredString(result.getReason(), "red");
    }

    private String getColoredString(String s, String color) {
        return "<font color=\"" + color + "\">" + s + "</font>";
    }

    private void startNewNote(int groupPos) {
        NoteData noteData = mResults.get(groupPos).getNoteData();
        getDefaultSharedPreferences(getContext()).edit()
                .putString(getString(R.string.pref_country_key      ), noteData.getCountry())
                .putString(getString(R.string.pref_city_key         ), noteData.getCity())
                .putString(getString(R.string.pref_postal_code_key  ), noteData.getPostalCode())
                .putString(getString(R.string.pref_denomination_key ), noteData.getDenomination())
                .putString(getString(R.string.pref_short_code_key   ), noteData.getShortCode())
                .putString(getString(R.string.pref_serial_number_key), noteData.getSerialNumber())
                .putString(getString(R.string.pref_comment_key      ), noteData.getComment()).apply();
        ((EbtNewNote) getActivity()).switchFragment(SUBMIT_FRAGMENT_INDEX);
    }

    private void showInBrowser(int groupPos) {
        startActivity(new Intent(ACTION_VIEW, Uri.parse(
                EBT_HOST + "notes/?id=" + Integer.toString(
                        mResults.get(groupPos).getBillId()))));
    }

    public class MyExpandableListAdapter extends SimpleExpandableListAdapter {
        private List<? extends                Map<String, ?> > mGroupData;
        private String[] mGroupFrom;
        private int[]    mGroupTo;

        private List<? extends List<? extends Map<String, ?>>> mChildData;
        private String[] mChildFrom;
        private int[]    mChildTo;

        MyExpandableListAdapter(Context context,
                                List<? extends                Map<String, ?> > groupData,
                                int groupLayout,
                                String[] groupFrom,
                                int[] groupTo,
                                List<? extends List<? extends Map<String, ?>>> childData,
                                int childLayout,
                                String[] childFrom,
                                int[] childTo) {
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
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
            View v = SubmittedFragment.this.getLayoutInflater().inflate(R.layout.list_parents, null);
            bindView(v, mGroupData.get(groupPosition), mGroupFrom, mGroupTo);
            return v;
        }

        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
                     View convertView, ViewGroup parent) {
            View v = SubmittedFragment.this.getLayoutInflater().inflate(R.layout.list_children, null);
            bindView(v, mChildData.get(groupPosition).get(childPosition), mChildFrom, mChildTo);
            return v;
        }

        private void bindView(View view, Map<String, ?> data, String[] from, int[] to) {
            for (int i = 0; i < to.length; ++i) {
                String s = (String) data.get(from[i]);
//            if (from[i].equals(DENOMINATION_IMAGE)) {
//               WebView v = (WebView) view.findViewById(to[i]);
//               if (v != null)
////                  v.loadUrl(s);
//                  v.setVisibility(View.GONE);
//            } else {
                TextView v = view.findViewById(to[i]);
                if (v != null) {
                    v.setText(fromHtml(s, FROM_HTML_MODE_COMPACT));
                    v.setTextColor(0xffffffff);
                    if (TextUtils.isEmpty(s))
                        v.setVisibility(GONE);
                }
//            }
            }
        }
    }
}
