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

package com.xtc.secure.pm;

import android.content.Context;
import android.os.RemoteException;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Manages a {@link AppInstallerClient} binding with a connection timeout.
 *
 * @hide
 */
public class AppInstallerClientMgr {
    private static final String TAG = "AppInstallerClientMgr";
    private static final long SERVICE_CONNECTION_TIME_OUT = 3000;

    private final Context mContext;
    private final Object mServiceMutex = new Object();
    private final ServiceConnectionTimerTask mTimerTask = new ServiceConnectionTimerTask();

    private AppInstallerClient mAppInstallerClient;
    private Timer mConnectionTimer;
    private boolean mConnected = false;

    private final AppInstallerClient.OnConnectedListener mListener =
            new AppInstallerClient.OnConnectedListener() {
        @Override
        public void onConnected(AppInstallerClient client) {
            synchronized (mServiceMutex) {
                mConnected = true;
                mServiceMutex.notify();
            }
        }

        @Override
        public void onDisconnected() {
            synchronized (mServiceMutex) {
                mConnected = false;
                mServiceMutex.notify();
            }
        }
    };

    private class ServiceConnectionTimerTask extends TimerTask {
        @Override
        public void run() {
            synchronized (mServiceMutex) {
                mServiceMutex.notify();
            }
        }
    }

    public AppInstallerClientMgr(Context context) {
        if (context == null) {
            throw new NullPointerException("context must not be null");
        }
        mContext = context;
        bindXtcInstallerService();
    }

    private void bindXtcInstallerService() {
        mAppInstallerClient = new AppInstallerClient(mContext, mListener);
        mConnectionTimer = new Timer();
        mConnectionTimer.schedule(mTimerTask, SERVICE_CONNECTION_TIME_OUT);
    }

    private boolean waitForConnect() {
        synchronized (mServiceMutex) {
            if (!mConnected) {
                try {
                    mServiceMutex.wait(SERVICE_CONNECTION_TIME_OUT);
                } catch (InterruptedException e) {
                    return false;
                }
            }
            if (!mConnected) {
                return false;
            }
            if (mConnectionTimer != null) {
                mConnectionTimer.cancel();
                mConnectionTimer = null;
            }
            return true;
        }
    }

    public void unbindXtcInstallerService() {
        if (mAppInstallerClient != null) {
            if (mAppInstallerClient.isConnected()) {
                mAppInstallerClient.shutdown();
            }
            mConnected = false;
            mAppInstallerClient = null;
        }
    }

    public void verifyOnXtcServer(String packagePath, int from, IAppVerifyCallback callback) {
        if (waitForConnect()) {
            if (mAppInstallerClient != null) {
                mAppInstallerClient.verifyOnServer(packagePath, from, callback);
            }
        } else {
            if (callback != null) {
                try {
                    callback.onVerifyResult(packagePath,
                            AppInstallerConstants.VerifyResult.FAIL_NOT_BIND,
                            "the xtc aidl server can not connect !");
                } catch (RemoteException e) {
                    Log.w(TAG, "IAppInstaller callback fail !");
                }
            }
            Log.w(TAG, "IAppInstaller service not usefully !");
        }
    }
}