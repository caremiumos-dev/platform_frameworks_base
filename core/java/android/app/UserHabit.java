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

package android.app;

import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Per-process usage statistics collected by the XTC performance subsystem
 * (launch counts, foreground time and recent launch times).
 *
 * <p><b>Condemnation.</b> This tracks detailed per-application usage and
 * uploads it to the vendor. Reproduced only for behavioural compatibility.
 *
 * @hide
 */
public class UserHabit {
    private static final String TAG = "xtcperformance-Habit";

    private static final String ATTR_USERID = "userid";
    private static final String ATTR_PKG = "pkgName";
    private static final String ATTR_LAUNCH_COUNT = "launchCount";
    private static final String ATTR_TOTAL_FG_TIME = "totalForegroundTime";
    private static final String ATTR_LAST_FG_TIME = "lastFgTime";
    private static final String ATTR_LAST_USE_TIME = "lastUseTime";
    private static final String ATTR_RECENT_LAUNCH_TIME = "recentLaunchTime";
    private static final int RECENT_LAUNCH_TIME_COUNT = 10;

    public String packageName;
    public long launchCount;
    public long totalForegroundTime;
    public int mLastEvent;
    public long lastForegroundTime;
    public long lastUseTime;
    public Integer mUserId;
    private ArrayList<String> mLaunchTime;

    public UserHabit() {
        this.mLaunchTime = new ArrayList<>();
        this.mLastEvent = -1;
    }

    public UserHabit(String packageName, int launchCount, long totalForegroundTime,
            long lastForegroundTime, long lastUseTime, int userId) {
        this.mLaunchTime = new ArrayList<>();
        this.packageName = packageName;
        this.launchCount = launchCount;
        this.totalForegroundTime = totalForegroundTime;
        this.mLastEvent = -1;
        this.lastForegroundTime = lastForegroundTime;
        this.lastUseTime = lastUseTime;
        this.mUserId = userId;
    }

    public UserHabit(String packageName, Integer userId) {
        this.mLaunchTime = new ArrayList<>();
        this.packageName = packageName;
        this.launchCount = 0L;
        this.totalForegroundTime = 0L;
        this.mLastEvent = -1;
        this.lastForegroundTime = 0L;
        this.lastUseTime = 0L;
        this.mUserId = userId;
    }

    public static UserHabit restoreFromFile(XmlPullParser parser)
            throws XmlPullParserException, IOException {
        UserHabit habit = new UserHabit();
        for (int i = parser.getAttributeCount() - 1; i >= 0; i--) {
            String name = parser.getAttributeName(i);
            String value = parser.getAttributeValue(i);
            if (ATTR_USERID.equals(name)) {
                habit.mUserId = Integer.valueOf(value);
            } else if (ATTR_PKG.equals(name)) {
                habit.packageName = value;
            } else if (ATTR_LAUNCH_COUNT.equals(name)) {
                habit.launchCount = Long.valueOf(value);
            } else if (ATTR_TOTAL_FG_TIME.equals(name)) {
                habit.totalForegroundTime = Long.valueOf(value);
            } else if (ATTR_LAST_FG_TIME.equals(name)) {
                habit.lastForegroundTime = Long.valueOf(value);
            } else if (ATTR_LAST_USE_TIME.equals(name)) {
                habit.lastUseTime = Long.valueOf(value);
            } else if (ATTR_RECENT_LAUNCH_TIME.equals(name)) {
                for (String time : value.split(";")) {
                    habit.mLaunchTime.add(time);
                }
            } else {
                Log.e(TAG, "error attr name....:" + name);
            }
        }
        return habit;
    }

    public UserHabit clone() {
        UserHabit habit = new UserHabit();
        habit.packageName = this.packageName;
        habit.launchCount = this.launchCount;
        habit.totalForegroundTime = this.totalForegroundTime;
        habit.mLastEvent = this.mLastEvent;
        habit.lastForegroundTime = this.lastForegroundTime;
        habit.lastUseTime = this.lastUseTime;
        habit.mUserId = this.mUserId;
        habit.mLaunchTime = (ArrayList<String>) this.mLaunchTime.clone();
        return habit;
    }

    public long getLastForegroundTime() {
        return this.lastForegroundTime;
    }

    public long getLastTimeUsed() {
        return this.lastUseTime;
    }

    public String getPackageName() {
        return this.packageName;
    }

    public ArrayList<String> getRecentLaunchTime() {
        synchronized (this.mLaunchTime) {
            return (ArrayList<String>) this.mLaunchTime.clone();
        }
    }

    public long getTotalTimeInForeground() {
        return this.totalForegroundTime;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("");
        synchronized (this.mLaunchTime) {
            for (String time : this.mLaunchTime) {
                builder.append(time).append(";");
            }
        }
        return "UID:" + this.mUserId + "|pkg:" + this.packageName + "|count:" + this.launchCount
                + "|LastUsed:" + this.lastUseTime + "|totalForegroundTime:"
                + this.totalForegroundTime + "|launchTime:" + builder.toString();
    }

    public void updateLaunchTime(String time) {
        synchronized (this.mLaunchTime) {
            this.mLaunchTime.add(time);
            if (this.mLaunchTime.size() >= RECENT_LAUNCH_TIME_COUNT) {
                this.mLaunchTime.remove(0);
            }
        }
    }

    public void writeToFile(String packageName, XmlSerializer serializer)
            throws XmlPullParserException, IOException {
        serializer.attribute(null, ATTR_USERID, String.valueOf(this.mUserId));
        serializer.attribute(null, ATTR_PKG, this.packageName);
        serializer.attribute(null, ATTR_LAUNCH_COUNT, String.valueOf(this.launchCount));
        serializer.attribute(null, ATTR_TOTAL_FG_TIME, String.valueOf(this.totalForegroundTime));
        serializer.attribute(null, ATTR_LAST_FG_TIME, String.valueOf(this.lastForegroundTime));
        serializer.attribute(null, ATTR_LAST_USE_TIME, String.valueOf(this.lastUseTime));
        StringBuilder builder = new StringBuilder("");
        synchronized (this.mLaunchTime) {
            for (String time : this.mLaunchTime) {
                builder.append(time).append(";");
            }
        }
        serializer.attribute(null, ATTR_RECENT_LAUNCH_TIME, builder.toString());
    }
}