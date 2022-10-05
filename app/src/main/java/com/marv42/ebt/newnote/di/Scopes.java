/*
 Copyright (c) 2010 - 2022 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote.di;

import java.lang.annotation.Retention;

import javax.inject.Scope;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Scope
@Retention(RUNTIME)
@interface ActivityScope {
}

@Scope
@Retention(RUNTIME)
@interface IntentServiceScope {
}

@Scope
@Retention(RUNTIME)
@interface SettingsScope {
}

@Scope
@Retention(RUNTIME)
@interface LicensesScope {
}

@Scope
@Retention(RUNTIME)
@interface SettingsFragmentScope {
}

@Scope
@Retention(RUNTIME)
@interface SubmitFragmentScope {
}

@Scope
@Retention(RUNTIME)
@interface ResultsFragmentScope {
}
