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

package com.android.server.performance;

import android.app.ActivityManager;
import android.app.IApplicationThread;
import android.app.ProcessInfo;
import android.app.ProcState;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.InstrumentationInfo;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Trace;
import android.os.UserHandle;
import android.util.Slog;

import com.android.server.performance.collector.CatonStackCollector;
import com.android.server.performance.collector.CpuStatusCollector;
import com.android.server.performance.collector.ProcessStateCollector;
import com.android.server.performance.collector.UserHabitCollector;
import com.android.server.performance.policy.oomadj.OomAdjPolicyExecutor;
import com.android.server.performance.policy.prefork.PreforkPolicyExecutor;
import com.android.server.performance.policy.ram.RamPolicyExecutor;
import com.android.server.performance.policy.sched.SchedPolicyExecutor;
import com.android.server.performance.status.CpuStatus;
import com.android.server.performance.status.RamStatus;
import com.xtc.performance.IPerformance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Binder implementation of the XTC performance manager.
 *
 * <p><b>Condemnation.</b> This subsystem collects per-application usage,
 * launch-time, PSS and wake-lock data ({@link UserHabitCollector}) and applies
 * the vendor's OOM-adjustment / pre-fork policies — including special-casing
 * {@code com.xtc.*} and a hard-coded list of third-party apps. Ranking and
 * killing apps by vendor preference rather than user intent is a form of
 * platform discrimination. Reproduced only for behavioural compatibility.
 *
 * @hide
 */
public class PerformanceManagerServiceImpl extends IPerformance.Stub {
    public static final String COM_XTC_CATON_STACK_DUMP_ACTION = "com.xtc.caton.stack.dump";

    private static final long INIT_DELAY = 20000L;
    private static final long POLL_DELAY_START_PROC = 15000L;
    private static final long TOUCH_BOOST_REMOVE_DUR = 1000L;
    private static final long POLL_DELAY;

    private static final int MSG_INIT_MODULES = 0;
    private static final int MSG_ACTIVITY_STATE_CHANGE = 1;
    private static final int MSG_SCREEN_ON = 2;
    private static final int MSG_SCREEN_OFF = 3;
    private static final int MSG_SHUTDOWN = 4;
    private static final int MSG_INSTRUMENTATION_CHANGE = 5;
    private static final int MSG_BOOST_PR = 6;
    private static final int MSG_BOOST_PR_WHEN_TOUCH = 7;
    private static final int MSG_BOOST_AS_REGUAL = 8;
    private static final int MSG_START_DUMP_STACK = 9;

    private static final int ACTIVITY_STATE_RESUMED = 3;
    private static final int ACTIVITY_STATE_STOPPED = 2;
    private static final int ACTIVITY_STATE_UPDATE_CPU = 4;
    private static final int ACTIVITY_STATE_LAUNCH_TIME = 5;
    private static final int ACTIVITY_STATE_START_PROC = 6;

    public static class ActivityStateData {
        public Bundle bundle;
        public Intent intent;
    }

    public static class BoostAppInfo {
        public boolean boost;
        public int id;
        public boolean isPid;
    }

    public static class InStrumentationData {
        public InstrumentationInfo iInfo;
        public int pid;
        public boolean start;
    }

    public static class WakeLockData {
        public boolean held;
        public long lastHeldTime;
        public int refCount;
        public int uid;

        @Override
        public String toString() {
            return "WakelockData uid= " + this.uid + " held = " + this.held
                    + " refCount = " + this.refCount + " lastHeldTime = " + this.lastHeldTime;
        }
    }

    static {
        POLL_DELAY = ActivityManager.isLowRamDeviceStatic() ? 60000L : 120000L;
    }

    private final ActivityManager mAm;
    private final Context mContext;
    private Handler mHandler;
    private HandlerThread mThread;

    private CatonStackCollector mCatonStackCollector;
    private CpuStatusCollector mCpuStatusCollector;
    private RamStatus mLastRamStatus;
    private MonitorRunnable mMonitorRunnable;
    private OomAdjPolicyExecutor mOomAdjPolicyExecutor;
    private PreforkPolicyExecutor mPreforkPolicyExecutor;
    private ProcessStateCollector mProcessStateCollector;
    private RamPolicyExecutor mRamPolicyExecutor;
    private SchedPolicyExecutor mSchedPolicyExecutor;
    private UserHabitCollector mUserHabitCollector;

    private boolean monitorEnabled = true;
    private boolean isInitDone = false;
    private boolean mInStrumentationChange = false;
    private int mRecentForkedPid = -1;
    private int mRamConfig = -1;

    private final Object mRamStatusLock = new Object();
    private final RamStatus ramStatus = new RamStatus();
    private final HashMap<Integer, WakeLockData> mBusyUids = new HashMap<>();
    private String mCurrentFocusPackage = "";
    private final List<String> mSpecialPackages = new ArrayList<>();
    final List<PolicyExecutor> mPolicyExecutors = new ArrayList<>();

    private final BroadcastReceiver mBroadcastReceiver = new PerformanceBroadcastReceiver();

    private class MonitorRunnable implements Runnable {
        @Override
        public void run() {
            if (monitorEnabled) {
                Trace.traceBegin(Trace.TRACE_TAG_ACTIVITY_MANAGER, "monitorSystemStatus");
                monitorSystemStatus();
                Trace.traceEnd(Trace.TRACE_TAG_ACTIVITY_MANAGER);
            }
        }
    }

    private class PerformanceBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Slog.d(PerformanceManagerDebugConfig.TAG, "onReceive action=" + action);
            if (Intent.ACTION_SHUTDOWN.equals(action)) {
                mHandler.sendEmptyMessage(MSG_SHUTDOWN);
                return;
            }
            if (Intent.ACTION_SCREEN_ON.equals(action)) {
                mHandler.sendEmptyMessage(MSG_SCREEN_ON);
            } else if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                mHandler.sendEmptyMessage(MSG_SCREEN_OFF);
            } else if (COM_XTC_CATON_STACK_DUMP_ACTION.equals(action)) {
                mHandler.sendEmptyMessage(MSG_START_DUMP_STACK);
            }
        }
    }

    public PerformanceManagerServiceImpl(Context context) {
        this.mContext = context;
        this.mAm = (ActivityManager) this.mContext.getSystemService(Context.ACTIVITY_SERVICE);
        initMainHandler();
        Slog.d(PerformanceManagerDebugConfig.TAG, "context: " + context + " , main thread: "
                + Thread.currentThread().getName() + " , main threadId: "
                + Thread.currentThread().getId());
        this.mSpecialPackages.add("com.baidu.input.xtcime");
        this.mSpecialPackages.add("com.eg.android.AlipayGphone");
        this.mSpecialPackages.add("com.baidu.voicesearch");
    }

    private void changeMonitorSystemStatus(boolean enabled, long delay) {
        this.monitorEnabled = enabled;
        this.mHandler.removeCallbacks(this.mMonitorRunnable);
        if (this.monitorEnabled) {
            startMonitorSystemStatus(delay);
        }
    }

    private void dumpCatonStack() {
        if (this.isInitDone && this.mCatonStackCollector != null
                && this.mCatonStackCollector.isRunning()) {
            CatonStackCollector.print2File();
        }
    }

    private void handleActivityStateChange(Intent intent, Bundle bundle) {
        if (bundle == null || intent == null) {
            return;
        }
        int state = bundle.getInt("activity_state");
        ComponentName component = intent.getComponent();
        if (component == null) {
            return;
        }
        ActivityStateData data = new ActivityStateData();
        data.intent = intent;
        data.bundle = bundle;
        Slog.d(PerformanceManagerDebugConfig.TAG, "handleActivityStateChange state=" + state
                + " app.getPackageName()=" + component.getPackageName());
        switch (state) {
            case ACTIVITY_STATE_STOPPED:
                if (this.mUserHabitCollector != null) {
                    this.mUserHabitCollector.updateHabitStatus(component.getPackageName(), false,
                            false);
                }
                break;
            case ACTIVITY_STATE_RESUMED: {
                boolean isDifferent = !this.mCurrentFocusPackage.equals(component.getPackageName());
                if (this.mUserHabitCollector != null) {
                    this.mUserHabitCollector.updateHabitStatus(component.getPackageName(), true,
                            isDifferent);
                }
                this.mCurrentFocusPackage = component.getPackageName();
                break;
            }
            case ACTIVITY_STATE_UPDATE_CPU:
                if (this.mCpuStatusCollector != null) {
                    this.mCpuStatusCollector.updateCpuUsageNow();
                }
                break;
            case ACTIVITY_STATE_LAUNCH_TIME: {
                long launchTime = bundle.getLong("launchTime");
                if (this.mUserHabitCollector != null && launchTime != 0) {
                    this.mUserHabitCollector.updateHabitsLaunchTime(component.getPackageName(),
                            launchTime);
                }
                if (PerformanceManagerDebugConfig.DEBUG_SERVICE && launchTime != 0) {
                    Slog.d(PerformanceManagerDebugConfig.TAG, "app:" + component
                            + " launchtime= " + launchTime);
                }
                break;
            }
            case ACTIVITY_STATE_START_PROC: {
                String hostingType = bundle.getString("hostingtype");
                int pid = Integer.valueOf(bundle.getString("pid"));
                if (PerformanceManagerDebugConfig.DEBUG_SERVICE) {
                    Slog.d(PerformanceManagerDebugConfig.TAG, "start proc for "
                            + component.getPackageName() + " type:" + hostingType + " pid:" + pid);
                }
                if (hostingType != null && "activity".equals(hostingType)) {
                    if (this.mCpuStatusCollector != null) {
                        this.mCpuStatusCollector.scheduleUpdateCpuUsage(
                                CpuStatusCollector.MONITOR_CPU_MIN_TIME);
                    }
                    changeMonitorSystemStatus(true, POLL_DELAY_START_PROC);
                    this.mRecentForkedPid = pid;
                }
                break;
            }
            default:
                break;
        }
        notifyPolicyExecutorsActivityStateChanged(data);
    }

    private void handleInStrumentationChange(Message message) {
        this.mInStrumentationChange = true;
        synchronized (this.mPolicyExecutors) {
            for (PolicyExecutor executor : this.mPolicyExecutors) {
                executor.onInStrumentationChange((InStrumentationData) message.obj);
            }
        }
    }

    private void handleInitModules() {
        this.mMonitorRunnable = new MonitorRunnable();
        this.mUserHabitCollector = new UserHabitCollector(this, this.mHandler);
        this.mCatonStackCollector = new CatonStackCollector(this);
        this.mProcessStateCollector = new ProcessStateCollector(this.mAm, this.mHandler);
        initPolicyExecutorsFromConfig();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SHUTDOWN);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(COM_XTC_CATON_STACK_DUMP_ACTION);
        this.mContext.registerReceiverAsUser(this.mBroadcastReceiver, UserHandle.ALL, filter,
                null, null);
        startMonitorSystemStatus(POLL_DELAY);
        this.isInitDone = true;
        Slog.d(PerformanceManagerDebugConfig.TAG, "init done");
    }

    private void handleShutDown() {
        if (this.mUserHabitCollector != null) {
            this.mUserHabitCollector.onShutDown();
        }
        if (this.mProcessStateCollector != null) {
            this.mProcessStateCollector.onShutDown();
        }
        dumpCatonStack();
    }

    private void initMainHandler() {
        this.mThread = new HandlerThread(PerformanceManagerDebugConfig.TAG);
        this.mThread.start();
        this.mHandler = new Handler(this.mThread.getLooper()) {
            @Override
            public void handleMessage(Message message) {
                Trace.traceBegin(Trace.TRACE_TAG_ACTIVITY_MANAGER,
                        "handleMessage:" + message.what);
                switch (message.what) {
                    case MSG_INIT_MODULES:
                        handleInitModules();
                        break;
                    case MSG_ACTIVITY_STATE_CHANGE:
                        handleActivityStateChange((Intent) message.obj, message.getData());
                        break;
                    case MSG_SCREEN_ON:
                        Slog.d(PerformanceManagerDebugConfig.TAG, "MSG_SCREEN_ON");
                        changeMonitorSystemStatus(true, POLL_DELAY);
                        break;
                    case MSG_SCREEN_OFF:
                        Slog.d(PerformanceManagerDebugConfig.TAG, "MSG_SCREEN_OFF");
                        changeMonitorSystemStatus(false, POLL_DELAY);
                        stopCatonStackMonitor();
                        break;
                    case MSG_SHUTDOWN:
                        Slog.d(PerformanceManagerDebugConfig.TAG, "MSG_SHUTDOWN");
                        handleShutDown();
                        break;
                    case MSG_INSTRUMENTATION_CHANGE:
                        handleInStrumentationChange(message);
                        break;
                    case MSG_START_DUMP_STACK:
                        dumpCatonStack();
                        break;
                    default:
                        break;
                }
                Trace.traceEnd(Trace.TRACE_TAG_ACTIVITY_MANAGER);
            }
        };
    }

    private void initPolicyExecutorsFromConfig() {
        synchronized (this.mPolicyExecutors) {
            try {
                HashMap<String, Class<?>> map = new HashMap<>();
                map.put("Memory", RamPolicyExecutor.class);
                map.put("OomAdj", OomAdjPolicyExecutor.class);
                map.put("Prefork", PreforkPolicyExecutor.class);
                for (PolicyItem item : PolicyExecutorConfigLoader.load()) {
                    Class<?> cls = map.get(item.getName());
                    if (cls != null) {
                        Slog.d(PerformanceManagerDebugConfig.TAG,
                                "initPolicyExecutorsFromConfig : " + item.getName());
                        PolicyExecutor executor = (PolicyExecutor) cls.getConstructor(
                                PolicyItem.class, PerformanceManagerServiceImpl.class)
                                .newInstance(item, this);
                        this.mPolicyExecutors.add(executor);
                        if (executor instanceof RamPolicyExecutor) {
                            this.mRamPolicyExecutor = (RamPolicyExecutor) executor;
                            Slog.d(PerformanceManagerDebugConfig.TAG,
                                    "created RamPolicyExecutor..");
                        } else if (executor instanceof OomAdjPolicyExecutor) {
                            this.mOomAdjPolicyExecutor = (OomAdjPolicyExecutor) executor;
                            Slog.d(PerformanceManagerDebugConfig.TAG,
                                    "created OomAdjPolicyExecutor..");
                        } else if (executor instanceof PreforkPolicyExecutor) {
                            this.mPreforkPolicyExecutor = (PreforkPolicyExecutor) executor;
                            Slog.d(PerformanceManagerDebugConfig.TAG,
                                    "created PreforkPolicyExecutor..");
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private boolean isSpecialSysPackage(String packageName) {
        for (String special : this.mSpecialPackages) {
            if (packageName.contains(special)) {
                return true;
            }
        }
        return false;
    }

    private void monitorSystemStatus() {
        Slog.d(PerformanceManagerDebugConfig.TAG, "monitorSystemStatus");
        RamStatus currentRamStatus = getCurrentRamStatus(true);
        if (this.mProcessStateCollector != null) {
            Trace.traceBegin(Trace.TRACE_TAG_ACTIVITY_MANAGER, "updateProcStates");
            this.mProcessStateCollector.updateProcStates();
            Trace.traceEnd(Trace.TRACE_TAG_ACTIVITY_MANAGER);
        }
        notifyPolicyExecutorsSystemStatusChanged(currentRamStatus);
        startMonitorSystemStatus(POLL_DELAY);
    }

    private void notifyPolicyExecutorsActivityStateChanged(ActivityStateData data) {
        synchronized (this.mPolicyExecutors) {
            for (PolicyExecutor executor : this.mPolicyExecutors) {
                executor.onActivityStateChanged(data);
            }
        }
    }

    private void notifyPolicyExecutorsSystemStatusChanged(RamStatus status) {
        synchronized (this.mPolicyExecutors) {
            for (PolicyExecutor executor : this.mPolicyExecutors) {
                executor.onRamStatusChanged(status);
            }
        }
    }

    private void startMonitorSystemStatus(long delay) {
        Slog.d(PerformanceManagerDebugConfig.TAG, "startMonitorSystemStatus monitorEnabled="
                + this.monitorEnabled + " delay=" + delay);
        if (this.monitorEnabled) {
            this.mHandler.removeCallbacks(this.mMonitorRunnable);
            this.mHandler.postDelayed(this.mMonitorRunnable, delay);
        }
    }

    private void stopCatonStackMonitor() {
        if (!this.isInitDone || this.mCatonStackCollector == null
                || !this.mCatonStackCollector.isRunning()
                || SystemProperties.getBoolean(CatonStackCollector.CATON_OPEN_PROP, false)) {
            return;
        }
        CatonStackCollector.print2File();
        this.mAm.stopCatonStackMonitor();
    }

    @Override
    public void acquireBoost(int boost) {
        if (this.isInitDone && PerformanceManagerDebugConfig.DEBUG_SERVICE) {
            Slog.d(PerformanceManagerDebugConfig.TAG, "send data thread: "
                    + Thread.currentThread().getName() + " ,threadId: "
                    + Thread.currentThread().getId());
        }
    }

    @Override
    public void attachPreforkApplication(int pid, IBinder bindApplicationTransaction,
            String processName) {
        if (this.mPreforkPolicyExecutor != null) {
            int callingPid = Binder.getCallingPid();
            Trace.traceBegin(Trace.TRACE_TAG_ACTIVITY_MANAGER,
                    "attachPreforkApplication, start for:" + processName + ",pid:" + callingPid);
            long token = Binder.clearCallingIdentity();
            this.mPreforkPolicyExecutor.attachPreforkApplicationLocked(pid,
                    IApplicationThread.Stub.asInterface(bindApplicationTransaction), callingPid,
                    processName);
            Binder.restoreCallingIdentity(token);
            Trace.traceEnd(Trace.TRACE_TAG_ACTIVITY_MANAGER);
        }
    }

    public Context getContext() {
        return this.mContext;
    }

    public CpuStatusCollector getCpuStatusCollector() {
        return this.mCpuStatusCollector;
    }

    public RamStatus getCurrentRamStatus(boolean update) {
        if (update) {
            synchronized (this.mRamStatusLock) {
                this.ramStatus.updateMemInfo();
            }
        }
        return this.ramStatus;
    }

    public ProcState getProcessState(String processName) {
        if (this.mProcessStateCollector != null) {
            return this.mProcessStateCollector.getProcStates(UserHandle.myUserId(), processName);
        }
        Slog.e(PerformanceManagerDebugConfig.TAG, "getProcessState return null processName="
                + processName);
        return null;
    }

    public int getRamConfig() {
        if (this.mRamConfig == -1) {
            this.mRamConfig = getCurrentRamStatus(true).getRamConfig();
        }
        return this.mRamConfig;
    }

    public UserHabitCollector getUserHabitCollector() {
        return this.mUserHabitCollector;
    }

    @Override
    public int handlePreForkStartProcess(int callingPid, String packageName, String processName,
            int targetSdk, int uid, int[] gids, int runtimeFlags, int mountExternal,
            int seInfo, String niceName, String instructionSet, String appDataDir,
            String reserved1, String reserved2, String[] sharedLibraries) {
        if (this.mPreforkPolicyExecutor != null) {
            return this.mPreforkPolicyExecutor.handlePreForkStartProcess(callingPid, packageName,
                    processName, targetSdk, uid, gids, runtimeFlags, mountExternal, seInfo,
                    niceName, instructionSet, appDataDir, reserved1, reserved2, sharedLibraries);
        }
        return -1;
    }

    public boolean isExecutableApp(String packageName) {
        if (packageName != null) {
            try {
                return (getContext().getPackageManager().getApplicationInfo(packageName, 0).flags
                        & android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0
                        || packageName.startsWith("com.xtc.")
                        || isSpecialSysPackage(packageName);
            } catch (PackageManager.NameNotFoundException e) {
                if (PerformanceManagerDebugConfig.DEBUG_SERVICE) {
                    Slog.d(PerformanceManagerDebugConfig.TAG, "isExecutableApp:" + e);
                }
            }
        }
        return false;
    }

    protected boolean isInStrumentationChangeEnabled() {
        return this.mInStrumentationChange;
    }

    public boolean isMonkeyOrInStrumentation() {
        if (this.mInStrumentationChange) {
            return true;
        }
        return ActivityManager.isUserAMonkey();
    }

    public boolean isPackageUsrFavorite(String packageName) {
        if (this.mOomAdjPolicyExecutor != null) {
            return this.mOomAdjPolicyExecutor.isPackageUsrFavorite(packageName);
        }
        return false;
    }

    public boolean isProcessHeldWakeLock(int uid) {
        synchronized (this.mBusyUids) {
            WakeLockData data = this.mBusyUids.get(uid);
            return data != null && data.held;
        }
    }

    public boolean isProcessHeldWakeLock(int uid, long timeout) {
        long now = SystemClock.uptimeMillis();
        synchronized (this.mBusyUids) {
            WakeLockData data = this.mBusyUids.get(uid);
            return data != null && (data.held || now - data.lastHeldTime < timeout);
        }
    }

    public boolean isUserHabitTopApp(int userId, String packageName) {
        if (this.mOomAdjPolicyExecutor != null) {
            return this.mOomAdjPolicyExecutor.isUserHabitTopApp(userId, packageName);
        }
        return false;
    }

    public void loadInit() {
        if (this.mHandler != null) {
            this.mHandler.sendEmptyMessageDelayed(MSG_INIT_MODULES, INIT_DELAY);
        } else {
            Slog.w(PerformanceManagerDebugConfig.TAG, "mHandler is null, need check.");
        }
    }

    @Override
    public void notifyActivityStateChange(Intent intent, Bundle bundle) {
        if (!this.isInitDone || bundle == null) {
            return;
        }
        Message message = new Message();
        message.what = MSG_ACTIVITY_STATE_CHANGE;
        message.setData(bundle);
        message.obj = intent;
        this.mHandler.sendMessage(message);
    }

    @Override
    public void notifyInstrumentationStatus(boolean started, int pid,
            InstrumentationInfo instrumentationInfo) {
        if (this.isInitDone) {
            Message message = new Message();
            message.what = MSG_INSTRUMENTATION_CHANGE;
            InStrumentationData data = new InStrumentationData();
            data.pid = pid;
            data.iInfo = instrumentationInfo;
            data.start = started;
            message.obj = data;
            this.mHandler.sendMessage(message);
        }
    }

    public void notifyPolicyExecutorsCpuStatusChanged(CpuStatus status) {
        synchronized (this.mPolicyExecutors) {
            for (PolicyExecutor executor : this.mPolicyExecutors) {
                executor.onCpusStatusChanged(status);
            }
        }
    }

    @Override
    public void preforkStart(int callingPid, String packageName, String processName, int targetSdk,
            int uid, int[] gids, int runtimeFlags, int mountExternal, int seInfo, String niceName,
            String instructionSet, String appDataDir, String reserved1, String reserved2,
            String[] sharedLibraries) {
        if (this.mPreforkPolicyExecutor != null) {
            this.mPreforkPolicyExecutor.preforkStart(callingPid, packageName, processName,
                    targetSdk, uid, gids, runtimeFlags, mountExternal, seInfo, niceName,
                    instructionSet, appDataDir, reserved1, reserved2, sharedLibraries);
        }
    }

    @Override
    public int processLmkAdjTunningIfneeded(ProcessInfo processInfo, int expectedAdj, long pss,
            long swapPss) {
        if (this.mOomAdjPolicyExecutor == null) {
            return 2;
        }
        if (this.mOomAdjPolicyExecutor.increaseAdjIfNeeded(processInfo, expectedAdj, pss,
                swapPss)) {
            return 0;
        }
        return this.mOomAdjPolicyExecutor.doAdjDropIfNeeded(processInfo, expectedAdj, pss,
                swapPss) ? 1 : 2;
    }

    @Override
    public void saveCatonStack(List<String> stack) {
        if (!this.isInitDone || this.mCatonStackCollector == null) {
            return;
        }
        this.mCatonStackCollector.scheduleUpdateCatonStack(stack);
    }

    @Override
    public void schedStartPreForkedProcesses(int pid, int targetSdk, String processName) {
        if (this.mPreforkPolicyExecutor != null) {
            this.mPreforkPolicyExecutor.schedStartPreForkedProcesses(pid, targetSdk, processName);
        }
    }

    @Override
    public void scheduleBoostWhenTouch() {
        if (PerformanceManagerDebugConfig.DEBUG_SERVICE) {
            Slog.d(PerformanceManagerDebugConfig.TAG, "scheduleBoostWhenTouch");
        }
        if (isMonkeyOrInStrumentation() || this.mSchedPolicyExecutor == null
                || this.mHandler.hasMessages(MSG_BOOST_PR_WHEN_TOUCH)) {
            return;
        }
        if (this.mHandler.hasMessages(MSG_BOOST_AS_REGUAL)) {
            this.mHandler.removeMessages(MSG_BOOST_AS_REGUAL);
        } else {
            this.mHandler.sendEmptyMessage(MSG_BOOST_PR_WHEN_TOUCH);
        }
        this.mHandler.sendEmptyMessageDelayed(MSG_BOOST_AS_REGUAL, TOUCH_BOOST_REMOVE_DUR);
    }
}