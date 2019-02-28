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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Toast;

import com.marv42.ebt.newnote.scanning.OcrHandler;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.android.support.DaggerAppCompatActivity;
import dagger.android.support.DaggerFragment;

import static android.widget.Toast.LENGTH_LONG;

public class EbtNewNote extends DaggerAppCompatActivity implements LoginChecker.Callback
        /*, LifecycleOwner*/ {
    @Inject
    ApiCaller mApiCaller;

    static final int CHECK_LOCATION_SETTINGS_REQUEST_CODE = 1;
    static final int IMAGE_CAPTURE_REQUEST_CODE = 2;
    static final int EBT_NOTIFICATION_ID = 1;
    static final int SUBMIT_FRAGMENT_INDEX = 0;

    private static final int SUBMITTED_FRAGMENT_INDEX = 1;
    static final String FRAGMENT_TYPE = "fragment_type";

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
                if (position == SUBMIT_FRAGMENT_INDEX)
                    ((SubmitFragment) mPagerAdapter.getItem(SUBMIT_FRAGMENT_INDEX)).loadPreferences();
                else {
                    // TODO ((SubmitFragment) mPagerAdapter.getItem(SUBMIT_FRAGMENT_INDEX)).savePreferences();
                    ((SubmittedFragment) mPagerAdapter.getItem(SUBMITTED_FRAGMENT_INDEX)).refreshResults();
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
        super.onResume();
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            final String fragmentType = extras.getString(FRAGMENT_TYPE);
            if (SubmittedFragment.class.getSimpleName().equals(fragmentType)) {
                switchFragment(SUBMITTED_FRAGMENT_INDEX);
            }
        }
    }

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

    @Override
    public void onLoginFailed() {
        new AlertDialog.Builder(this).setTitle(getString(R.string.info))
                .setMessage(getString(R.string.wrong_login_info))
                .setPositiveButton(getString(R.string.yes),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                startActivity(new Intent(getApplicationContext(), Settings.class));
                                dialog.dismiss();
                            }
                        }
                )
                .setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        }
                )
                .show();
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
