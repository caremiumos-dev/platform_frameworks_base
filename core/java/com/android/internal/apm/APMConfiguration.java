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

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Configuration for the application performance monitor (which tracers are
 * enabled and the corresponding thresholds).
 *
 * @hide
 */
public class APMConfiguration implements Parcelable {
    private static final String TAG = "APMConfiguration";

    public static final long DEFAULT_METHOD_COST_TIME_THRESHOLD = 200L;
    public static final int DEFAULT_DROP_FRAME_THRESHOLD = 24;
    public static final long DEFAULT_ACTIVITY_LAUNCH_TIME_THRESHOLD = 5000L;
    public static final long DEFAULT_MAIN_THREAD_SQL_COST_TIME_THRESHOLD = 100L;

    public static final Parcelable.Creator<APMConfiguration> CREATOR =
            new Parcelable.Creator<APMConfiguration>() {
                @Override
                public APMConfiguration createFromParcel(Parcel in) {
                    return new APMConfiguration(in);
                }

                @Override
                public APMConfiguration[] newArray(int size) {
                    return new APMConfiguration[size];
                }
            };

    private boolean mUIMonitorEnable;
    private boolean mEvilMethodTracerEnable;
    private boolean mFrameTracerEnable;
    private boolean mStartupTracerEnable;
    private long mMethodCostTimeThreshold;
    private int mDropFrameThreshold;
    private long mActivityLaunchTimeThreshold;
    private boolean mSQLiteMonitorEnable;
    private long mMainThreadSQLCostTimeThreshold;

    public APMConfiguration() {
        mMethodCostTimeThreshold = DEFAULT_METHOD_COST_TIME_THRESHOLD;
        mDropFrameThreshold = DEFAULT_DROP_FRAME_THRESHOLD;
        mActivityLaunchTimeThreshold = DEFAULT_ACTIVITY_LAUNCH_TIME_THRESHOLD;
        mMainThreadSQLCostTimeThreshold = DEFAULT_MAIN_THREAD_SQL_COST_TIME_THRESHOLD;
    }

    protected APMConfiguration(Parcel in) {
        mMethodCostTimeThreshold = DEFAULT_METHOD_COST_TIME_THRESHOLD;
        mDropFrameThreshold = DEFAULT_DROP_FRAME_THRESHOLD;
        mActivityLaunchTimeThreshold = DEFAULT_ACTIVITY_LAUNCH_TIME_THRESHOLD;
        mMainThreadSQLCostTimeThreshold = DEFAULT_MAIN_THREAD_SQL_COST_TIME_THRESHOLD;
        mUIMonitorEnable = in.readBoolean();
        mEvilMethodTracerEnable = in.readBoolean();
        mFrameTracerEnable = in.readBoolean();
        mStartupTracerEnable = in.readBoolean();
        mMethodCostTimeThreshold = in.readLong();
        mDropFrameThreshold = in.readInt();
        mActivityLaunchTimeThreshold = in.readLong();
        mSQLiteMonitorEnable = in.readBoolean();
        mMainThreadSQLCostTimeThreshold = in.readLong();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeBoolean(mUIMonitorEnable);
        out.writeBoolean(mEvilMethodTracerEnable);
        out.writeBoolean(mFrameTracerEnable);
        out.writeBoolean(mStartupTracerEnable);
        out.writeLong(mMethodCostTimeThreshold);
        out.writeInt(mDropFrameThreshold);
        out.writeLong(mActivityLaunchTimeThreshold);
        out.writeBoolean(mSQLiteMonitorEnable);
        out.writeLong(mMainThreadSQLCostTimeThreshold);
    }

    public boolean isUIMonitorEnable() {
        return mUIMonitorEnable;
    }

    public void setUIMonitorEnable(boolean enable) {
        mUIMonitorEnable = enable;
    }

    public boolean isEvilMethodTracerEnable() {
        return mEvilMethodTracerEnable;
    }

    public void setEvilMethodTracerEnable(boolean enable) {
        mEvilMethodTracerEnable = enable;
    }

    public boolean isFrameTracerEnable() {
        return mFrameTracerEnable;
    }

    public void setFrameTracerEnable(boolean enable) {
        mFrameTracerEnable = enable;
    }

    public boolean isStartupTracerEnable() {
        return mStartupTracerEnable;
    }

    public void setStartupTracerEnable(boolean enable) {
        mStartupTracerEnable = enable;
    }

    public long getMethodCostTimeThreshold() {
        return mMethodCostTimeThreshold;
    }

    public void setMethodCostTimeThreshold(long threshold) {
        mMethodCostTimeThreshold = threshold;
    }

    public int getDropFrameThreshold() {
        return mDropFrameThreshold;
    }

    public void setDropFrameThreshold(int threshold) {
        mDropFrameThreshold = threshold;
    }

    public long getActivityLaunchTimeThreshold() {
        return mActivityLaunchTimeThreshold;
    }

    public void setActivityLaunchTimeThreshold(long threshold) {
        mActivityLaunchTimeThreshold = threshold;
    }

    public boolean isSQLiteMonitorEnable() {
        return mSQLiteMonitorEnable;
    }

    public void setSQLiteMonitorEnable(boolean enable) {
        mSQLiteMonitorEnable = enable;
    }

    public long getMainThreadSQLCostTimeThreshold() {
        return mMainThreadSQLCostTimeThreshold;
    }

    public void setMainThreadSQLCostTimeThreshold(long threshold) {
        mMainThreadSQLCostTimeThreshold = threshold;
    }

    @Override
    public String toString() {
        return "APMConfiguration{mUIMonitorEnable=" + mUIMonitorEnable
                + ", mEvilMethodTracerEnable=" + mEvilMethodTracerEnable
                + ", mFrameTracerEnable=" + mFrameTracerEnable
                + ", mStartupTracerEnable=" + mStartupTracerEnable
                + ", mMethodCostTimeThreshold=" + mMethodCostTimeThreshold
                + ", mDropFrameThreshold=" + mDropFrameThreshold
                + ", mActivityLaunchTimeThreshold=" + mActivityLaunchTimeThreshold
                + ", mSQLiteMonitorEnable=" + mSQLiteMonitorEnable
                + ", mMainThreadSQLCostTimeThreshold=" + mMainThreadSQLCostTimeThreshold + '}';
    }
}