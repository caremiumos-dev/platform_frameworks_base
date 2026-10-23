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

package com.android.server.policy.hall;

import android.content.Context;
import android.content.res.Resources;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.util.Log;

import com.android.server.xgseserver.xss.HttpConfigOptions;
import com.xtc.utils.HallUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Per-model configuration for the hall animation views and sounds. The device
 * configuration is read from {@code flip_config/device_config.json}.
 *
 * @hide
 */
public class HallDeviceManager {
    private static final String TAG = "SysHallDeviceManager";

    private int animIndex;
    private String animListName;
    private String closeClass;
    private String openClass;
    private int soundIndex;
    private String soundListName;

    private static class Holder {
        private static final HallDeviceManager sInstance = new HallDeviceManager();
    }

    private HallDeviceManager() {
        this.animListName = "hall_anim_list";
        this.soundListName = "hall_sound_list";
        this.openClass = "com.android.server.policy.hall.HallOpenLottieView";
        this.closeClass = "com.android.server.policy.hall.HallLottieView";
        Log.d(TAG, "HallDeviceManager constructor");
    }

    private void getColorListData(int color, JSONArray colorList) throws JSONException {
        for (int i = 0; i < colorList.length(); i++) {
            JSONObject object = colorList.getJSONObject(i);
            if (color == object.getInt("value")) {
                this.animIndex = object.getInt("animIndex");
                this.soundIndex = object.getInt("soundIndex");
                return;
            }
        }
    }

    private static String[] getStringArray(Context context, String name) {
        Resources resources = context.getResources();
        int identifier = resources.getIdentifier(name, "array", context.getPackageName());
        if (identifier > 0) {
            return resources.getStringArray(identifier);
        }
        Log.e(TAG, "get array list error");
        return new String[0];
    }

    public static HallDeviceManager instance() {
        return Holder.sInstance;
    }

    public HallAnimationView createView(Context context, boolean open) {
        String className = open ? this.openClass : this.closeClass;
        try {
            return (HallAnimationView) context.getClassLoader().loadClass(className)
                    .getConstructor(Context.class).newInstance(context);
        } catch (Throwable t) {
            Log.w(TAG, "create instance err:" + className, t);
            return open ? new HallOpenLottieView(context) : new HallLottieView(context);
        }
    }

    public String[] getAnimationList(Context context) {
        return getStringArray(context, this.animListName);
    }

    public int getDefaultAnimIndex() {
        return this.animIndex;
    }

    public int getDefaultSoundIndex() {
        return this.soundIndex;
    }

    public String[] getSoundList(Context context) {
        return getStringArray(context, this.soundListName);
    }

    public void init(Context context) {
        long start = SystemClock.elapsedRealtime();
        int color = SystemProperties.getInt("ro.xtcwatch.color", 0);
        try {
            InputStream inputStream = context.getAssets().open("flip_config/device_config.json");
            StringBuilder builder = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            while (true) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                builder.append(line);
            }
            reader.close();
            JSONArray array = new JSONArray(builder.toString());
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.getJSONObject(i);
                if (HallUtil.INNER_MODEL.equals(object.getString(HttpConfigOptions.HeaderKey.MODEL))) {
                    this.openClass = object.getString("openClass");
                    this.closeClass = object.getString("closeClass");
                    this.animListName = object.getString("animListName");
                    this.soundListName = object.getString("soundListName");
                    getColorListData(color, object.getJSONArray("colorList"));
                    break;
                }
            }
            Log.d(TAG, "color:" + color + " anim:" + this.animIndex + " sound:" + this.soundIndex
                    + " cost:" + (SystemClock.elapsedRealtime() - start));
            Log.d(TAG, "open:" + this.openClass + " close:" + this.closeClass
                    + " listName:" + this.animListName);
        } catch (Throwable t) {
            Log.w(TAG, "error get config", t);
        }
    }
}