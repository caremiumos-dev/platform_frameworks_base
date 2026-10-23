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

package com.xtc.utils;

import android.content.Context;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Reads per-watch-color customization (default anim/sound indices and
 * ringtone/notification names) from the bundled flip_config/device_config.json.
 *
 * @hide
 */
public class CustomEditionUtil {
    private static final String TAG = "CustomEditionUtil";

    private static CustomEditionUtil sInstance;

    private final String DEFAULT_RINGTONE_PROPERTY_PREFIX = "ro.config.";

    private int animIndex;
    private int soundIndex;
    private String ringtoneName;
    private String notificationName;
    private boolean initialized;

    private CustomEditionUtil() {
    }

    public static CustomEditionUtil instance() {
        if (sInstance == null) {
            sInstance = new CustomEditionUtil();
        }
        return sInstance;
    }

    private void getColorListData(int color, JSONArray colorList) throws JSONException {
        for (int i = 0; i < colorList.length(); i++) {
            JSONObject colorData = colorList.getJSONObject(i);
            if (color == colorData.getInt("value")) {
                animIndex = colorData.getInt("animIndex");
                soundIndex = colorData.getInt("soundIndex");
                ringtoneName = colorData.optString(Settings.System.RINGTONE, ringtoneName);
                notificationName = colorData.optString("notification", notificationName);
                return;
            }
        }
    }

    public void init(Context context) {
        if (initialized) {
            return;
        }
        long start = SystemClock.elapsedRealtime();
        ringtoneName = SystemProperties.get(DEFAULT_RINGTONE_PROPERTY_PREFIX + "ringtone");
        notificationName =
                SystemProperties.get(DEFAULT_RINGTONE_PROPERTY_PREFIX + "notification_sound");
        int color = SystemProperties.getInt(HallUtil.WATCH_COLOR, 0);
        try {
            InputStream input = context.getAssets().open("flip_config/device_config.json");
            StringBuilder builder = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            reader.close();
            JSONArray config = new JSONArray(builder.toString());
            for (int i = 0; i < config.length(); i++) {
                JSONObject model = config.getJSONObject(i);
                if (HallUtil.INNER_MODEL.equals(model.getString("model"))) {
                    getColorListData(color, model.getJSONArray("colorList"));
                    break;
                }
            }
            Log.d(TAG, "color:" + color + " anim:" + animIndex + " sound:" + soundIndex
                    + " cost:" + (SystemClock.elapsedRealtime() - start));
            Log.d(TAG, "ringtone:" + ringtoneName + " notification:" + notificationName);
        } catch (Throwable t) {
            Log.w(TAG, "error get config", t);
        }
        initialized = true;
    }

    public int getDefaultAnimIndex() {
        return animIndex;
    }

    public int getDefaultSoundIndex() {
        return soundIndex;
    }

    public String getRingtoneName() {
        return ringtoneName;
    }

    public String getNotificationName() {
        return notificationName;
    }
}