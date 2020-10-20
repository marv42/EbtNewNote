/*
 Copyright (c) 2010 - 2020 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote;

import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.lifecycle.LifecycleOwner;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.marv42.ebt.newnote.scanning.OcrHandler;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.android.support.DaggerAppCompatActivity;

import static android.widget.Toast.LENGTH_LONG;
import static androidx.core.content.PermissionChecker.PERMISSION_GRANTED;

public class EbtNewNote extends DaggerAppCompatActivity
        implements SubmitFragment.Callback, ResultsFragment.Callback, CommentSuggestion.Callback,
        ActivityCompat.OnRequestPermissionsResultCallback, LifecycleOwner {

    public static final String FRAGMENT_TYPE = "fragment_type";
    public static final int IMAGE_CAPTURE_REQUEST_CODE = 2;
    public static final int LOCATION_PERMISSION_REQUEST_CODE = 3;
    public static final int CAMERA_PERMISSION_REQUEST_CODE = 4;
    static final int SUBMIT_FRAGMENT_INDEX = 0;
    private static final int SUBMITTED_FRAGMENT_INDEX = 1;
    @Inject
    EncryptedPreferenceDataStore dataStore;
    @Inject
    SharedPreferencesHandler sharedPreferencesHandler;
    @Inject
    SubmissionResultHandler submissionResultHandler;
    private SubmitFragment submitFragment = null;
    private ResultsFragment resultsFragment = null;
    private int fragmentToSwitchTo = -1;
    private String[] commentSuggestions;
    private boolean isDualPane = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setLayout();
        setDualPane();
        setFragments();
    }

    private void setLayout() {
        applyStyle();
        setContentView(R.layout.main);
    }

    private void applyStyle() {
        Resources.Theme theme = getTheme();
        TypedValue colorAccentValue = new TypedValue();
        if (theme.resolveAttribute(android.R.attr.colorAccent, colorAccentValue, true)) {
            @ColorRes int colorRes = colorAccentValue.resourceId != 0 ? colorAccentValue.resourceId : colorAccentValue.data;
            @ColorInt int color = ContextCompat.getColor(this, colorRes);
            theme.applyStyle(color, true);
        }
    }

    private void setDualPane() {
        ViewPager pager = findViewById(R.id.view_pager);
        if (pager == null)
            isDualPane = true;
    }

    private void setFragments() {
        ViewPager pager = findViewById(R.id.view_pager);
        if (isDualPane) {
            final FragmentManager manager = getSupportFragmentManager();
            submitFragment = (SubmitFragment) manager.findFragmentById(R.id.submit_fragment);
            resultsFragment = (ResultsFragment) manager.findFragmentById(R.id.submitted_fragment);
        } else {
            FragmentWithTitlePagerAdapter adapter = new FragmentWithTitlePagerAdapter();
            pager.setAdapter(adapter);
            setupTabLayout(pager);

            adapter.startUpdate(pager);
            instantiateFragments(adapter, pager);
            adapter.finishUpdate(pager);
        }
    }

    private void setupTabLayout(ViewPager pager) {
        TabLayout tabLayout = findViewById(R.id.tab_layout);
        tabLayout.setupWithViewPager(pager);
    }

    private void instantiateFragments(FragmentWithTitlePagerAdapter adapter, ViewPager pager) {
        submitFragment = (SubmitFragment) adapter.instantiateItem(pager, SUBMIT_FRAGMENT_INDEX);
        resultsFragment = (ResultsFragment) adapter.instantiateItem(pager, SUBMITTED_FRAGMENT_INDEX);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        submissionResultHandler.reset();
        if (!isDualPane) {
            checkFragmentToSwitchTo(intent);
            checkSwitchFragment();
        }
    }

    private void checkFragmentToSwitchTo(Intent intent) {
        eventuallySetFragmentToSwitchTo(intent, SubmitFragment.class.getSimpleName(), SUBMIT_FRAGMENT_INDEX);
        eventuallySetFragmentToSwitchTo(intent, ResultsFragment.class.getSimpleName(), SUBMITTED_FRAGMENT_INDEX);
    }

    private void eventuallySetFragmentToSwitchTo(Intent intent, String fragmentClassName, int fragmentIndex) {
        Bundle extras = intent.getExtras();
        if (extras != null && fragmentClassName.equals(extras.getString(FRAGMENT_TYPE)))
            fragmentToSwitchTo = fragmentIndex;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        inflateMenu(menu);
        super.onCreateOptionsMenu(menu);
        return true;
    }

    private void inflateMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        menu.findItem(R.id.settings).setIntent(new Intent(this, SettingsActivity.class));
        menu.findItem(R.id.about).setOnMenuItemClickListener(new About(this));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == IMAGE_CAPTURE_REQUEST_CODE)
            if (resultCode == RESULT_OK)
                processPhoto();
    }

    private void processPhoto() {
        Toast.makeText(this, getString(R.string.processing), LENGTH_LONG).show();
        String apiKey = dataStore.get(R.string.pref_settings_ocr_key, "");
        String photoPath = sharedPreferencesHandler.get(R.string.pref_photo_path_key, "");
        Uri photoUri = Uri.parse(sharedPreferencesHandler.get(R.string.pref_photo_uri_key, ""));
        new OcrHandler(submitFragment, photoPath, photoUri, getContentResolver(), apiKey).execute();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length == 0 || grantResults[0] != PERMISSION_GRANTED) {
            Toast.makeText(this, getString(R.string.no_permission), LENGTH_LONG).show();
            return;
        }
        Toast.makeText(this, getString(R.string.permission), LENGTH_LONG).show();
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE)
            submitFragment.locationButtonClicked();
        else if (requestCode == CAMERA_PERMISSION_REQUEST_CODE)
            submitFragment.takePhoto();
    }

    @Override
    public void onSubmitFragmentAdded() {
        if (commentSuggestions != null && commentSuggestions.length > 0) {
            submitFragment.setCommentsAdapter(commentSuggestions);
            commentSuggestions = null;
        }
        checkSwitchFragment();
    }

    @Override
    public void onSubmittedFragmentAdded() {
        checkSwitchFragment();
    }

    private void checkSwitchFragment() {
        checkSwitchFragment(SUBMIT_FRAGMENT_INDEX);
        checkSwitchFragment(SUBMITTED_FRAGMENT_INDEX);
    }

    private void checkSwitchFragment(int fragmentIndex) {
        if (!isDualPane)
            if (fragmentToSwitchTo == fragmentIndex) {
                switchFragment(fragmentIndex);
                fragmentToSwitchTo = -1;
            }
    }

    @Override
    public void switchFragment(int fragmentIndex) {
        ViewPager viewPager = findViewById(R.id.view_pager);
        viewPager.setCurrentItem(fragmentIndex);
    }

    @Override
    public void onSuggestions(String[] suggestions) {
        if (submitFragment.isAdded())
            submitFragment.setCommentsAdapter(suggestions);
        else
            commentSuggestions = suggestions;
    }

    private class FragmentWithTitlePagerAdapter extends FragmentPagerAdapter {

        private final List<String> fragmentTitles = new ArrayList<>();

        FragmentWithTitlePagerAdapter() {
            super(getSupportFragmentManager(), BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
            addTitles();
        }

        @Override
        public int getCount() {
            return fragmentTitles.size();
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            Fragment fragment;
            if (position == SUBMIT_FRAGMENT_INDEX) {
                fragment = submitFragment;
                if (fragment == null)
                    fragment = new SubmitFragment();
            } else if (position == SUBMITTED_FRAGMENT_INDEX) {
                fragment = resultsFragment;
                if (fragment == null)
                    fragment = new ResultsFragment();
            } else
                throw new IllegalArgumentException("position");
            return fragment;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return fragmentTitles.get(position);
        }

        private void addTitles() {
            fragmentTitles.add(getString(R.string.submit_fragment_title));
            fragmentTitles.add(getString(R.string.submitted_fragment_title));
        }
    }
}
