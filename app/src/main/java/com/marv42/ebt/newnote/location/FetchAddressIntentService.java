package com.marv42.ebt.newnote.location;

import android.app.IntentService;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.ResultReceiver;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.marv42.ebt.newnote.CountryCode;
import com.marv42.ebt.newnote.HttpCaller;

import org.json.JSONObject;

import okhttp3.FormBody;
import okhttp3.Request;

import static com.marv42.ebt.newnote.ErrorMessage.ERROR;
import static com.marv42.ebt.newnote.JsonHelper.getJsonObject;
import static com.marv42.ebt.newnote.ThisApp.RESULT_CODE_ERROR;
import static com.marv42.ebt.newnote.ThisApp.RESULT_CODE_SUCCESS;

public class FetchAddressIntentService extends IntentService {
    public static final String TAG = "FetchAddressIntentService";
    public static final String PACKAGE_NAME = "com.marv42.ebt.newnote"; // TODO ThisApp.getPackageName()
    public static final String RECEIVER = PACKAGE_NAME + ".RECEIVER";
    public static final String RESULT_DATA_KEY = PACKAGE_NAME + ".RESULT_DATA_KEY";
    public static final String LOCATION_DATA_EXTRA = PACKAGE_NAME + ".LOCATION_DATA_EXTRA";

    private static final String GEOCODING_URL = "https://geocode.arcgis.com/arcgis/rest/services/World/GeocodeServer/reverseGeocode";
    private static final String ELEMENT_ADDRESS = "address";

    private ResultReceiver mReceiver;

    public FetchAddressIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (intent == null) {
            deliverResultToReceiver(RESULT_CODE_ERROR, ERROR + "R.string.internal_error");
            return;
        }
        Location l = intent.getParcelableExtra(LOCATION_DATA_EXTRA);
        if (l == null) {
            deliverResultToReceiver(RESULT_CODE_ERROR, ERROR + "R.string.location_none");
            return;
        }
        mReceiver = intent.getParcelableExtra(RECEIVER);
        String latitude = String.valueOf(l.getLatitude());
        String longitude = String.valueOf(l.getLongitude());
        FormBody.Builder formBodyBuilder = new FormBody.Builder();
        formBodyBuilder.add("f", "json");
        formBodyBuilder.add("langCode", "EN");
        formBodyBuilder.add("location", longitude + "," + latitude);
        FormBody formBody = formBodyBuilder.build();
        Request request = new Request.Builder().url(GEOCODING_URL).post(formBody).build();
        String body = new HttpCaller().call(request);
        if (body.startsWith(ERROR)) {
            deliverResultToReceiver(RESULT_CODE_ERROR, body);
            return;
        }
        JSONObject json = getJsonObject(body);
        if (json == null || ! json.has(ELEMENT_ADDRESS)) {
            deliverResultToReceiver(RESULT_CODE_ERROR, ERROR + "R.string.server_error "
                    + request.url().host() + ": " + body);
            return;
        }
        JSONObject jsonAddress = json.optJSONObject(ELEMENT_ADDRESS);
        if (jsonAddress == null) {
            deliverResultToReceiver(RESULT_CODE_ERROR, ERROR + "R.string.internal_error "
                    + request.url().host());
            return;
        }
        String countryCode = jsonAddress.optString("CountryCode");
        String locality = jsonAddress.optString("City");
        String postalCode = jsonAddress.optString("Postal");
        String countryName = new CountryCode().convert(countryCode);
        String[] result = new String[]{countryName, locality, postalCode};
        deliverResultToReceiver(RESULT_CODE_SUCCESS, new Gson().toJson(result)); // TODO .toJson(jsonAddress)
    }

    private void deliverResultToReceiver(int resultCode, String message) {
        Bundle bundle = new Bundle();
        bundle.putString(RESULT_DATA_KEY, message);
        mReceiver.send(resultCode, bundle);
    }
}
