package com.marv42.ebt.newnote.di;

import com.marv42.ebt.newnote.SubmitFragment;

import dagger.Module;
import dagger.Subcomponent;
import dagger.android.AndroidInjector;

@Module
abstract class SubmitFragmentModule {
}

@FragmentScope
@Subcomponent(modules = {SubmitFragmentModule.class})
public interface SubmitFragmentComponent extends AndroidInjector<SubmitFragment> {
    @Subcomponent.Factory
    abstract class Factory implements AndroidInjector.Factory<SubmitFragment> {
    }
}
