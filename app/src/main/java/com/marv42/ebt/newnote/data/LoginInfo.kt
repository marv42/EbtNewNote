/*
 Copyright (c) 2010 - 2022 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */
package com.marv42.ebt.newnote.data

class LoginInfo {

    @JvmField
    val sessionId: String
    @JvmField
    val userName: String
    @JvmField
    val locationValues: LocationValues

    constructor(sessionId: String, userName: String, locationValues: LocationValues) {
        this.sessionId = sessionId
        this.userName = userName
        this.locationValues = locationValues
    }

    constructor() {
        sessionId = ""
        userName = ""
        locationValues = LocationValues()
    }
}