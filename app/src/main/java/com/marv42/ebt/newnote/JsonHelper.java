/*
 Copyright (c) 2010 - 2022 Marvin Horter.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the GNU Public License v2.0
 which accompanies this distribution, and is available at
 http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.marv42.ebt.newnote;

import com.marv42.ebt.newnote.exceptions.NoJsonElementException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JsonHelper {

    private final JSONObject json;

    public static JSONObject getJsonObject(String key, String value) throws JSONException {
        return new JSONObject("{\"" + key + "\":\"" + value + "\"}");
    }

    public JsonHelper(JSONObject json) {
        this.json = json;
    }

    public <T> T getElement(Class<?> c, String element) throws NoJsonElementException {
        if (!json.has(element) || json.isNull(element))
            throw new NoJsonElementException("No JSON '" + element + "' element");
        if (c == Integer.class || c == int.class)
            return (T) (Integer) json.optInt(element);
        if (c == String.class)
            return (T) json.optString(element);
        if (c == JSONObject.class)
            return (T) json.optJSONObject(element);
        if (c == JSONArray.class) {
            T array = (T) json.optJSONArray(element);
            if (array == null)
                throw new NoJsonElementException("Empty JSON '" + element + "' array element");
            return array;
        }
        throw new IllegalArgumentException("Unhandled class " + c);
    }
}
