package com.marv42.ebt.newnote.di;

import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceDataStore;

import com.marv42.ebt.newnote.ApiCaller;
import com.marv42.ebt.newnote.EbtNewNote;
import com.marv42.ebt.newnote.EncryptedPreferenceDataStore;
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
    static EncryptedPreferenceDataStore provideEncryptedPreferenceDataStore (@NonNull ThisApp app) {
        return new EncryptedPreferenceDataStore(app);
    }

    @Provides
    @Singleton
    static ApiCaller provideApiCaller(@NonNull ThisApp app, EncryptedPreferenceDataStore dataStore) {
        return new ApiCaller(app, dataStore);
    }

    @Provides
    @Singleton
    static SubmissionResults provideSubmissionResults(
            @NonNull ThisApp app, @NonNull EncryptedPreferenceDataStore dataStore) {
        return new SubmissionResults(app, dataStore);
    }

    @Provides
    @Singleton
    static NoteDataSubmitter provideNoteDataSubmitter(
            @NonNull ThisApp app, @NonNull ApiCaller apiCaller,
            @NonNull SubmissionResults submissionResults, @NonNull EncryptedPreferenceDataStore dataStore) {
        return new NoteDataSubmitter(app, apiCaller, submissionResults, dataStore);
    }

//    @Provides
//    @Singleton
//    static LoginChecker provideLoginChecker(@NonNull ThisApp app, @NonNull ApiCaller apiCaller,
//            @NonNull SharedPreferencesHandler sharedPreferencesHandler) {
//        return new LoginChecker(app, apiCaller, sharedPreferencesHandler);
//    }
}
