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

package com.xtc.xtcdiag;

import android.content.Context;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.util.Slog;

/**
 * Manager for the XTC diag (socket diag) service.
 *
 * @hide
 */
public class XtcDiagMgr {
    private static final String TAG = "XtcDiag-MgrInner";

    /** System property that determines whether diag is enabled. */
    public static final String DIAG_ENABLE = "persist.sys.xtc.diag";

    private final Context mContext;
    private final IXtcDiag mService;

    public XtcDiagMgr(Context context, IXtcDiag service) {
        mContext = context;
        mService = service;
        Slog.d(TAG, "XtcDiagMgr init");
    }

    public static boolean isEnable() {
        return 1 == SystemProperties.getInt(DIAG_ENABLE, 1);
    }

    public boolean isSocketConnected() {
        try {
            return mService.isSocketConnected();
        } catch (RemoteException e) {
            Slog.e(TAG, "isSocketConnected happen error:" + e);
            return false;
        }
    }

    public void registerCmdReceiveCallBack(IXtcDiagCmdReceiveCallback callback) {
        try {
            mService.registerCmdReceiveCallBack(callback);
        } catch (RemoteException e) {
            Slog.e(TAG, "registerDataCallBack happen error:" + e);
        }
    }

    public void registerConnStateCallBack(IXtcDiagConnStateCallback callback) {
        try {
            mService.registerConnStateCallBack(callback);
        } catch (RemoteException e) {
            Slog.e(TAG, "registerConnStateCallBack happen error:" + e);
        }
    }

    public void registerDiagListener(XtcDiagListener listener) {
        registerConnStateCallBack(listener.mConnStateCb);
        registerCmdReceiveCallBack(listener.mCmdReceiveCb);
    }

    public void send(int cmd, String receiver, byte[] data) {
        try {
            mService.send(cmd, receiver, data);
        } catch (RemoteException e) {
            Slog.e(TAG, "send happen error:" + e);
        }
    }

    public void sendFile(String path, String fileName, String receiver, int cmd) {
        try {
            mService.sendFile(path, fileName, receiver, cmd);
        } catch (RemoteException e) {
            Slog.e(TAG, "sendFile happen error:" + e);
        }
    }

    public void startSocketClient(String serverAddr) {
        try {
            mService.startSocketClient(serverAddr);
        } catch (RemoteException e) {
            Slog.e(TAG, "startSocketClient happen error:" + e);
        }
    }

    public void startSocketServer() {
        try {
            mService.startSocketServer();
        } catch (RemoteException e) {
            Slog.e(TAG, "startSocketServer happen error:" + e);
        }
    }

    public void stopSocketClient() {
        try {
            mService.stopSocketClient();
        } catch (RemoteException e) {
            Slog.e(TAG, "stopSocketClient happen error:" + e);
        }
    }

    public void stopSocketServer() {
        try {
            mService.stopSocketServer();
        } catch (RemoteException e) {
            Slog.e(TAG, "stopSocketServer happen error:" + e);
        }
    }

    public void unregisterCmdReceiveCallBack(IXtcDiagCmdReceiveCallback callback) {
        try {
            mService.unregisterCmdReceiveCallBack(callback);
        } catch (RemoteException e) {
            Slog.e(TAG, "unregisterDataCallBack happen error:" + e);
        }
    }

    public void unregisterConnStateCallBack(IXtcDiagConnStateCallback callback) {
        try {
            mService.unregisterConnStateCallBack(callback);
        } catch (RemoteException e) {
            Slog.e(TAG, "unregisterConnStateCallBack happen error:" + e);
        }
    }

    public void unregisterDiagListener(XtcDiagListener listener) {
        unregisterConnStateCallBack(listener.mConnStateCb);
        unregisterCmdReceiveCallBack(listener.mCmdReceiveCb);
    }
}