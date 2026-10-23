/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xtc.xtcdiag.socket;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Small builder for the JSON payloads of the diag protocol.
 *
 * @hide
 */
public class XtcSocketJsonFormat {
    private static final String TAG = "XtcDiag-JsonFormat";

    private final JSONObject jsonObj = new JSONObject();
    private final JSONArray jsonArray = new JSONArray();

    public JSONObject build() {
        int length = jsonArray.length();
        if (length > 0) {
            try {
                jsonObj.put(ItemJsonKey.ITEM_KEY_DATA_LIST, jsonArray);
            } catch (Exception e) {
                Log.w(TAG, "build fail dataLen:" + length);
            }
        }
        return jsonObj;
    }

    public XtcSocketJsonFormat put(String key, int value) {
        try {
            jsonObj.put(key, value);
        } catch (Exception e) {
            Log.w(TAG, "put format fail key:" + key + " ,value:" + value);
        }
        return this;
    }

    public XtcSocketJsonFormat put(String key, Object value) {
        try {
            jsonObj.put(key, value);
        } catch (Exception e) {
            Log.w(TAG, "put format fail key:" + key + " ,value:" + value);
        }
        return this;
    }

    public XtcSocketJsonFormat put(String key, String value) {
        try {
            jsonObj.put(key, value);
        } catch (Exception e) {
            Log.w(TAG, "put format fail key:" + key + " ,value:" + value);
        }
        return this;
    }

    public XtcSocketJsonFormat put2Array(JSONObject object) {
        try {
            jsonArray.put(object);
        } catch (Exception e) {
            Log.w(TAG, "put2Array fail obj:" + object);
        }
        return this;
    }
}