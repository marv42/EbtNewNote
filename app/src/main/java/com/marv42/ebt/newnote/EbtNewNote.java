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
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.marv42.ebt.newnote.scanning.OcrHandler;

import java.util.ArrayList;
import java.util.List;

import dagger.android.support.DaggerAppCompatActivity;
import dagger.android.support.DaggerFragment;

import static android.widget.Toast.LENGTH_LONG;

public class EbtNewNote extends DaggerAppCompatActivity /*implements LifecycleOwner*/
        implements SubmittedFragment.Callback {
    static final int CHECK_LOCATION_SETTINGS_REQUEST_CODE = 1;
    static final int IMAGE_CAPTURE_REQUEST_CODE = 2;
    static final int EBT_NOTIFICATION_ID = 1;
    static final int SUBMIT_FRAGMENT_INDEX = 0;

    private static final int SUBMITTED_FRAGMENT_INDEX = 1;
    static final String FRAGMENT_TYPE = "fragment_type";

    private FragmentWithTitlePagerAdapter mAdapter;
    private SubmitFragment mSubmitFragment = null;
    private SubmittedFragment mSubmittedFragment = null;
    private boolean mSwitchToResults;

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
                    ((SubmitFragment) mAdapter.getItem(SUBMIT_FRAGMENT_INDEX))
                            .setViewValuesFromPreferences();
                else if (position == SUBMITTED_FRAGMENT_INDEX) {
                    ((SubmittedFragment) mAdapter.getItem(SUBMITTED_FRAGMENT_INDEX))
                            .refreshResults();
                }
            }
        }); // TODO do we need to removeOnPageChangeListener?

        mSwitchToResults = false;
        Bundle extras = getIntent().getExtras();
        if (extras != null && SubmittedFragment.class.getSimpleName().equals(
                extras.getString(FRAGMENT_TYPE)))
            mSwitchToResults = true;
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
        if (resultCode == RESULT_OK) {
            if (requestCode == IMAGE_CAPTURE_REQUEST_CODE) {
                Toast.makeText(this, getString(R.string.processing), LENGTH_LONG).show();
                new OcrHandler((OcrHandler.Callback) mAdapter.getItem(SUBMIT_FRAGMENT_INDEX),
                        (ThisApp) getApplication()).execute();
            }
            if (requestCode == CHECK_LOCATION_SETTINGS_REQUEST_CODE) {
                ((SubmitFragment) mAdapter.getItem(SUBMIT_FRAGMENT_INDEX)).requestLocation();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for (Fragment fragment : getSupportFragmentManager().getFragments()) {
            fragment.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public void switchFragment(int index) {
        ViewPager viewPager = findViewById(R.id.view_pager);
        viewPager.setCurrentItem(index);
    }

    @Override
    public void submittedFragmentStarted() {
        if (mSwitchToResults) {
            switchFragment(SUBMITTED_FRAGMENT_INDEX);
            mSwitchToResults = false;
        }
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
