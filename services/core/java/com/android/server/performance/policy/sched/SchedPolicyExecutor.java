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

package com.android.server.performance.policy.sched;

import android.app.ActivityManager;
import android.app.ProcessInfo;
import android.content.ComponentName;
import android.content.Context;
import android.os.Process;
import android.os.Trace;
import android.util.Slog;

import com.android.server.performance.PerformanceManagerDebugConfig;
import com.android.server.performance.PerformanceManagerServiceImpl;
import com.android.server.performance.PolicyExecutor;
import com.android.server.performance.PolicyItem;
import com.android.server.performance.collector.CpuStatusCollector;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Boosts the scheduler class/priority of vendor-selected processes and threads.
 *
 * <p><b>Condemnation.</b> Grants elevated scheduling to a vendor white-list of
 * applications/threads, and to those deemed "top" by the vendor usage profile.
 * This is preferential treatment of selected applications. Reproduced only for
 * compatibility.
 *
 * @hide
 */
public class SchedPolicyExecutor extends PolicyExecutor {
    private static final String TAG = "xtcperformance-Sched";
    private static final String THREAD_NAME = "SchedPolicyExecutor";

    private static final int TOUCH_APP_PRIO = -19;
    private static final int ACTIVITY_STATE_RESUMED = 3;

    private final ActivityManager mAm;
    private final Context mContext;
    private final SchedAdjustment mSchedAdjustment;
    private final PerformanceManagerServiceImpl mService;
    private final SchedTunningRunnable mSchedTunningRunnable = new SchedTunningRunnable();

    private String mCurrPackageName;
    private ArrayList<Integer> mCurrTouchBoostThreads;
    public ProcessSchedBoostInfo mGameBoosts;
    public ProcessSchedBoostInfo mStartBoosts;
    public ProcessSchedBoostInfo mTouchBoosts;

    private class ProcessSchedBoostInfo {
        Integer pid;
        HashMap<Integer, ThreadInfo> boostThreads;

        ProcessSchedBoostInfo(Integer pid, HashMap<Integer, ThreadInfo> boostThreads) {
            this.pid = pid;
            this.boostThreads = boostThreads;
        }

        @Override
        public String toString() {
            String tids = "TID:";
            if (this.boostThreads != null && this.boostThreads.size() != 0) {
                for (Integer tid : this.boostThreads.keySet()) {
                    tids = tids + tid + " ";
                }
            }
            return "PID:" + this.pid + "|" + tids;
        }
    }

    private class SchedTunningRunnable implements Runnable {
        @Override
        public void run() {
            schedBoostForGame();
        }
    }

    private class ThreadInfo {
        int tid;
        int prio;
        int schedClass;

        ThreadInfo(int tid, int prio, int schedClass) {
            this.tid = tid;
            this.prio = prio;
            this.schedClass = schedClass;
        }

        @Override
        public String toString() {
            return " |tid:" + this.tid + "|prio" + this.prio + "|schedClass:" + this.schedClass;
        }
    }

    public SchedPolicyExecutor(PolicyItem policyItem, PerformanceManagerServiceImpl service) {
        this.mSchedAdjustment = SchedAdjustment.loadFromConfig(
                policyItem.getConfig(SchedAdjustment.TAG_NAME));
        this.mService = service;
        this.mContext = this.mService.getContext();
        this.mAm = (ActivityManager) this.mContext.getSystemService(Context.ACTIVITY_SERVICE);
    }

    private ArrayList<Integer> getBoostThreadsOfApp(int pid) {
        ArrayList<Integer> tids = new ArrayList<>();
        File[] tasks = new File("/proc/" + pid + "/task").listFiles();
        if (tasks == null) {
            return tids;
        }
        int found = 0;
        for (File task : tasks) {
            if (found > 1) {
                break;
            }
            try (BufferedReader reader = new BufferedReader(
                    new FileReader(new File(task.getAbsolutePath() + "/comm")))) {
                if ("RenderThread".equals(reader.readLine())
                        || pid == Integer.valueOf(task.getName())) {
                    tids.add(Integer.valueOf(task.getName()));
                    found++;
                }
            } catch (Exception e) {
                if (PerformanceManagerDebugConfig.DEBUG_SERVICE) {
                    e.printStackTrace();
                }
            }
        }
        if (PerformanceManagerDebugConfig.DEBUG_SERVICE) {
            Slog.d(TAG, "getBoostThreadsOfApp: tids:" + tids);
        }
        return tids;
    }

    private boolean isProcessRunning(int pid) {
        return new File("/proc/" + pid + "/status").exists();
    }

    private void schedBoostForGame() {
        ProcessInfo topApp = getTopApp();
        HashMap<Integer, ThreadInfo> current = new HashMap<>();
        HashMap<Integer, ThreadInfo> boost = new HashMap<>();
        synchronized (this) {
            if (topApp != null) {
                Slog.e(TAG, "schedBoostForGame top=" + topApp.packageName);
                try {
                    SchedAdjustment.SchedProcessInfo processInfo =
                            this.mSchedAdjustment.mMap.get(topApp.packageName);
                    if (processInfo == null) {
                        return;
                    }
                    File[] tasks = new File("/proc/" + topApp.pid + "/task").listFiles();
                    if (tasks == null) {
                        return;
                    }
                    for (File task : tasks) {
                        try (BufferedReader reader = new BufferedReader(
                                new FileReader(new File(task.getAbsolutePath() + "/comm")))) {
                            String comm = reader.readLine();
                            SchedAdjustment.SchedThreadInfo threadInfo =
                                    processInfo.schedThreadInfos.get(comm);
                            if (threadInfo == null) {
                                Iterator<String> keys =
                                        processInfo.schedThreadInfos.keySet().iterator();
                                while (keys.hasNext()) {
                                    String key = keys.next();
                                    if (key.endsWith("*")
                                            && comm.startsWith(key.replace("*", ""))) {
                                        threadInfo = processInfo.schedThreadInfos.get(key);
                                        break;
                                    }
                                }
                            }
                            if (threadInfo != null) {
                                int tid = Integer.valueOf(task.getName());
                                current.put(tid, new ThreadInfo(tid, Process.getThreadPriority(tid),
                                        Process.getThreadScheduler(tid)));
                                boost.put(tid, new ThreadInfo(tid, threadInfo.prio,
                                        threadInfo.schedClass));
                            }
                        }
                    }
                    this.mGameBoosts = new ProcessSchedBoostInfo(topApp.pid, current);
                    ProcessSchedBoostInfo info = new ProcessSchedBoostInfo(topApp.pid, boost);
                    Slog.d(TAG, "tunningSchedBoostIfNeeded,mGameBoosts:" + this.mGameBoosts
                            + ",currBoost:" + info);
                    scheduleBoost(info);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void scheduleBoost(ProcessSchedBoostInfo info) {
        if (PerformanceManagerDebugConfig.DEBUG_SERVICE) {
            Slog.w(TAG, "scheduleBoost,pid:" + info.pid + ",boostthread:" + info.boostThreads);
        }
        for (Integer tid : info.boostThreads.keySet()) {
            ThreadInfo threadInfo = info.boostThreads.get(tid);
            try {
                if (threadInfo.schedClass == 0) {
                    Process.setThreadScheduler(threadInfo.tid, threadInfo.schedClass, 0);
                    Process.setThreadPriority(threadInfo.tid, threadInfo.prio);
                    Slog.w(TAG, "scheduleBoost11 tid:" + tid + ", ThreadScheduler"
                            + Process.getThreadScheduler(tid) + ", ThreadPriority:"
                            + Process.getThreadPriority(tid));
                } else {
                    Process.setThreadScheduler(tid, 1073741826, 1);
                    Slog.w(TAG, "scheduleBoost22 tid:" + tid + ", ThreadScheduler"
                            + Process.getThreadScheduler(tid) + ", ThreadPriority:"
                            + Process.getThreadPriority(tid));
                }
            } catch (IllegalArgumentException e) {
                if (PerformanceManagerDebugConfig.DEBUG_SERVICE) {
                    Slog.w(TAG, "Failed to set scheduling policy, :\n" + e);
                    e.printStackTrace();
                }
            } catch (SecurityException e) {
                Slog.w(TAG, "Failed to set scheduling policy, not allowed:\n" + e);
            } catch (Exception e) {
                Slog.w(TAG, "Failed to set scheduling policy, error:\n" + e);
            }
        }
    }

    private void tunningSchedBoostIfNeeded(String packageName) {
        ProcessInfo topApp = getTopApp();
        synchronized (this) {
            SchedAdjustment.SchedProcessInfo processInfo = null;
            if (topApp != null) {
                Slog.d(TAG, "tunningSchedBoostIfNeeded top=" + topApp.packageName);
                processInfo = this.mSchedAdjustment.mMap.get(topApp.packageName);
                if (processInfo == null) {
                    if (this.mCurrPackageName != null
                            && !this.mCurrPackageName.equals(topApp.packageName)) {
                        scheduleBoostsAsRegual();
                    }
                    this.mCurrTouchBoostThreads = getBoostThreadsOfApp(topApp.pid);
                    this.mCurrPackageName = topApp.packageName;
                } else {
                    this.mCurrTouchBoostThreads = null;
                }
            }
            if (PerformanceManagerDebugConfig.DEBUG_SERVICE) {
                Slog.d(TAG, "tunningSchedBoostIfNeeded,top:" + topApp + ",mCurrentBoost:"
                        + this.mGameBoosts + " ,schedProcessInfo:" + processInfo);
            }
            Slog.d(TAG, "tunningSchedBoostIfNeeded mCurrTouchBoostThreads="
                    + this.mCurrTouchBoostThreads);
            if (this.mGameBoosts != null && this.mGameBoosts.boostThreads != null) {
                boolean appSwitch = topApp.pid != this.mGameBoosts.pid;
                Slog.d(TAG, "tunningSchedBoostIfNeeded appSwitch=" + appSwitch);
                boolean restore = appSwitch;
                if (!appSwitch && this.mGameBoosts.boostThreads.size() == 1) {
                    this.mHandler.post(this.mSchedTunningRunnable);
                    restore = false;
                }
                if (!restore || this.mGameBoosts == null) {
                    return;
                }
                this.mHandler.removeCallbacks(this.mSchedTunningRunnable);
                if (isProcessRunning(this.mGameBoosts.pid)) {
                    scheduleBoost(this.mGameBoosts);
                }
                this.mGameBoosts = null;
            }
            if (topApp != null && processInfo != null) {
                Slog.d(TAG, "tunningSchedBoostIfNeeded postDelayed mSchedTunningRunnable");
                this.mHandler.postDelayed(this.mSchedTunningRunnable,
                        CpuStatusCollector.MONITOR_CPU_MIN_TIME);
            }
        }
    }

    @Override
    public void dump(PrintWriter writer, String[] args) {
        writer.println("PRIO SCHED:");
        synchronized (this) {
            writer.println("CURRENT SCHED BOOST ->" + this.mGameBoosts);
        }
        this.mSchedAdjustment.dump(writer);
    }

    @Override
    public String getThreadName() {
        return THREAD_NAME;
    }

    ProcessInfo getTopApp() {
        java.util.List<ProcessInfo> processes = this.mAm.getRunningProcessesInfo();
        if (processes == null) {
            return null;
        }
        for (int i = 0; i < processes.size(); i++) {
            ProcessInfo info = processes.get(i);
            if (info != null && info.curAdj == 0 && "top-activity".equals(info.curAdjType)) {
                return info;
            }
        }
        return null;
    }

    @Override
    protected void handleActivityStateChange(
            PerformanceManagerServiceImpl.ActivityStateData data) {
        if (data != null) {
            int state = data.bundle.getInt("activity_state");
            ComponentName component = data.intent.getComponent();
            if (state != ACTIVITY_STATE_RESUMED || component == null) {
                return;
            }
            try {
                Slog.d(TAG, "handleActivityStateChange -> tunningSchedBoostIfNeeded"
                        + " PackageName=" + component.getPackageName());
                tunningSchedBoostIfNeeded(component.getPackageName());
            } catch (Exception e) {
                // ignored
            }
        }
    }

    public void scheduleBoostsAsRegual() {
        synchronized (this) {
            if (PerformanceManagerDebugConfig.DEBUG_SERVICE) {
                Slog.w(TAG, "scheduleBoostsAsRegual,pid:" + this.mTouchBoosts);
            }
            if (this.mTouchBoosts != null) {
                scheduleBoost(this.mTouchBoosts);
                Trace.asyncTraceEnd(Trace.TRACE_TAG_ACTIVITY_MANAGER,
                        "scheduleBoost" + this.mTouchBoosts.pid, 666);
                this.mTouchBoosts = null;
            }
        }
    }

    public void scheduleBoostsWhenTouch() {
        synchronized (this) {
            if (this.mCurrTouchBoostThreads == null || this.mCurrTouchBoostThreads.size() == 0) {
                return;
            }
            try {
                HashMap<Integer, ThreadInfo> current = new HashMap<>();
                HashMap<Integer, ThreadInfo> boost = new HashMap<>();
                for (Integer tid : this.mCurrTouchBoostThreads) {
                    if (PerformanceManagerDebugConfig.DEBUG_SERVICE) {
                        Slog.d(TAG, "scheduleBoostsWhenTouch,cur tid:" + tid + ", ThreadScheduler:"
                                + Process.getThreadScheduler(tid) + ", ThreadPriority:"
                                + Process.getThreadPriority(tid));
                    }
                    if (Process.getThreadScheduler(tid) == 0) {
                        if (Process.getThreadPriority(tid) <= TOUCH_APP_PRIO) {
                            break;
                        }
                        current.put(tid, new ThreadInfo(tid, Process.getThreadPriority(tid),
                                Process.getThreadScheduler(tid)));
                        boost.put(tid, new ThreadInfo(tid, TOUCH_APP_PRIO,
                                Process.getThreadScheduler(tid)));
                        Slog.d(TAG, "scheduleBoostsWhenTouch: touchBoost:" + this.mTouchBoosts);
                    }
                }
                if (current.size() == 0) {
                    return;
                }
                this.mTouchBoosts = new ProcessSchedBoostInfo(this.mCurrTouchBoostThreads.get(0),
                        current);
                if (PerformanceManagerDebugConfig.DEBUG_SERVICE) {
                    Slog.d(TAG, "scheduleBoostsWhenTouch: touchBoost:" + this.mTouchBoosts);
                }
                ProcessSchedBoostInfo boostInfo = new ProcessSchedBoostInfo(
                        this.mCurrTouchBoostThreads.get(0), boost);
                Trace.asyncTraceBegin(Trace.TRACE_TAG_ACTIVITY_MANAGER,
                        "scheduleBoost" + boostInfo.pid, 666);
                scheduleBoost(boostInfo);
            } catch (IllegalArgumentException e) {
                if (PerformanceManagerDebugConfig.DEBUG_SERVICE) {
                    Slog.w(TAG, "Failed to scheduleBoostsWhenTouch :\n" + e);
                    e.printStackTrace();
                }
            } catch (SecurityException e) {
                Slog.w(TAG, "Failed to scheduleBoostsWhenTouch, not allowed:\n" + e);
            } catch (Exception e) {
                Slog.w(TAG, "Failed to scheduleBoostsWhenTouch, error:\n" + e);
            }
        }
    }
}