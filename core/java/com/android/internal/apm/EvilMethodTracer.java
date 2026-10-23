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

import android.os.Looper;
import android.os.Message;
import android.text.format.DateFormat;

/**
 * Tracks main thread message dispatch cost time to find "evil" (slow)
 * methods, reporting over-threshold dispatches to the UI monitor.
 *
 * @hide
 */
public class EvilMethodTracer {
    private static final String TAG = "EvilMethodTracer";

    private final UIMonitor mUIMonitor;
    private final Object mLock = new Object();
    private long mMethodCostTimeThreshold;
    private long mTotalMethodCostTime;
    private long mTotalMethodCount;
    private EvilMethodTracerData mEvilMethodTracerData = new EvilMethodTracerData();

    private final LooperMonitorCallback mLooperMonitorCallback = new LooperMonitorCallback() {
        @Override
        public void onDispatchFinished(Message msg, long costTime) {
            final boolean changed;
            boolean dataChanged = true;
            synchronized (mLock) {
                final String mainInfo = msg.getMainInfo();
                if (costTime > mMethodCostTimeThreshold) {
                    reportOverThresholdLocked(System.currentTimeMillis(), costTime, mainInfo);
                }
                if (costTime <= mEvilMethodTracerData.mMinMethodCostTime
                        || mEvilMethodTracerData.mMinMethodCostTime == -1) {
                    mEvilMethodTracerData.mMinMethodCostTime = costTime;
                    mEvilMethodTracerData.mMinMethodCostTimeScene = mainInfo;
                    changed = true;
                } else if (costTime >= mEvilMethodTracerData.mMaxMethodCostTime) {
                    mEvilMethodTracerData.mMaxMethodCostTime = costTime;
                    mEvilMethodTracerData.mMaxMethodCostTimeScene = mainInfo;
                    changed = true;
                } else {
                    changed = false;
                }
                mTotalMethodCostTime += costTime;
                mTotalMethodCount++;
                if (mTotalMethodCount > 0) {
                    final long avg = mTotalMethodCostTime / mTotalMethodCount;
                    if (mEvilMethodTracerData.mAvgMethodCostTime != avg) {
                        mEvilMethodTracerData.mAvgMethodCostTime = avg;
                    } else {
                        dataChanged = changed;
                    }
                } else {
                    dataChanged = changed;
                }
                if (dataChanged) {
                    mUIMonitor.onEvilMethodTracerDataChanged(mEvilMethodTracerData);
                }
            }
        }
    };

    public EvilMethodTracer(UIMonitor uiMonitor, long methodCostTimeThreshold) {
        mUIMonitor = uiMonitor;
        mMethodCostTimeThreshold = methodCostTimeThreshold;
    }

    public void enableLooperMonitor() {
        APMDebugLog.d(TAG, "enableLooperMonitor");
        Looper.getMainLooper().setLooperMonitorCallback(mLooperMonitorCallback);
    }

    public void disableLooperMonitor() {
        APMDebugLog.d(TAG, "disableLooperMonitor");
        Looper.getMainLooper().setLooperMonitorCallback(null);
    }

    public void updateMethodCostTimeThreshold(long threshold) {
        APMDebugLog.d(TAG, "updateMethodCostTimeThreshold = " + threshold);
        synchronized (mLock) {
            mMethodCostTimeThreshold = threshold;
        }
    }

    private void reportOverThresholdLocked(long timestamp, long costTime, String scene) {
        APMDebugLog.d(TAG, "reportOverThresholdLocked");
        mUIMonitor.reportOverThresholdData(APMOverThresholdEventType.TYPE_METHOD_COST_TIME,
                timestamp, String.valueOf(mMethodCostTimeThreshold), String.valueOf(costTime),
                scene);
    }

    public class EvilMethodTracerData {
        public long mMinMethodCostTime = -1;
        public long mMaxMethodCostTime = 0;
        public long mAvgMethodCostTime;
        public String mMinMethodCostTimeScene;
        public String mMaxMethodCostTimeScene;

        public EvilMethodTracerData() {
        }

        @Override
        public String toString() {
            return "EvilMethodTracerData{mMinMethodCostTime=" + mMinMethodCostTime
                    + ", mMaxMethodCostTime=" + mMaxMethodCostTime
                    + ", mAvgMethodCostTime=" + mAvgMethodCostTime
                    + ", mMinMethodCostTimeScene='" + mMinMethodCostTimeScene + DateFormat.QUOTE
                    + ", mMaxMethodCostTimeScene='" + mMaxMethodCostTimeScene + DateFormat.QUOTE + '}';
        }
    }
}