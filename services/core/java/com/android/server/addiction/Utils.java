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

package com.android.server.addiction;

import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Slog;

import com.android.server.xtc.apm.APMDatabaseHelper;
import com.xtc.behavior.XtcBehavior;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * Helpers for the anti-addiction (screen-time) statistics.
 *
 * @hide
 */
public class Utils {
    private static final String TAG = "AntiAddictionManager";

    public static void UploadBigData(Context context, String packageName, int type) {
        String applicationName = getApplicationNameByPackageName(context, packageName);
        LinkedHashMap<String, String> data = new LinkedHashMap<>();
        data.put("ApplicationName", applicationName);
        data.put(APMDatabaseHelper.COLUMN_TIMESTAMP, String.valueOf(System.currentTimeMillis()));
        data.put("packageName", packageName);
        data.put(APMDatabaseHelper.COLUMN_TYPE, String.valueOf(type));
        new XtcBehavior.Builder(context, "ANTI_DURATION_STATS", data).build().sendData();
    }

    public static String getAppNameByPid(int pid) {
        try {
            List<ActivityManager.RunningAppProcessInfo> processes =
                    ActivityManagerNative.getDefault().getRunningAppProcesses();
            if (processes == null || processes.size() == 0) {
                Slog.e(TAG, "processesList is null");
                return null;
            }
            for (int i = 0; i < processes.size(); i++) {
                ActivityManager.RunningAppProcessInfo process = processes.get(i);
                if (process.pid == pid) {
                    return process.processName;
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private static String getApplicationNameByPackageName(Context context, String packageName) {
        PackageManager pm = context.getPackageManager();
        try {
            return pm.getApplicationLabel(pm.getApplicationInfo(packageName, 128)).toString();
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean isSystemApp(Context context, String packageName) {
        PackageInfo packageInfo;
        try {
            packageInfo = context.getPackageManager().getPackageInfo(packageName, 0);
        } catch (Throwable t) {
            Slog.e(TAG, t.getMessage(), t);
            packageInfo = null;
        }
        return packageInfo != null && (packageInfo.applicationInfo.flags & 1) == 1;
    }
}