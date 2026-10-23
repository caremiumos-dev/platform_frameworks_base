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

package com.xtc.behavior;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import java.util.LinkedHashMap;

/**
 * Helper around the native {@code xtc_behavior} message hub, converting native
 * key/value strings into behavior reports.
 *
 * @hide
 */
public class XtcBehaviorUtil {
    private static final boolean DEBUG = false;
    private static final String TAG = "XtcBehaviorUtil";

    static {
        System.loadLibrary("xtc_behavior");
    }

    private void logd(String message) {
    }

    private LinkedHashMap<String, String> str2Map(String value) {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        if (TextUtils.isEmpty(value)) {
            return map;
        }
        String[] elements = value.split(",");
        if (elements == null || elements.length == 0) {
            return map;
        }
        for (String element : elements) {
            try {
                String[] pair = element.split("=");
                if (pair == null || pair.length == 0 || pair.length % 2 != 0) {
                    Log.e(TAG, "str2Map format error ! keyValues= " + value);
                    return map;
                }
                int i = 0;
                while (i < pair.length) {
                    map.put(pair[i], pair[i + 1]);
                    i += 2;
                }
            } catch (Exception e) {
                Log.w(TAG, "str2Map exception: " + e);
                map.clear();
            }
        }
        logd("str2Map hashMapValues = " + map);
        return map;
    }

    public native void listen_recv_msg();

    public void sendData(Context context, XtcBehaviorMsgInfo msgInfo) {
        logd("##### sendData context= " + context + " ,msgInfo= " + msgInfo);
        if (context == null || msgInfo == null) {
            Log.w(TAG, "sendData is return ! context= " + context + " ,msgInfo= " + msgInfo);
            return;
        }
        String module = msgInfo.module;
        String functionName = msgInfo.functionName;
        String keyValue = msgInfo.keyValue;
        if (TextUtils.isEmpty(functionName) || keyValue == null) {
            Log.w(TAG, "sendData is return ! functionName= " + functionName
                    + " ,values= " + keyValue);
        } else {
            new XtcBehavior.Builder(context, functionName, str2Map(keyValue))
                    .moduleName(module).packageName("sys_behavior").build().sendData();
        }
    }
}