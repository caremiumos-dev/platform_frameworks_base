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
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import android.view.IWindowManager;

import com.android.internal.policy.IKeyguardEventCallback;

/**
 * Client helper for observing keyguard (lock screen) lifecycle events.
 *
 * @hide
 */
public class KeyguardEventManager {
    private static final String TAG = KeyguardEventManager.class.getSimpleName();

    public static final String REASON_GO_TO_SLEEP = "GO_TO_SLEEP";
    public static final String REASON_POWER_KEY = "POWER_KEY";
    public static final String REASON_HALL = "HALL";
    public static final String REASON_HALL_CLOSE = "HALL_CLOSE";
    public static final String REASON_SHOW_WHEN_LOCKED = "FLAG_SHOW_WHEN_LOCKED";
    public static final String EXTRA_SHOW_WHEN_LOCKED = "SHOW_WHEN_LOCKED";

    private final IWindowManager mService = IWindowManager.Stub.asInterface(
            ServiceManager.getService(Context.WINDOW_SERVICE));

    public static class Listener extends IKeyguardEventCallback.Stub {
        @Override
        public void onStartedGoingToSleep() throws RemoteException {
        }

        @Override
        public void onFinishedGoingToSleep() throws RemoteException {
        }

        @Override
        public void onStartedWakingUp() throws RemoteException {
        }

        @Override
        public void onFinishedWakingUp() throws RemoteException {
        }

        @Override
        public void showKeyguard(String reason) throws RemoteException {
        }

        @Override
        public void hideKeyguard(String reason) throws RemoteException {
        }
    }

    public void registerListener(Listener listener) {
        try {
            mService.registerKeyguardListener(listener);
        } catch (RemoteException e) {
            Log.w(TAG, "binder call error", e);
        }
    }

    public void unregisterListener(Listener listener) {
        try {
            mService.unregisterKeyguardListener(listener);
        } catch (RemoteException e) {
            Log.w(TAG, "binder call error", e);
        }
    }
}