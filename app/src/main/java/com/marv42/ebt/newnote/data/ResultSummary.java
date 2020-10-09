/*
 Copyright (c) 2010 - 2020 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote.data;

public class ResultSummary {
    public final int hits;
    public final int successful;
    public final int failed;

    public ResultSummary(final int hits, final int successful, final int failed) {
        this.hits = hits;
        this.successful = successful;
        this.failed = failed;
    }
}
