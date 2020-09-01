package com.marv42.ebt.newnote;

import java.io.IOException;
import java.net.SocketTimeoutException;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import static com.marv42.ebt.newnote.ErrorMessage.ERROR;

public class HttpCaller {
    String getServer(Request request) {
        return request.url().host();
    }

    public String call(Request request) {
        Call call = new OkHttpClient().newCall(request);
        try (Response response = call.execute()) {
            if (!response.isSuccessful()) {
                return ERROR + "R.string.http_error " + getServer(request) + ", R.string.response_code: "
                        + String.valueOf(response.code());
            }
            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                return ERROR + "R.string.server_error " + getServer(request);
            }
            return responseBody.string();
        } catch (SocketTimeoutException e) {
            return ERROR + "R.string.error_no_connection: " + getServer(request) + ": " + e.getMessage();
        } catch (IOException e) {
            return ERROR + "R.string.internal_error: " + getServer(request) + ": " + e.getMessage();
        }
    }
}
