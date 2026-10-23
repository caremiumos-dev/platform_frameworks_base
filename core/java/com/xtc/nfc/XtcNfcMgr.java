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

package com.xtc.nfc;

import android.content.Context;
import android.os.RemoteException;
import android.util.Slog;

/**
 * Handler for the XTC NFC service.
 *
 * @hide
 */
public final class XtcNfcMgr {
    private static final String TAG = "XtcNfcManager";

    private final IXtcNfc mService;

    public XtcNfcMgr(Context context, IXtcNfc service) {
        mService = service;
    }

    public void configRoute(String route) {
        try {
            mService.configRoute(route);
        } catch (RemoteException e) {
            Slog.e(TAG, "XtcNfcMgr service dead call configRoute ex: " + e);
        }
    }

    public void selectSE(String seId, String aid) {
        try {
            mService.selectSE(seId, aid);
        } catch (RemoteException e) {
            Slog.e(TAG, "XtcNfcMgr service dead call selectSE ex: " + e);
        }
    }

    public int setConfig(String key, String value) {
        XtcNfcSecure.enforceUserPerm(value);
        try {
            return mService.setConfig(key, value);
        } catch (RemoteException e) {
            Slog.e(TAG, "XtcNfcMgr service dead call setConfig ex: " + e);
            return 255;
        }
    }

    public void setDiscoveryTech(String tech) {
        try {
            mService.setDiscoveryTech(tech);
        } catch (RemoteException e) {
            Slog.e(TAG, "XtcNfcMgr service dead call setDiscoveryTech ex: " + e);
        }
    }

    public boolean setHceUid(byte[] uid) {
        try {
            return mService.setHceUid(uid);
        } catch (RemoteException e) {
            Slog.e(TAG, "XtcNfcMgr service dead call setHceUid ex: " + e);
            return false;
        }
    }

    public void startRfDiscovery(boolean on, int tech) {
        try {
            mService.startRfDiscovery(on, tech);
        } catch (RemoteException e) {
            Slog.e(TAG, "XtcNfcMgr service dead call startRfDiscovery ex: " + e);
        }
    }
}