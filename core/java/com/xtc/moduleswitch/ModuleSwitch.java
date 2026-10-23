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
import android.provider.SettingsStringUtil;
import android.util.Log;

import java.util.Hashtable;

/**
 * Queries the XTC module switch database exposed by the module data provider
 * and caches the results.
 *
 * @hide
 */
public class ModuleSwitch {
    private static final String TAG = "sys_ModuleSwitch";
    private static final Uri URI_QUERY_MODULE_SWITCH =
            Uri.parse("content://com.xtc.moduledata/query");

    public static final int DISPLAY = 0;
    public static final int HIDDEN = 1;
    public static final int DISPLAY_HAS_TIP = 2;

    public static final String ACTION_LAUNCHER_MODULE_INIT =
            "com.xtc.launcher.moduleswitch.INIT_ACTION";

    private static Object sync_obj = new Object();
    private static Hashtable<Integer, Boolean> mModuleDbResult = new Hashtable<>();

    public static boolean queryModuleSwitch(Context context, int moduleId, boolean defaultValue) {
        switch (queryModuleSwitchInt(context, moduleId, defaultValue ? DISPLAY : HIDDEN)) {
            case DISPLAY:
                Log.d(TAG, "module:" + moduleId + " display.");
                return true;
            case HIDDEN:
                Log.d(TAG, "module:" + moduleId + " hidden.");
                return false;
            case DISPLAY_HAS_TIP:
                Log.d(TAG, "module:" + moduleId + " display and has tip.");
                return true;
            default:
                return false;
        }
    }

    public static synchronized int queryModuleSwitchInt(Context context, int moduleId,
            int defaultValue) {
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(URI_QUERY_MODULE_SWITCH, null,
                    "module=" + moduleId, null, null);
            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        defaultValue = cursor.getInt(cursor.getColumnIndex(Context.DISPLAY_SERVICE));
                        setModuleDbResult(moduleId, true);
                    } else {
                        Log.e(TAG, "queryModuleSwitchInt cursor is null:" + cursor);
                        setModuleDbResult(moduleId, false);
                    }
                } catch (Throwable th) {
                    Log.e(TAG, "ex:" + th);
                    setModuleDbResult(moduleId, false);
                } finally {
                    cursor.close();
                }
            } else {
                Log.e(TAG, "queryModuleSwitchInt cursor is null:" + cursor);
                setModuleDbResult(moduleId, false);
            }
        } catch (Throwable th) {
            if (cursor != null) {
                cursor.close();
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        Log.i(TAG, "queryModuleSwitchInt(DISPLAY:0, HIDDEN:1) module=" + moduleId
                + SettingsStringUtil.DELIMITER + defaultValue);
        return defaultValue;
    }

    public static synchronized String queryModuleSwitchTip(Context context, int moduleId,
            String defaultTip) {
        Cursor cursor = null;
        synchronized (ModuleSwitch.class) {
            try {
                cursor = context.getContentResolver().query(URI_QUERY_MODULE_SWITCH, null,
                        "module=" + moduleId, null, null);
                if (cursor != null) {
                    try {
                        if (cursor.moveToFirst()) {
                            defaultTip = cursor.getString(cursor.getColumnIndex("tips"));
                        } else {
                            Log.e(TAG,
                                    "queryModuleSwitchInt cursor is null, and use default "
                                            + "defaultModuleSwitchTip");
                        }
                    } catch (Throwable th) {
                        if (cursor != null) {
                            cursor.close();
                        }
                        throw th;
                    } finally {
                        cursor.close();
                    }
                } else {
                    Log.e(TAG,
                            "queryModuleSwitchInt cursor is null, and use default "
                                    + "defaultModuleSwitchTip");
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            } catch (Throwable th) {
                cursor = null;
            }
            Log.i(TAG, "tip:" + defaultTip);
        }
        return defaultTip;
    }

    public static synchronized String queryModuleSwitchExtraByInt(Context context, int moduleId,
            String defaultExtra) {
        Cursor cursor = null;
        synchronized (ModuleSwitch.class) {
            try {
                cursor = context.getContentResolver().query(
                        Uri.parse("content://com.xtc.moduledata/query"), null,
                        "module=" + moduleId, null, null);
                if (cursor != null) {
                    try {
                        if (cursor.moveToFirst()) {
                            defaultExtra = cursor.getString(cursor.getColumnIndex("extra"));
                        } else {
                            Log.e(TAG, "cursor is null, use defaultModuleSwitchExtra");
                        }
                    } catch (Throwable th) {
                        if (cursor != null) {
                            cursor.close();
                        }
                        throw th;
                    } finally {
                        cursor.close();
                    }
                } else {
                    Log.e(TAG, "cursor is null, use defaultModuleSwitchExtra");
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            } catch (Throwable th) {
                cursor = null;
            }
            Log.i(TAG, "module:" + moduleId + " extra:" + defaultExtra);
        }
        return defaultExtra;
    }

    public static boolean getModuleDbReuslt(int moduleId) {
        if (mModuleDbResult.containsKey(moduleId)) {
            return mModuleDbResult.get(moduleId);
        }
        return false;
    }

    private static void setModuleDbResult(int moduleId, boolean result) {
        mModuleDbResult.put(moduleId, result);
    }
}