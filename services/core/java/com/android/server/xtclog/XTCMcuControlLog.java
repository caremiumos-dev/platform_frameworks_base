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
import android.text.TextUtils;
import android.util.Log;

import com.xtc.moduleswitch.ModuleSwitch;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Applies the vendor MCU log switch by writing a sysfs node.
 *
 * @hide
 */
public class XTCMcuControlLog {
    static final String TAG = "XTCMcuControlLog";
    public static final boolean DEBUG = true;

    private static final int MCU_CONTROL_MODULE_SWITCH = 2790;
    private static final String MCU_LOG_SWITCH_NODE = "/sys/bbd/mcu_log_switch";

    private final Context mContext;

    public XTCMcuControlLog(Context context) {
        this.mContext = context;
    }

    private void LOGD(String message) {
        if (!TextUtils.isEmpty(message)) {
            Log.d(TAG, message);
        }
    }

    private boolean getControlSwitch() {
        return ModuleSwitch.queryModuleSwitch(this.mContext, MCU_CONTROL_MODULE_SWITCH, false);
    }

    public static String readFile(String path) {
        File file = new File(path);
        if (!file.exists()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        try (FileReader reader = new FileReader(file)) {
            char[] buffer = new char[1024];
            int count;
            while ((count = reader.read(buffer)) > 0) {
                builder.append(buffer, 0, count);
            }
            return builder.toString();
        } catch (IOException e) {
            return "";
        }
    }

    public static boolean writeFile(String path, String value) {
        File file = new File(path);
        File parent = new File(file.getParent());
        if (!parent.exists()) {
            parent.mkdirs();
        }
        try {
            if (file.exists()) {
                file.delete();
                file.createNewFile();
            } else {
                file.createNewFile();
            }
            try (FileWriter writer = new FileWriter(file, false)) {
                writer.write(value);
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public void doMcuControl() {
        boolean controlSwitch = getControlSwitch();
        LOGD("doMcuControl moduleSwitch(DISPLAY:0, HIDDEN:1):" + controlSwitch);
        if (controlSwitch) {
            writeFile(MCU_LOG_SWITCH_NODE, "1\n");
            LOGD("doMcuControl moduleSwitch is on.");
        } else {
            writeFile(MCU_LOG_SWITCH_NODE, "0\n");
            LOGD("doMcuControl moduleSwitch is off.");
        }
    }
}