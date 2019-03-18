package com.marv42.ebt.newnote.di;

import com.marv42.ebt.newnote.SubmittedFragment;

import dagger.Module;
import dagger.Subcomponent;
import dagger.android.AndroidInjector;

@Module
abstract class SubmittedFragmentModule {
}

@FragmentScope
@Subcomponent(modules = {SubmittedFragmentModule.class})
public interface SubmittedFragmentComponent extends AndroidInjector<SubmittedFragment> {
    @Subcomponent.Builder
    abstract class Builder extends AndroidInjector.Builder<SubmittedFragment> {
    }
}
