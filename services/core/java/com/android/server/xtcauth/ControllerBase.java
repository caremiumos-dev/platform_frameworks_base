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
import android.content.Context;
import android.content.pm.IPackageManager;
import android.os.IBinder;
import android.os.IUserManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

import com.android.server.performance.policy.sched.SchedAdjustment;

import java.io.PrintWriter;

/**
 * Base class for the various "mobile manager" controllers. Each controller can be
 * enabled/disabled and can have a listener registered by the management
 * application.
 *
 * @hide
 */
public abstract class ControllerBase {
    private static final String TAG = "ControllerBase";

    private boolean mEnabled = false;
    private IBinder mManagerApListener = null;

    private class ListenerDeathRecipient implements IBinder.DeathRecipient {
        IBinder mToken;

        ListenerDeathRecipient(IBinder token) {
            mToken = token;
        }

        @Override
        public void binderDied() {
            Log.e(TAG, "binderDied! mToken:" + mToken);
            mToken = null;
        }
    }

    public static IActivityManager getActivityManagerService() {
        IActivityManager am = ActivityManagerNative.getDefault();
        if (am == null) {
            throw new RuntimeException("null activity manager service");
        }
        return am;
    }

    public static IPackageManager getPackageManagerService() {
        IPackageManager pm = IPackageManager.Stub.asInterface(
                ServiceManager.getService(SchedAdjustment.TAG_PACKAGE));
        if (pm == null) {
            throw new RuntimeException("null package manager service");
        }
        return pm;
    }

    public static IUserManager getUserManagerService() {
        IUserManager um = IUserManager.Stub.asInterface(ServiceManager.getService("user"));
        if (um == null) {
            throw new RuntimeException("null user manager service");
        }
        return um;
    }

    public void enableController(boolean enable) {
        if (enable) {
            try {
                if (ActivityManagerNative.getDefault().isUserAMonkey()) {
                    Log.w(TAG, "Controller is disabled during monkey test");
                    enable = false;
                }
            } catch (RemoteException e) {
                Log.e(TAG, "isUserAMonkey() failed", e);
            }
        }
        mEnabled = enable;
    }

    public boolean isControllerEnabled() {
        return mEnabled;
    }

    public IBinder getManagerApListener() {
        return mManagerApListener;
    }

    public void registerManagerApListener(IBinder listener) {
        mManagerApListener = listener;
        Log.d(TAG, "registerManagerApListener() listener: " + listener);
        if (listener != null) {
            try {
                listener.linkToDeath(new ListenerDeathRecipient(listener), 0);
            } catch (RemoteException e) {
                Log.e(TAG, "LinkToDeath() failed! listener: " + listener);
            }
        }
    }

    public boolean isValidUserId(int userId) {
        if (userId >= 0 && userId < 100000) {
            return true;
        }
        Log.e(TAG, "Invalid userId: " + userId);
        return false;
    }

    public void dump(PrintWriter pw, MobileManagerService.DumpState dumpState) {
    }

    public abstract void systemReady(Context context);
}