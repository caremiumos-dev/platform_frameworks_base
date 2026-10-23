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
import android.util.Log;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Tracks how long the user has been confirming permissions, used to decide when
 * a permission prompt is legitimate.
 *
 * @hide
 */
class PermissionHistoryHelper {
    private static final String TAG = "PermissionHistoryHelper";
    private static final int HISTORY_TIME_BOUND = 60000;
    private static final int STATUS_ALL = -1;

    private static PermissionHistoryHelper mInstance = null;
    private final List<PackagePermissionHistory> mHistoryList = new ArrayList<>();

    PermissionHistoryHelper() {
    }

    public static PermissionHistoryHelper getInstance() {
        if (mInstance == null) {
            mInstance = new PermissionHistoryHelper();
        }
        return mInstance;
    }

    private PackagePermissionHistory findHistoryNotEnd(int userId, String packageName,
            String permissionName, int status) {
        synchronized (mHistoryList) {
            for (PackagePermissionHistory history : mHistoryList) {
                if (history.getEndTime() == 0
                        && history.getUserId() == userId
                        && history.getPackageName().equals(packageName)
                        && history.getPermissionName().equals(permissionName)
                        && (status == STATUS_ALL || history.getStatus() == status)) {
                    return history;
                }
            }
            return null;
        }
    }

    public void addHistory(int userId, String packageName, String permissionName, int status) {
        synchronized (mHistoryList) {
            PackagePermissionHistory existing = findHistoryNotEnd(userId, packageName,
                    permissionName, status);
            if (existing != null) {
                Log.w(TAG, "Avoid adding non-finished permission history: " + existing.toString());
                return;
            }
            long now = SystemClock.uptimeMillis();
            PackagePermissionHistory history = new PackagePermissionHistory(userId, packageName,
                    permissionName, now, status);
            mHistoryList.add(history);
            Log.d(TAG, "addHistory() history: " + history.toString());
            long bound = now > HISTORY_TIME_BOUND ? now - HISTORY_TIME_BOUND : -1L;
            if (bound > 0) {
                Iterator<PackagePermissionHistory> iterator = mHistoryList.iterator();
                while (iterator.hasNext()) {
                    PackagePermissionHistory next = iterator.next();
                    if (next.getEndTime() != 0 && next.getEndTime() < bound) {
                        iterator.remove();
                    }
                }
            }
        }
    }

    public void setEnd(int userId, String packageName, String permissionName, int status) {
        synchronized (mHistoryList) {
            PackagePermissionHistory history = findHistoryNotEnd(userId, packageName,
                    permissionName, status);
            if (history != null) {
                history.setEndTime(SystemClock.uptimeMillis());
                Log.d(TAG, "setEnd() userId: " + userId + " packageName: " + packageName
                        + "permission: " + permissionName + " status: " + status + " endTime: "
                        + SystemClock.uptimeMillis());
            }
        }
    }

    public boolean isPackageChecking(int userId, String packageName, String permissionName) {
        synchronized (mHistoryList) {
            SystemClock.uptimeMillis();
            return findHistoryNotEnd(userId, packageName, permissionName, 2) != null;
        }
    }

    private List<PermissionHistoryBase> getMergedHistory(int userId, long start, long end) {
        List<PermissionHistoryBase> merged = new ArrayList<>();
        synchronized (mHistoryList) {
            long mergedStart = 0;
            long mergedEnd = 0;
            for (PackagePermissionHistory history : mHistoryList) {
                if (userId != history.getUserId()) {
                    continue;
                }
                if (history.overlap(start, end)) {
                    if (mergedStart == 0) {
                        mergedStart = history.getStartTime();
                        mergedEnd = history.getEndTime() == 0 ? end : history.getEndTime();
                    } else if (history.getStartTime() > mergedEnd) {
                        merged.add(new PermissionHistoryBase(mergedStart, mergedEnd));
                        mergedStart = history.getStartTime();
                        mergedEnd = history.getEndTime();
                    } else if (history.getEndTime() > mergedEnd) {
                        mergedEnd = history.getEndTime();
                    }
                } else if (history.getEndTime() == 0 && history.getStartTime() < start) {
                    mergedEnd = end;
                    mergedStart = start;
                }
            }
            if (mergedStart != 0) {
                merged.add(new PermissionHistoryBase(mergedStart, mergedEnd));
            }
        }
        return merged;
    }

    public long getInteractionTime(int userId, long timeBound) {
        Log.d(TAG, "getInteractionTime() userId: " + userId + " timeBound(ns): " + timeBound);
        synchronized (mHistoryList) {
            long now = SystemClock.uptimeMillis();
            if (now < timeBound) {
                Log.e(TAG, "Invalid timeBound! currentTime: " + now + " timeBound: " + timeBound);
                return 0L;
            }
            long searchTime = now - timeBound;
            Log.d(TAG, "getInteractionTime() currentTime: " + now + " searchTime: " + searchTime);
            List<PermissionHistoryBase> merged = getMergedHistory(userId, searchTime, now);
            if (merged == null) {
                return 0L;
            }
            long total = 0;
            for (PermissionHistoryBase history : merged) {
                if (history.within(searchTime) && history.within(now)) {
                    return timeBound;
                }
                if (history.within(searchTime)) {
                    total += history.getEndTime() - searchTime;
                } else if (history.within(now)) {
                    total += now - history.getStartTime();
                } else {
                    total += history.totalTime();
                }
            }
            return Math.min(total, timeBound);
        }
    }
}