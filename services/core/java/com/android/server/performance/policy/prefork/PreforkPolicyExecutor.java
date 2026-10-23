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

package com.android.server.performance.policy.prefork;

import android.app.IApplicationThread;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.os.Trace;
import android.util.Slog;

import com.android.server.am.ProcessList;
import com.android.server.performance.PerformanceManagerDebugConfig;
import com.android.server.performance.PerformanceManagerServiceImpl;
import com.android.server.performance.PolicyItem;
import com.android.server.performance.PolicyExecutor;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Pre-forks ("warms up") processes so that launching a favoured application is
 * faster.
 *
 * <p><b>Condemnation.</b> Pre-forking is restricted to a vendor white-list and
 * applications the vendor profile deems "top". It also kills pre-forked
 * processes that do not match the anticipated arguments. This privileges a
 * vendor-selected set of applications. Reproduced only for compatibility.
 *
 * @hide
 */
public class PreforkPolicyExecutor extends PolicyExecutor {
    private static final String TAG = "xtcperformance-Prefork";
    private static final String THREAD_NAME = "PreforkPolicyExecutor";

    private static final int PREFORK_PROCESS_ADJ = 800;
    private static final String PREFORK_PROCESS_PREFIX = "--preforkprocess-";

    private final PreforkConstants mPreforkConstants;
    private final PerformanceManagerServiceImpl mService;
    private final boolean mPreforkEnable = true;
    private final HashMap<Integer, HashMap<String, PreForkProcessStartResult>> mPreforkProcsResults =
            new HashMap<>();
    private final HashMap<String, PreForkArgs> mPreforkArgs = new HashMap<>();

    class PreForkArgs {
        String processClass;
        String niceName;
        int uid;
        int gid;
        int[] gids;
        int runtimeFlags;
        int mountExternal;
        int targetSdkVersion;
        String seInfo;
        String abi;
        String instructionSet;
        String appDataDir;
        String invokeWith;
        String[] zygoteArgs;

        public PreForkArgs(String processClass, String niceName, int uid, int gid, int[] gids,
                int runtimeFlags, int mountExternal, int targetSdkVersion, String seInfo,
                String abi, String instructionSet, String appDataDir, String invokeWith,
                String[] zygoteArgs) {
            this.processClass = processClass;
            this.niceName = niceName;
            this.uid = uid;
            this.gid = gid;
            this.gids = gids;
            this.runtimeFlags = runtimeFlags;
            this.mountExternal = mountExternal;
            this.targetSdkVersion = targetSdkVersion;
            this.seInfo = seInfo;
            this.abi = abi;
            this.instructionSet = instructionSet;
            this.appDataDir = appDataDir;
            this.invokeWith = invokeWith;
            this.zygoteArgs = zygoteArgs;
        }

        private boolean objectMatch(Object a, Object b) {
            if (a == null && b == null) {
                return true;
            }
            return a != null && b != null && a.equals(b);
        }

        public boolean match(PreForkArgs other) {
            return match(other.processClass, other.niceName, other.uid, other.gid, other.gids,
                    other.runtimeFlags, other.mountExternal, other.targetSdkVersion, other.seInfo,
                    other.abi, other.instructionSet, other.appDataDir, other.invokeWith,
                    other.zygoteArgs);
        }

        public boolean match(String processClass, String niceName, int uid, int gid, int[] gids,
                int runtimeFlags, int mountExternal, int targetSdkVersion, String seInfo,
                String abi, String instructionSet, String appDataDir, String invokeWith,
                String[] zygoteArgs) {
            return processClass.equals(this.processClass)
                    && niceName.equals(this.niceName)
                    && uid == this.uid
                    && gid == this.gid
                    && runtimeFlags == this.runtimeFlags
                    && mountExternal == this.mountExternal
                    && targetSdkVersion == this.targetSdkVersion
                    && objectMatch(seInfo, this.seInfo)
                    && objectMatch(abi, this.abi)
                    && objectMatch(instructionSet, this.instructionSet)
                    && objectMatch(appDataDir, this.appDataDir)
                    && objectMatch(invokeWith, this.invokeWith);
        }
    }

    class PreForkDeathRecipient implements IBinder.DeathRecipient {
        final IApplicationThread mApp;
        final PreForkProcessStartResult result;
        final int userId;

        PreForkDeathRecipient(int userId, IApplicationThread app, PreForkProcessStartResult result) {
            this.mApp = app;
            this.result = result;
            this.userId = userId;
        }

        @Override
        public void binderDied() {
            if (this.result.args != null && this.result.result != null) {
                Slog.w(TAG, "preforked process die..." + this.result.args.niceName + ", pid:"
                        + this.result.result.pid);
                if (this.result.watcher != null && this.mApp != null) {
                    this.mApp.asBinder().unlinkToDeath(this.result.watcher, 0);
                }
            }
            removePreforkResult(this.userId, this.result.args.niceName);
        }
    }

    class PreForkProcessStartResult {
        public Process.ProcessStartResult result = null;
        IApplicationThread app = null;
        int userId = 0;
        String procName = null;
        PreForkArgs args = null;
        PreForkDeathRecipient watcher = null;
    }

    public PreforkPolicyExecutor(PolicyItem policyItem,
            PerformanceManagerServiceImpl service) {
        this.mPreforkConstants = PreforkConstants.loadFromConfig(
                policyItem.getConfig(PreforkConstants.TAG_NAME));
        this.mService = service;
    }

    private void addPreForkArgs(String niceName, PreForkArgs args) {
        synchronized (this.mPreforkArgs) {
            if (niceName != null) {
                this.mPreforkArgs.put(niceName, args);
            }
        }
    }

    private void dumpResults() {
        synchronized (this.mPreforkProcsResults) {
            for (Integer userId : this.mPreforkProcsResults.keySet()) {
                HashMap<String, PreForkProcessStartResult> map =
                        this.mPreforkProcsResults.get(userId);
                for (String packageName : map.keySet()) {
                    PreForkProcessStartResult result = map.get(packageName);
                    if (result.result == null) {
                        Slog.d(TAG, "dumpResults PreforkProcsResults result.result==null"
                                + " packageName:" + packageName);
                    } else {
                        Slog.d(TAG, "dumpResults PreforkProcsResults packageName:" + packageName
                                + ",result.procName:" + result.procName + ",result.result.pid:"
                                + result.result.pid);
                    }
                }
            }
        }
    }

    private PreForkProcessStartResult getPreForkedProcessLocked(int userId, String processClass,
            String niceName, int uid, int gid, int[] gids, int runtimeFlags, int mountExternal,
            int targetSdkVersion, String seInfo, String abi, String instructionSet,
            String appDataDir, String invokeWith, String[] zygoteArgs) {
        PreForkProcessStartResult pending = getOrCreatPreforkResult(userId, niceName);
        if (pending == null || pending.result == null || pending.app == null
                || pending.args == null) {
            return null;
        }
        if (pending.args.match(processClass, niceName, uid, gid, gids, runtimeFlags, mountExternal,
                targetSdkVersion, seInfo, abi, instructionSet, appDataDir, invokeWith,
                zygoteArgs)) {
            Slog.d(TAG, "getPreForkedProcessLocked match result.result.pid="
                    + pending.result.pid);
            return pending;
        }
        if (pending.result.pid > 0 && pending.result.pid != Process.myPid()) {
            Process.killProcessQuiet(pending.result.pid);
            Slog.e(TAG, "getPreForkedProcessLocked killProcessQuiet pid=" + pending.result.pid);
        }
        removePreforkResult(userId, niceName);
        return null;
    }

    private HashMap<String, PreForkProcessStartResult> getPreforkResultHashMapLocked(int userId) {
        HashMap<String, PreForkProcessStartResult> map = this.mPreforkProcsResults.get(userId);
        if (map != null) {
            return map;
        }
        map = new HashMap<>();
        this.mPreforkProcsResults.put(userId, map);
        Slog.d(TAG, "getPreforkResultHashMapLocked new HashMap");
        return map;
    }

    private void handleStartPreForkedProcesses(int userId, int lastMemoryLevel) {
        removePreforkThread(userId);
        synchronized (this.mPreforkArgs) {
            for (String niceName : this.mPreforkArgs.keySet()) {
                PreForkArgs args = this.mPreforkArgs.get(niceName);
                if (getOrCreatPreforkResult(userId, niceName).result == null) {
                    startPreForkedProcess(userId, args);
                }
            }
        }
    }

    private boolean isEnablePreFork(String processName) {
        return this.mPreforkEnable && !this.mService.isMonkeyOrInStrumentation()
                && processName != null
                && !com.android.server.xgseserver.xss.HttpConfigOptions.PackageName
                        .PACKAGE_NAME_LAUNCHER.equals(processName);
    }

    private boolean isPackageSupportPreFork(int userId, String processName) {
        if (!isEnablePreFork(processName)) {
            if (PerformanceManagerDebugConfig.DEBUG_SERVICE) {
                Slog.e(TAG, "not prefork, procName:" + processName);
            }
            return false;
        }
        if (this.mService.isUserHabitTopApp(userId, processName)) {
            if (PerformanceManagerDebugConfig.DEBUG_SERVICE) {
                Slog.e(TAG, "isPackageSupportPreFork isUserHabitTopApp procName:" + processName);
            }
            return true;
        }
        boolean supported = this.mPreforkConstants.isPreForkSupportedDefault(processName);
        if (supported) {
            Slog.e(TAG, "isPackageSupportPreFork isPreForkSupportedDefault procName:" + processName);
        }
        return supported;
    }

    private void removePreForkArgs(String niceName) {
        synchronized (this.mPreforkArgs) {
            if (niceName != null) {
                this.mPreforkArgs.remove(niceName);
            }
        }
    }

    private void removePreforkResult(int userId, String niceName) {
        synchronized (this.mPreforkProcsResults) {
            if (this.mPreforkProcsResults.get(userId) != null) {
                this.mPreforkProcsResults.get(userId).remove(niceName);
            }
        }
    }

    private void removePreforkThread(int userId) {
        synchronized (this.mPreforkProcsResults) {
            HashMap<String, PreForkProcessStartResult> map = this.mPreforkProcsResults.get(userId);
            if (map == null) {
                return;
            }
            ArrayList<String> names = new ArrayList<>(map.keySet());
            for (int i = 0; i < names.size(); i++) {
                if (this.mPreforkProcsResults.get(userId).size()
                        > this.mPreforkConstants.getPreforkPackagesSize() + 2) {
                    String name = names.get(i);
                    PreForkProcessStartResult result = name == null ? null : map.get(name);
                    if (result != null && result.result != null && result.result.pid > 0
                            && result.result.pid != Process.myPid()) {
                        Process.killProcessQuiet(result.result.pid);
                        if (PerformanceManagerDebugConfig.DEBUG_SERVICE) {
                            Slog.w(TAG, "removePreforkThread niceName=" + result.args.niceName
                                    + " pid=" + result.result.pid);
                        }
                        removePreforkResult(userId, result.args.niceName);
                        removePreForkArgs(result.args.niceName);
                    }
                }
            }
        }
    }

    private void startPreForkedProcess(int userId, PreForkArgs args) {
        Slog.d(TAG, "startPreForkedProcess start");
        if (args == null) {
            return;
        }
        String preforkArg = PREFORK_PROCESS_PREFIX + args.niceName;
        String[] zygoteArgs;
        if (args.zygoteArgs == null) {
            zygoteArgs = new String[]{preforkArg};
        } else {
            zygoteArgs = new String[args.zygoteArgs.length + 1];
            System.arraycopy(args.zygoteArgs, 0, zygoteArgs, 0, args.zygoteArgs.length);
            zygoteArgs[args.zygoteArgs.length] = preforkArg;
        }
        synchronized (this.mPreforkProcsResults) {
            PreForkProcessStartResult pending = getOrCreatPreforkResult(userId, args.niceName);
            if (pending != null && pending.args != null) {
                if (pending.args.match(args)) {
                    Slog.w(TAG, "startPreForkedProcess :" + args.niceName
                            + "old match one exist..");
                    return;
                }
                Slog.w(TAG, "startPreForkedProcess :" + args.niceName + "kill old one...:"
                        + pending.result.pid);
                if (pending.result.pid > 0 && pending.result.pid != Process.myPid()) {
                    Process.killProcessQuiet(pending.result.pid);
                }
            }
            Trace.traceBegin(Trace.TRACE_TAG_ACTIVITY_MANAGER, "startPreForkedProcess, start");
            Process.ProcessStartResult startResult = Process.start(args.processClass,
                    args.niceName, args.uid, args.gid, args.gids, args.runtimeFlags,
                    args.mountExternal, args.targetSdkVersion, args.seInfo, args.abi,
                    args.instructionSet, args.appDataDir, args.invokeWith, zygoteArgs);
            Trace.traceEnd(Trace.TRACE_TAG_ACTIVITY_MANAGER);
            pending.result = startResult;
            pending.procName = args.niceName;
            pending.args = args;
            if (pending.result == null) {
                Slog.d(TAG, "startPreForkedProcess end add result=null");
            } else {
                Slog.d(TAG, "startPreForkedProcess end add result.pid = " + pending.result.pid
                        + " save.niceName=" + args.niceName);
            }
        }
    }

    public void attachPreforkApplicationLocked(int userId, IApplicationThread app, int pid,
            String processName) {
        if (PerformanceManagerDebugConfig.DEBUG_SERVICE) {
            Slog.d(TAG, "like-attachPreforkApplicationLocked start userId=" + userId
                    + " procName=" + processName);
        }
        synchronized (this.mPreforkProcsResults) {
            PreForkProcessStartResult pending = getOrCreatPreforkResult(userId, processName);
            if (pending.result != null) {
                Trace.traceBegin(Trace.TRACE_TAG_ACTIVITY_MANAGER,
                        "set IApplicationThread for:" + processName + ",pid:" + pid);
                pending.app = app;
                try {
                    PreForkDeathRecipient recipient =
                            new PreForkDeathRecipient(userId, app, pending);
                    app.asBinder().linkToDeath(recipient, 0);
                    pending.watcher = recipient;
                    ProcessList.setOomAdj(pending.result.pid, pending.args.uid,
                            PREFORK_PROCESS_ADJ);
                } catch (RemoteException e) {
                    Slog.w(TAG, "link to death fail for " + processName);
                    if (pid > 0 && pid != Process.myPid()) {
                        Process.killProcessQuiet(pid);
                    }
                    removePreforkResult(userId, processName);
                }
                Trace.traceEnd(Trace.TRACE_TAG_ACTIVITY_MANAGER);
            } else {
                Slog.w(TAG, "Wtf ? preforkedapp " + app + "for " + processName
                        + " can't find pending proc to attach");
                if (pid > 0 && pid != Process.myPid()) {
                    Process.killProcessQuiet(pid);
                }
            }
        }
        if (PerformanceManagerDebugConfig.DEBUG_SERVICE) {
            Slog.d(TAG, "like-attachPreforkApplicationLocked end");
        }
    }

    @Override
    public void dump(PrintWriter writer, String[] args) {
        writer.println("PreforkPolicyExecutor");
    }

    PreForkProcessStartResult getOrCreatPreforkResult(int userId, String niceName) {
        synchronized (this.mPreforkProcsResults) {
            HashMap<String, PreForkProcessStartResult> map =
                    getPreforkResultHashMapLocked(userId);
            PreForkProcessStartResult result = map.get(niceName);
            if (result == null) {
                result = new PreForkProcessStartResult();
                map.put(niceName, result);
                Slog.d(TAG, "getOrCreatPreforkResult new PreForkProcessStartResult pkgName="
                        + niceName);
            }
            return result;
        }
    }

    @Override
    public String getThreadName() {
        return THREAD_NAME;
    }

    @Override
    protected void handleActivityStateChange(
            PerformanceManagerServiceImpl.ActivityStateData data) {
    }

    public int handlePreForkStartProcess(int userId, String processClass, String niceName, int uid,
            int gid, int[] gids, int runtimeFlags, int mountExternal, int targetSdkVersion,
            String seInfo, String abi, String instructionSet, String appDataDir,
            String invokeWith, String[] zygoteArgs) {
        if (!isPackageSupportPreFork(userId, niceName)) {
            return -1;
        }
        if (PerformanceManagerDebugConfig.DEBUG_SERVICE) {
            Slog.d(TAG, "like-handlePreForkStartProcess start niceName=" + niceName);
        }
        Trace.traceBegin(Trace.TRACE_TAG_ACTIVITY_MANAGER,
                "handlePreForkStartProcess, addPreForkArgs: " + niceName);
        PreForkProcessStartResult result;
        synchronized (this.mPreforkProcsResults) {
            result = getPreForkedProcessLocked(userId, processClass, niceName, uid, gid, gids,
                    runtimeFlags, mountExternal, targetSdkVersion, seInfo, abi, instructionSet,
                    appDataDir, invokeWith, zygoteArgs);
        }
        addPreForkArgs(niceName, new PreForkArgs(processClass, niceName, uid, gid, gids,
                runtimeFlags, mountExternal, targetSdkVersion, seInfo, abi, instructionSet,
                appDataDir, invokeWith, zygoteArgs));
        Trace.traceEnd(Trace.TRACE_TAG_ACTIVITY_MANAGER);
        if (result == null || result.result == null) {
            Slog.d(TAG, "like-handlePreForkStartProcess end startResult=null");
            return -1;
        }
        if (PerformanceManagerDebugConfig.DEBUG_SERVICE) {
            Slog.d(TAG, "like-handlePreForkStartProcess end startResult.pid=" + result.result.pid);
        }
        return result.result.pid;
    }

    public void preforkStart(int userId, String processClass, String niceName, int uid, int gid,
            int[] gids, int runtimeFlags, int mountExternal, int targetSdkVersion, String seInfo,
            String abi, String instructionSet, String appDataDir, String invokeWith,
            String[] zygoteArgs) {
        if (isPackageSupportPreFork(userId, niceName)) {
            Trace.traceBegin(Trace.TRACE_TAG_ACTIVITY_MANAGER, "preforkStart, start:" + niceName);
            if (PerformanceManagerDebugConfig.DEBUG_SERVICE) {
                Slog.d(TAG, "like-preforkStart start niceName=" + niceName);
            }
            synchronized (this.mPreforkProcsResults) {
                PreForkProcessStartResult result = getPreForkedProcessLocked(userId, processClass,
                        niceName, uid, gid, gids, runtimeFlags, mountExternal, targetSdkVersion,
                        seInfo, abi, instructionSet, appDataDir, invokeWith, zygoteArgs);
                if (result == null) {
                    if (PerformanceManagerDebugConfig.DEBUG_SERVICE) {
                        Slog.d(TAG, "preforkStart, result null ");
                    }
                    return;
                }
                try {
                    if (PerformanceManagerDebugConfig.DEBUG_SERVICE) {
                        Slog.d(TAG, "preforkStart -> schedPreforkStart() niceName=" + niceName);
                    }
                    result.app.schedPreforkStart();
                    result.app.asBinder().unlinkToDeath(result.watcher, 0);
                    removePreforkResult(userId, niceName);
                    Trace.traceEnd(Trace.TRACE_TAG_ACTIVITY_MANAGER);
                    if (PerformanceManagerDebugConfig.DEBUG_SERVICE) {
                        Slog.d(TAG, "like-preforkStart end");
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                    if (result.result.pid > 0 && result.result.pid != Process.myPid()) {
                        Process.killProcessQuiet(result.result.pid);
                    }
                }
            }
        }
    }

    public void schedStartPreForkedProcesses(final int userId, final int lastMemoryLevel,
            String processName) {
        if (isPackageSupportPreFork(userId, processName)) {
            if (PerformanceManagerDebugConfig.DEBUG_SERVICE) {
                Slog.e(TAG, "like-schedStartPreForkedProcesses userId=" + userId
                        + " lastMemoryLevel=" + lastMemoryLevel);
            }
            Trace.traceBegin(Trace.TRACE_TAG_ACTIVITY_MANAGER, "schedStartPreForkedProcesses");
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    handleStartPreForkedProcesses(userId, lastMemoryLevel);
                }
            });
            Trace.traceEnd(Trace.TRACE_TAG_ACTIVITY_MANAGER);
        }
    }
}