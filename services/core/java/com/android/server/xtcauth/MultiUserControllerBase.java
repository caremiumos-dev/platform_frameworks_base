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
import android.os.IBinder;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseBooleanArray;

/**
 * Controller base that keeps per-user enabled state and listeners.
 *
 * @hide
 */
public abstract class MultiUserControllerBase extends ControllerBase {
    private static final String TAG = "MultiUserControllerBase";

    private final SparseBooleanArray mSwitchers = new SparseBooleanArray();
    private final SparseArray<IBinder> mManagerApListeners = new SparseArray<>();

    @Override
    public void enableController(boolean enable) {
        int callingUserId = UserHandle.getCallingUserId();
        if (enable) {
            try {
                if (ActivityManagerNative.getDefault().isUserAMonkey()) {
                    Log.w(TAG, "Contrller is disabled during monkey test");
                    enable = false;
                }
            } catch (RemoteException e) {
                Log.e(TAG, "isUserAMonkey() failed", e);
            }
        }
        mSwitchers.put(callingUserId, enable);
    }

    @Override
    public boolean isControllerEnabled() {
        return isControllerEnabled(UserHandle.getCallingUserId());
    }

    public boolean isControllerEnabled(int userId) {
        return mSwitchers.get(userId);
    }

    @Override
    public IBinder getManagerApListener() {
        return mManagerApListeners.get(UserHandle.getCallingUserId());
    }

    public IBinder getManagerApListener(int userId) {
        return mManagerApListeners.get(userId);
    }

    @Override
    public void registerManagerApListener(IBinder listener) {
        int callingUserId = UserHandle.getCallingUserId();
        mManagerApListeners.put(callingUserId, listener);
        Log.d(TAG, "registerManagerApListener() listener: " + listener + " for user "
                + callingUserId);
    }
}