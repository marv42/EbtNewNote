/*
 Copyright (c) 2010 - 2022 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote.di;

import com.marv42.ebt.newnote.ResultsFragmentData;

import dagger.Module;
import dagger.Subcomponent;
import dagger.android.AndroidInjector;

@Module
abstract class ResultsDataFragmentModule {
}

@ResultsFragmentScope
@Subcomponent(modules = {ResultsDataFragmentModule.class})
public interface ResultsDataFragmentComponent extends AndroidInjector<ResultsFragmentData> {
    @Subcomponent.Factory
    abstract class Factory implements AndroidInjector.Factory<ResultsFragmentData> {
    }
}
