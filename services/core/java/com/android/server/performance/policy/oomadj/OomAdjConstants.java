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

import android.util.Slog;

import com.android.server.performance.PolicyConfig;

import java.io.PrintWriter;

/**
 * OOM-adj tuning constants loaded from the performance configuration.
 *
 * <p><b>Condemnation.</b> These thresholds demote the OOM adj of "cached",
 * "recent" and "section-habit" applications, letting the vendor preferentially
 * retain or kill applications based on its own usage profile. Reproduced only
 * for compatibility.
 *
 * @hide
 */
public class OomAdjConstants {
    public static final String TAG_NAME = "OomAdjConstants";
    public static final String CONFIG_TYPE_1G = "OomAdjConstans1G";
    public static final String CONFIG_TYPE_512M = "OomAdjConstans512M";

    public static final String ITEM_COMMON_CONFIGS = "commonConfig";
    public static final String ITEM_MEMPRESSURE = "memPressure";

    public static final int MEMPRESSURE_NORMAL = 0;
    public static final int MEMPRESSURE_MORDERATE = 1;
    public static final int MEMPRESSURE_LOW = 2;
    public static final int MEMPRESSURE_CRITICAL = 3;
    public static final int MEMPRESSURE_COUNT = 4;

    public static final String TAG_MEMPRESSURE_NORMAL = "normal";
    public static final String TAG_MEMPRESSURE_MORDERATE = "moderate";
    public static final String TAG_MEMPRESSURE_LOW = "low";
    public static final String TAG_MEMPRESSURE_CRITICAL = "critical";

    public static final String TAG_VISIBLE_DROP_TO_ADJ = "visible_adj";
    public static final String TAG_PERCEPTIBLE_DROP_TO_ADJ = "perceptible_adj";
    public static final String TAG_SERVICE_DROP_TO_ADJ = "service_adj";
    public static final String TAG_SERVICEB_DROP_TO_ADJ = "serviceb_adj";
    public static final String TAG_APP_IDLETIME_LIMIT = "idletime";
    public static final String TAG_RECENTAPP_PROTECTED_TIME = "recentprotect";
    public static final String TAG_NOTIFICATION_PROTECTED_TIME = "notificationtime";
    public static final String TAG_HOTAPP_LAUNCHCOUNT = "hotapp_launchcount";
    public static final String TAG_HOTAPP_ADJ = "hotapp_adj";
    public static final String TAG_HOTAPP_COUNT = "hotapp_count";
    public static final String TAG_RECENTAPP_ADJ = "recentapp_adj";
    public static final String TAG_SECTIONHABITAPP_COUNT = "sectionhabitapp_count";
    public static final String TAG_SECTIONHABITAPP_LAUNCHCOUNT = "sectionhabitapp_launchcount";
    public static final String TAG_SECTIONHABIT_ADJ = "sectionhabit_adj";

    public static final String OOM_ADJ_VISIBLE = "visible";
    public static final String OOM_ADJ_PERCEPTIBLE = "perceptible";
    public static final String OOM_ADJ_SERVICE = "service";
    public static final String OOM_ADJ_SERVICEB = "serviceb";
    public static final String OOM_ADJ_CACHED = "cached";

    public static final String TAG_SERVICE_COUNT = "service_count";
    public static final String TAG_SERVICE_PSS = "service_pss";
    public static final String TAG_SERVICEB_COUNT = "service_count";
    public static final String TAG_SERVICEB_PSS = "serviceb_pss";
    public static final String TAG_PERCEPTIBLE_COUNT = "perceptible_count";
    public static final String TAG_PERCEPTIBLE_PSS = "perceptible_pss";

    private static final String TAG = "xtcperformance-OomAdj";

    private static final int ADJ_VISIBLE = 100;
    private static final int ADJ_PERCEPTIBLE = 200;
    private static final int ADJ_SERVICE = 500;
    private static final int ADJ_SERVICEB = 800;
    private static final int ADJ_CACHED = 900;

    private final MemPressureTunning[] mTunnings = new MemPressureTunning[MEMPRESSURE_COUNT];
    private int mHotAppAdj;
    private int mHotAppCount;
    private int mHotAppMinLaunchCount;
    private int mRecentAppAdj;
    private int mSectionHabitAdj;
    private int mSectionHabitAppCount;
    private int mSectionHabitAppLaunchCount;

    public static class MemPressureTunning {
        private long mAppIdleTimeLimit;
        private final String mMemPressure;
        private long mNotifcationProtectedTimeLimit;
        private int mPerceptibleTunnedAdj;
        private long mRecentAppProtectedTimeLimit;
        private int mServiceBTunnedAdj;
        private int mServiceTunnedAdj;
        private int mVisibleTunnedAdj;

        public MemPressureTunning(PolicyConfig.ConfigItem item, String memPressure) {
            this.mVisibleTunnedAdj = StringToOomAdj(item.getString(TAG_VISIBLE_DROP_TO_ADJ));
            this.mPerceptibleTunnedAdj = StringToOomAdj(item.getString(TAG_PERCEPTIBLE_DROP_TO_ADJ));
            this.mServiceTunnedAdj = StringToOomAdj(item.getString(TAG_SERVICE_DROP_TO_ADJ));
            this.mServiceBTunnedAdj = StringToOomAdj(item.getString(TAG_SERVICEB_DROP_TO_ADJ));
            this.mAppIdleTimeLimit = minToms(item.getString(TAG_APP_IDLETIME_LIMIT));
            this.mRecentAppProtectedTimeLimit = minToms(
                    item.getString(TAG_RECENTAPP_PROTECTED_TIME));
            this.mNotifcationProtectedTimeLimit = minToms(
                    item.getString(TAG_NOTIFICATION_PROTECTED_TIME));
            this.mMemPressure = memPressure;
        }

        public void dump() {
            Slog.d(TAG, "Dump MemPressureTunning: " + this.mMemPressure);
            Slog.d(TAG, "---------------------------");
            Slog.d(TAG, "mVisibleTunnedAdj: " + this.mVisibleTunnedAdj);
            Slog.d(TAG, "mPerceptibleTunnedAdj: " + this.mPerceptibleTunnedAdj);
            Slog.d(TAG, "mServiceTunnedAdj: " + this.mServiceTunnedAdj);
            Slog.d(TAG, "mServiceBTunnedAdj: " + this.mServiceBTunnedAdj);
            Slog.d(TAG, "mAppIdleTimeLimit: " + this.mAppIdleTimeLimit);
            Slog.d(TAG, "mRecentAppProtectedTimeLimit: " + this.mRecentAppProtectedTimeLimit);
            Slog.d(TAG, "mNotifcationProtectedTimeLimit: "
                    + this.mNotifcationProtectedTimeLimit);
            Slog.d(TAG, "---------------------------");
        }

        public void dump(PrintWriter writer) {
            writer.println("Dump MemPressureTunning: " + this.mMemPressure);
            writer.println("---------------------------");
            writer.println("mVisibleTunnedAdj: " + this.mVisibleTunnedAdj);
            writer.println("mPerceptibleTunnedAdj: " + this.mPerceptibleTunnedAdj);
            writer.println("mServiceTunnedAdj: " + this.mServiceTunnedAdj);
            writer.println("mServiceBTunnedAdj: " + this.mServiceBTunnedAdj);
            writer.println("mAppIdleTimeLimit: " + this.mAppIdleTimeLimit);
            writer.println("mRecentAppProtectedTimeLimit: " + this.mRecentAppProtectedTimeLimit);
            writer.println("mNotifcationProtectedTimeLimit: "
                    + this.mNotifcationProtectedTimeLimit);
            writer.println("---------------------------");
        }

        public long getAppIdleTimeLimit() {
            return this.mAppIdleTimeLimit;
        }

        public long getNotifcationProtectedTimeLimit() {
            return this.mNotifcationProtectedTimeLimit;
        }

        public int getPerceptibleTunnedAdj() {
            return this.mPerceptibleTunnedAdj;
        }

        public long getRecentAppProtectedTimeLimit() {
            return this.mRecentAppProtectedTimeLimit;
        }

        public int getServiceBTunnedAdj() {
            return this.mServiceBTunnedAdj;
        }

        public int getServiceTunnedAdj() {
            return this.mServiceTunnedAdj;
        }

        public int getVisibleTunnedAdj() {
            return this.mVisibleTunnedAdj;
        }

        public int tunningAdj(int adj) {
            switch (adj) {
                case ADJ_VISIBLE:
                    return getVisibleTunnedAdj();
                case ADJ_PERCEPTIBLE:
                    return getPerceptibleTunnedAdj();
                case ADJ_SERVICE:
                    return getServiceTunnedAdj();
                case ADJ_SERVICEB:
                    return getServiceBTunnedAdj();
                default:
                    return adj;
            }
        }
    }

    private static int StringToOomAdj(String value) {
        if (value.equals(OOM_ADJ_VISIBLE)) {
            return ADJ_VISIBLE;
        }
        if (value.equals(OOM_ADJ_PERCEPTIBLE)) {
            return ADJ_PERCEPTIBLE;
        }
        if (value.equals(OOM_ADJ_SERVICE)) {
            return ADJ_SERVICE;
        }
        if (value.equals(OOM_ADJ_SERVICEB)) {
            return ADJ_SERVICEB;
        }
        return value.equals(OOM_ADJ_CACHED) ? ADJ_CACHED : -1;
    }

    public static OomAdjConstants loadFromConfig(PolicyConfig policyConfig) {
        PolicyConfig.ConfigItem common = policyConfig.getConfigItem(ITEM_COMMON_CONFIGS);
        PolicyConfig.ConfigItem normal = policyConfig.getConfigItem(TAG_MEMPRESSURE_NORMAL);
        PolicyConfig.ConfigItem moderate = policyConfig.getConfigItem(TAG_MEMPRESSURE_MORDERATE);
        PolicyConfig.ConfigItem low = policyConfig.getConfigItem(TAG_MEMPRESSURE_LOW);
        PolicyConfig.ConfigItem critical = policyConfig.getConfigItem(TAG_MEMPRESSURE_CRITICAL);
        if (common == null || normal == null || moderate == null || low == null
                || critical == null) {
            return null;
        }
        OomAdjConstants constants = new OomAdjConstants();
        constants.mTunnings[MEMPRESSURE_NORMAL] =
                new MemPressureTunning(normal, TAG_MEMPRESSURE_NORMAL);
        constants.mTunnings[MEMPRESSURE_MORDERATE] =
                new MemPressureTunning(moderate, TAG_MEMPRESSURE_MORDERATE);
        constants.mTunnings[MEMPRESSURE_LOW] =
                new MemPressureTunning(low, TAG_MEMPRESSURE_LOW);
        constants.mTunnings[MEMPRESSURE_CRITICAL] =
                new MemPressureTunning(critical, TAG_MEMPRESSURE_CRITICAL);
        constants.mHotAppMinLaunchCount =
                Integer.valueOf(common.getString(TAG_HOTAPP_LAUNCHCOUNT));
        constants.mHotAppAdj = StringToOomAdj(common.getString(TAG_HOTAPP_ADJ));
        constants.mHotAppCount = Integer.valueOf(common.getString(TAG_HOTAPP_COUNT));
        constants.mRecentAppAdj = StringToOomAdj(common.getString(TAG_RECENTAPP_ADJ));
        constants.mSectionHabitAppCount =
                Integer.valueOf(common.getString(TAG_SECTIONHABITAPP_COUNT));
        constants.mSectionHabitAppLaunchCount =
                Integer.valueOf(common.getString(TAG_SECTIONHABITAPP_LAUNCHCOUNT));
        constants.mSectionHabitAdj = StringToOomAdj(common.getString(TAG_SECTIONHABIT_ADJ));
        return constants;
    }

    private static long minToms(String value) {
        return Long.valueOf(value) * 60 * 1000;
    }

    public void dump() {
        Slog.d(TAG, "Dump OomAdjConstants:");
        Slog.d(TAG, "---------------------------");
        Slog.d(TAG, "mHotAppMinLaunchCount: " + this.mHotAppMinLaunchCount);
        Slog.d(TAG, "mHotAppAdj: " + this.mHotAppAdj);
        Slog.d(TAG, "mHotAppCount: " + this.mHotAppCount);
        Slog.d(TAG, "mRecentAppAdj: " + this.mRecentAppAdj);
        Slog.d(TAG, "mSectionHabitAdj: " + this.mSectionHabitAdj);
        Slog.d(TAG, "mSectionHabitAppCount: " + this.mSectionHabitAppCount);
        Slog.d(TAG, "mSectionHabitAppLaunchCount: " + this.mSectionHabitAppLaunchCount);
        for (MemPressureTunning tunning : this.mTunnings) {
            tunning.dump();
        }
        Slog.d(TAG, "---------------------------");
    }

    public void dump(PrintWriter writer) {
        writer.println("Dump OomAdjConstants:");
        writer.println("---------------------------");
        writer.println("mHotAppMinLaunchCount: " + this.mHotAppMinLaunchCount);
        writer.println("mHotAppAdj: " + this.mHotAppAdj);
        writer.println("mHotAppCount: " + this.mHotAppCount);
        writer.println("mRecentAppAdj: " + this.mRecentAppAdj);
        writer.println("mSectionHabitAdj: " + this.mSectionHabitAdj);
        writer.println("mSectionHabitAppCount: " + this.mSectionHabitAppCount);
        writer.println("mSectionHabitAppLaunchCount: " + this.mSectionHabitAppLaunchCount);
        for (MemPressureTunning tunning : this.mTunnings) {
            tunning.dump(writer);
        }
        writer.println("---------------------------");
    }

    public int getAdjTunningValue(int adj, int memPressure) {
        return this.mTunnings[memPressure].tunningAdj(adj);
    }

    public long getAppIdleTimeLimit(int memPressure) {
        return this.mTunnings[memPressure].getAppIdleTimeLimit();
    }

    public int getHotAppAdj() {
        return this.mHotAppAdj;
    }

    public int getHotAppCount() {
        return this.mHotAppCount;
    }

    public int getHotAppMinLaunchCount() {
        return this.mHotAppMinLaunchCount;
    }

    public long getNotifcationProtectedTimeLimit(int memPressure) {
        return this.mTunnings[memPressure].getNotifcationProtectedTimeLimit();
    }

    public int getRecentAppAdj() {
        return this.mRecentAppAdj;
    }

    public long getRecentAppProtectedTimeLimit(int memPressure) {
        return this.mTunnings[memPressure].getRecentAppProtectedTimeLimit();
    }

    public int getSectionHabitAdj() {
        return this.mSectionHabitAdj;
    }

    public int getSectionHabitAppCount() {
        return this.mSectionHabitAppCount;
    }

    public int getSectionHabitAppLaunchCount() {
        return this.mSectionHabitAppLaunchCount;
    }
}