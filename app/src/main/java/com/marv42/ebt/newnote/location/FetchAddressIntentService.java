package com.marv42.ebt.newnote.location;

import android.app.IntentService;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.marv42.ebt.newnote.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FetchAddressIntentService extends IntentService {
    public static final String TAG = "FetchAddressIntentService";
    public static final String PACKAGE_NAME = "com.marv42.ebt.newnote"; // TODO ThisApp.getPackageName()
    public static final String RECEIVER = PACKAGE_NAME + ".RECEIVER";
    public static final String RESULT_DATA_KEY = PACKAGE_NAME + ".RESULT_DATA_KEY";
    public static final String LOCATION_DATA_EXTRA = PACKAGE_NAME + ".LOCATION_DATA_EXTRA";
    public static final int SUCCESS_RESULT = 0;

    private static final int NUMBER_ADDRESSES = 5;

    private ResultReceiver mReceiver;

    public FetchAddressIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (intent == null) {
            deliverResultToReceiver(R.string.internal_error, "");
            return;
        }
        Location l = intent.getParcelableExtra(LOCATION_DATA_EXTRA);
        if (l == null) {
            deliverResultToReceiver(R.string.location_none, "");
            return;
        }
        mReceiver = intent.getParcelableExtra(RECEIVER);
        Geocoder geocoder = new Geocoder(this, Locale.US);
        List<Address> addresses = null;
        try {
            addresses = geocoder.getFromLocation(l.getLatitude(), l.getLongitude(), NUMBER_ADDRESSES);
        } catch (IOException ioException) {
            deliverResultToReceiver(R.string.location_geocoder_io_exception, "");
            return;
        } catch (IllegalArgumentException illegalArgumentException) {
            deliverResultToReceiver(R.string.location_geocoder_illegal_argument_exception, "");
            return;
        }
        if (addresses == null || addresses.size() == 0) {
            deliverResultToReceiver(R.string.location_no_address, "");
            return;
        }
        String[] previousLocation = new String[3];
        ArrayList<String[]> result = new ArrayList<String[]>();
        for (Address a : addresses) {
            if (a == null)
                continue;
            String countryName = a.getCountryName();
            String locality = a.getLocality();
            String postalCode = a.getPostalCode();
            if ((!TextUtils.isEmpty(countryName) && !countryName.equals(previousLocation[0])) ||
                    (!TextUtils.isEmpty(locality) && !locality.equals(previousLocation[1])) ||
                    (!TextUtils.isEmpty(postalCode) && !postalCode.equals(previousLocation[2]))) {
                previousLocation = new String[]{countryName, locality, postalCode};
                result.add(new String[]{countryName, locality, postalCode});
            }
        }
        deliverResultToReceiver(SUCCESS_RESULT, new Gson().toJson(result));
    }

    private void deliverResultToReceiver(int resultCode, String message) {
        Bundle bundle = new Bundle();
        bundle.putString(RESULT_DATA_KEY, message);
        mReceiver.send(resultCode, bundle);
    }
}
