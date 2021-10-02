/*
 Copyright (c) 2010 - 2021 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote.di;

import static androidx.preference.PreferenceManager.getDefaultSharedPreferences;

import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.marv42.ebt.newnote.ApiCaller;
import com.marv42.ebt.newnote.EbtNewNote;
import com.marv42.ebt.newnote.ThisApp;
import com.marv42.ebt.newnote.location.FetchAddressIntentService;
import com.marv42.ebt.newnote.preferences.EncryptedPreferenceDataStore;
import com.marv42.ebt.newnote.preferences.SettingsActivity;
import com.marv42.ebt.newnote.preferences.SharedPreferencesHandler;

import javax.inject.Singleton;

import dagger.Component;
import dagger.Module;
import dagger.Provides;
import dagger.android.AndroidInjector;
import dagger.android.ContributesAndroidInjector;
import dagger.android.support.AndroidSupportInjectionModule;

@Module(subcomponents = {EbtNewNoteComponent.class, FetchAddressIntentServiceComponent.class,
        SettingsComponent.class})
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

    @IntentServiceScope
    @ContributesAndroidInjector(modules = FetchAddressIntentServiceModule.class)
    abstract FetchAddressIntentService contributeFetchAddressIntentServiceInjector();

    @SettingsScope
    @ContributesAndroidInjector(modules = SettingsModule.class)
    abstract SettingsActivity contributeSettingsActivityInjector();
}

@Singleton
@Component(modules = {AndroidSupportInjectionModule.class, ApplicationModule.class})
public interface ApplicationComponent extends AndroidInjector<ThisApp> {
    @Component.Factory
    abstract class Factory implements AndroidInjector.Factory<ThisApp> {
    }
}
