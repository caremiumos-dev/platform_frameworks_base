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

package com.android.server.performance.policy.ram;

import android.app.ActivityManager;
import android.app.ProcessInfo;
import android.app.ProcState;
import android.app.UserHabit;
import android.content.ComponentName;
import android.os.Message;
import android.os.Process;
import android.os.SystemClock;
import android.os.Trace;
import android.os.UserHandle;
import android.util.Slog;
import android.view.inputmethod.InputMethodInfo;

import com.android.internal.view.IInputMethodManager;
import com.android.server.performance.PerformanceManagerDebugConfig;
import com.android.server.performance.PerformanceManagerServiceImpl;
import com.android.server.performance.PolicyExecutor;
import com.android.server.performance.PolicyItem;
import com.android.server.performance.collector.UserHabitCollector;
import com.android.server.performance.status.RamStatus;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * Memory reclaim / force-stop policy. Runs on memory pressure and reclaims or
 * kills least-recently-used processes.
 *
 * <p><b>Strong condemnation.</b> This force-stops and kills third-party
 * applications based on a vendor usage profile and a hard-coded special-case
 * package list, and explicitly excludes {@code org.cloud.sonic.android} (a
 * test package) from reclaim. Killing user applications by vendor policy is a
 * form of platform discrimination. Reproduced only for compatibility.
 *
 * @hide
 */
public class RamPolicyExecutor extends PolicyExecutor {
    private static final String TAG = "xtcperformance-Ram";
    private static final String THREAD_NAME = "RamPolicyExecutor";

    private static final long ONE_MEGABYTE = 1048576L;
    private static final long FOCUS_CHANGE_PROTECT_LIMIT = 10000L;
    private static final long FOCU_CHANGE_PROTECT_IDLE_LIMIT = 3600000L;
    private static final long MIN_IDLE_WAIT_TIME = 5000L;
    private static final long WAKELOCK_PROTECTED_TIME = 15000L;
    private static final long MEMORY_LEVEL_RECLAIM_GAP = 20L;
    private static final long FULL_BACKUP_INTERVAL = 1800000L;

    private static final int RECLAIM_TYPE_ALL = 2;
    private static final int SCENE_EM = 1;
    private static final int PROC_STATE_START = 6;
    private static final int ACTIVITY_STATE_LAUNCH_DONE = 5;
    private static final int ACTIVITY_STATE_RESUMED = 3;

    private static final String TEST_PACKAGE = "org.cloud.sonic.android";

    private final int CPU_LOADING_SAMPLE_LOW_COUNT = 3;
    private final int CPU_LOADING_SAMPLE_HIGH_COUNT = 2;
    private final int RECLAIM_ADJ_THROL_RECLAIM = 500;
    private final int RECLAIM_ADJ_BOOST_KILL = 900;
    private final int FS_RECLAIM_ADJ = 200;
    private final int MAX_RECLAIM_COUNT = 3;
    private final int MAX_RECLAIM_PERSIST_COUNT = 10;

    private final ActivityManager mAm;
    private final MemoryConstant mMemoryConstant;
    private final MemoryScene mMemoryScene;
    private final PerformanceManagerServiceImpl mService;
    private final UserHabitCollector mUserHabitCollector;
    private final ArrayList<String> mSpecialPackages = new ArrayList<>();
    private final boolean mReclaimProcessEnabled;

    private boolean mInReclaimProcess = false;
    private int mCpuLoadingLowCount = 0;
    private int mCpuLoadingHighCount = 0;
    private final boolean mBoostKillEnable = true;
    private boolean mInStrumentation = false;
    private String mCurrentFocusPackage = "";
    private int mCurrentBgRunningPkgCount = 0;
    private final long mLastVmEventTime = -1;
    private final boolean mLowRam = false;

    class LRUReclaimProcessRecord {
        UserHabit habit;
        String pkgName;
        ArrayList<ProcessInfo> procs;
        long rss;
        int uid;
    }

    class ReclaimProcessRecord {
        ProcessInfo info;
        UserHabit habit;
        long launchCount;
        String policy;
        long pss;
        long uss;

        public ReclaimProcessRecord(ProcessInfo processInfo, UserHabit habit, String policy) {
            this.info = processInfo;
            this.habit = habit;
            this.launchCount = habit.launchCount;
            this.policy = policy;
        }

        public long getReclaimableMemory() {
            if (MemoryScene.POLICY_KILL.equals(this.policy)
                    || MemoryScene.POLICY_QUICK_KILL.equals(this.policy)) {
                return this.pss;
            }
            return (MemoryScene.POLICY_RECLAIM.equals(this.policy)
                    || MemoryScene.POLILCY_RECLAIM_PERSIST.equals(this.policy))
                    ? this.uss : this.pss;
        }

        public long reclaim() {
            if (MemoryScene.POLICY_KILL.equals(this.policy)
                    || MemoryScene.POLICY_QUICK_KILL.equals(this.policy)) {
                killOneProcess(this.info.pid);
                return this.pss;
            }
            if (MemoryScene.POLICY_RECLAIM.equals(this.policy)
                    || MemoryScene.POLILCY_RECLAIM_PERSIST.equals(this.policy)) {
                return reclaimOneProcess(this.info.pid, RECLAIM_TYPE_ALL);
            }
            return 0L;
        }

        public void setPss(long pss) {
            this.pss = pss;
        }

        public void setUss(long uss) {
            this.uss = uss;
        }

        @Override
        public String toString() {
            return "pid:" + this.info.pid + "|proc:" + this.info.processName + "|habit:("
                    + this.habit + ")|uss:" + this.uss + "|pss:" + this.pss + "|curAdj:"
                    + this.info.curAdj + "|policy:" + this.policy;
        }
    }

    public RamPolicyExecutor(PolicyItem policyItem, PerformanceManagerServiceImpl service) {
        this.mMemoryConstant = MemoryConstant.loadFromConfig(
                policyItem.getConfig(MemoryConstant.TAG_NAME), service.getRamConfig());
        this.mMemoryScene = MemoryScene.loadFromConfig(
                policyItem.getConfig(MemoryScene.TAG_NAME));
        this.mService = service;
        this.mAm = (ActivityManager) this.mService.getContext().getSystemService(
                android.content.Context.ACTIVITY_SERVICE);
        this.mReclaimProcessEnabled = reclaimProcessEnabled();
        this.mUserHabitCollector = this.mService.getUserHabitCollector();
        this.mSpecialPackages.add("com.android");
    }

    private void checkMemoryStatus(RamStatus ramStatus) {
        if (this.mInReclaimProcess) {
            return;
        }
        this.mInReclaimProcess = true;
        try {
            long availableMemMb = ramStatus.getAvailableMemKb() / ONE_MEGABYTE;
            if (PerformanceManagerDebugConfig.DEBUG_RAMPOLICY) {
                Slog.d(TAG, "checkMemoryStatus : " + availableMemMb + " MB");
            }
            if (this.mReclaimProcessEnabled && isMemoryPressureReclaim(availableMemMb)) {
                doMemoryReclaim(this.mMemoryConstant.getNormalMemory() * ONE_MEGABYTE,
                        this.mMemoryScene.getPolicyForScene(MemoryScene.SCENE_IDLE));
            } else if (this.mReclaimProcessEnabled
                    && isMemoryPressureEmergency(availableMemMb)) {
                doMemoryReclaim(this.mMemoryConstant.getEmergencyMemory() * ONE_MEGABYTE,
                        this.mMemoryScene.getPolicyForScene(MemoryScene.SCENE_EMERGENCY));
            }
            if (!this.mInStrumentation) {
                long idleTime = this.mMemoryConstant.getCurrentForceStopIdleTime(availableMemMb);
                if (idleTime != -1) {
                    long wait = idleTime >= MIN_IDLE_WAIT_TIME ? idleTime : MIN_IDLE_WAIT_TIME;
                    if (PerformanceManagerDebugConfig.DEBUG_RAMPOLICY) {
                        Slog.d(TAG, "system under memPressure current available ->"
                                + availableMemMb + "MB idleTime=" + wait);
                    }
                    doLRUReclaim(wait, false, "available memory:" + availableMemMb);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            this.mInReclaimProcess = false;
        }
    }

    private void doLRUReclaim(long idleTime, boolean force, String reason) {
        List<LRUReclaimProcessRecord> records = getLeastRecentUsedPkg(idleTime, force);
        if (records == null || records.size() <= 0) {
            return;
        }
        for (LRUReclaimProcessRecord record : records) {
            if (isSpecialPackage(record.pkgName)) {
                Slog.d(TAG, "doLRUReclaim Killing uid:" + record.uid + " pkgName:"
                        + record.pkgName + " due to " + reason + " to free " + record.rss + "KB");
                this.mAm.killUid(record.uid, "rampolicy");
            } else {
                Slog.d(TAG, "doLRUReclaim force-stop " + record.pkgName + " due to " + reason
                        + " to free " + record.rss + "KB");
                this.mAm.forceStopPackage(record.pkgName);
            }
        }
    }

    private void doMemoryReclaim(final long requestMem, final String policy) {
        if (policy == null) {
            return;
        }
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                Trace.traceBegin(Trace.TRACE_TAG_ACTIVITY_MANAGER,
                        "doMemoryReclaimInner:" + policy);
                try {
                    doMemoryReclaimInner(requestMem, policy);
                } catch (Exception e) {
                    // ignored
                }
                Trace.traceEnd(Trace.TRACE_TAG_ACTIVITY_MANAGER);
            }
        });
    }

    private void doMemoryReclaimInner(long requestMem, String policy) {
        long availableMemKb = this.mService.getCurrentRamStatus(true).getAvailableMemKb();
        long requestKb = requestMem - availableMemKb;
        long start = SystemClock.uptimeMillis();
        if (requestKb <= 0 || policy == null) {
            return;
        }
        Slog.d(TAG, "doMemoryReclaimInner requestMem=" + requestKb + " currentMem="
                + availableMemKb + " request=" + requestMem);
        ArrayList<ReclaimProcessRecord> targets = getReclaimTargetProcesses(policy);
        if (targets == null || targets.size() == 0) {
            return;
        }
        long reclaimed = 0;
        int index = -1;
        while (requestKb >= reclaimed && index + 1 < targets.size()) {
            index++;
            ReclaimProcessRecord record = targets.get(index);
            Trace.traceBegin(Trace.TRACE_TAG_ACTIVITY_MANAGER, "reclaim:" + record.info.pid);
            long pages = record.reclaim();
            Trace.traceEnd(Trace.TRACE_TAG_ACTIVITY_MANAGER);
            reclaimed += 4 * pages;
            if (PerformanceManagerDebugConfig.DEBUG_RAMPOLICY) {
                Slog.d(TAG, "reclaimed -->" + pages + " pages for" + record);
            }
        }
        Slog.d(TAG, "doMemoryReclaimInner reclaimed : " + reclaimed + "KB cost "
                + (SystemClock.uptimeMillis() - start) + "ms");
    }

    private void dumpMemInfo() {
        try (BufferedReader reader = new BufferedReader(
                new FileReader(new java.io.File("/proc/meminfo")))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Slog.d(TAG, line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private ArrayList<String> getEnabledInputMethods() {
        try {
            List<InputMethodInfo> list = IInputMethodManager.Stub.asInterface(
                    android.os.ServiceManager.getService("input_method"))
                    .getEnabledInputMethodList();
            ArrayList<String> packages = new ArrayList<>();
            if (list != null && list.size() != 0) {
                for (InputMethodInfo info : list) {
                    if (info != null && info.getPackageName() != null) {
                        packages.add(info.getPackageName());
                    }
                }
            }
            return packages;
        } catch (android.os.RemoteException e) {
            return null;
        }
    }

    private String getHomePackageName() {
        ProcessInfo home = this.mAm.getHomeProcess();
        return home != null ? home.packageName : null;
    }

    private List<LRUReclaimProcessRecord> getLeastRecentUsedPkg(long idleTime, boolean force) {
        ArrayList<LRUReclaimProcessRecord> candidates = new ArrayList<>();
        String homePackage = getHomePackageName();
        UserHabit focusHabit = null;
        int bgLimit = this.mMemoryConstant.getBackgroundPkgRunningLimit();
        ArrayList<UserHabit> habits = this.mUserHabitCollector != null
                ? (ArrayList<UserHabit>) this.mUserHabitCollector.getUserHabits(
                        UserHandle.myUserId()).clone() : new ArrayList<UserHabit>();
        if (habits.size() == 0) {
            return null;
        }
        this.mCurrentBgRunningPkgCount = 0;
        ArrayList<String> inputMethods = getEnabledInputMethods();
        List<ProcessInfo> processes = this.mAm.getRunningProcessesInfo();
        for (UserHabit habit : habits) {
            if (this.mCurrentFocusPackage != null
                    && this.mCurrentFocusPackage.equals(habit.getPackageName())) {
                focusHabit = habit;
            }
        }
        for (UserHabit habit : habits) {
            LRUReclaimProcessRecord record = new LRUReclaimProcessRecord();
            if (isPackageLRUReclaimable(record, homePackage, inputMethods, processes, habit,
                    focusHabit, idleTime, force)) {
                record.habit = habit;
                record.pkgName = habit.getPackageName();
                candidates.add(record);
            }
        }
        if (PerformanceManagerDebugConfig.DEBUG_RAMPOLICY) {
            Slog.d(TAG, "current bgrunning count:" + this.mCurrentBgRunningPkgCount
                    + ", force-stop target count:" + candidates.size() + ", bglimit is "
                    + bgLimit);
        }
        if (candidates.size() == 0) {
            return null;
        }
        if (!force && this.mCurrentBgRunningPkgCount <= bgLimit) {
            return null;
        }
        int range = this.mCurrentBgRunningPkgCount - bgLimit;
        if (range <= 0) {
            range = 1;
        }
        int reclaimRange = (this.mLowRam || range <= 2) ? range : 2;
        if (PerformanceManagerDebugConfig.DEBUG_RAMPOLICY) {
            Slog.d(TAG, "reclaimRange:" + reclaimRange);
        }
        if (reclaimRange >= candidates.size()) {
            return candidates;
        }
        Collections.sort(candidates, new Comparator<LRUReclaimProcessRecord>() {
            @Override
            public int compare(LRUReclaimProcessRecord a, LRUReclaimProcessRecord b) {
                if (a.habit.lastUseTime == b.habit.lastUseTime) {
                    return 0;
                }
                return a.habit.lastUseTime <= b.habit.lastUseTime ? -1 : 1;
            }
        });
        ArrayList<LRUReclaimProcessRecord> result = new ArrayList<>();
        for (int i = 0; i < candidates.size(); i++) {
            if (i < reclaimRange) {
                result.add(candidates.get(i));
            }
        }
        return result;
    }

    private int getMaxReclaimCount(String policy) {
        return (MemoryScene.POLICY_KILL.equals(policy) || MemoryScene.POLICY_QUICK_KILL.equals(policy)
                || MemoryScene.POLICY_RECLAIM.equals(policy)
                || !MemoryScene.POLILCY_RECLAIM_PERSIST.equals(policy))
                ? MAX_RECLAIM_COUNT : MAX_RECLAIM_PERSIST_COUNT;
    }

    private long getProcRss(int pid) {
        try {
            long[] out = new long[2];
            Process.readProcFile("/proc/" + pid + "/statm", new int[]{8224, 8224}, null, out, null);
            return out[1] * 4;
        } catch (Exception e) {
            Slog.e(TAG, "getProcRss e=" + e);
            return 0L;
        }
    }

    private ArrayList<ProcessInfo> getProcessInfoListByPkgName(List<ProcessInfo> processes,
            String packageName) {
        if (packageName == null) {
            return null;
        }
        ArrayList<ProcessInfo> result = new ArrayList<>();
        for (ProcessInfo info : processes) {
            if (packageName.equals(info.packageName)) {
                result.add(info);
            }
        }
        return result;
    }

    private ArrayList<ReclaimProcessRecord> getReclaimTargetProcesses(String policy) {
        ArrayList<ReclaimProcessRecord> result = new ArrayList<>();
        List<ProcessInfo> processes = this.mAm.getRunningProcessesInfo();
        for (int i = 0; i < processes.size(); i++) {
            ProcessInfo info = processes.get(i);
            if (info != null && processImportanceReclaimable(info.curAdj, policy)) {
                UserHabit habit = this.mService.getUserHabitCollector().getPackageUserHabits(
                        UserHandle.myUserId(), info.packageName);
                if (System.currentTimeMillis() - habit.getLastTimeUsed() > FULL_BACKUP_INTERVAL) {
                    result.add(new ReclaimProcessRecord(info, habit, policy));
                    if (PerformanceManagerDebugConfig.DEBUG_RAMPOLICY) {
                        Slog.d(TAG, "add process:" + habit.getPackageName() + ", curAdj="
                                + info.curAdj + ", policy:" + policy);
                    }
                } else if (PerformanceManagerDebugConfig.DEBUG_RAMPOLICY) {
                    Slog.d(TAG, "skip process:" + habit.getPackageName() + ", curAdj="
                            + info.curAdj + ", policy:" + policy);
                }
            }
        }
        sortReclaimListByScore(result, policy);
        return result;
    }

    private boolean hasRelativeToptask(String packageName) {
        String topCallingPkg = this.mAm.getTopActivityCallingPkg();
        return topCallingPkg != null && topCallingPkg.equals(packageName);
    }

    private boolean isMemoryPressureEmergency(long availableMb) {
        return availableMb <= this.mMemoryConstant.getEmergencyMemory();
    }

    private boolean isMemoryPressureReclaim(long availableMb) {
        return availableMb > this.mMemoryConstant.getDefaultCriticalMemory()
                + MEMORY_LEVEL_RECLAIM_GAP && availableMb < this.mMemoryConstant.getNormalMemory();
    }

    private boolean isPackageLRUReclaimable(LRUReclaimProcessRecord record, String homePackage,
            List<String> inputMethods, List<ProcessInfo> processes, UserHabit habit,
            UserHabit focusHabit, long idleTime, boolean force) {
        String packageName = habit.getPackageName();
        if (TEST_PACKAGE.equals(packageName)) {
            if (PerformanceManagerDebugConfig.DEBUG_RAMPOLICY) {
                Slog.d(TAG, "Stop-LRU skip org.cloud.sonic.android is test!");
            }
            return false;
        }
        if (homePackage != null && homePackage.equals(packageName)) {
            if (PerformanceManagerDebugConfig.DEBUG_RAMPOLICY) {
                Slog.d(TAG, "Stop-LRU skip " + packageName + " is HOME!");
            }
            return false;
        }
        if (this.mCurrentFocusPackage != null
                && this.mCurrentFocusPackage.equals(packageName)) {
            if (PerformanceManagerDebugConfig.DEBUG_RAMPOLICY) {
                Slog.d(TAG, "Stop-LRU skip " + packageName + " is focus");
            }
            return false;
        }
        if (packageName != null && !this.mService.isExecutableApp(packageName)) {
            return false;
        }
        ArrayList<ProcessInfo> pkgProcs = getProcessInfoListByPkgName(processes, packageName);
        if ((inputMethods != null && inputMethods.contains(packageName)) || pkgProcs == null
                || pkgProcs.size() == 0) {
            return false;
        }
        this.mCurrentBgRunningPkgCount++;
        if (!(this.mLowRam && force) && isSpecialPackage(packageName)) {
            if (PerformanceManagerDebugConfig.DEBUG_RAMPOLICY) {
                Slog.d(TAG, "Stop-LRU skip " + packageName + " is special");
            }
            return false;
        }
        if (this.mService.isProcessHeldWakeLock(pkgProcs.get(0).uid, WAKELOCK_PROTECTED_TIME)) {
            if (PerformanceManagerDebugConfig.DEBUG_RAMPOLICY) {
                Slog.d(TAG, "Stop-LRU skip " + packageName + " is doing something");
            }
            return false;
        }
        if (!force && this.mService.isPackageUsrFavorite(packageName)) {
            if (PerformanceManagerDebugConfig.DEBUG_RAMPOLICY) {
                Slog.d(TAG, "Stop-LRU skip " + packageName + " is Favorite!:");
            }
            return false;
        }
        long unused = System.currentTimeMillis() - habit.getLastTimeUsed();
        if (unused < idleTime) {
            if (PerformanceManagerDebugConfig.DEBUG_RAMPOLICY) {
                Slog.d(TAG, "Stop-LRU skip " + packageName + " is recent used!:" + unused);
            }
            return false;
        }
        long focusDelta;
        if (this.mLowRam) {
            focusDelta = 0;
        } else {
            focusDelta = focusHabit.getLastForegroundTime() - habit.getLastTimeUsed();
            if (focusDelta <= FOCUS_CHANGE_PROTECT_LIMIT
                    && unused <= FOCU_CHANGE_PROTECT_IDLE_LIMIT) {
                if (PerformanceManagerDebugConfig.DEBUG_RAMPOLICY) {
                    Slog.d(TAG, "Stop-LRU skip " + packageName + " has Relative-Toptask:focus!");
                }
                return false;
            }
        }
        if (hasRelativeToptask(packageName)) {
            if (PerformanceManagerDebugConfig.DEBUG_RAMPOLICY) {
                Slog.d(TAG, "Stop-LRU skip " + packageName + " hasRelativeToptask: caller!");
            }
            return false;
        }
        if (!force) {
            for (ProcessInfo info : pkgProcs) {
                if (info != null && info.curAdj <= this.FS_RECLAIM_ADJ) {
                    if (PerformanceManagerDebugConfig.DEBUG_RAMPOLICY) {
                        Slog.d(TAG, "Stop-LRU skip " + packageName + " curAdj ->" + info.curAdj);
                    }
                    return false;
                }
            }
        }
        long rss = 0;
        for (ProcessInfo info : pkgProcs) {
            rss += info != null ? getProcRss(info.pid) : 0;
        }
        if (PerformanceManagerDebugConfig.DEBUG_RAMPOLICY) {
            Slog.d(TAG, "isPackageLRUReclaimable add " + packageName + " RSS: " + rss
                    + " KB, lastUsed:" + unused + " focus:" + focusDelta);
        }
        record.uid = pkgProcs.get(0).uid;
        record.rss = rss;
        record.procs = pkgProcs;
        return true;
    }

    private void killOneProcess(int pid) {
        Process.killProcess(pid);
    }

    private boolean processImportanceReclaimable(int adj, String policy) {
        if (MemoryScene.POLICY_KILL.equals(policy)
                || MemoryScene.POLICY_QUICK_KILL.equals(policy)) {
            return adj >= this.RECLAIM_ADJ_BOOST_KILL;
        }
        if (MemoryScene.POLICY_RECLAIM.equals(policy)) {
            return adj >= this.RECLAIM_ADJ_THROL_RECLAIM;
        }
        if (MemoryScene.POLILCY_RECLAIM_PERSIST.equals(policy)) {
            return adj == -800 || adj == -700 || adj == 200 || adj == 100;
        }
        return false;
    }

    private long reclaimOneProcess(int pid, int type) {
        long reclaimed = 0;
        try {
            reclaimed = Process.reclaimProcess(pid, type);
        } catch (Exception e) {
            Slog.d(TAG, " exception in reclaimOneProcess " + e);
        }
        if (PerformanceManagerDebugConfig.DEBUG_RAMPOLICY) {
            Slog.d(TAG, "reclaimOneProcess reclaimed=" + reclaimed + " type_id=" + type
                    + " pid=" + pid);
        }
        return reclaimed;
    }

    private boolean reclaimProcessEnabled() {
        boolean enabled = true;
        try {
            long[] out = new long[1];
            Process.readProcFile(
                    "/sys/module/process_reclaim/parameters/enable_process_reclaim",
                    new int[]{8224}, null, out, null);
            if (PerformanceManagerDebugConfig.DEBUG_RAMPOLICY) {
                Slog.d(TAG, "enable_process_reclaim=" + out[0]);
            }
            if (out[0] != 1) {
                enabled = false;
            }
        } catch (Exception e) {
            Slog.d(TAG, "reclaimProcessEnabled e=" + e);
            enabled = false;
        }
        if (PerformanceManagerDebugConfig.DEBUG_RAMPOLICY) {
            Slog.d(TAG, "process-reclaim current enabled:" + enabled);
        }
        return enabled;
    }

    private void sortReclaimListByScore(ArrayList<ReclaimProcessRecord> list, String policy) {
        int size = list.size();
        int maxReclaimCount = getMaxReclaimCount(policy);
        if (PerformanceManagerDebugConfig.DEBUG_RAMPOLICY) {
            Slog.d(TAG, "sortReclaimListByScore clip before size:" + list.size());
        }
        Collections.sort(list, new Comparator<ReclaimProcessRecord>() {
            @Override
            public int compare(ReclaimProcessRecord a, ReclaimProcessRecord b) {
                if (a.launchCount != b.launchCount) {
                    return a.launchCount <= b.launchCount ? -1 : 1;
                }
                if (a.info.curAdj != b.info.curAdj) {
                    return a.info.curAdj >= b.info.curAdj ? -1 : 1;
                }
                return 0;
            }
        });
        if (size > maxReclaimCount) {
            list.removeAll(new ArrayList<>(list.subList(maxReclaimCount, list.size())));
        }
        if (PerformanceManagerDebugConfig.DEBUG_RAMPOLICY) {
            Slog.d(TAG, "sortReclaimListByScore clip after size:" + list.size());
        }
        for (ReclaimProcessRecord record : list) {
            String p = record.policy;
            if (MemoryScene.POLICY_KILL.equals(p) || MemoryScene.POLICY_QUICK_KILL.equals(p)) {
                record.setPss(Process.getPss(record.info.pid) / ONE_MEGABYTE);
            }
        }
        if (PerformanceManagerDebugConfig.DEBUG_RAMPOLICY) {
            for (ReclaimProcessRecord record : list) {
                Slog.d(TAG, "sortReclaimListByScore after-->" + record);
            }
        }
    }

    @Override
    public void dump(PrintWriter writer, String[] args) {
        writer.println("RamPolicy:");
        writer.println("----------------------------");
        writer.println("Process-reclaim current enabled=" + this.mReclaimProcessEnabled);
        if (this.mMemoryConstant != null && this.mMemoryScene != null) {
            this.mMemoryConstant.dump(writer);
            this.mMemoryScene.dump(writer);
        }
        writer.println("----------------------------");
    }

    @Override
    public void executorPolicy(RamStatus ramStatus) {
        checkMemoryStatus(ramStatus);
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
        if (state == PROC_STATE_START && component != null) {
            ProcState processState = this.mService.getProcessState(component.getPackageName());
            if (processState == null || processState.avgTopPss == 0) {
                return;
            }
            long availableMemKb = this.mService.getCurrentRamStatus(true).getAvailableMemKb();
            long defaultCriticalMemory = processState.avgTopPss
                    + this.mMemoryConstant.getDefaultCriticalMemory() * ONE_MEGABYTE;
            if (PerformanceManagerDebugConfig.DEBUG_RAMPOLICY) {
                Slog.d(TAG, "handleActivityStateChange PROC_START -> " + component + " avg :"
                        + processState.avgTopPss + ", current:" + availableMemKb);
            }
            if (processState.avgTopPss
                    < this.mMemoryConstant.getBigMemCriticalMemory() * ONE_MEGABYTE
                    || availableMemKb >= defaultCriticalMemory) {
                return;
            }
            Slog.d(TAG, "handleActivityStateChange -> doMemoryReclaim SCENE_BIG_MEM");
            doMemoryReclaim(defaultCriticalMemory,
                    this.mMemoryScene.getPolicyForScene(MemoryScene.SCENE_BIG_MEM));
            return;
        }
        if (state == ACTIVITY_STATE_LAUNCH_DONE && component != null) {
            if (PerformanceManagerDebugConfig.DEBUG_RAMPOLICY) {
                Slog.d(TAG, "handleActivityStateChange LAUNCHDONE-> checkMemoryStatus"
                        + " PackageName=" + component.getPackageName());
            }
            checkMemoryStatus(this.mService.getCurrentRamStatus(true));
        } else if (state == ACTIVITY_STATE_RESUMED && component != null) {
            String homePackage = getHomePackageName();
            if (homePackage != null && homePackage.equals(component.getPackageName())) {
                if (PerformanceManagerDebugConfig.DEBUG_RAMPOLICY) {
                    Slog.d(TAG, "handleActivityStateChange RESUME-> checkMemoryStatus"
                            + " PackageName=" + component.getPackageName());
                }
                checkMemoryStatus(this.mService.getCurrentRamStatus(true));
            }
            this.mCurrentFocusPackage = component.getPackageName();
        }
    }

    @Override
    protected void handleInStrumentationChange(Message message) {
        this.mInStrumentation = true;
    }

    boolean isSpecialPackage(String packageName) {
        for (String special : this.mSpecialPackages) {
            if (packageName.contains(special)) {
                return true;
            }
        }
        return false;
    }
}