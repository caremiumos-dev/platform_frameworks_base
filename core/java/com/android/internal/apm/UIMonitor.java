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

import com.android.internal.apm.EvilMethodTracer.EvilMethodTracerData;
import com.android.internal.apm.FrameTracer.FrameTracerData;
import com.android.internal.apm.StartupTracer.StartupTracerData;

/**
 * Aggregates data from the evil method / frame / startup tracers into the
 * shared {@link UIMonitorData} and forwards callbacks to the APM manager.
 *
 * @hide
 */
public class UIMonitor {
    private static final String TAG = "UIMonitor";

    private final AppPerformanceMonitorManager mAPMManager;
    private final AppPerformanceMonitorManager.CallBackHandler mCallBackHandler;
    private final Object mLock = new Object();
    private APMConfiguration mAPMConfiguration;
    private EvilMethodTracer mEvilMethodTracer;
    private FrameTracer mFrameTracer;
    private StartupTracer mStartupTracer;
    private UIMonitorData mUIMonitorData = new UIMonitorData();

    public UIMonitor(APMConfiguration config, AppPerformanceMonitorManager apmManager) {
        mAPMConfiguration = config;
        mAPMManager = apmManager;
        mCallBackHandler = apmManager.getCallBackHandler();
        APMDebugLog.d(TAG, "mAPMConfiguration = " + mAPMConfiguration);
        mEvilMethodTracer = new EvilMethodTracer(this, mAPMConfiguration.getMethodCostTimeThreshold());
        mEvilMethodTracer.enableLooperMonitor();
        mFrameTracer = new FrameTracer(this, mAPMConfiguration.getDropFrameThreshold());
        mFrameTracer.enableFrameMonitor();
        mStartupTracer = new StartupTracer(this, mAPMConfiguration.getActivityLaunchTimeThreshold());
    }

    public void onAPMConfigurationChanged(APMConfiguration config) {
        APMDebugLog.d(TAG, "onAPMConfigurationChanged = " + config);
        synchronized (mLock) {
            mAPMConfiguration = config;
            mEvilMethodTracer.updateMethodCostTimeThreshold(
                    mAPMConfiguration.getMethodCostTimeThreshold());
            mFrameTracer.updateDropFrameThreshold(mAPMConfiguration.getDropFrameThreshold());
            mStartupTracer.updateActivityLaunchTimeThreshold(
                    mAPMConfiguration.getMainThreadSQLCostTimeThreshold());
        }
    }

    public void onEvilMethodTracerDataChanged(EvilMethodTracerData data) {
        synchronized (mLock) {
            if (!mAPMConfiguration.isUIMonitorEnable()
                    || !mAPMConfiguration.isEvilMethodTracerEnable()) {
                return;
            }
            if (data != null) {
                mUIMonitorData.setMinMethodCostTime(data.mMinMethodCostTime);
                mUIMonitorData.setMinMethodCostTimeScene(data.mMinMethodCostTimeScene);
                mUIMonitorData.setMaxMethodCostTime(data.mMaxMethodCostTime);
                mUIMonitorData.setMaxMethodCostTimeScene(data.mMaxMethodCostTimeScene);
                mUIMonitorData.setAvgMethodCostTime(data.mAvgMethodCostTime);
                mAPMManager.onUIMonitorDataChanged(mUIMonitorData);
                mCallBackHandler.onEvilMethodTracerDataChanged(data);
            }
        }
    }

    public void onFrameTracerDataChanged(FrameTracerData data) {
        synchronized (mLock) {
            if (!mAPMConfiguration.isUIMonitorEnable()
                    || !mAPMConfiguration.isFrameTracerEnable()) {
                return;
            }
            if (data != null) {
                mUIMonitorData.setMinDropFrame(data.mMinDropFrame);
                mUIMonitorData.setMinDropFrameScene(data.mMinDropFrameScene);
                mUIMonitorData.setMaxDropFrame(data.mMaxDropFrame);
                mUIMonitorData.setMaxDropFrameScene(data.mMaxDropFrameScene);
                mUIMonitorData.setAvgDropFrame(data.mAvgDropFrame);
                mUIMonitorData.setAvgFPS(data.mAvgFPS);
                mAPMManager.onUIMonitorDataChanged(mUIMonitorData);
                mCallBackHandler.onFrameTracerDataChanged(data);
            }
        }
    }

    public void onStartupTracerDataChanged(StartupTracerData data) {
        synchronized (mLock) {
            if (!mAPMConfiguration.isUIMonitorEnable()
                    || !mAPMConfiguration.isStartupTracerEnable()) {
                return;
            }
            if (data != null) {
                mUIMonitorData.setMinActivityLaunchTime(data.mMinActivityLaunchTime);
                mUIMonitorData.setMinActivityLaunchTimeScene(data.mMinActivityLaunchTimeScene);
                mUIMonitorData.setMaxActivityLaunchTime(data.mMaxActivityLaunchTime);
                mUIMonitorData.setMaxActivityLaunchTimeScene(data.mMaxActivityLaunchTimeScene);
                mUIMonitorData.setAvgActivityLaunchTime(data.mAvgActivityLaunchTime);
                mUIMonitorData.setActivityColdLaunchTime(data.mActivityColdLaunchTime);
                mUIMonitorData.setActivityColdLaunchTimeScene(data.mActivityColdLaunchTimeScene);
                mAPMManager.onUIMonitorDataChanged(mUIMonitorData);
                mCallBackHandler.onStartupTracerDataChanged(data);
            }
        }
    }

    public void reportActivityLaunchTimeLocked(LaunchTimeData launchTimeData) {
        synchronized (mLock) {
            mStartupTracer.reportActivityLaunchTimeLocked(launchTimeData);
        }
    }

    public void reportOverThresholdData(int eventType, long timestamp, String threshold,
            String value, String scene) {
        synchronized (mLock) {
            if (mAPMConfiguration.isUIMonitorEnable()) {
                if (eventType == APMOverThresholdEventType.TYPE_METHOD_COST_TIME
                        && !mAPMConfiguration.isEvilMethodTracerEnable()) {
                    return;
                }
                if (eventType == APMOverThresholdEventType.TYPE_DROP_FRAME
                        && !mAPMConfiguration.isFrameTracerEnable()) {
                    return;
                }
                if (eventType == APMOverThresholdEventType.TYPE_ACTIVITY_LAUNCH_TIME
                        && !mAPMConfiguration.isStartupTracerEnable()) {
                    return;
                }
                mAPMManager.reportOverThresholdData(eventType, timestamp, threshold, value, scene);
            }
        }
    }
}