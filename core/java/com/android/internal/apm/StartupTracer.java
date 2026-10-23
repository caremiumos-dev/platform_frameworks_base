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

/**
 * Tracks activity cold launch and average launch times of the current
 * process, reporting over-threshold launches to the UI monitor.
 *
 * @hide
 */
public class StartupTracer {
    private static final String TAG = "StartupTracer";

    private final UIMonitor mUIMonitor;
    private final Object mLock = new Object();
    private long mActivityLaunchTimeThreshold;
    private boolean mColdLaunch = true;
    private long mTotalTime;
    private int mCount;
    private StartupTracerData mStartupTracerData = new StartupTracerData();

    public StartupTracer(UIMonitor uiMonitor, long activityLaunchTimeThreshold) {
        mUIMonitor = uiMonitor;
        mActivityLaunchTimeThreshold = activityLaunchTimeThreshold;
    }

    public void reportActivityLaunchTimeLocked(LaunchTimeData launchTimeData) {
        long currentTimeMillis = System.currentTimeMillis();
        synchronized (mLock) {
            if (launchTimeData != null) {
                final long launchTime = launchTimeData.getTime();
                final String componentName = launchTimeData.getComponentName();
                if (componentName == null) {
                    APMDebugLog.w(TAG, "Undefined scene: no focused activity");
                    return;
                }
                if (launchTime > mActivityLaunchTimeThreshold) {
                    reportOverThresholdLocked(currentTimeMillis, launchTime, componentName);
                }
                boolean dataChanged = true;
                if (mColdLaunch) {
                    mStartupTracerData.mActivityColdLaunchTime = launchTime;
                    mStartupTracerData.mActivityColdLaunchTimeScene = componentName;
                    mColdLaunch = false;
                } else {
                    boolean changed;
                    if (launchTime <= mStartupTracerData.mMinActivityLaunchTime
                            || mStartupTracerData.mMinActivityLaunchTime == -1) {
                        mStartupTracerData.mMinActivityLaunchTime = launchTime;
                        mStartupTracerData.mMinActivityLaunchTimeScene = componentName;
                        changed = true;
                    } else if (launchTime >= mStartupTracerData.mMaxActivityLaunchTime) {
                        mStartupTracerData.mMaxActivityLaunchTime = launchTime;
                        mStartupTracerData.mMaxActivityLaunchTimeScene = componentName;
                        changed = true;
                    } else {
                        changed = false;
                    }
                    mTotalTime = launchTime + mTotalTime;
                    mCount++;
                    if (mCount > 0) {
                        final long avg = mTotalTime / ((long) mCount);
                        if (mStartupTracerData.mAvgActivityLaunchTime != avg) {
                            mStartupTracerData.mAvgActivityLaunchTime = avg;
                        } else {
                            dataChanged = changed;
                        }
                    } else {
                        dataChanged = changed;
                    }
                }
                if (dataChanged) {
                    mUIMonitor.onStartupTracerDataChanged(mStartupTracerData);
                }
            }
            APMDebugLog.d(TAG, "reportActivityLaunchTimeLocked used = "
                    + (System.currentTimeMillis() - currentTimeMillis) + "ms, data = "
                    + launchTimeData);
        }
    }

    public void updateActivityLaunchTimeThreshold(long threshold) {
        synchronized (mLock) {
            mActivityLaunchTimeThreshold = threshold;
        }
    }

    private void reportOverThresholdLocked(long timestamp, long launchTime, String scene) {
        APMDebugLog.d(TAG, "reportOverThresholdLocked");
        mUIMonitor.reportOverThresholdData(
                APMOverThresholdEventType.TYPE_ACTIVITY_LAUNCH_TIME, timestamp,
                String.valueOf(mActivityLaunchTimeThreshold), String.valueOf(launchTime), scene);
    }

    public class StartupTracerData {
        public long mActivityColdLaunchTime;
        public String mActivityColdLaunchTimeScene;
        public long mMinActivityLaunchTime = -1;
        public long mMaxActivityLaunchTime = 0;
        public long mAvgActivityLaunchTime;
        public String mMinActivityLaunchTimeScene;
        public String mMaxActivityLaunchTimeScene;

        public StartupTracerData() {
        }
    }
}