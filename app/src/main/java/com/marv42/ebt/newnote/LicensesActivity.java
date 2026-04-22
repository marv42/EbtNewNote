/*
 Copyright (c) 2010 - 2026 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote;

import static com.marv42.ebt.newnote.MyOnApplyWindowInsetsListener.getOnApplyWindowInsetsListener;

import android.os.Bundle;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.core.view.MenuProvider;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import com.mikepenz.aboutlibraries.LibsBuilder;

import dagger.android.support.DaggerAppCompatActivity;

public class LicensesActivity extends DaggerAppCompatActivity {

    @NonNull
    private LibsBuilder getLibsBuilder() {
        return new LibsBuilder()
                // We do this in aboutlibraries_description.xml
//                .withAboutIconShown(true)
//                .withAboutVersionShown(true)
//                .withAboutAppName(getResources().getString(R.string.app_name))
                .withLicenseShown(true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        ViewGroup content = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(content, getOnApplyWindowInsetsListener());
        getSupportFragmentManager()
                .beginTransaction()
                .setReorderingAllowed(true)
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
