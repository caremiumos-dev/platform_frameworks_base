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

package com.android.server.xtclog;

import android.content.Context;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Log;

import com.xtc.moduleswitch.ModuleSwitch;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Applies the vendor systrace collection switch.
 *
 * @hide
 */
public class XTCSystraceControl {
    static final String TAG = "XTCSystraceControl";
    public static final boolean DEBUG = true;

    private static final int SYSTRACE_CONTROL_MODULE_SWITCH = 3323;
    private static final int DEFAULT = 0;
    private static final int OPEN_VALUE = 1;
    private static final int CLOSE_VALUE = 2;

    private static final String PROP_ENABLE_ATRACE = "persist.service.enable_atrace";
    private static final String PROP_ENABLE_COLLECT = "persist.service.enable_collect";
    private static final String PROP_START_ATRACE = "persist.sys.bbk.bootatrace";
    private static final String PROP_STOP_ATRACE = "persist.sys.bbk.stopatrace";
    private static final String EXTRA_STATE = "state";

    private final Context mContext;

    public XTCSystraceControl(Context context) {
        this.mContext = context;
    }

    private void LOGD(String message) {
        if (!TextUtils.isEmpty(message)) {
            Log.d(TAG, message);
        }
    }

    private int getControlSwitchExtra() {
        String extra = ModuleSwitch.queryModuleSwitchExtraByInt(this.mContext,
                SYSTRACE_CONTROL_MODULE_SWITCH, "");
        if (TextUtils.isEmpty(extra)) {
            LOGD("getControlSwitchExtra extra is null.");
            return DEFAULT;
        }
        try {
            return new JSONObject(extra).getInt(EXTRA_STATE);
        } catch (JSONException e) {
            Log.e(TAG, "getControlSwitchExtra ex:" + e);
            return DEFAULT;
        }
    }

    public void doSystraceControl() {
        int controlSwitch = getControlSwitchExtra();
        if (controlSwitch == DEFAULT) {
            LOGD("doSystraceControl moduleSwitch=" + controlSwitch);
            return;
        }
        String enable = SystemProperties.get(PROP_ENABLE_ATRACE, "0");
        LOGD("doSystraceControl moduleSwitch=" + controlSwitch + " enableValue=" + enable);
        if (controlSwitch == OPEN_VALUE && !enable.equals("1")) {
            SystemProperties.set(PROP_ENABLE_ATRACE, "1");
            SystemProperties.set(PROP_ENABLE_COLLECT, "1");
            SystemProperties.set(PROP_START_ATRACE, "1");
            LOGD("doSystraceControl start systrace");
        }
        if (controlSwitch == CLOSE_VALUE && enable.equals("1")) {
            SystemProperties.set(PROP_STOP_ATRACE, "1");
            SystemProperties.set(PROP_ENABLE_ATRACE, "0");
            SystemProperties.set(PROP_ENABLE_COLLECT, "1");
            LOGD("doSystraceControl stop systrace");
        }
    }
}