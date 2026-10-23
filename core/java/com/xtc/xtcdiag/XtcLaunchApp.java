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

package com.xtc.xtcdiag;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.ArrayList;

/**
 * Launches the vendor MMI / self-test / validation applications on behalf of the
 * diag service.
 *
 * @hide
 */
public class XtcLaunchApp {
    private static final String TAG = "XtcDiag-launch";

    private Context mContext;

    public XtcLaunchApp(Context context) {
        mContext = context;
    }

    public void launchMmiApp(int testId, String testParam) {
        Intent intent = new Intent("com.xtc.mmitest.action.START_TEST");
        intent.putExtra("com.xtc.mmitest.extra.TEST_ID", testId);
        intent.putExtra("com.xtc.mmitest.extra.TEST_PARAM", testParam);
        intent.setPackage("com.xtc.mmitest");
        mContext.startService(intent);
        Log.d(TAG, "launchTargetApp mmi apk");
    }

    public void launchSelfTestApp() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setPackage("com.xtc.selftest");
        mContext.startActivity(intent);
        Log.d(TAG, "launchTargetApp selftest apk");
    }

    public void launchValidationApp(ArrayList<Integer> cmdCodeList) {
        if (cmdCodeList == null || cmdCodeList.isEmpty()) {
            return;
        }
        Intent intent = new Intent("com.xtc.validation.action.START_TEST");
        intent.putIntegerArrayListExtra("com.xtc.validation.extra.CMD_CODE_LIST", cmdCodeList);
        intent.setPackage("com.xtc.validationtest");
        mContext.startService(intent);
        Log.d(TAG, "launchTargetApp validation apk");
    }
}