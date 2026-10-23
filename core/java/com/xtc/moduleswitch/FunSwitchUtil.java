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

package com.xtc.moduleswitch;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import java.util.Calendar;

/**
 * Queries the XTC function switch database exposed by the fun manager
 * provider.
 *
 * @hide
 */
public class FunSwitchUtil {
    private static final String TAG = "SysFunSwitchUtil";

    public static final Uri FUN_URI =
            Uri.parse("content://com.xtc.fundata/FunSwitch/");
    public static final Uri EXTRA_URI =
            Uri.parse("content://com.xtc.fundata/FunExtra/");

    public interface SwitchStatus {
        int SWITCH_STATUS_OPEN = 0;
        int SWITCH_STATUS_CLOSE = 1;
    }

    private FunSwitchUtil() {
        throw new UnsupportedOperationException("u can't instantiate me...");
    }

    public static boolean queryFunSwitchByBoolean(Context context, String packageName,
            boolean defaultValue) {
        int result = queryFunSwitchByInt(context, packageName, defaultValue);
        switch (result) {
            case SwitchStatus.SWITCH_STATUS_OPEN:
                return true;
            case SwitchStatus.SWITCH_STATUS_CLOSE:
                return false;
            default:
                Log.e(TAG, "queryFunSwitchByBoolean: switchInt is error. switchInt: " + result);
                return defaultValue;
        }
    }

    public static int queryFunSwitchByInt(Context context, String packageName,
            boolean defaultValue) {
        int result = defaultValue ? SwitchStatus.SWITCH_STATUS_OPEN
                : SwitchStatus.SWITCH_STATUS_CLOSE;
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(FUN_URI, null, "appPackage=?",
                    new String[]{packageName}, null);
            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        result = cursor.getInt(cursor.getColumnIndex("switchStatus"));
                        Log.i(TAG, "queryFunSwitchByInt: appPackage: " + packageName
                                + ", switch: " + result);
                    } else {
                        Log.e(TAG, "queryFunSwitchByInt: cursor is null.");
                    }
                } catch (Throwable th) {
                    Log.e(TAG, "query error", th);
                } finally {
                    cursor.close();
                }
            } else {
                Log.e(TAG, "queryFunSwitchByInt: cursor is null.");
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return result;
    }

    public static boolean queryInDisabledPeriod(Context context, String packageName,
            boolean defaultResult) {
        return queryInDisabledPeriod(queryDisabledPeriod(context, packageName), defaultResult);
    }

    public static boolean queryInDisabledPeriod(DisabledPeriod period, boolean defaultResult) {
        if (period == null) {
            return defaultResult;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        return period.isDisabledPeriod(calendar);
    }

    public static DisabledPeriod queryDisabledPeriod(Context context, String packageName) {
        DisabledPeriod period = null;
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(EXTRA_URI, null,
                    "appPackage=? AND type=?", new String[]{packageName, String.valueOf(0)}, null);
            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        period = DisabledPeriod.fromJson(
                                cursor.getString(cursor.getColumnIndex("extra")));
                    } else {
                        Log.e(TAG, "queryDisabledPeriod: cursor is null.");
                    }
                } catch (Throwable th) {
                    Log.e(TAG, "queryDisabledPeriod error", th);
                } finally {
                    cursor.close();
                }
            } else {
                Log.e(TAG, "queryDisabledPeriod: cursor is null.");
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return period;
    }
}