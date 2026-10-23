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

import com.xtc.auth.ICallInterceptionListener;

/**
 * Asks the management application whether an incoming call may be intercepted.
 *
 * @hide
 */
final class CallInterceptionListenerTrigger extends ListenerTriggerBase {
    public static final String TAG = "CallInterceptionListenerTrigger";

    public CallInterceptionListenerTrigger(int controllerID, int callingPid, IBinder listener,
            Bundle params, int defaultResult) {
        super(controllerID, callingPid, listener, params, defaultResult);
    }

    public CallInterceptionListenerTrigger(int controllerID, int callingPid, IBinder listener,
            Bundle params, int defaultResult, IBinder callback) {
        super(controllerID, callingPid, listener, params, defaultResult, callback);
    }

    @Override
    public int triggerListenerFunc() {
        int result = 0;
        Bundle params = getParams();
        IBinder func = getFunc();
        Log.d(TAG, "[triggerListenerFunc]" + params.getString("phoneNumber") + ", "
                + params.getInt("callType") + ", " + params.getInt("slotId"));
        ICallInterceptionListener listener = ICallInterceptionListener.Stub.asInterface(func);
        if (listener != null) {
            try {
                if (!listener.onIncomingCallCheck(params)) {
                    result = -1;
                }
            } catch (android.os.RemoteException e) {
                Log.e(TAG, "onNotificationBlocked() failed!", e);
            }
        } else {
            Log.w(TAG, "Ignore null listener");
        }
        Log.d(TAG, "[MoMS << MoMA] onIncomingCallCheck() returned: " + result);
        return result;
    }

    @Override
    public void onTriggerListenerDone() {
    }

    @Override
    public void onTriggerListenerTimeout() {
    }
}