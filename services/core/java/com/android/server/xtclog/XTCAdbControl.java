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
 * Applies the vendor module switch that controls whether ADB is available.
 *
 * <p><b>Strong condemnation.</b> This lets a remote/module policy silently
 * disable ADB and revoke debugging access on the user's own device, and
 * toggles the "de permissions" flag accordingly. Controlling whether the owner
 * of a device may use its debug interface is an unacceptable restriction of
 * user freedom. Reproduced only for behavioural compatibility.
 *
 * @hide
 */
public class XTCAdbControl {
    private static final String TAG = "XTCAdbControl";
    public static final boolean DEBUG = true;

    private static final int ADB_CONTROL_MODULE_SWITCH = 105;
    private static final int INVALID_ADB_SWITCH = -1000;

    public static final String ADB_FROM_105_SWITCH_ON = "105_switch_on";
    public static final String ADB_FROM_105_SWITCH_OFF = "105_switch_off";
    public static final String ADB_FROM_ALTER_SALE_ON = "after_sale_on";
    public static final String ADB_FROM_ALTER_SALE_OFF = "after_sale_off";
    public static final String ADB_FROM_SELF_TEST_ON = "self_test_on";
    public static final String ADB_FROM_SELF_TEST_OFF = "self_test_off";
    public static final String ADB_FROM_TIMEOUT_OFF = "timeout_off";

    public static final String PROP_ADB_ENABLE_FROM = "sys.adb.enable_from";

    private static final String PROP_ADB_APK = "persist.sys.adb.apk";
    private static final String PROP_INSTALL_APK = "persist.sys.adb.install";
    private static final String PROP_USB_CHARGE_ENABLE = "persist.sys.charge.usable";
    private static final String PROP_USB_CONFIG = "persist.sys.usb.config";
    private static final String PROP_USB_FLAG = "persist.sys.usb.flag";

    private final Context mContext;

    public XTCAdbControl(Context context) {
        this.mContext = context;
    }

    private void LOGD(String message) {
        if (!TextUtils.isEmpty(message)) {
            Log.d(TAG, message);
        }
    }

    private int getControlSwitch() {
        return ModuleSwitch.queryModuleSwitchInt(this.mContext, ADB_CONTROL_MODULE_SWITCH,
                INVALID_ADB_SWITCH);
    }

    public void doAdbControl() {
        if (Build.IS_USERDEBUG) {
            return;
        }
        int controlSwitch = getControlSwitch();
        int adbApk = SystemProperties.getInt(PROP_ADB_APK, -1);
        LOGD("doAdbControl moduleSwitch(DISPLAY:0, HIDDEN:1):" + controlSwitch
                + " ,adbApk:" + adbApk);
        if (controlSwitch == 0 || controlSwitch == 2) {
            Build.setDePermissions(true);
            if (adbApk == 1) {
                SystemProperties.set(PROP_INSTALL_APK, "1");
                return;
            }
            SystemProperties.set(PROP_USB_CONFIG, "adb");
            SystemProperties.set(PROP_USB_FLAG, "1");
            SystemProperties.set(PROP_USB_CHARGE_ENABLE, "true");
            SystemProperties.set(PROP_INSTALL_APK, "1");
            SystemProperties.set(PROP_ADB_ENABLE_FROM, ADB_FROM_105_SWITCH_ON);
            return;
        }
        if (controlSwitch != 1) {
            LOGD("doAdbControl no-op, keep last value.");
            return;
        }
        if (SystemProperties.get(PROP_INSTALL_APK).equals("1")) {
            SystemProperties.set(PROP_INSTALL_APK, "0");
        }
        Build.setDePermissions(false);
        if (ADB_FROM_ALTER_SALE_ON.equals(SystemProperties.get(PROP_ADB_ENABLE_FROM))) {
            return;
        }
        SystemProperties.set(PROP_USB_CONFIG, "none");
        SystemProperties.set(PROP_ADB_ENABLE_FROM, ADB_FROM_105_SWITCH_OFF);
    }
}