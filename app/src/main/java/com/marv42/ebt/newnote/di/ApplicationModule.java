package com.marv42.ebt.newnote.di;

import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.marv42.ebt.newnote.ApiCaller;
import com.marv42.ebt.newnote.EbtNewNote;
import com.marv42.ebt.newnote.EncryptedSharedPreferencesProvider;
import com.marv42.ebt.newnote.NoteDataSubmitter;
import com.marv42.ebt.newnote.SettingsActivity;
import com.marv42.ebt.newnote.SharedPreferencesHandler;
import com.marv42.ebt.newnote.SubmissionResults;
import com.marv42.ebt.newnote.ThisApp;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.android.ContributesAndroidInjector;

import static androidx.preference.PreferenceManager.getDefaultSharedPreferences;

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
        return getDefaultSharedPreferences(app);
    }

    @Provides
    @Singleton
    static SharedPreferencesHandler provideSharedPreferencesHandler(
            @NonNull ThisApp app, @NonNull SharedPreferences sharedPreferences) {
        return new SharedPreferencesHandler(app, sharedPreferences);
    }

    @Provides
    @Singleton
    static EncryptedSharedPreferencesProvider provideEncryptedSharedPreferencesProvider (@NonNull ThisApp app) {
        return new EncryptedSharedPreferencesProvider(app);
    }

    @Provides
    @Singleton
    static ApiCaller provideApiCaller(@NonNull ThisApp app, @NonNull EncryptedSharedPreferencesProvider encryptedSharedPreferencesProvider) {
        return new ApiCaller(app, encryptedSharedPreferencesProvider);
    }

    @Provides
    @Singleton
    static SubmissionResults provideSubmissionResults(@NonNull ThisApp app) {
        return new SubmissionResults(app);
    }

    @Provides
    @Singleton
    static NoteDataSubmitter provideNoteDataSubmitter(
            @NonNull ThisApp app, @NonNull ApiCaller apiCaller, @NonNull SubmissionResults submissionResults) {
        return new NoteDataSubmitter(app, apiCaller, submissionResults);
    }

//    @Provides
//    @Singleton
//    static LoginChecker provideLoginChecker(@NonNull ThisApp app, @NonNull ApiCaller apiCaller,
//            @NonNull SharedPreferencesHandler sharedPreferencesHandler) {
//        return new LoginChecker(app, apiCaller, sharedPreferencesHandler);
//    }
}
