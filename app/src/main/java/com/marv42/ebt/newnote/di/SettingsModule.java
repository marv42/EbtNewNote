package com.marv42.ebt.newnote.di;

import com.marv42.ebt.newnote.SettingsActivity;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;

@Module(subcomponents = {SettingsFragmentComponent.class})
abstract class SettingsModule {
    @SettingsFragmentScope
    @ContributesAndroidInjector(modules = SettingsFragmentModule.class)
    abstract SettingsActivity.SettingsFragment contributeSettingsFragmentInjector();
}
