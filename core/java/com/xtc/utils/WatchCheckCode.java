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

package com.xtc.utils;

import android.content.Context;
import android.os.Binder;
import android.util.Log;

import com.xtc.behavior.XtcBehavior;

import java.util.LinkedHashMap;

/**
 * Generates and verifies watch service/ADB check codes backed by the native
 * libwatchcheckcode.so implementation.
 *
 * @hide
 */
public class WatchCheckCode {
    private static final String TAG = "WatchCheckCode";

    private static final String FUNC_NAME = "xtc_selftest_check_code";
    private static final String KEY_CHECK_CODE = "check_code";
    private static final String KEY_TYPE = "type";
    private static final String KEY_VERIFY = "verify";
    private static final String KEY_VERIFY_RESULT = "verify_result";

    public static final int TYPE_SELF_TEST = 1;
    public static final int TYPE_ADB = 2;

    static {
        System.load("/system/lib/libwatchcheckcode.so");
    }

    private static native String generateCheckCode(int type);

    private static native boolean check(String checkCode, int type);

    public static String generateWatchCheckCode(int type) {
        int callingUid = Binder.getCallingUid();
        if (callingUid != 1000) {
            Log.w(TAG, "generateWatchCheckCode failure: Ignoring calls from non-system server. Uid: "
                    + callingUid);
            return null;
        }
        if (type != TYPE_SELF_TEST && type != TYPE_ADB) {
            Log.w(TAG, "fail to generateWatchCheckCode, due to invalid type: " + type);
            return null;
        }
        String checkCode = generateCheckCode(type);
        Log.d(TAG, "generateWatchCheckCode = " + checkCode + ", type = " + type);
        return checkCode;
    }

    public static boolean verifyCheckCode(String checkCode, int type) {
        int callingUid = Binder.getCallingUid();
        if (callingUid != 1000) {
            Log.w(TAG, "verifyCheckCode failure: Ignoring calls from non-system server. Uid: "
                    + callingUid);
            return false;
        }
        if (type != TYPE_SELF_TEST && type != TYPE_ADB) {
            Log.w(TAG, "fail to verifyCheckCode, due to invalid type: " + type);
            return false;
        }
        boolean result = check(checkCode, type);
        Log.d(TAG, "verifyCheckCode tools check code = " + checkCode + ", type = " + type
                + ", result = " + result);
        return result;
    }

    public static void reportDataByXtcBehavior(Context context, String checkCode, int type,
            boolean verify, boolean verifyResult) {
        LinkedHashMap<String, String> extend = new LinkedHashMap<>();
        extend.put(KEY_TYPE, String.valueOf(type));
        extend.put(KEY_CHECK_CODE, checkCode);
        extend.put(KEY_VERIFY, String.valueOf(verify ? 1 : 0));
        extend.put(KEY_VERIFY_RESULT, String.valueOf(verifyResult ? 1 : 0));
        new XtcBehavior.Builder(context, FUNC_NAME, extend).build().sendData();
    }
}