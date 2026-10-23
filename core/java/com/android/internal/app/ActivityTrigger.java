/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.internal.app;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.util.Log;

/**
 * QTI activity-trigger shim. Notifies a vendor native component of activity lifecycle
 * transitions so it can tune per-app density/scaling and animation behaviour.
 */
public class ActivityTrigger {
    public static final int ANIMATION_SCALE = 3;
    private static final int FLAG_HARDWARE_ACCELERATED = 512;
    private static final int FLAG_OVERRIDE_RESOLUTION = 1;
    public static final int NETWORK_OPTS = 2;
    public static final int START_PROCESS = 1;
    private static final String TAG = "ActivityTrigger";

    private native void native_at_deinit();

    private native float native_at_miscActivity(int i, String str, int i2, int i3);

    private native void native_at_pauseActivity(String str);

    private native void native_at_resumeActivity(String str);

    private native int native_at_startActivity(String str, int i);

    private native void native_at_stopActivity(String str);

    public float activityMiscTrigger(int i, String str, int i2, int i3) {
        return native_at_miscActivity(i, str, i2, i3);
    }

    public void activityPauseTrigger(Intent intent, ActivityInfo activityInfo,
            ApplicationInfo applicationInfo) {
        String str = null;
        ComponentName component = intent.getComponent();
        Log.d(TAG, "ActivityTrigger activityPauseTrigger ");
        if (component != null && applicationInfo != null) {
            str = component.flattenToString() + "/" + applicationInfo.versionCode;
        }
        native_at_pauseActivity(str);
    }

    public void activityResumeTrigger(Intent intent, ActivityInfo activityInfo,
            ApplicationInfo applicationInfo, boolean z) {
        ComponentName component = intent.getComponent();
        String str = component != null
                ? component.flattenToString() + "/" + applicationInfo.versionCode : null;
        native_at_resumeActivity(str);
        if (!z) {
            Log.d(TAG, "activityResumeTrigger: Activity is not Triggerred in full screen "
                    + applicationInfo);
            applicationInfo.setOverrideDensity(0);
            return;
        }
        Log.d(TAG, "activityResumeTrigger: The activity in " + applicationInfo
                + " is now in focus and seems to be in full-screen mode");
        if (applicationInfo.isAppWhiteListed()) {
            Log.d(TAG, "activityResumeTrigger: whiteListed " + str + " appInfo.flags - "
                    + Integer.toHexString(applicationInfo.flags));
            applicationInfo.setAppOverrideDensity();
        } else {
            applicationInfo.setOverrideDensity(0);
            Log.e(TAG, "activityResumeTrigger: not whiteListed" + str);
        }
    }

    public void activityStartTrigger(Intent intent, ActivityInfo activityInfo,
            ApplicationInfo applicationInfo, boolean z) {
        ComponentName component = intent.getComponent();
        String str = component != null
                ? component.flattenToString() + "/" + applicationInfo.versionCode : null;
        int iNative_at_startActivity = native_at_startActivity(str, 0);
        if ((iNative_at_startActivity & FLAG_HARDWARE_ACCELERATED) != 0) {
            activityInfo.flags |= FLAG_HARDWARE_ACCELERATED;
        }
        if (!z) {
            Log.d(TAG, "activityStartTrigger: Activity is not Triggerred in full screen "
                    + applicationInfo);
            applicationInfo.setOverrideDensity(0);
            return;
        }
        Log.d(TAG, "activityStartTrigger: Activity is Triggerred in full screen "
                + applicationInfo);
        if ((iNative_at_startActivity & FLAG_OVERRIDE_RESOLUTION) != 0) {
            Log.e(TAG, "activityStartTrigger: whiteListed " + str + " appInfo.flags - "
                    + Integer.toHexString(applicationInfo.flags));
            applicationInfo.setAppOverrideDensity();
            applicationInfo.setAppWhiteListed(1);
        } else {
            applicationInfo.setOverrideDensity(0);
            applicationInfo.setAppWhiteListed(0);
            Log.e(TAG, "activityStartTrigger: not whiteListed" + str);
        }
    }

    public void activityStopTrigger(Intent intent, ActivityInfo activityInfo,
            ApplicationInfo applicationInfo) {
        String str = null;
        ComponentName component = intent.getComponent();
        Log.d(TAG, "ActivityTrigger activityStopTrigger ");
        if (component != null && applicationInfo != null) {
            str = component.flattenToString() + "/" + applicationInfo.versionCode;
        }
        native_at_stopActivity(str);
    }

    protected void finalize() {
        native_at_deinit();
    }
}
