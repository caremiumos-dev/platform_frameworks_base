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

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Lightweight representation of a running process used by the XTC performance
 * and LMK tuning services.
 *
 * @hide
 */
public class ProcessInfo implements Parcelable {

    /** Activity state constants. */
    public static final int ACTIVITY_STATE_START = 0;
    public static final int ACTIVITY_STATE_STOP = 1;
    public static final int ACTIVITY_STATE_PAUSE = 2;
    public static final int ACTIVITY_STATE_RESUME = 3;
    public static final int ACTIVITY_STATE_FINISH = 4;
    public static final int ACTIVITY_STATE_LAUNCHDONE = 5;
    public static final int ACTIVITY_STATE_PROC_START = 6;
    public static final int ACTIVITY_STATE_ATTOP = 7;

    /** LMK adj levels. */
    public static final int PERSISTENT_PROC_ADJ = -800;
    public static final int PERSISTENT_SERVICE_ADJ = -700;
    public static final int SYSTEM_ADJ = -900;
    public static final int VISIBLE_APP_ADJ = 100;
    public static final int FOREGROUND_APP_ADJ = 0;
    public static final int PERCEPTIBLE_APP_ADJ = 200;
    public static final int BACKUP_APP_ADJ = 300;
    public static final int HEAVY_WEIGHT_APP_ADJ = 400;
    public static final int SERVICE_ADJ = 500;
    public static final int HOME_APP_ADJ = 600;
    public static final int PREVIOUS_APP_ADJ = 700;
    public static final int SERVICE_B_ADJ = 800;
    public static final int CACHED_APP_MIN_ADJ = 900;
    public static final int CACHED_APP_MAX_ADJ = 906;
    public static final int VISIBLE_APP_LAYER_MAX = 99;

    /** LMK adj tunning results. */
    public static final int PROCESS_LMK_ADJ_TUNNING_INCREASE = 0;
    public static final int PROCESS_LMK_ADJ_TUNNING_DECREASE = 1;
    public static final int PROCESS_LMK_ADJ_TUNNING_NONEED = 2;

    /** Scheduling groups. */
    public static final int SCHED_GROUP_BACKGROUND = 0;
    public static final int SCHED_GROUP_DEFAULT = 1;
    public static final int SCHED_GROUP_TOP_APP = 2;
    public static final int SCHED_GROUP_TOP_APP_BOUND = 3;

    /** Process hosting types. */
    public static final int TYPE_NONE = 0;
    public static final int TYPE_CONFIG_CHANGE = 1;

    /** Keys used in process start information. */
    public static final String KEY_PKGNAME = "pkgname";
    public static final String KEY_CLSNAME = "clsname";
    public static final String KEY_START_PROC_PID = "pid";
    public static final String KEY_START_PROC_HOST_TYPE = "hostingtype";
    public static final String KEY_ACTIVITY_STATE = "activity_state";
    public static final String KEY_FULLSCREEN = "fullscreen";
    public static final String KEY_LAUNCH_TIME = "launchTime";

    public static final Parcelable.Creator<ProcessInfo> CREATOR =
            new Parcelable.Creator<ProcessInfo>() {
                @Override
                public ProcessInfo createFromParcel(Parcel in) {
                    return new ProcessInfo(in);
                }

                @Override
                public ProcessInfo[] newArray(int size) {
                    return new ProcessInfo[size];
                }
            };

    public String packageName;
    public String processName;
    public int pid;
    public int uid;
    public int curAdj;
    public String curAdjType;
    public long lastPss;
    public int flags;
    public int curProcState;
    public int curSchedGroup;
    public boolean adjTunned;
    public int tunnedAdj;
    public String tunnedAdjType;
    public int tunnedProcState;
    public int tunnedSchedGroup;

    public ProcessInfo() {
    }

    public ProcessInfo(String packageName, int pid) {
        this.packageName = packageName;
        this.processName = packageName;
        this.pid = pid;
    }

    public ProcessInfo(Parcel in) {
        readFromParcel(in);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(packageName);
        out.writeString(processName);
        out.writeInt(pid);
        out.writeInt(uid);
        out.writeInt(curAdj);
        out.writeString(curAdjType);
        out.writeLong(lastPss);
        out.writeInt(this.flags);
        out.writeInt(curProcState);
        out.writeInt(curSchedGroup);
        out.writeInt(adjTunned ? 1 : 0);
        out.writeInt(tunnedAdj);
        out.writeString(tunnedAdjType);
        out.writeInt(tunnedProcState);
        out.writeInt(tunnedSchedGroup);
    }

    public void readFromParcel(Parcel in) {
        packageName = in.readString();
        processName = in.readString();
        pid = in.readInt();
        uid = in.readInt();
        curAdj = in.readInt();
        curAdjType = in.readString();
        lastPss = in.readLong();
        flags = in.readInt();
        curProcState = in.readInt();
        curSchedGroup = in.readInt();
        adjTunned = in.readInt() == 1;
        tunnedAdj = in.readInt();
        tunnedAdjType = in.readString();
        tunnedProcState = in.readInt();
        tunnedSchedGroup = in.readInt();
    }

    public void resetTunningParams() {
        adjTunned = false;
        tunnedAdj = curAdj;
        tunnedAdjType = curAdjType;
        tunnedProcState = curProcState;
        tunnedSchedGroup = curSchedGroup;
    }

    @Override
    public String toString() {
        return "Process:" + processName + ", pkg:" + packageName + ", pid:" + pid
                + ", adj:" + curAdj + ", curAdjType:" + curAdjType;
    }
}