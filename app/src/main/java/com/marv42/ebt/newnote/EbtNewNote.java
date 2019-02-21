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
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.marv42.ebt.newnote.scanning.OcrHandler;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.android.support.DaggerAppCompatActivity;
import dagger.android.support.DaggerFragment;

import static android.widget.Toast.LENGTH_LONG;

public class EbtNewNote extends DaggerAppCompatActivity implements
        SharedPreferences.OnSharedPreferenceChangeListener/*, LifecycleOwner*/ {
    @Inject
    SharedPreferences mSharedPreferences;
    @Inject
    ApiCaller mApiCaller;
    @Inject
    LoginChecker mLoginChecker;
//    @Inject
//    SubmitFragment mSubmitFragment;
//    @Inject
//    SubmittedFragment mSubmittedFragment;

    public static final String LOG_TAG = EbtNewNote.class.getSimpleName();
    static final int IMAGE_CAPTURE_REQUEST_CODE = 1;
    static final int EBT_NOTIFICATION_ID = 1;
    static final int SUBMIT_FRAGMENT_INDEX = 0;
    private static final int SUBMITTED_FRAGMENT_INDEX = 1;
    static final String FRAGMENT_TYPE = "result";

    private FragmentWithTitlePagerAdapter mPagerAdapter;

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
                if (position == SUBMIT_FRAGMENT_INDEX) {
                    ((SubmitFragment) mPagerAdapter.getItem(SUBMIT_FRAGMENT_INDEX)).loadPreferences();
                }
            }
        }); // TODO do we need to removeOnPageChangeListener?
        TabLayout tabLayout = findViewById(R.id.tab_layout);
        tabLayout.setupWithViewPager(viewPager);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        menu.findItem(R.id.settings).setIntent(new Intent(this, Settings.class));
        menu.findItem(R.id.about).setOnMenuItemClickListener(new About(this));
        super.onCreateOptionsMenu(menu);
        return true;
    }

    @Override
    protected void onResume() {
        Log.d(LOG_TAG, "onResume");
        super.onResume();
        mSharedPreferences.registerOnSharedPreferenceChangeListener(this);
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            final String fragmentType = extras.getString(FRAGMENT_TYPE);
            if (SubmittedFragment.class.getSimpleName().equals(fragmentType)) {
                switchFragment(SUBMITTED_FRAGMENT_INDEX);
            }
        }
    }

    @Override
    protected void onPause() {
        Log.d(LOG_TAG, "onPause");
        mSharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (resultCode == RESULT_OK) {
            if (requestCode == IMAGE_CAPTURE_REQUEST_CODE) {
                Toast.makeText(this, getString(R.string.processing), LENGTH_LONG).show();
                new OcrHandler((OcrHandler.Callback) mPagerAdapter.getItem(SUBMIT_FRAGMENT_INDEX)).execute();
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

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (sharedPreferences == mSharedPreferences) {
            String loginChangedKey = getString(R.string.pref_login_changed_key);
            if (key.equals(loginChangedKey)) {
                CallManager.weAreCalling(R.string.pref_calling_login_key, this);
                sharedPreferences.edit().putBoolean(loginChangedKey, false).apply();
                Log.d(LOG_TAG, loginChangedKey + ": " + sharedPreferences.getBoolean(loginChangedKey, false));
                mLoginChecker.execute();
            }
        }
    }

    void switchFragment(int index) {
        ViewPager viewPager = findViewById(R.id.view_pager);
        viewPager.setCurrentItem(index);
    }

    /*public*/ class FragmentWithTitlePagerAdapter extends FragmentPagerAdapter {
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
