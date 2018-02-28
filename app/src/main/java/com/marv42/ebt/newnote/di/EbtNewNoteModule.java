package com.marv42.ebt.newnote.di;

import dagger.Module;
import dagger.android.AndroidInjectionModule;
import dagger.android.support.AndroidSupportInjectionModule;

@Module(includes = {AndroidInjectionModule.class, AndroidSupportInjectionModule.class}/*,
        subcomponents = {WebViewFragmentComponent.class}*/)
abstract class EbtNewNoteModule {
//    @FragmentScope
//    @ContributesAndroidInjector(modules = WebViewFragmentModule.class)
//    abstract WebViewFragment contributeWebViewFragmentInjector();

    //    @Binds
//    abstract Context bindContext(Activity activity);
    // prefer static over virtual: https://developer.android.com/training/articles/perf-tips.html#PreferStatic
//    @Provides
//    @ActivityScope
//    @Named("Activity")
//    //@Contract(pure = true)
//    static Context provideContext(@NonNull EbtNewNote activity) {
//        return activity;
//    }
}
