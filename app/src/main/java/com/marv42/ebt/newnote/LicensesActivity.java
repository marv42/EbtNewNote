/*
 Copyright (c) 2010 - 2022 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;

import com.mikepenz.aboutlibraries.LibsBuilder;

import dagger.android.support.DaggerAppCompatActivity;

public class LicensesActivity extends DaggerAppCompatActivity /*implements MenuItem.OnMenuItemClickListener*/ {

    @NonNull
    private LibsBuilder getLibsBuilder() {
        return new LibsBuilder()
//                .withAboutIconShown(true)
//                .withAboutVersionShown(true)
//                .withAboutAppName(getResources().getString(R.string.app_name))
                .withLicenseShown(true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, getLibsBuilder().supportFragment())
                .commit();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
