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

package com.android.internal.apm;

import android.provider.SettingsStringUtil;

/**
 * Monitors main thread SQL execution cost time and reports min/max/avg
 * statistics together with over-threshold events to the APM manager.
 *
 * @hide
 */
public class SQLiteMonitor {
    private static final String TAG = "SQLiteMonitor";

    private static SQLiteMonitor sInstance;

    private final AppPerformanceMonitorManager mAPMManager;
    private final Object mLock = new Object();
    private final SQLiteMonitorData mSQLiteMonitorData = new SQLiteMonitorData();
    private APMConfiguration mAPMConfiguration;
    private long mTotalSQLCostTime;
    private long mTotalSQLCount;

    protected SQLiteMonitor(APMConfiguration config, AppPerformanceMonitorManager apmManager) {
        mAPMConfiguration = config;
        mAPMManager = apmManager;
        mSQLiteMonitorData.setMinSqlCostTime(mAPMConfiguration.getMainThreadSQLCostTimeThreshold());
        mSQLiteMonitorData.setMaxSqlCostTime(0L);
    }

    public static SQLiteMonitor getInstance() {
        return sInstance;
    }

    public static void init(APMConfiguration config, AppPerformanceMonitorManager apmManager) {
        sInstance = new SQLiteMonitor(config, apmManager);
    }

    public void onAPMConfigurationChanged(APMConfiguration config) {
        APMDebugLog.d(TAG, "onAPMConfigurationChanged = " + config);
        synchronized (mLock) {
            mAPMConfiguration = config;
        }
    }

    public void onSqliteProfileCallback(String dbName, String file, long costTime) {
        boolean changed;
        boolean dataChanged = true;
        long currentTimeMillis = System.currentTimeMillis();
        synchronized (mLock) {
            if (mAPMConfiguration.isSQLiteMonitorEnable()) {
                String scene = dbName + SettingsStringUtil.DELIMITER + file;
                if (costTime > mAPMConfiguration.getMainThreadSQLCostTimeThreshold()) {
                    reportOverThresholdLocked(currentTimeMillis, costTime, scene);
                }
                if (costTime < mSQLiteMonitorData.getMinSqlCostTime()) {
                    mSQLiteMonitorData.setMinSqlCostTime(costTime);
                    mSQLiteMonitorData.setMinSqlCostTimeScene(scene);
                    changed = true;
                } else if (costTime > mSQLiteMonitorData.getMaxSqlCostTime()) {
                    mSQLiteMonitorData.setMaxSqlCostTime(costTime);
                    mSQLiteMonitorData.setMaxSqlCostTimeScene(scene);
                    changed = true;
                } else {
                    changed = false;
                }
                mTotalSQLCostTime += costTime;
                mTotalSQLCount++;
                if (mTotalSQLCount > 0) {
                    final long avg = mTotalSQLCostTime / mTotalSQLCount;
                    if (mSQLiteMonitorData.getAvgSqlCostTime() != avg) {
                        mSQLiteMonitorData.setAvgSqlCostTime(avg);
                    } else {
                        dataChanged = changed;
                    }
                } else {
                    dataChanged = changed;
                }
                if (dataChanged) {
                    mAPMManager.onSQLiteMonitorDataChanged(mSQLiteMonitorData);
                }
                APMDebugLog.d(TAG, "onSqliteProfileCallback used = "
                        + (System.currentTimeMillis() - currentTimeMillis));
            }
        }
    }

    private void reportOverThresholdLocked(long timestamp, long costTime, String scene) {
        APMDebugLog.d(TAG, "reportOverThresholdLocked");
        mAPMManager.reportOverThresholdData(
                APMOverThresholdEventType.TYPE_MAIN_THREAD_SQL_COST_TIME, timestamp,
                String.valueOf(mAPMConfiguration.getMainThreadSQLCostTimeThreshold()),
                String.valueOf(costTime), scene);
    }
}