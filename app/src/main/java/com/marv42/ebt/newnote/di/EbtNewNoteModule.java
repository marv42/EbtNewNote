package com.marv42.ebt.newnote.di;

import android.content.Context;
import android.support.annotation.NonNull;

import com.marv42.ebt.newnote.EbtNewNote;
import com.marv42.ebt.newnote.SubmittedFragment;
import com.marv42.ebt.newnote.SubmitFragment;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;
import dagger.android.ContributesAndroidInjector;

@Module(subcomponents = {SubmitFragmentComponent.class, ResultFragmentComponent.class})
abstract class EbtNewNoteModule {
    @FragmentScope
    @ContributesAndroidInjector(modules = SubmitFragmentModule.class)
    abstract SubmitFragment contributeSubmitFragmentInjector();

    @FragmentScope
    @ContributesAndroidInjector(modules = ResultFragmentModule.class)
    abstract SubmittedFragment contributeResultFragmentInjector();

//    @Binds
//    abstract Context bindContext(EbtNewNote activity);
    // prefer static over virtual: https://developer.android.com/training/articles/perf-tips.html#PreferStatic
    @Provides
    @ActivityScope
    @Named("Activity")
    static Context provideContext(@NonNull EbtNewNote activity) {
        return activity;
    }
}
