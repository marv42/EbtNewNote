/*
 Copyright (c) 2010 - 2022 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.marv42.ebt.newnote.data.NoteData;

public class SubmitViewModel extends AndroidViewModel {

    private final MutableLiveData<String> country = new MutableLiveData<>();;
    private final MutableLiveData<String> city = new MutableLiveData<>();;
    private final MutableLiveData<String> postalCode = new MutableLiveData<>();;
    private final MutableLiveData<String> denomination = new MutableLiveData<>();;
    private final MutableLiveData<String> shortCode = new MutableLiveData<>();;
    private final MutableLiveData<String> serialNumber = new MutableLiveData<>();;
    private final MutableLiveData<String> comment = new MutableLiveData<>();;

    public SubmitViewModel(@NonNull Application application) {
        super(application);
    }

    public void setNoteData(NoteData noteData)
    {
        setCountry(noteData.mCountry);
        setCity(noteData.mCity);
        setPostalCode(noteData.mPostalCode);
        setDenomination(noteData.mDenomination);
        setShortCode(noteData.mShortCode);
        setSerialNumber(noteData.mSerialNumber);
        setComment(noteData.mComment);
    }

    public void setCountry(String aCountry) {
        country.setValue(aCountry);
    }

    public LiveData<String> getCountry() {
        return country;
    }

    public void setCity(String aCity) {
        city.setValue(aCity);
    }

    public LiveData<String> getCity() {
        return city;
    }

    public void setPostalCode(String aPostalCode) {
        postalCode.setValue(aPostalCode);
    }

    public LiveData<String> getPostalCode() {
        return postalCode;
    }

    public void setDenomination(String aDomination) {
        denomination.setValue(aDomination);
    }

    public LiveData<String> getDenomination() {
        return denomination;
    }

    public void setShortCode(String aShortCode) {
        shortCode.setValue(aShortCode);
    }

    public LiveData<String> getShortCode() {
        return shortCode;
    }

    public void setSerialNumber(String aSerialNumber) {
        serialNumber.setValue(aSerialNumber);
    }

    public LiveData<String> getSerialNumber() {
        return serialNumber;
    }

    public void setComment(String aComment) {
        comment.setValue(aComment);
    }

    public LiveData<String> getComment() {
        return comment;
    }
}
