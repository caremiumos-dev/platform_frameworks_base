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

package com.android.server.performance.policy.oomadj;

import android.app.ActivityManager;
import android.app.ProcessInfo;
import android.app.SectionHabit;
import android.app.UserHabit;
import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;
import android.os.Message;
import android.os.UserHandle;
import android.util.Slog;

import com.android.server.performance.PerformanceManagerDebugConfig;
import com.android.server.performance.PerformanceManagerServiceImpl;
import com.android.server.performance.PolicyConfig;
import com.android.server.performance.PolicyExecutor;
import com.android.server.performance.PolicyItem;
import com.android.server.performance.collector.UserHabitCollector;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

/**
 * Adjusts OOM adj values based on the vendor usage profile.
 *
 * <p><b>Condemnation.</b> Preferentially raises the adj of "hot", "recent" and
 * "section-habit" applications and lowers it for others, using a vendor usage
 * profile. This is platform discrimination between applications based on
 * vendor-chosen criteria. Reproduced only for behavioural compatibility.
 *
 * @hide
 */
public class OomAdjPolicyExecutor extends PolicyExecutor {
    private static final String TAG = "xtcperformance-OomAdj";
    private static final String THREAD_NAME = "OomAdjPolicyExecutor";

    private static final int ADJ_VISIBLE = 100;
    private static final int ADJ_PERCEPTIBLE = 200;
    private static final int ADJ_SERVICE = 500;
    private static final int ADJ_SERVICEB = 800;
    private static final int ADJ_CACHED = 900;

    private static final int ACTIVITY_STATE_RESUMED = 3;
    private static final int ACTIVITY_STATE_LAUNCH_TIME = 5;

    private static final int PROC_STATE_FOREGROUND_SERVICE = 11;

    final HashMap<String, UsageData> mUsageData = new HashMap<>();
    private final ActivityManager mAm;
    private final OomAdjConstants mConstants;
    private final Context mContext;
    private final PerformanceManagerServiceImpl mService;
    private final UserHabitCollector mUserHabitCollector;
    private boolean mInStrumentation = false;
    private String mCurrentFocusPkg = "";

    class UsageData {
        public UserHabit stats;
        public float launchCountPos;
        public int launchCountIndex;

        public UsageData(UserHabit stats, float launchCountPos, int launchCountIndex) {
            this.stats = stats;
            this.launchCountPos = launchCountPos;
            this.launchCountIndex = launchCountIndex;
        }
    }

    public OomAdjPolicyExecutor(PolicyItem policyItem, PerformanceManagerServiceImpl service) {
        this.mService = service;
        this.mContext = this.mService.getContext();
        this.mAm = (ActivityManager) this.mContext.getSystemService(Context.ACTIVITY_SERVICE);
        this.mUserHabitCollector = this.mService.getUserHabitCollector();
        this.mConstants = createOomAdjConstants(policyItem);
    }

    private OomAdjConstants createOomAdjConstants(PolicyItem policyItem) {
        String type = this.mService.getRamConfig() >= 1024
                ? OomAdjConstants.CONFIG_TYPE_1G : OomAdjConstants.CONFIG_TYPE_512M;
        PolicyConfig config = policyItem.getConfig(type);
        if (PerformanceManagerDebugConfig.DEBUG_PRIOADJ) {
            Slog.d(TAG, "createOomAdjConstants type=" + type);
        }
        if (config != null) {
            return OomAdjConstants.loadFromConfig(config);
        }
        return null;
    }

    private void dropAdj(ProcessInfo processInfo, int memPressure, long pss, long swapPss) {
        int tunedAdj = this.mConstants.getAdjTunningValue(processInfo.curAdj, memPressure);
        processInfo.adjTunned = true;
        processInfo.tunnedAdj = tunedAdj;
        processInfo.tunnedSchedGroup = 0;
        this.mAm.applyOomAdjByProcessInfoLocked(processInfo, pss, swapPss);
    }

    private boolean isPackagePerceptibleToUser(ProcessInfo processInfo, int memPressure) {
        if (this.mService.isProcessHeldWakeLock(processInfo.uid)) {
            if (PerformanceManagerDebugConfig.DEBUG_PRIOADJ) {
                Slog.d(TAG, "isPackagePerceptibleToUser " + processInfo.packageName
                        + " is doing something");
            }
            return true;
        }
        if (isProcessHotApp(processInfo)) {
            return true;
        }
        UserHabit habit = this.mUserHabitCollector != null
                ? this.mUserHabitCollector.getPackageUserHabits(UserHandle.myUserId(),
                        processInfo.packageName) : null;
        return habit == null || !isPackageBeenUnusedForLongTime(habit, memPressure);
    }

    private boolean isProcessAdjCare(int adj) {
        return adj == ADJ_VISIBLE || adj == ADJ_PERCEPTIBLE || adj == ADJ_SERVICE
                || adj == ADJ_SERVICEB;
    }

    private boolean isProcessHotApp(ProcessInfo processInfo) {
        if (this.mConstants == null) {
            return false;
        }
        if (processInfo != null && !processInfo.packageName.equals(processInfo.processName)) {
            return false;
        }
        synchronized (this.mUsageData) {
            UsageData data = this.mUsageData.get(processInfo.packageName);
            return data != null && data.launchCountIndex < this.mConstants.getHotAppCount()
                    && data.stats.launchCount >= this.mConstants.getHotAppMinLaunchCount();
        }
    }

    private void updateUsageStat() {
        try {
            ArrayList<UserHabit> executable = new ArrayList<>();
            ArrayList<UserHabit> habits = new ArrayList<>();
            if (this.mUserHabitCollector != null) {
                habits = (ArrayList<UserHabit>) this.mUserHabitCollector
                        .getUserHabits(UserHandle.myUserId()).clone();
            }
            for (UserHabit habit : habits) {
                if (this.mService.isExecutableApp(habit.getPackageName())) {
                    executable.add(habit);
                }
            }
            Collections.sort(executable, new Comparator<UserHabit>() {
                @Override
                public int compare(UserHabit a, UserHabit b) {
                    if (a.launchCount == b.launchCount) {
                        return 0;
                    }
                    return a.launchCount >= b.launchCount ? -1 : 1;
                }
            });
            synchronized (this.mUsageData) {
                for (UserHabit habit : executable) {
                    int index = executable.indexOf(habit);
                    float pos = ((index + 1) * 1.0f) / (executable.size() + 1);
                    UsageData data = this.mUsageData.get(habit.getPackageName());
                    if (data == null) {
                        data = new UsageData(habit, 1.0f, executable.size() - 1);
                    }
                    data.launchCountPos = pos;
                    data.launchCountIndex = index;
                    this.mUsageData.put(habit.getPackageName(), data);
                }
            }
        } catch (Exception e) {
            // ignored
        }
    }

    public boolean doAdjDropIfNeeded(ProcessInfo processInfo, int memPressure, long pss,
            long swapPss) {
        if (this.mConstants == null || this.mInStrumentation || memPressure < 1) {
            return false;
        }
        if ((processInfo != null && processInfo.packageName != null
                && !this.mService.isExecutableApp(processInfo.packageName))
                || !isProcessAdjCare(processInfo.curAdj)
                || isPackagePerceptibleToUser(processInfo, memPressure)) {
            return false;
        }
        dropAdj(processInfo, memPressure, pss, swapPss);
        return true;
    }

    @Override
    public void dump(PrintWriter writer, String[] args) {
        writer.println("PRIO ADJ:");
        synchronized (this.mUsageData) {
            for (String packageName : this.mUsageData.keySet()) {
                UsageData data = this.mUsageData.get(packageName);
                if (data != null) {
                    writer.println("usage :" + data.stats.getPackageName() + " launchCount = "
                            + data.stats.launchCount + "index = " + data.launchCountIndex
                            + "pos :" + data.launchCountPos);
                }
            }
        }
        if (this.mConstants != null) {
            this.mConstants.dump(writer);
        }
    }

    ProcessInfo getCurrentFocusPackage() {
        java.util.List<ProcessInfo> processes = this.mAm.getRunningProcessesInfo();
        for (int i = 0; i < processes.size(); i++) {
            ProcessInfo processInfo = processes.get(i);
            if (processInfo != null && processInfo.curAdj == 0
                    && "top-activity".equals(processInfo.curAdjType)) {
                return processInfo;
            }
        }
        return null;
    }

    long getPackageLastUseTime(String packageName) {
        synchronized (this.mUsageData) {
            UsageData data = this.mUsageData.get(packageName);
            return data == null ? 0L : data.stats.getLastTimeUsed();
        }
    }

    long getPackageLaunchCount(String packageName) {
        synchronized (this.mUsageData) {
            UsageData data = this.mUsageData.get(packageName);
            return data == null ? 0L : data.stats.launchCount;
        }
    }

    @Override
    public String getThreadName() {
        return THREAD_NAME;
    }

    @Override
    protected void handleActivityStateChange(
            PerformanceManagerServiceImpl.ActivityStateData data) {
        if (data == null) {
            return;
        }
        int state = data.bundle.getInt("activity_state");
        ComponentName component = data.intent.getComponent();
        if (component == null) {
            return;
        }
        switch (state) {
            case ACTIVITY_STATE_RESUMED:
                this.mCurrentFocusPkg = component.getPackageName();
                break;
            case ACTIVITY_STATE_LAUNCH_TIME:
                updateUsageStat();
                break;
            default:
                break;
        }
    }

    @Override
    protected void handleInStrumentationChange(Message message) {
        this.mInStrumentation = true;
    }

    public boolean increaseAdjIfNeeded(ProcessInfo processInfo, int memPressure, long pss,
            long swapPss) {
        boolean tune;
        if (this.mConstants == null || this.mInStrumentation) {
            return false;
        }
        if (processInfo != null && processInfo.packageName != null
                && !this.mService.isExecutableApp(processInfo.packageName)) {
            return false;
        }
        if (processInfo.curAdj > this.mConstants.getHotAppAdj() && isProcessHotApp(processInfo)) {
            processInfo.tunnedAdj = this.mConstants.getHotAppAdj();
            tune = true;
        } else if (processInfo.curAdj >= ADJ_CACHED
                && isPackageRecentUsedForAWhile(processInfo.packageName, memPressure)) {
            processInfo.tunnedAdj = this.mConstants.getRecentAppAdj();
            tune = true;
        } else if (processInfo.curAdj >= ADJ_CACHED
                && processInfo.processName.equals(processInfo.packageName)
                && isUserHabitTopApp(UserHandle.myUserId(), processInfo.packageName)) {
            processInfo.tunnedAdj = this.mConstants.getSectionHabitAdj();
            tune = true;
        } else {
            tune = false;
        }
        if (!tune) {
            return false;
        }
        processInfo.adjTunned = true;
        processInfo.tunnedProcState = PROC_STATE_FOREGROUND_SERVICE;
        processInfo.tunnedAdjType = "force-fg";
        processInfo.tunnedSchedGroup = 0;
        this.mAm.applyOomAdjByProcessInfoLocked(processInfo, pss, swapPss);
        return true;
    }

    boolean isPackageBeenUnusedForLongTime(UserHabit habit, int memPressure) {
        return habit != null && System.currentTimeMillis() - habit.getLastTimeUsed()
                >= this.mConstants.getAppIdleTimeLimit(memPressure);
    }

    boolean isPackageRecentUsedForAWhile(String packageName, int memPressure) {
        if (this.mUserHabitCollector == null || this.mConstants == null) {
            return false;
        }
        UserHabit habit = this.mUserHabitCollector.getPackageUserHabits(UserHandle.myUserId(),
                packageName);
        return habit != null && System.currentTimeMillis() - habit.getLastTimeUsed()
                <= this.mConstants.getAppIdleTimeLimit(memPressure);
    }

    public boolean isPackageUsrFavorite(String packageName) {
        synchronized (this.mUsageData) {
            UsageData data = this.mUsageData.get(packageName);
            return data != null && data.launchCountIndex < this.mConstants.getHotAppCount()
                    && data.stats.launchCount >= this.mConstants.getHotAppMinLaunchCount();
        }
    }

    public boolean isUserHabitTopApp(int userId, String packageName) {
        SectionHabit.HabitDate date = this.mUserHabitCollector
                .getOrCreatSectionHabit(userId).getHistoryHabitDate(packageName);
        if (date != null) {
            return (date.launchCountIndex >= 0
                    && date.launchCountIndex < this.mConstants.getSectionHabitAppCount()
                    || date.fgtimeIndex >= 0
                    && date.fgtimeIndex < this.mConstants.getSectionHabitAppCount())
                    && date.launchCount >= this.mConstants.getSectionHabitAppLaunchCount();
        }
        return false;
    }
}