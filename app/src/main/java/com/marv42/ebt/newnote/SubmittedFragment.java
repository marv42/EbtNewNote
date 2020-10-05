/*
 Copyright (c) 2010 - 2020 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Spanned;
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
import androidx.lifecycle.LifecycleOwner;

import com.marv42.ebt.newnote.exceptions.ErrorMessage;
import com.squareup.picasso.Picasso;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import dagger.android.support.DaggerFragment;

import static android.content.Intent.ACTION_VIEW;
import static android.view.View.GONE;
import static androidx.core.content.ContextCompat.getColor;
import static androidx.core.text.HtmlCompat.FROM_HTML_MODE_COMPACT;
import static androidx.core.text.HtmlCompat.fromHtml;
import static com.marv42.ebt.newnote.EbtNewNote.SUBMIT_FRAGMENT_INDEX;
import static com.marv42.ebt.newnote.Utils.getColoredString;

public class SubmittedFragment extends DaggerFragment implements LifecycleOwner {
    public interface Callback {
        void onSubmittedFragmentAdded();
        void switchFragment(int index);
    }

    @Inject SharedPreferencesHandler sharedPreferencesHandler;
    @Inject SubmissionResults submissionResults;
    @Inject EncryptedPreferenceDataStore dataStore;

    private static final String EBT_HOST = "https://en.eurobilltracker.com/";
    private static final String BUTTON_PLACEHOLDER = "place holder";
    private static final String DENOMINATION = "denomination";
    protected static final String DENOMINATION_IMAGE = "denomination image";
    private static final String SERIAL_NUMBER = "serial number";
    private static final String RESULT = "result";
    private static final String REASON = "reason";
    private static final String NOTE = "note";
    private static final String LOCATION = "location";
    private static final String COMMENT = "comment";
    private static final int MENU_ITEM_EDIT = 0;
    private static final int MENU_ITEM_SHOW = 1;

    private ExpandableListView listView;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.results, container, false);
        listView = rootView.findViewById(R.id.list);
        return rootView;
    }

    @Override
    public void onCreateContextMenu(@NonNull ContextMenu menu, @NonNull View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) menuInfo;
        final SubmissionResult submissionResult = getSubmissionResult(info);
        menu.add(Menu.NONE, MENU_ITEM_EDIT, Menu.NONE, R.string.edit_data);
        if (submissionResult.mBillId > 0)
            menu.add(Menu.NONE, MENU_ITEM_SHOW, Menu.NONE, R.string.show_in_browser);
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) item.getMenuInfo();
        final SubmissionResult submissionResult = getSubmissionResult(info);
        switch (item.getItemId()) {
            case MENU_ITEM_EDIT:
                startNewNote(submissionResult);
                return true;
            case MENU_ITEM_SHOW:
                showInBrowser(submissionResult);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Activity activity = getActivity();
        if (activity != null)
            ((Callback) activity).onSubmittedFragmentAdded();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshResults();
    }

    private SubmissionResult getSubmissionResult(ExpandableListContextMenuInfo info) {
        int group = ExpandableListView.getPackedPositionGroup(info.packedPosition);
        return submissionResults.getResults().get(group);
    }

    void refreshResults() {
        boolean showImages = dataStore.get(R.string.pref_settings_images, true);
        ArrayList<SubmissionResult> results = submissionResults.getResults();
        List<Map<String, String>> groupData = new ArrayList<>();
        List<List<Map<String, String>>> childData = new ArrayList<>();
        for (SubmissionResult sr : results) {
            addGroupData(groupData, sr, showImages);
            addChildData(childData, sr);
        }
        String[] groupFrom = getGroupFrom(showImages);
        int[] groupTo = getGroupTo(showImages);
        listView.setAdapter(new MyExpandableListAdapter(
                this,
                groupData,
                R.layout.list_parents,
                groupFrom,
                groupTo,
                childData,
                R.layout.list_children,
                new String[] { REASON, NOTE, COMMENT, LOCATION },
                new int[] { R.id.list_reason,
                        R.id.list_note,
                        R.id.list_comment,
                        R.id.list_location }));
        registerForContextMenu(listView);
        listView.setSelection(results.size());
    }

    private void addGroupData(List<Map<String, String>> groupData, SubmissionResult sr, boolean showImages) {
        String denomination = sr.mNoteData.mDenomination;
        String denominationUrl = EBT_HOST + "img/bills/ebt" + denomination.replace(" €", "") + "b.gif";
        String serialNumber = sr.mNoteData.mSerialNumber;
        String result = sr.getResult(getActivity());
        Map<String, String> groupMap = getGroupMap(showImages, denomination, denominationUrl, serialNumber, result);
        groupData.add(groupMap);
    }

    @NotNull
    private Map<String, String> getGroupMap(boolean showImages, String denomination, String denominationUrl, String sn, String result) {
        Map<String, String> groupMap = new HashMap<>();
        groupMap.put(BUTTON_PLACEHOLDER, " ");
        groupMap.put(DENOMINATION, denomination);
        if (showImages)
            groupMap.put(DENOMINATION_IMAGE, denominationUrl);
        // TODO Make the space disappear if no image
        groupMap.put(SERIAL_NUMBER, sn.length() > 0 ? sn : "-");
        groupMap.put(RESULT, result);
        return groupMap;
    }

    private void addChildData(List<List<Map<String, String>>> childData, SubmissionResult sr) {
        String reason = getReason(sr);
        String denomination = sr.mNoteData.mDenomination;
        String sn = sr.mNoteData.mSerialNumber;
        String serialNumber = sn.length() > 0 ? ", " + sn : "";
        String sc = sr.mNoteData.mShortCode;
        String shortCode = sc.length() > 0 ? ", " + sc : "";
        String note = getString(R.string.note) + ": " + denomination + serialNumber + shortCode;
        String comment = getString(R.string.comment) + ": " + sr.mNoteData.mComment;
        String location = getString(R.string.location) + ": " + getLocation(sr);
        List<Map<String, String>> children = getChildMap(reason, note, comment, location);
        childData.add(children);
    }

    @NotNull
    private List<Map<String, String>> getChildMap(String reason, String note, String comment, String location) {
        Map<String, String> childMap = new HashMap<>();
        childMap.put(REASON, reason);
        childMap.put(NOTE, note);
        childMap.put(COMMENT, comment);
        childMap.put(LOCATION, location);
        List<Map<String, String>> children = new ArrayList<>();
        children.add(childMap);
        return children;
    }

    @NotNull
    private String[] getGroupFrom(boolean showImages) {
        if (showImages)
            return new String[]{BUTTON_PLACEHOLDER, DENOMINATION, DENOMINATION_IMAGE, SERIAL_NUMBER, RESULT};
        else
            return new String[]{BUTTON_PLACEHOLDER, DENOMINATION, SERIAL_NUMBER, RESULT};
    }

    private int[] getGroupTo(boolean showImages) {
        if (showImages)
            return new int[]{R.id.list_place_holder,
                    R.id.list_denomination,
                    R.id.list_denomination_image,
                    R.id.list_serial,
                    R.id.list_result};
        else
            return new int[]{R.id.list_place_holder,
                    R.id.list_denomination,
                    R.id.list_serial,
                    R.id.list_result};
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
                        getColor(activity, R.color.success)) :
                getColoredString(new ErrorMessage(activity).getErrorMessage(result.mReason),
                        getColor(activity, R.color.failed));
    }

    private void startNewNote(SubmissionResult submissionResult) {
        Activity activity = getActivity();
        if (activity == null)
            throw new IllegalStateException("No activity");
        setSharedPreferences(submissionResult);
        ((Callback) activity).switchFragment(SUBMIT_FRAGMENT_INDEX);
    }

    private void setSharedPreferences(SubmissionResult submissionResult) {
        NoteData noteData = submissionResult.mNoteData;
        sharedPreferencesHandler.set(getString(R.string.pref_country_key), noteData.mCountry);
        sharedPreferencesHandler.set(getString(R.string.pref_city_key), noteData.mCity);
        sharedPreferencesHandler.set(getString(R.string.pref_postal_code_key), noteData.mPostalCode);
        sharedPreferencesHandler.set(getString(R.string.pref_denomination_key), noteData.mDenomination);
        sharedPreferencesHandler.set(getString(R.string.pref_short_code_key), noteData.mShortCode);
        sharedPreferencesHandler.set(getString(R.string.pref_serial_number_key), noteData.mSerialNumber);
        sharedPreferencesHandler.set(getString(R.string.pref_comment_key), noteData.mComment);
    }

    private void showInBrowser(SubmissionResult submissionResult) {
        startActivity(new Intent(ACTION_VIEW, Uri.parse(EBT_HOST + "notes/?id=" +
                submissionResult.mBillId)));
    }
}
