/*
 Copyright (c) 2010 - 2022 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote.ui;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.marv42.ebt.newnote.SubmissionResult;

import java.util.ArrayList;

public class ResultsViewModel extends ViewModel {

    private final MutableLiveData<ArrayList<SubmissionResult>> results = new MutableLiveData<>();

    public ResultsViewModel() {
        super();
    }

    public MutableLiveData<ArrayList<SubmissionResult>> getResults() {
        return results;
    }

    public void setResults(ArrayList<SubmissionResult> aResults) {
        results.setValue(aResults);
    }
}
