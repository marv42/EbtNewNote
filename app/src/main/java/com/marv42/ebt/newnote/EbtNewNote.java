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

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.marv42.ebt.newnote.scanning.OcrHandler;

import java.util.ArrayList;
import java.util.List;

import dagger.android.support.DaggerAppCompatActivity;

import static android.widget.Toast.LENGTH_LONG;
import static androidx.core.content.PermissionChecker.PERMISSION_GRANTED;

public class EbtNewNote extends DaggerAppCompatActivity
        implements SubmitFragment.Callback, SubmittedFragment.Callback, CommentSuggestion.Callback,
        ActivityCompat.OnRequestPermissionsResultCallback /*, LifecycleOwner*/ {
    public static final String FRAGMENT_TYPE = "fragment_type";
    public static final String NOTIFICATION_NOTE_CHANNEL_ID = "default";
    public static final String NOTIFICATION_OCR_CHANNEL_ID = "ebt_ocr_channel";
    public static final int OCR_NOTIFICATION_ID = 2;

    static final int NOTE_NOTIFICATION_ID = 1;
    static final int IMAGE_CAPTURE_REQUEST_CODE = 2;
    static final int LOCATION_PERMISSION_REQUEST_CODE = 3;
    static final int CAMERA_PERMISSION_REQUEST_CODE = 4;
    static final int SUBMIT_FRAGMENT_INDEX = 0;

    private static final int SUBMITTED_FRAGMENT_INDEX = 1;

    private FragmentWithTitlePagerAdapter mAdapter;
    private SubmitFragment mSubmitFragment = null;
    private SubmittedFragment mSubmittedFragment = null;
    private boolean mSwitchToResults;
    private String[] mCommentSuggestions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mAdapter = new FragmentWithTitlePagerAdapter();
        ViewPager pager = findViewById(R.id.view_pager);
        pager.setAdapter(mAdapter);
        TabLayout tabLayout = findViewById(R.id.tab_layout);
        tabLayout.setupWithViewPager(pager);

        mAdapter.startUpdate(pager);
        mSubmitFragment = (SubmitFragment) mAdapter.instantiateItem(pager, SUBMIT_FRAGMENT_INDEX);
        mSubmittedFragment = (SubmittedFragment) mAdapter.instantiateItem(pager, SUBMITTED_FRAGMENT_INDEX);
        mAdapter.finishUpdate(pager);

        pager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                if (position == SUBMIT_FRAGMENT_INDEX)
                    getSubmitFragment().setViewValuesFromPreferences();
                else if (position == SUBMITTED_FRAGMENT_INDEX) {
                    ((SubmittedFragment) mAdapter.getItem(SUBMITTED_FRAGMENT_INDEX))
                            .refreshResults();
                }
            }
        });

        mSwitchToResults = false;
        Bundle extras = getIntent().getExtras();
        if (extras != null && SubmittedFragment.class.getSimpleName().equals(
                extras.getString(FRAGMENT_TYPE)))
            mSwitchToResults = true;
    }

    private SubmitFragment getSubmitFragment() {
        return (SubmitFragment) mAdapter.getItem(SUBMIT_FRAGMENT_INDEX);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        menu.findItem(R.id.settings).setIntent(new Intent(this, SettingsActivity.class));
        menu.findItem(R.id.about).setOnMenuItemClickListener(new About(this));
        super.onCreateOptionsMenu(menu);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == IMAGE_CAPTURE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, getString(R.string.processing), LENGTH_LONG).show();
                new OcrHandler((ThisApp) getApplication(), getSubmitFragment(),
                        mSubmitFragment.getPhotoPath()).execute();
            } else
                getSubmitFragment().setPhotoPath("");
        }
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
            getSubmitFragment().checkLocationSetting();
        else if (requestCode == CAMERA_PERMISSION_REQUEST_CODE)
            getSubmitFragment().takePhoto();
    }

    @Override
    public void onSubmitFragmentAdded() {
        if (mCommentSuggestions != null && mCommentSuggestions.length > 0) {
            getSubmitFragment().setCommentsAdapter(mCommentSuggestions);
            mCommentSuggestions = null;
        }
    }

    @Override
    public void onSubmittedFragmentAdded() {
        if (mSwitchToResults) {
            switchFragment(SUBMITTED_FRAGMENT_INDEX);
            mSwitchToResults = false;
        }
    }

    @Override
    public void switchFragment(int index) {
        ViewPager viewPager = findViewById(R.id.view_pager);
        viewPager.setCurrentItem(index);
    }

    @Override
    public void onSuggestions(String[] suggestions) {
        SubmitFragment submitFragment = getSubmitFragment();
        if (submitFragment.isAdded())
            submitFragment.setCommentsAdapter(suggestions);
        else
            mCommentSuggestions = suggestions;
    }

    private class FragmentWithTitlePagerAdapter extends FragmentPagerAdapter {
        private final List<String> mFragmentTitles = new ArrayList<>();

        FragmentWithTitlePagerAdapter() {
            super(getSupportFragmentManager(), BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
            addTitles();
        }

        @Override
        public int getCount() {
            return mFragmentTitles.size();
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            Fragment fragment;
            if (position == SUBMIT_FRAGMENT_INDEX) {
                fragment = mSubmitFragment;
                if (fragment == null) {
                    fragment = new SubmitFragment();
                }
            } else if (position == SUBMITTED_FRAGMENT_INDEX) {
                fragment = mSubmittedFragment;
                if (fragment == null) {
                    fragment = new SubmittedFragment();
                }
            } else {
                throw new IllegalArgumentException("position");
            }
            return fragment;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitles.get(position);
        }

        private void addTitles() {
            mFragmentTitles.add(getString(R.string.submit_fragment_title));
            mFragmentTitles.add(getString(R.string.submitted_fragment_title));
        }
    }
}
