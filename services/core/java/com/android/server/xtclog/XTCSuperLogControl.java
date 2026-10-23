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
import android.os.Build;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Log;

import com.xtc.moduleswitch.ModuleSwitch;

/**
 * Applies the vendor super-log switch.
 *
 * @hide
 */
public class XTCSuperLogControl {
    static final String TAG = "XTCSuperLogControl";
    public static final boolean DEBUG = true;

    private static final int SUPERLOG_CONTROL_MODULE_SWITCH = 2891;
    private static final String PROP_SUPERLOG_SWITCH = "persist.sys.superlog.disable";

    private final Context mContext;

    public XTCSuperLogControl(Context context) {
        this.mContext = context;
    }

    private void LOGD(String message) {
        if (!TextUtils.isEmpty(message)) {
            Log.d(TAG, message);
        }
    }

    private boolean getControlSwitch() {
        return ModuleSwitch.queryModuleSwitch(this.mContext, SUPERLOG_CONTROL_MODULE_SWITCH, false);
    }

    public void doSuperLogControl() {
        String status = SystemProperties.get(PROP_SUPERLOG_SWITCH, "0");
        if ("userdebug".equals(Build.TYPE)) {
            return;
        }
        boolean controlSwitch = getControlSwitch();
        if (controlSwitch && status.equals("1")) {
            LOGD("doSuperLogControl moduleSwitch already trun on : status = " + status);
        }
        LOGD("doSuperLogControl moduleSwitch(Disalbe Log:true, Default:false):" + controlSwitch);
        if (!controlSwitch) {
            SystemProperties.set(PROP_SUPERLOG_SWITCH, "0");
            LOGD("turn on SuperLog.");
        } else {
            SystemProperties.set(PROP_SUPERLOG_SWITCH, "1");
            LOGD("doSuperLogControl prop value :"
                    + SystemProperties.get(PROP_SUPERLOG_SWITCH, "0"));
        }
    }
}