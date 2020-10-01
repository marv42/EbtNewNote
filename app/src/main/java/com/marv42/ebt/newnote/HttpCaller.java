/*
 Copyright (c) 2010 - 2020 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote;

import com.marv42.ebt.newnote.exceptions.HttpCallException;

import org.jetbrains.annotations.NotNull;

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
                throw new HttpCallException("R.string.http_error " + getServer(request) + ", R.string.response_code: "
                        + response.code());
            ResponseBody responseBody = response.body();
            if (responseBody == null)
                throw new HttpCallException("R.string.server_error " + getServer(request));
            return responseBody.string();
        } catch (SocketTimeoutException e) {
            throw new HttpCallException("R.string.error_no_connection " + getServer(request) + ":\n" + e.getMessage());
        } catch (IOException e) {
            throw new HttpCallException("R.string.internal_error, " + getServer(request) + ":\n" + e.getMessage());
        }
    }
}
