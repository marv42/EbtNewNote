package com.marv42.ebt.newnote.di;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import com.marv42.ebt.newnote.ApiCaller;
import com.marv42.ebt.newnote.EbtNewNote;
import com.marv42.ebt.newnote.LoginChecker;
import com.marv42.ebt.newnote.ThisApp;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.android.ContributesAndroidInjector;

@Module(subcomponents = EbtNewNoteComponent.class)
abstract class ApplicationModule {
    @ActivityScope
    @ContributesAndroidInjector(modules = EbtNewNoteModule.class)
    abstract EbtNewNote contributeEbtNewNoteInjector();

//    @Provides
//    static Application provideApplication(@NonNull ThisApp application) {
//        return application;
//    }

//    @Singleton
//    abstract Context bindContext(AndroidApplication application);
    // prefer static over virtual: https://developer.android.com/training/articles/perf-tips.html#PreferStatic
    @Provides
    @Singleton
    static Context provideContext(@NonNull ThisApp application) {
        return application.getApplicationContext();
    }

    @Provides
    @Singleton
    static SharedPreferences provideSharedPreferences(@NonNull Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    @Provides
    @Singleton
    static ApiCaller provideApiCaller(@NonNull Context context, @NonNull SharedPreferences sharedPreferences) {
        return new ApiCaller(context, sharedPreferences);
    }

    @Provides
    @Singleton
    static LoginChecker provideLoginChecker(Context context, ApiCaller apiCaller) {
        return new LoginChecker(context, apiCaller);
    }
}
