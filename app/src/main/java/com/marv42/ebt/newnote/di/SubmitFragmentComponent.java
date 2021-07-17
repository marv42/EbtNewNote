/*
 Copyright (c) 2010 - 2021 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote.di;

import android.app.Activity;

import androidx.annotation.NonNull;

import com.marv42.ebt.newnote.SubmitFragment;
import com.marv42.ebt.newnote.ThisApp;
import com.marv42.ebt.newnote.location.LocationButtonHandler;

import dagger.Module;
import dagger.Provides;
import dagger.Subcomponent;
import dagger.android.AndroidInjector;

@Module
abstract class SubmitFragmentModule {
    @Provides
    @SubmitFragmentScope
    static LocationButtonHandler provideLocationButtonHandler(@NonNull ThisApp app, @NonNull Activity activity) {
        return new LocationButtonHandler(app, activity);
    }
}

@SubmitFragmentScope
@Subcomponent(modules = {SubmitFragmentModule.class})
public interface SubmitFragmentComponent extends AndroidInjector<SubmitFragment> {
    @Subcomponent.Factory
    abstract class Factory implements AndroidInjector.Factory<SubmitFragment> {
    }
}
