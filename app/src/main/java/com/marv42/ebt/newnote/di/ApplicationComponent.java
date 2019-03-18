package com.marv42.ebt.newnote.di;

import com.marv42.ebt.newnote.ThisApp;

import javax.inject.Singleton;

import dagger.Component;
import dagger.android.AndroidInjector;
import dagger.android.support.AndroidSupportInjectionModule;

@Singleton
@Component(modules = {/*AndroidInjectionModule.class,*/ AndroidSupportInjectionModule.class,
        ApplicationModule.class})
public interface ApplicationComponent extends AndroidInjector<ThisApp> {
    @Component.Builder
    abstract class Builder extends AndroidInjector.Builder<ThisApp> {
    }
}
