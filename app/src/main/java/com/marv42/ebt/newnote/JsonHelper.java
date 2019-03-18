package com.marv42.ebt.newnote;

import org.json.JSONException;
import org.json.JSONObject;

public class JsonHelper {
    public static JSONObject getJsonObject(String key, String value) {
        return getJsonObject("{\"" + key + "\":\"" + value + "\"}");
    }

    public static JSONObject getJsonObject(String s) {
        JSONObject jsonObject = null;
        try {
            jsonObject = new JSONObject(s);
        } catch (JSONException ignored) {
        }
        return jsonObject;
    }
}
