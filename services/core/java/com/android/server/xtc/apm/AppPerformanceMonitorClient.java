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

package com.android.server.xtc.apm;

import android.os.IBinder;
import android.os.RemoteException;

import com.android.internal.apm.APMDebugLog;
import com.android.internal.apm.APMGeneralData;
import com.android.internal.apm.IAppPerformanceMonitorCallback;
import com.android.internal.apm.SQLiteMonitorData;
import com.android.internal.apm.UIMonitorData;

/**
 * A single monitored application process as seen by
 * {@link AppPerformanceMonitorService}.
 *
 * @hide
 */
public class AppPerformanceMonitorClient {
    private static final String TAG = "AppPerformanceMonitorClient";

    private final APMGeneralData mAPMData;
    protected final IAppPerformanceMonitorCallback mCallback;
    private final String mPackageName;
    private final String mProcessName;
    private final AppPerformanceMonitorService mService;
    private final int mVersionCode;
    private final String mVersionName;
    private final long mStartTimestamp = System.currentTimeMillis();

    private final IBinder.DeathRecipient mDeathRecipient = new IBinder.DeathRecipient() {
        @Override
        public void binderDied() {
            APMDebugLog.d(TAG, "binderDied pkgName = " + mPackageName
                    + ", processName = " + mProcessName);
            mService.onAppDied(AppPerformanceMonitorClient.this);
        }
    };

    public AppPerformanceMonitorClient(AppPerformanceMonitorService service, String packageName,
            int versionCode, String versionName, String processName,
            IAppPerformanceMonitorCallback callback) {
        this.mService = service;
        this.mPackageName = packageName;
        this.mProcessName = processName;
        this.mVersionCode = versionCode;
        this.mVersionName = versionName;
        this.mCallback = callback;
        this.mAPMData = new APMGeneralData(packageName, processName, versionCode, versionName);
        this.mAPMData.setStartTimestamp(this.mStartTimestamp);
        try {
            this.mCallback.asBinder().linkToDeath(this.mDeathRecipient, 0);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public APMGeneralData getAPMData() {
        return this.mAPMData;
    }

    public String getPackageName() {
        return this.mPackageName;
    }

    public String getProcessName() {
        return this.mProcessName;
    }

    public long getStartTimestamp() {
        return this.mStartTimestamp;
    }

    public int getVersionCode() {
        return this.mVersionCode;
    }

    @Override
    public String toString() {
        return "AppPerformanceMonitorClient{mPackageName='" + this.mPackageName
                + "', mProcessName='" + this.mProcessName + "', mVersionCode="
                + this.mVersionCode + '}';
    }

    public void updateSQLiteMonitorData(SQLiteMonitorData data) {
        this.mAPMData.setSQLiteMonitorData(data);
    }

    public void updateUIMonitorData(UIMonitorData data) {
        this.mAPMData.setUIMonitorData(data);
    }
}