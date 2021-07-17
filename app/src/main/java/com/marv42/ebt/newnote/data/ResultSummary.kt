/*
 Copyright (c) 2010 - 2021 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */
package com.marv42.ebt.newnote.data

class ResultSummary(@JvmField val hits: Int, @JvmField val successful: Int, @JvmField val failed: Int) {

    val total: Int
        get() = hits + successful + failed
}