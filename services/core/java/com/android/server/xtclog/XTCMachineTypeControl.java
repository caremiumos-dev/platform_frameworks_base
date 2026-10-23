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

/**
 * Applies the vendor machine-type logging switch.
 *
 * @hide
 */
public class XTCMachineTypeControl {
    static final String TAG = "XTCMachineTypeControl";
    public static final boolean DEBUG = true;

    private static final int MCU_CONTROL_MODULE_SWITCH = 2892;
    private static final String PROP_MACHINE_TYPE = "ro.boot.xtc.machinetype";
    private static final String PROP_AREA_CODE_LOG = "log.tag.area_code";
    private static final String MACHINE_TYPE_SHOW = "SHOW";

    private final Context mContext;

    public XTCMachineTypeControl(Context context) {
        this.mContext = context;
    }

    private void LOGD(String message) {
        if (!TextUtils.isEmpty(message)) {
            Log.d(TAG, message);
        }
    }

    private boolean getControlSwitch() {
        return ModuleSwitch.queryModuleSwitch(this.mContext, MCU_CONTROL_MODULE_SWITCH, true);
    }

    public void doMachineTypeControl() {
        boolean controlSwitch = getControlSwitch();
        String machineType = SystemProperties.get(PROP_MACHINE_TYPE, "");
        LOGD("handleSetMachineType: machineSwitch=" + controlSwitch + ", machineType="
                + machineType);
        if (MACHINE_TYPE_SHOW.equals(machineType) && !controlSwitch) {
            SystemProperties.set(PROP_AREA_CODE_LOG, "NULL");
        }
    }
}