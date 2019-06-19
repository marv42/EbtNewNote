/*******************************************************************************
 * Copyright (c) 2010 marvin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Contributors:
 *     marvin - initial API and implementation
 ******************************************************************************/

package com.marv42.ebt.newnote;

import android.content.Context;

import com.marv42.ebt.newnote.di.DaggerApplicationComponent;

import org.acra.ACRA;
import org.acra.annotation.AcraCore;
import org.acra.annotation.AcraMailSender;
import org.acra.annotation.AcraToast;

import dagger.android.AndroidInjector;
import dagger.android.support.DaggerApplication;

@AcraCore(buildConfigClass = BuildConfig.class)
@AcraMailSender(mailTo = "marv42+acra@gmail.com")
@AcraToast(resText = R.string.crash_text)
public class ThisApp extends DaggerApplication {
    @Override
    protected AndroidInjector<? extends DaggerApplication> applicationInjector() {
        return DaggerApplicationComponent.factory().create(this);
    }

//    @Override
//    public void onCreate() {
//        super.onCreate();
//    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
//        MultiDex.install(this);
        ACRA.init(this);
    }
}
