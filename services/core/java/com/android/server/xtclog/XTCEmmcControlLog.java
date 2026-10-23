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

import java.io.BufferedReader;
import java.io.FileReader;

/**
 * Stops the vendor super-log once the eMMC life-time exceeds the configured
 * threshold.
 *
 * @hide
 */
public class XTCEmmcControlLog {
    static final String TAG = "XTCEmmcControlLog";
    public static final boolean DEBUG = true;

    private static final int EMMC_CONTROL_MODULE_SWITCH = 2514;
    private static final int INVALID_LIFE_TIME = -1;
    private static final String PROP_STOP_LOG = "log.tag.stopsplog";
    private static final String LIFE_TIME_NODE = "/sys/kernel/debug/life_time_typeA";
    private static final String KEY_EMMC_OVER_LIFE = "emmc_over_life";
    private static final String OVER_USED = "over used";

    private final Context mContext;

    public XTCEmmcControlLog(Context context) {
        this.mContext = context;
    }

    private void LOGD(String message) {
        if (!TextUtils.isEmpty(message)) {
            Log.d(TAG, message);
        }
    }

    private boolean getControlSwitch() {
        return ModuleSwitch.queryModuleSwitch(this.mContext, EMMC_CONTROL_MODULE_SWITCH, true);
    }

    private int getControlSwitchExtra() {
        String extra = ModuleSwitch.queryModuleSwitchExtraByInt(this.mContext,
                EMMC_CONTROL_MODULE_SWITCH, "");
        if (TextUtils.isEmpty(extra)) {
            LOGD("getControlSwitchExtra extra is null.");
            return INVALID_LIFE_TIME;
        }
        try {
            return new JSONObject(extra).getInt(KEY_EMMC_OVER_LIFE);
        } catch (JSONException e) {
            Log.e(TAG, "getModuleSwitchExtra ex:" + e);
            return INVALID_LIFE_TIME;
        }
    }

    public void doControlLog() {
        if (!getControlSwitch()) {
            LOGD("doControlLog moduleSwitch is off.");
            return;
        }
        int extraLifeTime = getControlSwitchExtra();
        if (extraLifeTime == INVALID_LIFE_TIME) {
            LOGD("doControlLog extraLifeTime invalid.");
            return;
        }
        String nodeLifeTime;
        try (BufferedReader reader = new BufferedReader(new FileReader(LIFE_TIME_NODE))) {
            nodeLifeTime = reader.readLine();
        } catch (Exception e) {
            Log.e(TAG, "doControlLog read nodeLifeTime ex:" + e);
            nodeLifeTime = null;
        }
        if (TextUtils.isEmpty(nodeLifeTime)) {
            LOGD("doControlLog read node life_time_typeA is null.");
            return;
        }
        if (nodeLifeTime.contains(OVER_USED)) {
            SystemProperties.set(PROP_STOP_LOG, "1");
            LOGD("set log.tag.stopsplog 1 by net config emmc life time, nodeLifeTime over used!");
            return;
        }
        int time1;
        try {
            String first = nodeLifeTime.substring(0, nodeLifeTime.indexOf(","));
            time1 = Integer.valueOf(first.substring(0, first.indexOf("%")));
        } catch (Exception e) {
            Log.w(TAG, "doControlLog format time1 ex:" + e);
            time1 = 0;
        }
        int time2;
        try {
            int start = nodeLifeTime.indexOf(",") + 1;
            String second = nodeLifeTime.substring(start, nodeLifeTime.indexOf(",", start));
            time2 = Integer.valueOf(second.substring(0, second.indexOf("%")));
        } catch (Exception e) {
            Log.w(TAG, "doControlLog format time2 ex:" + e);
            time2 = 0;
        }
        if (time1 >= time2) {
            time2 = time1;
        }
        LOGD("doControlLog nodeLifeTime:" + time2 + " ,extraLifeTime:" + extraLifeTime
                + " ,nodeStr:" + nodeLifeTime);
        if (time2 >= extraLifeTime) {
            SystemProperties.set(PROP_STOP_LOG, "1");
            LOGD("set log.tag.stopsplog 1 by net config emmc life time, extraLifeTime:"
                    + extraLifeTime + " ,nodeLifeTime:" + time2);
        }
    }
}