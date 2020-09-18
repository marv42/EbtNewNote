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
import com.marv42.ebt.newnote.LocationValues;
import com.marv42.ebt.newnote.R;
import com.marv42.ebt.newnote.exceptions.HttpCallException;
import com.marv42.ebt.newnote.exceptions.JsonObjectException;
import com.marv42.ebt.newnote.exceptions.NoIntentException;
import com.marv42.ebt.newnote.exceptions.NoLocationException;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import okhttp3.FormBody;
import okhttp3.Request;

import static com.marv42.ebt.newnote.BuildConfig.APPLICATION_ID;
import static com.marv42.ebt.newnote.ErrorMessage.ERROR;
import static com.marv42.ebt.newnote.JsonHelper.getJsonObject;
import static com.marv42.ebt.newnote.ThisApp.RESULT_CODE_ERROR;
import static com.marv42.ebt.newnote.ThisApp.RESULT_CODE_SUCCESS;

public class FetchAddressIntentService extends IntentService {
    public static final String TAG = FetchAddressIntentService.class.getSimpleName();
    public static final String RECEIVER = APPLICATION_ID + ".RECEIVER";
    public static final String RESULT_DATA_KEY = APPLICATION_ID + ".RESULT_DATA_KEY";
    public static final String LOCATION_DATA_EXTRA = APPLICATION_ID + ".LOCATION_DATA_EXTRA";

    private static final String GEOCODING_HOST = "geocode.arcgis.com";
    private static final String GEOCODING_URL = "https://" + GEOCODING_HOST + "/arcgis/rest/services/World/GeocodeServer/reverseGeocode";
    private static final String ELEMENT_ADDRESS = "address";

    private ResultReceiver receiver;
    private String result;
    private int resultCode = RESULT_CODE_ERROR;

    public FetchAddressIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        try {
            fetchAddress(intent);
        } catch (NoIntentException | NoLocationException | HttpCallException | JsonObjectException e) {
            result = e.getMessage();
        } finally {
            deliverResultToReceiver();
        }
    }

    private void fetchAddress(Intent intent) throws NoIntentException, NoLocationException, JsonObjectException, HttpCallException {
        getReceiver(intent);
        Location location = getLocation(intent);
        LocationValues locationValues = getLocationValues(location);
        result = new Gson().toJson(locationValues);
        resultCode = RESULT_CODE_SUCCESS;
    }

    private void getReceiver(Intent intent) throws NoIntentException {
        if (intent == null)
            throw new NoIntentException(ERROR + getString(R.string.internal_error));
        receiver = intent.getParcelableExtra(RECEIVER);
    }

    private Location getLocation(Intent intent) throws NoLocationException {
        Location l = intent.getParcelableExtra(LOCATION_DATA_EXTRA);
        if (l == null)
            throw new NoLocationException(ERROR + getString(R.string.location_none));
        return l;
    }

    private LocationValues getLocationValues(Location location) throws HttpCallException, JsonObjectException {
        String body = executeHttpCall(location);
        JSONObject jsonAddress = getAddress(body);
        String countryCode = jsonAddress.optString("CountryCode");
        String city = jsonAddress.optString("City");
        String postalCode = jsonAddress.optString("Postal");
        String countryName = new CountryCode().convert(countryCode);
        return new LocationValues(countryName, city, postalCode);
    }

    private String executeHttpCall(Location location) throws HttpCallException {
        FormBody formBody = getFormBody(location);
        Request request = new Request.Builder().url(GEOCODING_URL).post(formBody).build();
        String body = new HttpCaller().call(request);
        if (body.startsWith(ERROR))
            throw new HttpCallException(body);
        return body;
    }

    private JSONObject getAddress(String body) throws JsonObjectException {
        JSONObject json = getJsonObject(body);
        if (json == null || ! json.has(ELEMENT_ADDRESS))
            throw new JsonObjectException(ERROR + getString(R.string.server_error) + " " + GEOCODING_HOST + ": " + body);
        JSONObject jsonAddress = json.optJSONObject(ELEMENT_ADDRESS);
        if (jsonAddress == null)
            throw new JsonObjectException(ERROR + getString(R.string.internal_error) + " " + GEOCODING_HOST);
        return jsonAddress;
    }

    @NotNull
    private FormBody getFormBody(Location location) {
        FormBody.Builder formBodyBuilder = getFormBodyBuilder(location);
        return formBodyBuilder.build();
    }

    @NotNull
    private FormBody.Builder getFormBodyBuilder(Location location) {
        String latitude = String.valueOf(location.getLatitude());
        String longitude = String.valueOf(location.getLongitude());
        FormBody.Builder formBodyBuilder = new FormBody.Builder();
        formBodyBuilder.add("f", "json");
        formBodyBuilder.add("langCode", "EN");
        formBodyBuilder.add("location", longitude + "," + latitude);
        return formBodyBuilder;
    }

    private void deliverResultToReceiver() {
        Bundle bundle = new Bundle();
        bundle.putString(RESULT_DATA_KEY, result);
        receiver.send(resultCode, bundle);
    }
}
