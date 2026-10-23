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

/**
 * Manager exposing XTC utility operations (display density, mobile data,
 * battery test and keep-awake controls).
 *
 * @hide
 */
public class XTCUtilsManager {
    private final IXTCUtilsManager mService;

    public XTCUtilsManager(Context context, IXTCUtilsManager service) {
        mService = service;
    }

    public void clearDisplayDensity() {
        try {
            mService.clearDisplayDensity();
        } catch (RemoteException e) {
            e.rethrowFromSystemServer();
        }
    }

    public void disableBatteryCurrentTestEnable() {
        try {
            mService.disableBatteryCurrentTestEnable();
        } catch (RemoteException e) {
            e.rethrowFromSystemServer();
        }
    }

    public void enableBatteryCurrentTestEnable(boolean enable) {
        try {
            mService.enableBatteryCurrentTestEnable(enable);
        } catch (RemoteException e) {
            e.rethrowFromSystemServer();
        }
    }

    public String getBatteryCurrentTestRecord() {
        try {
            return mService.getBatteryCurrentTestRecord();
        } catch (RemoteException e) {
            e.rethrowFromSystemServer();
            return null;
        }
    }

    public boolean getMobileDataEnabled() {
        try {
            return mService.getMobileDataEnabled();
        } catch (RemoteException e) {
            e.rethrowFromSystemServer();
            return false;
        }
    }

    public boolean isBatteryCurrentTestEnable() {
        try {
            return mService.isBatteryCurrentTestEnable();
        } catch (RemoteException e) {
            e.rethrowFromSystemServer();
            return false;
        }
    }

    public boolean isBatteryCurrentTestEnableWithLock() {
        try {
            return mService.isBatteryCurrentTestEnableWithLock();
        } catch (RemoteException e) {
            e.rethrowFromSystemServer();
            return false;
        }
    }

    public boolean isKeepSystemAwake() {
        try {
            return mService.isKeepSystemAwake();
        } catch (RemoteException e) {
            e.rethrowFromSystemServer();
            return false;
        }
    }

    public boolean isRoundedCornerScreen() {
        try {
            return mService.isRoundedCornerScreen();
        } catch (RemoteException e) {
            e.rethrowFromSystemServer();
            return false;
        }
    }

    public boolean isSupportChangeDisplayDensity() {
        try {
            return mService.isSupportChangeDisplayDensity();
        } catch (RemoteException e) {
            e.rethrowFromSystemServer();
            return false;
        }
    }

    public void setDisplayDensity(int density, boolean isDefault) {
        try {
            mService.setDisplayDensity(density, isDefault);
        } catch (RemoteException e) {
            e.rethrowFromSystemServer();
        }
    }

    public void setKeepSystemAwake(boolean keepAwake) {
        try {
            mService.setKeepSystemAwake(keepAwake);
        } catch (RemoteException e) {
            e.rethrowFromSystemServer();
        }
    }

    public void setMobileDataEnabled(boolean enable) {
        try {
            mService.setMobileDataEnabled(enable);
        } catch (RemoteException e) {
            e.rethrowFromSystemServer();
        }
    }
}