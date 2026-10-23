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
 * Applies the vendor caton (jank) collection level from the module switch.
 *
 * @hide
 */
public class XTCCatonControl {
    static final String TAG = "XTCCatonControl";
    public static final boolean DEBUG = true;

    private static final int CATON_CONTROL_MODULE_SWITCH = 2899;
    private static final int INVALID_CATON_VALUE = -1;
    private static final String PROP_COLLECT_FRAME_LIMIT = "persist.sys.collect.xtc_frames";
    private static final int MIN_CATON_LEVEL = 10;

    private final Context mContext;

    public XTCCatonControl(Context context) {
        this.mContext = context;
    }

    private void LOGD(String message) {
        if (!TextUtils.isEmpty(message)) {
            Log.d(TAG, message);
        }
    }

    private int getControlSwitchExtra() {
        String extra = ModuleSwitch.queryModuleSwitchExtraByInt(this.mContext,
                CATON_CONTROL_MODULE_SWITCH, "");
        if (TextUtils.isEmpty(extra)) {
            LOGD("getControlSwitchExtra extra is null.");
            return INVALID_CATON_VALUE;
        }
        try {
            return new JSONObject(extra).getInt("catonLevel");
        } catch (JSONException e) {
            Log.e(TAG, "getControlSwitchExtra ex:" + e);
            return INVALID_CATON_VALUE;
        }
    }

    public void doCatonControl() {
        int level = getControlSwitchExtra();
        if (level == INVALID_CATON_VALUE || level < MIN_CATON_LEVEL) {
            LOGD("doCatonControl extraCatonValue invalid.");
            return;
        }
        int limit = SystemProperties.getInt(PROP_COLLECT_FRAME_LIMIT, 60);
        if (level != limit) {
            SystemProperties.set(PROP_COLLECT_FRAME_LIMIT, String.valueOf(level));
            LOGD("doCatonControl set COLLECT_FRAME from " + limit + " to " + level);
        }
    }
}