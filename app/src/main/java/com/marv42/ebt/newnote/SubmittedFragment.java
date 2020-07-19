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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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
import android.widget.ImageView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import dagger.android.support.DaggerFragment;

import static android.content.Intent.ACTION_VIEW;
import static android.text.Html.FROM_HTML_MODE_COMPACT;
import static android.text.Html.fromHtml;
import static android.view.View.GONE;
import static com.marv42.ebt.newnote.EbtNewNote.SUBMIT_FRAGMENT_INDEX;

public class SubmittedFragment extends DaggerFragment {
    public interface Callback {
        void onSubmittedFragmentAdded();
        void switchFragment(int index);
    }

    @Inject
    SharedPreferencesHandler mSharedPreferencesHandler;
    @Inject
    SubmissionResults mSubmissionResults;

    private static final String EBT_HOST = "https://en.eurobilltracker.com/";
    private static final String BUTTON_PLACEHOLDER = "place holder";
    private static final String DENOMINATION_IMAGE = "denomination image";
    private static final String DENOMINATION = "denomination";
    private static final String SERIAL_NUMBER = "serial number";
    private static final String RESULT = "result";
    private static final String REASON = "reason";
    private static final String NOTE = "note";
    private static final String LOCATION = "location";
    private static final String COMMENT = "comment";
    private static final int MENU_ITEM_EDIT = 0;
    private static final int MENU_ITEM_SHOW = 1;

    private ExpandableListView mListView;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.results, container, false);
        mListView = rootView.findViewById(R.id.list);
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Activity activity = getActivity();
        if (activity != null)
            ((Callback) activity).onSubmittedFragmentAdded();
    }

    void refreshResults() {
        ArrayList<SubmissionResult> results = mSubmissionResults.getResults();
        Map<String, String> groupMap;
        List<Map<String, String>> groupData = new ArrayList<>();
        Map<String, String> childMap;
        List<List<Map<String, String>>> childData = new ArrayList<>();
        for (SubmissionResult sr : results) {
            String denomination = sr.mNoteData.mDenomination;
            String denominationUrl = EBT_HOST + "img/bills/ebt" +
                    denomination.replace(" €", "") + "b.gif";
            String sn = sr.mNoteData.mSerialNumber;
            String serialNumber = sn.length() > 0 ? ", " + sn : "";
            String sc = sr.mNoteData.mShortCode;
            String shortCode = sc.length() > 0 ? ", " + sc : "";

            String result = sr.getResult(getActivity());
            String reason = getReason(sr);
            String note = getString(R.string.note) + ": " + denomination + serialNumber + shortCode;
            String comment = getString(R.string.comment) + ": " + sr.mNoteData.mComment;
            String location = getString(R.string.location) + ": " + getLocation(sr);

            groupMap = new HashMap<>();
            groupMap.put(BUTTON_PLACEHOLDER, " ");
            groupMap.put(DENOMINATION_IMAGE, denominationUrl);
            groupMap.put(DENOMINATION, denomination);
            groupMap.put(SERIAL_NUMBER, sn.length() > 0 ? sn : "-");
            groupMap.put(RESULT, result);
            groupData.add(groupMap);

            childMap = new HashMap<>();
            childMap.put(REASON, reason);
            childMap.put(NOTE, note);
            childMap.put(COMMENT, comment);
            childMap.put(LOCATION, location);

            List<Map<String, String>> children = new ArrayList<>();
            children.add(childMap);
            childData.add(children);
        }
        String[] groupFrom = new String[] { BUTTON_PLACEHOLDER, DENOMINATION_IMAGE,
                DENOMINATION, SERIAL_NUMBER, RESULT };
        mListView.setAdapter(new MyExpandableListAdapter(
                getContext(),
                groupData,
                R.layout.list_parents,
                groupFrom,
                new int[] { R.id.list_place_holder,
                        R.id.list_denomination_image,
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
        registerForContextMenu(mListView);
        mListView.setSelection(results.size());
    }

    @Override
    public void onCreateContextMenu(@NonNull ContextMenu menu, @NonNull View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) menuInfo;
        int group = ExpandableListView.getPackedPositionGroup(info.packedPosition);
        menu.add(Menu.NONE, MENU_ITEM_EDIT, Menu.NONE, R.string.edit_data);
        if (mSubmissionResults.getResults().get(group).mBillId > 0)
            menu.add(Menu.NONE, MENU_ITEM_SHOW, Menu.NONE, R.string.show_in_browser);
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) item.getMenuInfo();
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
        String postalCode = result.mNoteData.mPostalCode;
        return result.mNoteData.mCity + (postalCode.length() > 0 ? " (" + postalCode + ") " : " ")
                + result.mNoteData.mCountry;
    }

    private String getReason(SubmissionResult result) {
        Activity activity = getActivity();
        if (activity == null)
            throw new IllegalStateException("No activity");
        return result.isSuccessful(activity) ?
                getColoredString(getString(R.string.insertion) + " " + getString(R.string.successful),
                        "green") :
                getColoredString(result.mReason, "red");
    }

    static String getColoredString(String s, String color) {
        return "<font color=\"" + color + "\">" + s + "</font>";
    }

    private void startNewNote(int groupPos) {
        NoteData noteData = mSubmissionResults.getResults().get(groupPos).mNoteData;
        mSharedPreferencesHandler.set(getString(R.string.pref_country_key), noteData.mCountry);
        mSharedPreferencesHandler.set(getString(R.string.pref_city_key), noteData.mCity);
        mSharedPreferencesHandler.set(getString(R.string.pref_postal_code_key), noteData.mPostalCode);
        mSharedPreferencesHandler.set(getString(R.string.pref_denomination_key), noteData.mDenomination);
        mSharedPreferencesHandler.set(getString(R.string.pref_short_code_key), noteData.mShortCode);
        mSharedPreferencesHandler.set(getString(R.string.pref_serial_number_key), noteData.mSerialNumber);
        mSharedPreferencesHandler.set(getString(R.string.pref_comment_key), noteData.mComment);
        Activity activity = getActivity();
        if (activity == null)
            throw new IllegalStateException("No activity");
        ((Callback) activity).switchFragment(SUBMIT_FRAGMENT_INDEX);
    }

    private void showInBrowser(int groupPos) {
        startActivity(new Intent(ACTION_VIEW, Uri.parse(EBT_HOST + "notes/?id=" +
                mSubmissionResults.getResults().get(groupPos).mBillId)));
    }

    public class MyExpandableListAdapter extends SimpleExpandableListAdapter {
        private List<? extends Map<String, ?> > mGroupData;
        private String[] mGroupFrom;
        private int[] mGroupTo;
        private List<? extends List<? extends Map<String, ?>>> mChildData;
        private String[] mChildFrom;
        private int[] mChildTo;

        MyExpandableListAdapter(Context context,
                                List<? extends Map<String, ?> > groupData,
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
            mGroupTo = groupTo;
            mChildData = childData;
            mChildFrom = childFrom;
            mChildTo = childTo;
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
            View v = SubmittedFragment.this.getLayoutInflater().inflate(R.layout.list_parents, parent, false);
            bindView(v, mGroupData.get(groupPosition), mGroupFrom, mGroupTo);
            return v;
        }

        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
                     View convertView, ViewGroup parent) {
            View v = SubmittedFragment.this.getLayoutInflater().inflate(R.layout.list_children, parent, false);
            bindView(v, mChildData.get(groupPosition).get(childPosition), mChildFrom, mChildTo);
            return v;
        }

        private void bindView(View view, Map<String, ?> data, String[] from, int[] to) {
            for (int i = 0; i < to.length; ++i) {
                String s = (String) data.get(from[i]);
                if (from[i].equals(DENOMINATION_IMAGE)) {
                   ImageView v = view.findViewById(to[i]);
                   if (v != null)
                       Picasso.get().load(s).into(v);
                } else {
                    TextView v = view.findViewById(to[i]);
                    if (v != null) {
                        v.setText(fromHtml(s, FROM_HTML_MODE_COMPACT));
                        if (TextUtils.isEmpty(s))
                            v.setVisibility(GONE);
                    }
                }
            }
        }
    }
}
