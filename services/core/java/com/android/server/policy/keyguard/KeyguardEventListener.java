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

package com.android.server.policy.keyguard;

import android.os.Binder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;

import com.android.internal.policy.IKeyguardEventCallback;

/**
 * Fans out keyguard lifecycle events to interested remote listeners.
 *
 * @hide
 */
public class KeyguardEventListener {
    private static final int START_WAKEUP = 0;
    private static final int FINISH_WAKEUP = 1;
    private static final int START_GOTO_SLEEP = 2;
    private static final int FINISH_GOTO_SLEEP = 3;
    private static final int SHOW_KEYGUARD = 4;
    private static final int HIDE_KEYGUARD = 5;

    private final String TAG = "Sys" + KeyguardEventListener.class.getSimpleName();
    private final RemoteCallbackList<IKeyguardEventCallback> mCallbackList =
            new RemoteCallbackList<>();

    private synchronized void notifyEvent(int type, String reason) {
        int count = this.mCallbackList.getRegisteredCallbackCount();
        if (count <= 0) {
            Log.d(this.TAG, "remote callback listener count:" + count);
            return;
        }
        int size = this.mCallbackList.beginBroadcast();
        for (int i = 0; i < size; i++) {
            notifyEvent(type, reason, this.mCallbackList.getBroadcastItem(i));
        }
        Log.d(this.TAG, "notify event type:" + type + " msg:" + reason + " size:" + size);
        this.mCallbackList.finishBroadcast();
    }

    private void notifyEvent(int type, String reason, IKeyguardEventCallback callback) {
        try {
            switch (type) {
                case START_WAKEUP:
                    callback.onStartedWakingUp();
                    break;
                case FINISH_WAKEUP:
                    callback.onFinishedWakingUp();
                    break;
                case START_GOTO_SLEEP:
                    callback.onStartedGoingToSleep();
                    break;
                case FINISH_GOTO_SLEEP:
                    callback.onFinishedGoingToSleep();
                    break;
                case SHOW_KEYGUARD:
                    callback.showKeyguard(reason);
                    break;
                case HIDE_KEYGUARD:
                    callback.hideKeyguard(reason);
                    break;
                default:
                    Log.w(this.TAG, "invalid type:" + type);
                    break;
            }
        } catch (RemoteException e) {
            Log.d(this.TAG, "keyguard callback error", e);
        }
    }

    public void finishGoToSleep() {
        notifyEvent(FINISH_GOTO_SLEEP, null);
    }

    public void finishWakeup() {
        notifyEvent(FINISH_WAKEUP, null);
    }

    public void hideKeyguard(String reason) {
        notifyEvent(HIDE_KEYGUARD, reason);
    }

    public void registerKeyguardListener(IKeyguardEventCallback callback) {
        Log.d(this.TAG, "register pid:" + Binder.getCallingPid() + " uid:"
                + Binder.getCallingUid() + " result:" + this.mCallbackList.register(callback));
    }

    public void showKeyguard(String reason) {
        notifyEvent(SHOW_KEYGUARD, reason);
    }

    public void startGoToSleep() {
        notifyEvent(START_GOTO_SLEEP, null);
    }

    public void startWakeup() {
        notifyEvent(START_WAKEUP, null);
    }

    public void unregisterKeyguardListener(IKeyguardEventCallback callback) {
        Log.d(this.TAG, "unregister pid:" + Binder.getCallingPid() + " uid:"
                + Binder.getCallingUid() + " result:" + this.mCallbackList.unregister(callback));
    }
}