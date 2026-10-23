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

package com.xtc.health;

import android.content.Context;
import android.os.RemoteException;
import android.util.Log;

/**
 * Manager for the XTC health service (MCU communication).
 *
 * @hide
 */
public class XtcHealthManager {
    private static final String TAG = "XtcHealthManager";

    final Context mContext;
    private final IXtcHealthManager mService;

    public XtcHealthManager(Context context, IXtcHealthManager service) {
        mContext = context;
        mService = service;
    }

    public void registerHealthCallback(IXtcHealthCallback callback) {
        try {
            mService.registerHealthCallback(callback);
        } catch (RemoteException e) {
            Log.e(TAG, "registerAddictionCallback got an exception");
            e.printStackTrace();
        }
    }

    public int sendDataMcu(String mcuCmd, byte[] data) {
        try {
            return mService.sendDataMcu(mcuCmd, data);
        } catch (RemoteException e) {
            Log.e(TAG, "setXtcPPGTestapi got an exception");
            e.printStackTrace();
            return -1;
        }
    }

    public void unregisterHealthCallback(IXtcHealthCallback callback) {
        try {
            mService.unregisterHealthCallback(callback);
        } catch (RemoteException e) {
            Log.e(TAG, "unregisterAddictionCallback got an exception");
            e.printStackTrace();
        }
    }
}