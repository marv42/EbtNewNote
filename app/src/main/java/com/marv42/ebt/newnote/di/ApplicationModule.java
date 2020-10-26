/*
 Copyright (c) 2010 - 2020 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote.di;

import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.marv42.ebt.newnote.ApiCaller;
import com.marv42.ebt.newnote.EbtNewNote;
import com.marv42.ebt.newnote.EncryptedPreferenceDataStore;
import com.marv42.ebt.newnote.SettingsActivity;
import com.marv42.ebt.newnote.SharedPreferencesHandler;
import com.marv42.ebt.newnote.ThisApp;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.android.ContributesAndroidInjector;

import static androidx.preference.PreferenceManager.getDefaultSharedPreferences;

@Module(subcomponents = {EbtNewNoteComponent.class, SettingsComponent.class})
abstract class ApplicationModule {

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
    static EncryptedPreferenceDataStore provideEncryptedPreferenceDataStore(@NonNull ThisApp app) {
        return new EncryptedPreferenceDataStore(app);
    }

    @Provides
    @Singleton
    static ApiCaller provideApiCaller(EncryptedPreferenceDataStore dataStore) {
        return new ApiCaller(dataStore);
    }

    @ActivityScope
    @ContributesAndroidInjector(modules = EbtNewNoteModule.class)
    abstract EbtNewNote contributeEbtNewNoteInjector();

    @SettingsScope
    @ContributesAndroidInjector(modules = SettingsModule.class)
    abstract SettingsActivity contributeSettingsActivityInjector();
}
