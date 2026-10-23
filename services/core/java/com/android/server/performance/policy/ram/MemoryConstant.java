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

import android.util.Log;

import com.android.server.performance.PolicyConfig;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

/**
 * Memory thresholds and force-stop parameters for the RAM policy.
 *
 * @hide
 */
public class MemoryConstant {
    public static final String TAG_NAME = "MemoryConstant";

    private static final String TAG = "xtcperformance-Ram";
    private static final String TAG_LOW_CPU_LOAD = "lowCpuLoad";
    private static final String TAG_HIGH_CPU_LOAD = "highCpuLoad";
    private static final String TAG_EMER_MEM = "emergencyMemory";
    private static final String TAG_BIG_CRITIAL_MEM = "bigMemCriticalMemory";
    private static final String TAG_DEF_CRITIAL_MEM = "defaultCriticalMemory";
    private static final String TAG_NORMAL_MEM = "normalMemory";
    private static final String TAG_FS_MEM = "fsMemory";
    private static final String TAG_FS_IDLE = "fsIdleTime";
    private static final String TAG_SWAP_USAGE = "swapUsage";
    private static final String TAG_BG_PKG_LIMIT = "bgPkgLimit";

    private static final long MINUTE_MILLIS = 60000L;

    private PolicyConfig.ConfigItem mConfig;

    long lowCpuLoad = 30L;
    long highCpuLoad = 60L;
    long emergencyMemory = 150L;
    long bigMemCriticalMemory = 80L;
    long defaultCriticalMemory = 180L;
    long normalMemory = 300L;
    ArrayList<ForceStopParams> mFsParams = new ArrayList<>();
    int backgroundRunningLimit = 3;

    private static class ForceStopParams {
        public long fsMemory;
        public long fsIdleTime;
        public int swapUsage;
    }

    public static MemoryConstant loadFromConfig(PolicyConfig policyConfig, int ramConfig) {
        MemoryConstant constant = new MemoryConstant();
        PolicyConfig.ConfigItem item = policyConfig.getConfigItem(String.valueOf(ramConfig));
        PolicyConfig.ConfigItem target = item == null ? policyConfig.getDefaultItem() : item;
        if (target != null) {
            try {
                constant.lowCpuLoad = Long.valueOf(target.getString(TAG_LOW_CPU_LOAD));
                constant.highCpuLoad = Long.valueOf(target.getString(TAG_HIGH_CPU_LOAD));
                constant.emergencyMemory = Long.valueOf(target.getString(TAG_EMER_MEM));
                constant.bigMemCriticalMemory = Long.valueOf(target.getString(TAG_BIG_CRITIAL_MEM));
                constant.defaultCriticalMemory = Long.valueOf(target.getString(TAG_DEF_CRITIAL_MEM));
                constant.normalMemory = Long.valueOf(target.getString(TAG_NORMAL_MEM));
                String[] fsMemory = target.getString(TAG_FS_MEM).split(" ");
                String[] fsIdleTime = target.getString(TAG_FS_IDLE).split(" ");
                String swap = target.getString(TAG_SWAP_USAGE);
                String[] swapUsage = swap != null ? swap.split(" ") : new String[10];
                if (fsMemory != null && fsIdleTime != null
                        && fsMemory.length == fsIdleTime.length) {
                    for (int i = 0; i < fsMemory.length; i++) {
                        ForceStopParams params = new ForceStopParams();
                        params.fsMemory = Long.valueOf(fsMemory[i]);
                        params.fsIdleTime = Long.valueOf(fsIdleTime[i]) * MINUTE_MILLIS;
                        params.swapUsage = -1;
                        if (swapUsage != null && swapUsage.length == fsMemory.length) {
                            params.swapUsage = Integer.valueOf(swapUsage[i]);
                        }
                        constant.mFsParams.add(params);
                    }
                    Collections.sort(constant.mFsParams, new Comparator<ForceStopParams>() {
                        @Override
                        public int compare(ForceStopParams a, ForceStopParams b) {
                            if (a.fsMemory == b.fsMemory) {
                                return 0;
                            }
                            return a.fsMemory <= b.fsMemory ? -1 : 1;
                        }
                    });
                }
                constant.backgroundRunningLimit = Integer.valueOf(target.getString(TAG_BG_PKG_LIMIT));
                constant.mConfig = target;
                return constant;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return constant;
    }

    public void dump() {
        if (this.mConfig == null) {
            return;
        }
        HashMap<String, String> all = this.mConfig.getAllString();
        if (all == null) {
            return;
        }
        for (String key : all.keySet()) {
            Log.d(TAG, "--->" + key + "  : " + all.get(key));
        }
    }

    public void dump(PrintWriter writer) {
        writer.println("Dump MemoryConstant:");
        writer.println("----------------------------");
        if (this.mConfig != null) {
            HashMap<String, String> all = this.mConfig.getAllString();
            if (all != null) {
                for (String key : all.keySet()) {
                    writer.println("--->" + key + "  : " + all.get(key));
                }
            }
        }
        writer.println("----------------------------");
    }

    public int getBackgroundPkgRunningLimit() {
        return this.backgroundRunningLimit;
    }

    public long getBigMemCriticalMemory() {
        return this.bigMemCriticalMemory;
    }

    public long getCurrentForceStopIdleTime(int swapUsage) {
        if (this.mFsParams.size() == 0
                || this.mFsParams.get(this.mFsParams.size() - 1).swapUsage == -1
                || this.mFsParams.get(this.mFsParams.size() - 1).swapUsage > swapUsage) {
            return -1L;
        }
        for (ForceStopParams params : this.mFsParams) {
            if (params.swapUsage <= swapUsage) {
                return params.fsIdleTime;
            }
        }
        return -1L;
    }

    public long getCurrentForceStopIdleTime(long memory) {
        if (this.mFsParams.size() == 0
                || this.mFsParams.get(this.mFsParams.size() - 1).fsMemory < memory) {
            return -1L;
        }
        for (ForceStopParams params : this.mFsParams) {
            if (params.fsMemory > memory) {
                return params.fsIdleTime;
            }
        }
        return -1L;
    }

    public long getDefaultCriticalMemory() {
        return this.defaultCriticalMemory;
    }

    public long getEmergencyMemory() {
        return this.emergencyMemory;
    }

    public long getHighCpuLoad() {
        return this.highCpuLoad;
    }

    public long getLowCpuLoad() {
        return this.lowCpuLoad;
    }

    public long getNormalMemory() {
        return this.normalMemory;
    }
}