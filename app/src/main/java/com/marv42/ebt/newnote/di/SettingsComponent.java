package com.marv42.ebt.newnote.di;

import com.marv42.ebt.newnote.SettingsActivity;

import dagger.Subcomponent;
import dagger.android.AndroidInjector;

@SettingsScope
@Subcomponent(modules = {SettingsModule.class})
public interface SettingsComponent extends AndroidInjector<SettingsActivity> {
    @Subcomponent.Factory
    abstract class Factory implements AndroidInjector.Factory<SettingsActivity> {
    }
}
