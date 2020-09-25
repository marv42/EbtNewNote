/*
 Copyright (c) 2010 - 2020 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote;

class LoginInfo {
    final String sessionId;
    final String userName;
    final String myCountry;
    final String myCity;
    final String myZip;

    public LoginInfo(final String sessionId, final String userName,
                     final String myCountry, final String myCity, final String myZip) {
        this.sessionId = sessionId;
        this.userName = userName;
        this.myCountry = myCountry;
        this.myCity = myCity;
        this.myZip = myZip;
    }

    public LoginInfo() {
        sessionId = "";
        userName = "";
        myCountry = "";
        myCity = "";
        myZip = "";
    }
}
