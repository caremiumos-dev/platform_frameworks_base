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

import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.xtc.auth.INotificationListener;

/**
 * Asks the management application whether a notification may be blocked.
 *
 * @hide
 */
final class NotificationListenerTrigger extends ListenerTriggerBase {
    public static final String TAG = "NotificationListenerTrigger";

    public NotificationListenerTrigger(int controllerID, int callingPid, IBinder listener,
            Bundle params, int defaultResult) {
        super(controllerID, callingPid, listener, params, defaultResult);
    }

    public NotificationListenerTrigger(int controllerID, int callingPid, IBinder listener,
            Bundle params, int defaultResult, IBinder callback) {
        super(controllerID, callingPid, listener, params, defaultResult, callback);
    }

    @Override
    public int triggerListenerFunc() {
        Bundle params = getParams();
        IBinder func = getFunc();
        String packageName = params.getString("notificationPkg");
        Log.d(TAG, "triggerListenerFunc() packageName: " + packageName + "id : "
                + params.getInt("notificationID"));
        Log.d(TAG, "[MoMS >> MoMA] onNotificationBlocked()");
        INotificationListener listener = INotificationListener.Stub.asInterface(func);
        if (listener != null) {
            try {
                listener.onNotificationBlocked(packageName, params);
            } catch (android.os.RemoteException e) {
                Log.e(TAG, "onNotificationBlocked() failed!", e);
            }
        } else {
            Log.w(TAG, "Ignore null listener");
        }
        Log.d(TAG, "[MoMS << MoMA] onNotificationBlocked() returned: 0");
        return 0;
    }

    @Override
    public void onTriggerListenerDone() {
    }

    @Override
    public void onTriggerListenerTimeout() {
    }
}