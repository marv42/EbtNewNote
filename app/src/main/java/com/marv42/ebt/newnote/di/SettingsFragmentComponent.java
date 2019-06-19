package com.marv42.ebt.newnote.di;

import com.marv42.ebt.newnote.SettingsActivity;

import dagger.Module;
import dagger.Subcomponent;
import dagger.android.AndroidInjector;

@Module
abstract class SettingsFragmentModule {
}

@SettingsFragmentScope
@Subcomponent(modules = {SettingsFragmentModule.class})
public interface SettingsFragmentComponent extends AndroidInjector<SettingsActivity.SettingsFragment> {
    @Subcomponent.Factory
    abstract class Factory implements AndroidInjector.Factory<SettingsActivity.SettingsFragment> {
    }
}
