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

package com.android.server;

import android.os.Binder;
import android.os.PowerManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Qualcomm extension that lets the framework block alarms from selected uids.
 *
 * @hide
 */
public final class QCNsrmAlarmExtension {
    static final String TAG = "QCNsrmAlarmExtn";
    static final boolean localLOGV = false;

    private static final int BLOCKED_UID_CHECK_INTERVAL = 1000;
    private static final ArrayList<Integer> mTriggeredUids = new ArrayList<>();
    private static final ArrayList<Integer> mBlockedUids = new ArrayList<>();

    private final AlarmManagerService almHandle;

    class CheckBlockedUidTimerTask extends TimerTask {
        private final int mUid;
        private final PowerManager.WakeLock mWakeLock;

        CheckBlockedUidTimerTask(int uid, PowerManager.WakeLock wakeLock) {
            mUid = uid;
            mWakeLock = wakeLock;
        }

        @Override
        public void run() {
            if (mBlockedUids.contains(mUid) && mTriggeredUids.contains(mUid)) {
                synchronized (almHandle.mLock) {
                    if (mWakeLock.isHeld()) {
                        mWakeLock.release();
                    }
                }
            }
        }
    }

    public QCNsrmAlarmExtension(AlarmManagerService alarmManagerService) {
        almHandle = alarmManagerService;
    }

    protected void addTriggeredUid(int uid) {
        mTriggeredUids.add(new Integer(uid));
    }

    protected void removeTriggeredUid(int uid) {
        mTriggeredUids.remove(new Integer(uid));
    }

    protected boolean hasBlockedUid(int uid) {
        return mBlockedUids.contains(Integer.valueOf(uid));
    }

    protected void processBlockedUids(int uid, boolean blocked,
            PowerManager.WakeLock wakeLock) {
        if (Binder.getCallingUid() != 1000) {
            return;
        }
        if (!blocked) {
            mBlockedUids.clear();
        } else {
            mBlockedUids.add(new Integer(uid));
            new Timer().schedule(new CheckBlockedUidTimerTask(uid, wakeLock),
                    BLOCKED_UID_CHECK_INTERVAL);
        }
    }
}