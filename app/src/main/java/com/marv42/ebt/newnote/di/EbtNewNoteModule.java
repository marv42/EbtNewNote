/*
 Copyright (c) 2010 - 2020 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote.di;

import android.app.Activity;

import androidx.annotation.NonNull;

import com.marv42.ebt.newnote.EbtNewNote;
import com.marv42.ebt.newnote.SubmitFragment;
import com.marv42.ebt.newnote.SubmittedFragment;

import dagger.Module;
import dagger.Provides;
import dagger.android.ContributesAndroidInjector;

@Module(subcomponents = {SubmitFragmentComponent.class, SubmittedFragmentComponent.class,
        SettingsComponent.class})
abstract class EbtNewNoteModule {
    @Provides
    @ActivityScope
    //    @Named("Activity")
    static Activity provideActivity(@NonNull EbtNewNote activity) {
        return activity;
    }

//    @Binds
//    abstract Context bindContext(EbtNewNote activity);
    // prefer static over virtual: https://developer.android.com/training/articles/perf-tips.html#PreferStatic
//    @Provides
//    @ActivityScope
//    @Named("Activity")
//    static Context provideContext(@NonNull EbtNewNote activity) {
//        return activity;
//    }

    @SubmitFragmentScope
    @ContributesAndroidInjector(modules = SubmitFragmentModule.class)
    abstract SubmitFragment contributeSubmitFragmentInjector();

    @SubmittedFragmentScope
    @ContributesAndroidInjector(modules = SubmittedFragmentModule.class)
    abstract SubmittedFragment contributeSubmittedFragmentInjector();
}
