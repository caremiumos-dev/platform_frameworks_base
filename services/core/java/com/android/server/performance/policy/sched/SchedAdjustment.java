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

import android.util.Log;

import com.android.server.performance.PolicyConfig;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Scheduling (thread priority) adjustments loaded from the performance
 * configuration.
 *
 * <p><b>Condemnation.</b> This applies vendor-chosen scheduler classes and
 * priorities to specific third-party processes/threads, i.e. preferential
 * treatment of selected applications. Reproduced only for compatibility.
 *
 * @hide
 */
public class SchedAdjustment {
    public static final String TAG_NAME = "SchedAdjustment";
    public static final String TAG_PACKAGE = "package";
    public static final String TAG_PROCESS = "process";
    public static final String TAG_THREADS = "threads";
    public static final String TAG_SCHEDCLASS = "schedClass";
    public static final String TAG_PRIO = "prio";

    private static final String TAG = "xtcperformance-Sched";

    private static final int SCHED_OTHER = 0;
    private static final int SCHED_FIFO = 1;
    private static final int SCHED_RR = 2;

    public HashMap<String, SchedProcessInfo> mMap = new HashMap<>();

    public static class SchedProcessInfo {
        String packageName;
        String processName;
        ArrayList<String> boostThreads = new ArrayList<>();
        ArrayList<String> boostLikeThreads = new ArrayList<>();
        HashMap<String, SchedThreadInfo> schedThreadInfos = new HashMap<>();

        @Override
        public String toString() {
            String result = "Threads:";
            Iterator<String> iterator = this.schedThreadInfos.keySet().iterator();
            while (iterator.hasNext()) {
                SchedThreadInfo info = this.schedThreadInfos.get(iterator.next());
                result = result + info.thread + "|schedClass:" + info.schedClass
                        + "|prio:" + info.prio;
            }
            return "pkg:" + this.packageName + "|NAME:" + this.processName + "|" + result;
        }
    }

    public static class SchedThreadInfo {
        String thread;
        int schedClass;
        int prio;

        @Override
        public String toString() {
            return "PID NAME:" + this.thread + "|schedClass:" + this.schedClass
                    + "|prio" + this.prio;
        }
    }

    public static SchedAdjustment loadFromConfig(PolicyConfig policyConfig) {
        SchedAdjustment adjustment = new SchedAdjustment();
        HashMap<String, PolicyConfig.ConfigItem> items = policyConfig.getAllConfigItems();
        if (items == null) {
            return null;
        }
        for (String packageName : items.keySet()) {
            PolicyConfig.ConfigItem item = items.get(packageName);
            SchedProcessInfo processInfo = new SchedProcessInfo();
            processInfo.packageName = packageName;
            processInfo.processName = item.getString(TAG_PROCESS);
            for (String thread : item.getStringArray(TAG_THREADS)) {
                SchedThreadInfo threadInfo = new SchedThreadInfo();
                if (thread.contains(",")) {
                    String[] parts = thread.split(",");
                    threadInfo.thread = parts[0];
                    threadInfo.schedClass = transferSchedPolicy(parts[1].trim());
                    threadInfo.prio = Integer.parseInt(parts[2].trim());
                    processInfo.schedThreadInfos.put(parts[0], threadInfo);
                    if (parts[0].endsWith("*")) {
                        processInfo.boostLikeThreads.add(parts[0].replace("*", "").trim());
                    } else {
                        processInfo.boostThreads.add(parts[0]);
                    }
                }
            }
            if (processInfo.processName != null) {
                adjustment.mMap.put(packageName, processInfo);
            }
        }
        return adjustment;
    }

    private static int transferSchedPolicy(String policy) {
        if (policy.equals("other")) {
            return SCHED_OTHER;
        }
        if (policy.equals("rr")) {
            return SCHED_RR;
        }
        return policy.equals("fifo") ? SCHED_FIFO : SCHED_OTHER;
    }

    public void dump() {
        if (this.mMap != null) {
            for (String packageName : this.mMap.keySet()) {
                SchedProcessInfo info = this.mMap.get(packageName);
                if (info != null) {
                    Log.e(TAG, "" + info);
                }
            }
        }
    }

    public void dump(PrintWriter writer) {
        writer.println("Dump ScheduleAdjust:");
        writer.println("----------------------------");
        if (this.mMap != null) {
            for (String packageName : this.mMap.keySet()) {
                SchedProcessInfo info = this.mMap.get(packageName);
                if (info != null) {
                    writer.println("" + info);
                }
            }
        }
        writer.println("----------------------------");
    }

    public boolean isPackageSupported(String packageName) {
        return this.mMap.get(packageName) != null;
    }

    public boolean isProcessSupported(String packageName, String processName) {
        SchedProcessInfo info = this.mMap.get(packageName);
        return info != null && info.processName.equals(processName);
    }

    public boolean isThreadBoostNeeded(String packageName, String processName, String thread) {
        SchedProcessInfo info = this.mMap.get(packageName);
        if (info != null && info.processName.equals(processName)
                && info.boostThreads.contains(thread)) {
            return true;
        }
        if (info == null || !info.processName.equals(processName)) {
            return false;
        }
        for (String prefix : info.boostLikeThreads) {
            if (thread.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}