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

package com.android.server.xtcauth;

import android.app.ActivityManagerNative;
import android.app.IActivityManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.UserHandle;
import android.util.Log;

/**
 * Handler used by the mobile manager service for timeout monitoring and package
 * killing.
 *
 * @hide
 */
class MobileManagerHandler extends Handler {
    public static final String TAG = "MobileManagerHandler";

    public static final int MSG_PACKAGE_CHANGE = 0;
    public static final int MSG_TIMEOUT_MONITOR = 1;
    public static final int MSG_KILL_PROCESS = 3;

    public MobileManagerHandler(Looper looper) {
        super(looper);
    }

    public void forceStopPackage(String packageName, int uid) {
        IActivityManager am = ActivityManagerNative.getDefault();
        Log.d(TAG, "forceStopPackage processName : " + packageName + ", uid = " + uid);
        try {
            am.forceStopPackage(packageName, UserHandle.myUserId());
        } catch (android.os.RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_PACKAGE_CHANGE:
                return;
            case MSG_TIMEOUT_MONITOR: {
                ListenerTriggerBase trigger = (ListenerTriggerBase) msg.obj;
                synchronized (trigger) {
                    if (!trigger.isCompleted()) {
                        trigger.timeouted();
                        trigger.notifyAll();
                        Log.d(TAG, "onTriggerListenerTimeout()");
                        trigger.onTriggerListenerTimeout();
                    }
                }
                return;
            }
            case MSG_KILL_PROCESS:
                forceStopPackage((String) msg.obj, msg.arg1);
                return;
            default:
                Log.w(TAG, "Unknown message: " + msg.what);
                return;
        }
    }
}