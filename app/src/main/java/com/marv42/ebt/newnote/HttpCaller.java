/*
 Copyright (c) 2010 - 2026 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote;

import com.marv42.ebt.newnote.exceptions.HttpCallException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.SocketTimeoutException;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class HttpCaller {

    private String getServer(Request request) {
        return request.url().host();
    }

    public String call(Request request) throws HttpCallException {
        Call call = new OkHttpClient().newCall(request);
        return getBody(request, call);
    }

    @NotNull
    private String getBody(Request request, Call call) throws HttpCallException {
        try (Response response = call.execute()) {
            if (!response.isSuccessful())
                throw responseException(request, response);
            return response.body().string();
        } catch (SocketTimeoutException e) {
            throw new HttpCallException("R.string.error_no_connection " + getServer(request) + ":\n" +
                    e.getMessage(), getServer(request), 0, null, null, true);
        } catch (IOException e) {
            throw new HttpCallException("R.string.error_no_connection " + getServer(request) + ":\n" +
                    e.getMessage(), getServer(request), 0, null, null, true);
        }
    }

    @NotNull
    private HttpCallException responseException(Request request, Response response) {
        return new HttpCallException(errorMessage(request, response), getServer(request),
                response.code(), response.message(), parseApiErrorMessage(readBody(response)),
                isRetryable(response.code()));
    }

    private static boolean isRetryable(int responseCode) {
        return responseCode == 408 || responseCode == 429 || responseCode >= 500;
    }

    @NotNull
    private String errorMessage(Request request, Response response) {
        String reason = response.message();
        return "R.string.http_error " + getServer(request) + ", R.string.response_code: "
                + response.code() + (reason.isEmpty() ? "" : " " + reason);
    }

    @Nullable
    static String parseApiErrorMessage(@Nullable String body) {
        if (body == null || body.isEmpty())
            return null;
        try {
            JSONObject json = new JSONObject(body);
            JSONObject error = json.optJSONObject("error");
            String message = error == null ? json.optString("message") : error.optString("message");
            if (message.isEmpty())
                message = firstErrorMessage(json);
            message = toSingleLine(message);
            return message.isEmpty() ? null : message;
        } catch (JSONException e) {
            return null;
        }
    }

    private static String firstErrorMessage(JSONObject json) {
        JSONArray errors = json.optJSONArray("errors");
        if (errors == null || errors.length() == 0)
            return "";
        JSONObject firstError = errors.optJSONObject(0);
        return firstError == null ? "" : firstError.optString("message");
    }

    @Nullable
    private static String readBody(Response response) {
        try {
            ResponseBody body = response.body();
            return body.string();
        } catch (IOException e) {
            return null;
        }
    }

    @NotNull
    private static String toSingleLine(String message) {
        return message.trim().replaceAll("\\s+", " ");
    }
}
