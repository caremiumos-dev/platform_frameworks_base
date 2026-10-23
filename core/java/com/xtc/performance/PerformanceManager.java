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

package com.xtc.performance;

import android.app.ProcessInfo;
import android.content.Context;
import android.content.Intent;
import android.content.pm.InstrumentationInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.util.Slog;

import java.util.ArrayList;

/**
 * Manager for the XTC performance (boost / prefork / LMK tuning) service.
 *
 * @hide
 */
public final class PerformanceManager {
    private static final String TAG = "xtcperformance";

    /** Whether the vendor-side performance service is supported. */
    public static boolean isSurportPMS =
            SystemProperties.getBoolean("persist.xtc.surportpms", true);

    private final IPerformance mService;

    public PerformanceManager(Context context, IPerformance service) {
        mService = service;
    }

    public void acquireBoost(int boost) {
        try {
            if (mService != null) {
                mService.acquireBoost(boost);
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "acquireBoost PerformanceManager service dead!");
        }
    }

    public void attachPreforkApplication(int pid, IBinder bindApplicationTransaction,
            String processName) {
        try {
            if (mService != null) {
                mService.attachPreforkApplication(pid, bindApplicationTransaction, processName);
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "attachPreforkApplication PerformanceManager service dead!");
        }
    }

    public int handlePreForkStartProcess(int callingPid, String packageName, String processName,
            int targetSdk, int createdPid, int[] pids, int uid, int debugFlags,
            int mountExternal, String seInfo, String appDataDir, String instructionSet,
            String appInfoDir, String zygoteForkName, String[] disabledCompatChanges) {
        try {
            if (mService != null) {
                return mService.handlePreForkStartProcess(callingPid, packageName, processName,
                        targetSdk, createdPid, pids, uid, debugFlags, mountExternal, seInfo,
                        appDataDir, instructionSet, appInfoDir, zygoteForkName,
                        disabledCompatChanges);
            }
            return -1;
        } catch (RemoteException e) {
            Slog.e(TAG, "preforkStart PerformanceManager service dead!");
            return -1;
        }
    }

    public void notifyActivityStateChange(Intent intent, Bundle extras) {
        try {
            if (mService != null) {
                mService.notifyActivityStateChange(intent, extras);
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "notifyActivityStateChange PerformanceManager service dead!");
        }
    }

    public void notifyInstrumentationStatus(boolean started, int pid, InstrumentationInfo info) {
        try {
            if (mService != null) {
                mService.notifyInstrumentationStatus(started, pid, info);
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "notifyInstrumentationStatus PerformanceManager service dead!");
        }
    }

    public void preforkStart(int callingPid, String packageName, String processName,
            int targetSdk, int createdPid, int[] pids, int uid, int debugFlags,
            int mountExternal, String seInfo, String appDataDir, String instructionSet,
            String appInfoDir, String zygoteForkName, String[] disabledCompatChanges) {
        try {
            if (mService != null) {
                mService.preforkStart(callingPid, packageName, processName, targetSdk,
                        createdPid, pids, uid, debugFlags, mountExternal, seInfo, appDataDir,
                        instructionSet, appInfoDir, zygoteForkName, disabledCompatChanges);
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "preforkStart PerformanceManager service dead!");
        }
    }

    public int processLmkAdjTunningIfneeded(ProcessInfo processInfo, int expectedAdj, long pss,
            long lastPss) {
        try {
            if (mService != null) {
                return mService.processLmkAdjTunningIfneeded(processInfo, expectedAdj, pss,
                        lastPss);
            }
            return 2;
        } catch (RemoteException e) {
            Slog.e(TAG, "processLmkAdjTunningIfneeded PerformanceManager service dead!");
            return 2;
        }
    }

    public void saveCatonStack(ArrayList<String> stack) {
        try {
            if (mService != null) {
                mService.saveCatonStack(stack);
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "saveCatonStack PerformanceManager service dead!");
        }
    }

    public void schedStartPreForkedProcesses(int pid, int targetSdk, String processName) {
        try {
            if (mService != null) {
                mService.schedStartPreForkedProcesses(pid, targetSdk, processName);
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "processLmkAdjTunningIfneeded PerformanceManager service dead!");
        }
    }

    public void scheduleBoostWhenTouch() {
        try {
            if (mService != null) {
                mService.scheduleBoostWhenTouch();
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "scheduleBoostWhenTouch PerformanceManager service dead!");
        }
    }
}