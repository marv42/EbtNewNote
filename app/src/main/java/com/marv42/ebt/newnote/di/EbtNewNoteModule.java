package com.marv42.ebt.newnote.di;

import com.marv42.ebt.newnote.SubmitFragment;
import com.marv42.ebt.newnote.SubmittedFragment;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;

@Module(subcomponents = {SubmitFragmentComponent.class, SubmittedFragmentComponent.class,
        SettingsComponent.class})
abstract class EbtNewNoteModule {
    @FragmentScope
    @ContributesAndroidInjector(modules = SubmitFragmentModule.class)
    abstract SubmitFragment contributeSubmitFragmentInjector();

    @FragmentScope
    @ContributesAndroidInjector(modules = SubmittedFragmentModule.class)
    abstract SubmittedFragment contributeSubmittedFragmentInjector();

//    @Binds
//    abstract Context bindContext(EbtNewNote activity);
    // prefer static over virtual: https://developer.android.com/training/articles/perf-tips.html#PreferStatic
//    @Provides
//    @ActivityScope
//    @Named("Activity")
//    static Context provideContext(@NonNull EbtNewNote activity) {
//        return activity;
//    }
}
