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

package com.xtc.secure.walle;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Reads the Walle channel metadata embedded in an APK's signing block.
 *
 * @hide
 */
public final class ChannelReader {
    public static final String CHANNEL_KEY = "channel";

    private ChannelReader() {
    }

    public static ChannelInfo get(File apkFile) {
        Map<String, String> map = getMap(apkFile);
        if (map == null) {
            return null;
        }
        String channel = map.get(CHANNEL_KEY);
        map.remove(CHANNEL_KEY);
        return new ChannelInfo(channel, map);
    }

    public static Map<String, String> getMap(File apkFile) {
        try {
            String raw = getRaw(apkFile);
            if (raw == null) {
                return null;
            }
            JSONObject json = new JSONObject(raw);
            Iterator<String> keys = json.keys();
            Map<String, String> map = new HashMap<>();
            while (keys.hasNext()) {
                String key = keys.next();
                map.put(key, json.getString(key));
            }
            return map;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String getRaw(File apkFile) {
        try {
            return PayloadReader.getString(apkFile, ApkUtil.APK_CHANNEL_BLOCK_ID);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}