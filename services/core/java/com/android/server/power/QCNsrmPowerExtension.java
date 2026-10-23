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

package com.android.server.power;

import android.os.Binder;
import android.os.Process;

import java.util.ArrayList;

/**
 * Qualcomm extension that lets the framework block wake locks from selected uids.
 *
 * @hide
 */
public final class QCNsrmPowerExtension {
    static final String TAG = "QCNsrmPowerExtn";
    static final boolean localLOGV = false;

    private static final int WAKE_LOCK_LEVEL_PARTIAL = 1;
    private static final int DIRTY_WAKE_LOCKS = 1;

    private final ArrayList<Integer> mPmsBlockedUids = new ArrayList<>();
    private PowerManagerService pmHandle;

    public QCNsrmPowerExtension(PowerManagerService powerManagerService) {
        pmHandle = powerManagerService;
    }

    private boolean checkWorkSourceObjectId(int uid, PowerManagerService.WakeLock wakeLock) {
        for (int i = 0; i < wakeLock.mWorkSource.size(); i++) {
            try {
                if (uid == wakeLock.mWorkSource.get(i)) {
                    return true;
                }
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    private void updatePmsBlockedUids(int uid, boolean blocked) {
        if (blocked) {
            mPmsBlockedUids.add(new Integer(uid));
        } else {
            mPmsBlockedUids.clear();
        }
    }

    private boolean updatePmsBlockedWakelock(PowerManagerService.WakeLock wakeLock,
            boolean disabled) {
        if (wakeLock == null || (wakeLock.mFlags & 0xffff) != WAKE_LOCK_LEVEL_PARTIAL
                || wakeLock.mDisabled == disabled || pmHandle == null) {
            return false;
        }
        wakeLock.mDisabled = disabled;
        if (wakeLock.mDisabled) {
            pmHandle.notifyWakeLockReleasedLocked(wakeLock);
        } else {
            pmHandle.notifyWakeLockAcquiredLocked(wakeLock);
        }
        return true;
    }

    protected void checkPmsBlockedWakelocks(int uid, int flags, int ownerUid, String tag,
            PowerManagerService.WakeLock wakeLock) {
        if (!mPmsBlockedUids.contains(new Integer(uid)) || uid == Process.myUid()) {
            return;
        }
        updatePmsBlockedWakelock(wakeLock, true);
    }

    protected boolean processPmsBlockedUid(int uid, boolean blocked,
            ArrayList<PowerManagerService.WakeLock> wakeLocks) {
        if (updatePmsBlockedUidAllowed(uid, blocked)) {
            return false;
        }
        boolean changed = false;
        for (int i = 0; i < wakeLocks.size(); i++) {
            PowerManagerService.WakeLock wakeLock = wakeLocks.get(i);
            if (wakeLock != null
                    && (wakeLock.mOwnerUid == uid
                            || checkWorkSourceObjectId(uid, wakeLock)
                            || (wakeLock.mTag.startsWith("*sync*") && wakeLock.mOwnerUid == 1000))
                    && updatePmsBlockedWakelock(wakeLock, blocked)) {
                changed = true;
            }
        }
        if (changed) {
            pmHandle.mDirty |= DIRTY_WAKE_LOCKS;
            pmHandle.updatePowerStateLocked();
        }
        return changed;
    }

    protected boolean updatePmsBlockedUidAllowed(int uid, boolean blocked) {
        if (Binder.getCallingUid() != 1000) {
            return true;
        }
        updatePmsBlockedUids(uid, blocked);
        return false;
    }
}