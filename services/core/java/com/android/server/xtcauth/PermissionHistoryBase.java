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

package com.android.server.xtcauth;

import android.os.SystemClock;

/**
 * A time interval during which a permission was being checked.
 *
 * @hide
 */
public class PermissionHistoryBase {
    private static final int ODD_PRIME_NUMBER = 13;

    private long mStartTime;
    private long mEndTime;

    public PermissionHistoryBase(long startTime) {
        this(startTime, 0L);
    }

    public PermissionHistoryBase(long startTime, long endTime) {
        if (startTime > endTime && endTime != 0) {
            long tmp = startTime;
            startTime = endTime;
            endTime = tmp;
        }
        mStartTime = startTime;
        mEndTime = endTime;
    }

    public long getStartTime() {
        return mStartTime;
    }

    public long getEndTime() {
        return mEndTime;
    }

    public void setEndTime(long endTime) {
        mEndTime = endTime;
    }

    public boolean overlap(long start, long end) {
        if (mEndTime == 0) {
            return start <= mStartTime;
        }
        return mStartTime <= end && mEndTime >= start;
    }

    public boolean within(long time) {
        if (mEndTime == 0) {
            return time >= mStartTime;
        }
        return time >= mStartTime && time <= mEndTime;
    }

    public long totalTime() {
        return mEndTime == 0 ? SystemClock.uptimeMillis() - mStartTime : mEndTime - mStartTime;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof PermissionHistoryBase)) {
            return false;
        }
        PermissionHistoryBase other = (PermissionHistoryBase) obj;
        return mStartTime == other.getStartTime() && mEndTime == other.getEndTime();
    }

    @Override
    public int hashCode() {
        return ((Long.valueOf(mStartTime).hashCode() + 299) * ODD_PRIME_NUMBER)
                + Long.valueOf(mEndTime).hashCode();
    }

    @Override
    public String toString() {
        return "PackagePermissionHistory {" + getStartTime() + ", " + getEndTime() + "}";
    }
}