/*
 Copyright (c) 2010 - 2020 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;

import com.marv42.ebt.newnote.data.NoteData;
import com.marv42.ebt.newnote.exceptions.ErrorMessage;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import dagger.android.support.DaggerFragment;

import static android.content.Intent.ACTION_VIEW;
import static androidx.core.content.ContextCompat.getColor;
import static com.marv42.ebt.newnote.EbtNewNote.SUBMIT_FRAGMENT_INDEX;
import static com.marv42.ebt.newnote.Utils.getColoredString;

public class SubmittedFragment extends DaggerFragment implements LifecycleOwner {
    protected static final String DENOMINATION_IMAGE = "denomination image";
    private static final String EBT_HOST = "https://en.eurobilltracker.com/";
    private static final String BUTTON_PLACEHOLDER = "place holder";
    private static final String DENOMINATION = "denomination";
    private static final String SERIAL_NUMBER = "serial number";
    private static final String RESULT = "result";
    private static final String REASON = "reason";
    private static final String NOTE = "note";
    private static final String LOCATION = "location";
    private static final String COMMENT = "comment";
    private static final int MENU_ITEM_EDIT = 0;
    private static final int MENU_ITEM_SHOW = 1;

    @Inject
    SharedPreferencesHandler sharedPreferencesHandler;
    @Inject
    SubmissionResults submissionResults;
    @Inject
    EncryptedPreferenceDataStore dataStore;
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
        final SubmissionResult submissionResult = getSubmissionResult(info.packedPosition);
        menu.add(Menu.NONE, MENU_ITEM_EDIT, Menu.NONE, R.string.edit_data);
        if (submissionResult.mBillId > 0)
            menu.add(Menu.NONE, MENU_ITEM_SHOW, Menu.NONE, R.string.show_in_browser);
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) item.getMenuInfo();
        final SubmissionResult submissionResult = getSubmissionResult(info.packedPosition);
        switch (item.getItemId()) {
            case MENU_ITEM_EDIT:
                startNewNote(submissionResult.mNoteData);
                return true;
            case MENU_ITEM_SHOW:
                showInBrowser(submissionResult.mBillId);
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

    private SubmissionResult getSubmissionResult(long packedPosition) {
        int group = ExpandableListView.getPackedPositionGroup(packedPosition);
        return submissionResults.getResults().get(group);
    }

    void refreshResults() {
        List<Map<String, String>> groupData = getGroupData();
        List<List<Map<String, String>>> childData = getChildData();
        String[] groupFrom = getGroupFrom();
        int[] groupTo = getGroupTo();
        listView.setAdapter(new MyExpandableListAdapter(
                getLayoutInflater(),
                shouldShowImages(),
                groupData,
                groupFrom,
                groupTo,
                childData,
                new String[]{REASON, NOTE, COMMENT, LOCATION},
                new int[]{R.id.list_reason, R.id.list_note, R.id.list_comment, R.id.list_location}));
        registerForContextMenu(listView);
        ArrayList<SubmissionResult> results = submissionResults.getResults();
        listView.setSelection(results.size());
    }

    @NotNull
    private Boolean shouldShowImages() {
        return dataStore.get(R.string.pref_settings_images, false);
    }

    @NotNull
    private List<Map<String, String>> getGroupData() {
        ArrayList<SubmissionResult> results = submissionResults.getResults();
        List<Map<String, String>> groupData = new ArrayList<>();
        for (SubmissionResult sr : results)
            addGroupData(groupData, sr);
        return groupData;
    }

    @NotNull
    private List<List<Map<String, String>>> getChildData() {
        ArrayList<SubmissionResult> results = submissionResults.getResults();
        List<List<Map<String, String>>> childData = new ArrayList<>();
        for (SubmissionResult sr : results)
            addChildData(childData, sr);
        return childData;
    }

    private void addGroupData(List<Map<String, String>> groupData, SubmissionResult sr) {
        String denomination = sr.mNoteData.mDenomination;
        String denominationUrl = EBT_HOST + "img/bills/ebt" + denomination.replace(" €", "") + "b.gif";
        String serialNumber = sr.mNoteData.mSerialNumber;
        String result = sr.getResult(getActivity());
        Map<String, String> groupMap = getGroupMap(denomination, denominationUrl, serialNumber, result);
        groupData.add(groupMap);
    }

    @NotNull
    private Map<String, String> getGroupMap(String denomination, String denominationUrl, String sn, String result) {
        Map<String, String> groupMap = new HashMap<>();
        groupMap.put(BUTTON_PLACEHOLDER, " ");
        groupMap.put(DENOMINATION, denomination);
        if (shouldShowImages())
            groupMap.put(DENOMINATION_IMAGE, denominationUrl);
        final String serialNumber = sn.length() > 0 ? sn : "-";
        groupMap.put(SERIAL_NUMBER, serialNumber);
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
        String location = getString(R.string.location) + ": " + getLocation(sr.mNoteData);
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
    private String[] getGroupFrom() {
        if (shouldShowImages())
            return new String[]{BUTTON_PLACEHOLDER, DENOMINATION, DENOMINATION_IMAGE, SERIAL_NUMBER, RESULT};
        else
            return new String[]{BUTTON_PLACEHOLDER, DENOMINATION, SERIAL_NUMBER, RESULT};
    }

    private int[] getGroupTo() {
        if (shouldShowImages())
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

    private String getLocation(NoteData noteData) {
        String postalCode = noteData.mPostalCode;
        postalCode = postalCode.length() > 0 ? " (" + postalCode + ") " : " ";
        return noteData.mCity + postalCode + noteData.mCountry;
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

    private void startNewNote(NoteData noteData) {
        Activity activity = getActivity();
        if (activity == null)
            throw new IllegalStateException("No activity");
        setSharedPreferences(noteData);
        ((Callback) activity).switchFragment(SUBMIT_FRAGMENT_INDEX);
    }

    private void setSharedPreferences(NoteData noteData) {
        sharedPreferencesHandler.set(getString(R.string.pref_country_key), noteData.mCountry);
        sharedPreferencesHandler.set(getString(R.string.pref_city_key), noteData.mCity);
        sharedPreferencesHandler.set(getString(R.string.pref_postal_code_key), noteData.mPostalCode);
        sharedPreferencesHandler.set(getString(R.string.pref_denomination_key), noteData.mDenomination);
        sharedPreferencesHandler.set(getString(R.string.pref_short_code_key), noteData.mShortCode);
        sharedPreferencesHandler.set(getString(R.string.pref_serial_number_key), noteData.mSerialNumber);
        sharedPreferencesHandler.set(getString(R.string.pref_comment_key), noteData.mComment);
    }

    private void showInBrowser(int billId) {
        final Uri uri = Uri.parse(EBT_HOST + "notes/?id=" + billId);
        startActivity(new Intent(ACTION_VIEW, uri));
    }

    public interface Callback {
        void onSubmittedFragmentAdded();

        void switchFragment(int index);
    }
}
