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
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuInflater;
import android.widget.Toast;

import com.marv42.ebt.newnote.scanning.OcrHandler;

import java.util.ArrayList;
import java.util.List;

import dagger.android.support.DaggerAppCompatActivity;
import dagger.android.support.DaggerFragment;

import static android.widget.Toast.LENGTH_LONG;

public class EbtNewNote extends DaggerAppCompatActivity /*implements LifecycleOwner*/ {
    static final int CHECK_LOCATION_SETTINGS_REQUEST_CODE = 1;
    static final int IMAGE_CAPTURE_REQUEST_CODE = 2;
    static final int EBT_NOTIFICATION_ID = 1;
    static final int SUBMIT_FRAGMENT_INDEX = 0;

    private static final int SUBMITTED_FRAGMENT_INDEX = 1;
    static final String FRAGMENT_TYPE = "fragment_type";

    private FragmentWithTitlePagerAdapter mPagerAdapter;
    private boolean mSwitchToResults;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mPagerAdapter = new FragmentWithTitlePagerAdapter(getSupportFragmentManager());
        mPagerAdapter.addFragment(new SubmitFragment(), getString(R.string.submit_fragment_title));
        mPagerAdapter.addFragment(new SubmittedFragment(), getString(R.string.submitted_fragment_title));
        ViewPager viewPager = findViewById(R.id.view_pager);
        viewPager.setAdapter(mPagerAdapter);
        viewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                if (position == SUBMIT_FRAGMENT_INDEX)
                    ((SubmitFragment) mPagerAdapter.getItem(SUBMIT_FRAGMENT_INDEX))
                            .setViewValuesfromPreferences();
                else {
                    ((SubmittedFragment) mPagerAdapter.getItem(SUBMITTED_FRAGMENT_INDEX))
                            .refreshResults();
                }
            }
        }); // TODO do we need to removeOnPageChangeListener?
        TabLayout tabLayout = findViewById(R.id.tab_layout);
        tabLayout.setupWithViewPager(viewPager);

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

//    @Override
//    protected void onResume() {
//        super.onResume();
//    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (resultCode == RESULT_OK) {
            if (requestCode == IMAGE_CAPTURE_REQUEST_CODE) {
                Toast.makeText(this, getString(R.string.processing), LENGTH_LONG).show();
                new OcrHandler((OcrHandler.Callback) mPagerAdapter.getItem(SUBMIT_FRAGMENT_INDEX)).execute();
            }
            if (requestCode == CHECK_LOCATION_SETTINGS_REQUEST_CODE) {
                ((SubmitFragment) mPagerAdapter.getItem(SUBMIT_FRAGMENT_INDEX)).requestLocation();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for (Fragment fragment : getSupportFragmentManager().getFragments()) {
            fragment.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    void switchFragment(int index) {
        ViewPager viewPager = findViewById(R.id.view_pager);
        viewPager.setCurrentItem(index);
    }

    void SubmittedFragmentStarted() {
        if (mSwitchToResults) {
            switchFragment(SUBMITTED_FRAGMENT_INDEX);
            mSwitchToResults = false;
        }
    }

    private static class FragmentWithTitlePagerAdapter extends FragmentPagerAdapter {
        private final List<DaggerFragment> mFragments = new ArrayList<>();
        private final List<String> mFragmentTitles = new ArrayList<>();

        FragmentWithTitlePagerAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);
        }

        void addFragment(DaggerFragment fragment, String title) {
            mFragments.add(fragment);
            mFragmentTitles.add(title);
        }

        @Override
        public int getCount() {
            return mFragments.size();
        }

        @Override
        public Fragment getItem(int position) {
            return mFragments.get(position);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitles.get(position);
        }
    }
}
