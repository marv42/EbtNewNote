package com.marv42.ebt.newnote.location;

import android.app.IntentService;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.ResultReceiver;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.marv42.ebt.newnote.R;

import org.json.JSONObject;

import java.io.IOException;
import java.net.SocketTimeoutException;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import static com.marv42.ebt.newnote.JsonHelper.getJsonObject;

public class FetchAddressIntentService extends IntentService {
    public static final String TAG = "FetchAddressIntentService";
    public static final String PACKAGE_NAME = "com.marv42.ebt.newnote"; // TODO ThisApp.getPackageName()
    public static final String RECEIVER = PACKAGE_NAME + ".RECEIVER";
    public static final String RESULT_DATA_KEY = PACKAGE_NAME + ".RESULT_DATA_KEY";
    public static final String LOCATION_DATA_EXTRA = PACKAGE_NAME + ".LOCATION_DATA_EXTRA";
    public static final int SUCCESS_RESULT = 0;

    private static final String GEOCODING_URL = "https://geocode.arcgis.com/arcgis/rest/services/World/GeocodeServer/reverseGeocode";
    private static final String COUNTRIES_URL = "https://restcountries.eu/rest/v2/alpha/";
    private static final String ELEMENT_ADDRESS = "address";
    private static final String ELEMENT_NAME = "name";

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
        String latitude = String.valueOf(l.getLatitude());
        String longitude = String.valueOf(l.getLongitude());
        FormBody.Builder formBodyBuilder = new FormBody.Builder();
        formBodyBuilder.add("f", "json");
        formBodyBuilder.add("langCode", "EN");
        formBodyBuilder.add("location", longitude + "," + latitude);
        FormBody formBody = formBodyBuilder.build();
        Request request = new Request.Builder().url(GEOCODING_URL).post(formBody).build();
        Call call = new OkHttpClient().newCall(request);
        try (Response response = call.execute()) {
            if (!response.isSuccessful()) {
                deliverResultToReceiver(R.string.http_error, String.valueOf(response.code())); // TODO which server?
                return;
            }
            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                deliverResultToReceiver(R.string.server_error, "");
                return;
            }
            String body = responseBody.string();
            JSONObject json = getJsonObject(body);
            if (json == null || ! json.has(ELEMENT_ADDRESS)) {
                deliverResultToReceiver(R.string.server_error, body);
                return;
            }
            JSONObject jsonAddress = json.optJSONObject(ELEMENT_ADDRESS);
            if (jsonAddress == null) {
                deliverResultToReceiver(R.string.internal_error, "");
                return;
            }
            String countryCode = jsonAddress.optString("CountryCode");
            String locality = jsonAddress.optString("City");
            String postalCode = jsonAddress.optString("Postal");
            String countryName = getCountryName(countryCode);
            String[] result = new String[]{countryName, locality, postalCode};
            deliverResultToReceiver(SUCCESS_RESULT, new Gson().toJson(result)); // TODO .toJson(jsonAddress)
        } catch (SocketTimeoutException e) {
            deliverResultToReceiver(R.string.error_no_connection, "");
        } catch (IOException e) {
            deliverResultToReceiver(R.string.internal_error, "");
        }
    }

    private String getCountryName(String countryCode) {
        Request request = new Request.Builder().url(COUNTRIES_URL + countryCode).build();
        Call call = new OkHttpClient().newCall(request);
        try (Response response = call.execute()) {
            if (!response.isSuccessful()) {
                return ""; //TODO R.string.http_error;
            }
            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                return ""; // TODO R.string.server_error
            }
            String body = responseBody.string();
            JSONObject json = getJsonObject(body);
            if (json == null || ! json.has(ELEMENT_NAME)) {
                return ""; // TODO R.string.server_error, body
            }
            return json.optString(ELEMENT_NAME);
        } catch (SocketTimeoutException e) {
            return ""; // TODO R.string.error_no_connection;
        } catch (IOException e) {
            return ""; // TODO R.string.internal_error;
        }
    }

    private void deliverResultToReceiver(int resultCode, String message) {
        Bundle bundle = new Bundle();
        bundle.putString(RESULT_DATA_KEY, message);
        mReceiver.send(resultCode, bundle);
    }
}
