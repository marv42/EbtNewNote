package com.marv42.ebt.newnote.di;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import com.marv42.ebt.newnote.ApiCaller;
import com.marv42.ebt.newnote.EbtNewNote;
import com.marv42.ebt.newnote.SettingsActivity;
import com.marv42.ebt.newnote.ThisApp;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.android.ContributesAndroidInjector;

@Module(subcomponents = {EbtNewNoteComponent.class, SettingsComponent.class})
abstract class ApplicationModule {
    @ActivityScope
    @ContributesAndroidInjector(modules = EbtNewNoteModule.class)
    abstract EbtNewNote contributeEbtNewNoteInjector();

    @SettingsScope
    @ContributesAndroidInjector(modules = SettingsModule.class)
    abstract SettingsActivity contributeSettingsActivityInjector();

//    @Provides
//    @Singleton
//    static Application provideApplication(@NonNull ThisApp app) {
//        return app;
//    }

//    @Singleton
//    abstract Context bindContext(ThisApp app);
    // prefer static over virtual: https://developer.android.com/training/articles/perf-tips.html#PreferStatic
//    @Provides
//    @Singleton
//    static Context provideContext(@NonNull ThisApp app) {
//        return app.getApplicationContext();
//    }

    @Provides
    @Singleton
    static SharedPreferences provideSharedPreferences(@NonNull ThisApp app) {
        return PreferenceManager.getDefaultSharedPreferences(app);
    }

    @Provides
    @Singleton
    static ApiCaller provideApiCaller(@NonNull ThisApp app, SharedPreferences sharedPreferences) {
        return new ApiCaller(app, sharedPreferences);
    }
}
