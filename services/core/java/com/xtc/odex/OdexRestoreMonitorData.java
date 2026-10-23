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

package com.xtc.odex;

/**
 * Reports odex-restore decisions through the vendor behaviour-telemetry channel.
 *
 * @hide
 */
public class OdexRestoreMonitorData {
    private static final String FUNC_NAME = "xtc_odex_restore_policy";
    private static final String KEY_PKG_NAME = "pkgName";
    private static final String KEY_VER_CODE = "verCode";
    private static final String KEY_VER_NAME = "verName";
    private static final String KEY_EXCEPTION_TYPE = "exception_type";
    private static final String KEY_RESTORE_STATE = "restore_state";

    public static void reportDataByXtcBehavior(android.content.Context context, String packageName,
            int versionCode, String versionName, String exceptionType, int restoreState) {
        java.util.LinkedHashMap<String, String> data = new java.util.LinkedHashMap<>();
        data.put(KEY_PKG_NAME, packageName);
        data.put(KEY_VER_CODE, String.valueOf(versionCode));
        data.put(KEY_VER_NAME, versionName);
        data.put(KEY_EXCEPTION_TYPE, exceptionType);
        data.put(KEY_RESTORE_STATE, String.valueOf(restoreState));
        new com.xtc.behavior.XtcBehavior.Builder(context, FUNC_NAME, data).build().sendData();
    }
}