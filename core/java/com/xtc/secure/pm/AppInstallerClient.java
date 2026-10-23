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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

/**
 * Client binding to the XTC app installer/verifier service.
 *
 * @hide
 */
public class AppInstallerClient {
    private static final String TAG = "AppInstallerClient";

    private final Context mContext;
    private final ServiceConnection mConnection;
    private volatile IAppInstaller mAppInstaller;

    public interface OnConnectedListener {
        void onConnected(AppInstallerClient client);

        void onDisconnected();
    }

    public AppInstallerClient(Context context, final OnConnectedListener listener) {
        if (context == null) {
            throw new NullPointerException("context must not be null");
        }
        mContext = context;
        mConnection = new ServiceConnection() {
            @Override
            public synchronized void onServiceConnected(ComponentName name, IBinder service) {
                mAppInstaller = IAppInstaller.Stub.asInterface(service);
                if (listener != null) {
                    listener.onConnected(AppInstallerClient.this);
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                mAppInstaller = null;
                if (listener != null) {
                    listener.onDisconnected();
                }
            }
        };
        bindService();
    }

    private void bindService() {
        Intent intent = new Intent();
        intent.setClassName("com.xtc.appupdate",
                "com.xtc.appupdate.service.AppInstallerService");
        intent.setAction("com.xtc.appsecure.appinstall.service");
        if (mContext.bindService(intent, mConnection, Context.BIND_AUTO_CREATE)) {
            Log.d(TAG, "IAppInstaller bind launcher Service Success");
        } else {
            Log.w(TAG, "IAppInstaller bind fail.");
        }
    }

    public boolean isConnected() {
        return mAppInstaller != null;
    }

    public void shutdown() {
        try {
            mContext.unbindService(mConnection);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "shutdown ex:" + e);
        }
        mAppInstaller = null;
    }

    public void verifyOnServer(String packagePath, int from, IAppVerifyCallback callback) {
        if (!isConnected()) {
            if (callback != null) {
                try {
                    callback.onVerifyResult(packagePath,
                            AppInstallerConstants.VerifyResult.FAIL_NOT_BIND,
                            "the xtc aidl server not connected !");
                } catch (RemoteException e) {
                    Log.w(TAG, "IAppInstaller callback fail !");
                }
            }
            Log.w(TAG, "IAppInstaller service not binded !");
            return;
        }
        try {
            mAppInstaller.verifyOnServer(packagePath, from, callback);
        } catch (RemoteException e) {
            if (callback != null) {
                try {
                    callback.onVerifyResult(packagePath,
                            AppInstallerConstants.VerifyResult.FAIL_REMOTE_EXCEPTION,
                            "the xtc aidl server call fail !");
                } catch (RemoteException e2) {
                    Log.w(TAG, "IAppInstaller callback fail !");
                }
            }
        }
    }
}