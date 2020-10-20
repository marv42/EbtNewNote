/*
 Copyright (c) 2010 - 2020 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class SubmitViewModel extends AndroidViewModel {

    private MutableLiveData<String> country;
    private MutableLiveData<String> city;
    private MutableLiveData<String> postalCode;
    private MutableLiveData<String> denomination;
    private MutableLiveData<String> shortCode;
    private MutableLiveData<String> serialNumber;
    private MutableLiveData<String> comment;

    public SubmitViewModel(@NonNull Application application) {
        super(application);
    }

    public void setCountry(String country) {
        this.country.setValue(country);
    }

    public LiveData<String> getCountry() {
        if (country == null)
            country = new MutableLiveData<>();
        return country;
    }

    public void setCity(String city) {
        this.city.setValue(city);
    }

    public LiveData<String> getCity() {
        if (city == null)
            city = new MutableLiveData<>();
        return city;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode.setValue(postalCode);
    }

    public LiveData<String> getPostalCode() {
        if (postalCode == null)
            postalCode = new MutableLiveData<>();
        return postalCode;
    }

    public void setDenomination(String denomination) {
        this.denomination.setValue(denomination);
    }

    public LiveData<String> getDenomination() {
        if (denomination == null)
            denomination = new MutableLiveData<>();
        return denomination;
    }

    public void setShortCode(String shortCode) {
        this.shortCode.setValue(shortCode);
    }

    public LiveData<String> getShortCode() {
        if (shortCode == null)
            shortCode = new MutableLiveData<>();
        return shortCode;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber.setValue(serialNumber);
    }

    public LiveData<String> getSerialNumber() {
        if (serialNumber == null)
            serialNumber = new MutableLiveData<>();
        return serialNumber;
    }

    public void setComment(String comment) {
        this.comment.setValue(comment);
    }

    public LiveData<String> getComment() {
        if (comment == null)
            comment = new MutableLiveData<>();
        return comment;
    }
}
