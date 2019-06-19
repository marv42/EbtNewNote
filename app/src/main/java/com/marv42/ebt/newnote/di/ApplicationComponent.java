package com.marv42.ebt.newnote.di;

import com.marv42.ebt.newnote.ThisApp;

import javax.inject.Singleton;

import dagger.Component;
import dagger.android.AndroidInjector;
import dagger.android.support.AndroidSupportInjectionModule;

@Singleton
@Component(modules = {AndroidSupportInjectionModule.class, ApplicationModule.class})
public interface ApplicationComponent extends AndroidInjector<ThisApp> {
    @Component.Factory
    abstract class Factory implements AndroidInjector.Factory<ThisApp> {
    }
}
