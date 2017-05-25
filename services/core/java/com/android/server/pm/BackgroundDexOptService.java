/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.server.pm;

import static com.android.server.pm.InstructionSets.getAppDexInstructionSets;
import static com.android.server.pm.InstructionSets.getDexCodeInstructionSets;
import static com.android.server.pm.PackageManagerService.DEBUG_DEXOPT;
import static com.android.server.pm.PackageManagerService.REASON_INSTALL;
import static com.android.server.pm.PackageManagerServiceCompilerMapping.getCompilerFilterForReason;
import static dalvik.system.DexFile.NO_DEXOPT_NEEDED;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageParser.Package;
import android.os.BatteryManager;
import android.os.Environment;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.storage.StorageManager;
import android.util.ArraySet;
import android.util.Log;

import com.android.server.pm.dex.DexManager;

import com.android.server.pm.dex.PackageDexUsage;
import dalvik.system.DexFile;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.TimeUnit;

/**
 * {@hide}
 */
public class BackgroundDexOptService extends JobService {
    private static final String TAG = "BackgroundDexOptService";

    private static final boolean DEBUG = false;

    private static final int JOB_IDLE_OPTIMIZE = 800;
    private static final int JOB_POST_BOOT_UPDATE = 801;

    private static final long IDLE_OPTIMIZATION_PERIOD = DEBUG
            ? TimeUnit.MINUTES.toMillis(1)
            : TimeUnit.DAYS.toMillis(1);

    private static ComponentName sDexoptServiceName = new ComponentName(
            "android",
            BackgroundDexOptService.class.getName());

    // Possible return codes of individual optimization steps.

    // Optimizations finished. All packages were processed.
    private static final int OPTIMIZE_PROCESSED = 0;
    // Optimizations should continue. Issued after checking the scheduler, disk space or battery.
    private static final int OPTIMIZE_CONTINUE = 1;
    // Optimizations should be aborted. Job scheduler requested it.
    private static final int OPTIMIZE_ABORT_BY_JOB_SCHEDULER = 2;
    // Optimizations should be aborted. No space left on device.
    private static final int OPTIMIZE_ABORT_NO_SPACE_LEFT = 3;
    // System property to hold threshold required for filtering unused apps.
    private static final String UNOPTIMIZE_UNUSED_APPS_THRSHOLD_SYS_PROP =
        "pm.dexopt.unopt_after_inactive_days";
    // Threshold for unoptimize unused apps.
    private static final long UNOPTIMIZE_UNUSED_APPS_THRESHOLD_IN_MILLIS =
        getUnoptimizeUnusedAppsThresholdInMillis();

    /**
     * Set of failed packages remembered across job runs.
     */
    static final ArraySet<String> sFailedPackageNamesPrimary = new ArraySet<String>();
    static final ArraySet<String> sFailedPackageNamesSecondary = new ArraySet<String>();

    /**
     * Atomics set to true if the JobScheduler requests an abort.
     */
    private final AtomicBoolean mAbortPostBootUpdate = new AtomicBoolean(false);
    private final AtomicBoolean mAbortIdleOptimization = new AtomicBoolean(false);

    /**
     * Atomic set to true if one job should exit early because another job was started.
     */
    private final AtomicBoolean mExitPostBootUpdate = new AtomicBoolean(false);

    private final File mDataDir = Environment.getDataDirectory();

    public static void schedule(Context context) {
        JobScheduler js = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

        // Schedule a one-off job which scans installed packages and updates
        // out-of-date oat files.
        js.schedule(new JobInfo.Builder(JOB_POST_BOOT_UPDATE, sDexoptServiceName)
                    .setMinimumLatency(TimeUnit.MINUTES.toMillis(1))
                    .setOverrideDeadline(TimeUnit.MINUTES.toMillis(1))
                    .build());

        // Schedule a daily job which scans installed packages and compiles
        // those with fresh profiling data.
        js.schedule(new JobInfo.Builder(JOB_IDLE_OPTIMIZE, sDexoptServiceName)
                    .setRequiresDeviceIdle(true)
                    .setRequiresCharging(true)
                    .setPeriodic(IDLE_OPTIMIZATION_PERIOD)
                    .build());

        if (DEBUG_DEXOPT) {
            Log.i(TAG, "Jobs scheduled");
        }
    }

    public static void notifyPackageChanged(String packageName) {
        // The idle maintanance job skips packages which previously failed to
        // compile. The given package has changed and may successfully compile
        // now. Remove it from the list of known failing packages.
        synchronized (sFailedPackageNamesPrimary) {
            sFailedPackageNamesPrimary.remove(packageName);
        }
        synchronized (sFailedPackageNamesSecondary) {
            sFailedPackageNamesSecondary.remove(packageName);
        }
    }

    // Returns the current battery level as a 0-100 integer.
    private int getBatteryLevel() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent intent = registerReceiver(null, filter);
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        if (level < 0 || scale <= 0) {
            // Battery data unavailable. This should never happen, so assume the worst.
            return 0;
        }

        return (100 * level / scale);
    }

    private long getLowStorageThreshold(Context context) {
        @SuppressWarnings("deprecation")
        final long lowThreshold = StorageManager.from(context).getStorageLowBytes(mDataDir);
        if (lowThreshold == 0) {
            Log.e(TAG, "Invalid low storage threshold");
        }

        return lowThreshold;
    }

    private boolean runPostBootUpdate(final JobParameters jobParams,
            final PackageManagerService pm, final ArraySet<String> pkgs) {
        if (mExitPostBootUpdate.get()) {
            // This job has already been superseded. Do not start it.
            return false;
        }
        new Thread("BackgroundDexOptService_PostBootUpdate") {
            @Override
            public void run() {
                postBootUpdate(jobParams, pm, pkgs);
            }

        }.start();
        return true;
    }

    private void postBootUpdate(JobParameters jobParams, PackageManagerService pm,
            ArraySet<String> pkgs) {
        // Load low battery threshold from the system config. This is a 0-100 integer.
        final int lowBatteryThreshold = getResources().getInteger(
                com.android.internal.R.integer.config_lowBatteryWarningLevel);
        final long lowThreshold = getLowStorageThreshold(this);

        mAbortPostBootUpdate.set(false);

        for (String pkg : pkgs) {
            if (mAbortPostBootUpdate.get()) {
                // JobScheduler requested an early abort.
                return;
            }
            if (mExitPostBootUpdate.get()) {
                // Different job, which supersedes this one, is running.
                break;
            }
            if (getBatteryLevel() < lowBatteryThreshold) {
                // Rather bail than completely drain the battery.
                break;
            }
            long usableSpace = mDataDir.getUsableSpace();
            if (usableSpace < lowThreshold) {
                // Rather bail than completely fill up the disk.
                Log.w(TAG, "Aborting background dex opt job due to low storage: " +
                        usableSpace);
                break;
            }

            if (DEBUG_DEXOPT) {
                Log.i(TAG, "Updating package " + pkg);
            }

            // Update package if needed. Note that there can be no race between concurrent
            // jobs because PackageDexOptimizer.performDexOpt is synchronized.

            // checkProfiles is false to avoid merging profiles during boot which
            // might interfere with background compilation (b/28612421).
            // Unfortunately this will also means that "pm.dexopt.boot=speed-profile" will
            // behave differently than "pm.dexopt.bg-dexopt=speed-profile" but that's a
            // trade-off worth doing to save boot time work.
            pm.performDexOpt(pkg,
                    /* checkProfiles */ false,
                    PackageManagerService.REASON_BOOT,
                    /* force */ false);
        }
        // Ran to completion, so we abandon our timeslice and do not reschedule.
        jobFinished(jobParams, /* reschedule */ false);
    }

    private boolean runIdleOptimization(final JobParameters jobParams,
            final PackageManagerService pm, final ArraySet<String> pkgs) {
        new Thread("BackgroundDexOptService_IdleOptimization") {
            @Override
            public void run() {
                int result = idleOptimization(pm, pkgs, BackgroundDexOptService.this);
                if (result != OPTIMIZE_ABORT_BY_JOB_SCHEDULER) {
                    Log.w(TAG, "Idle optimizations aborted because of space constraints.");
                    // If we didn't abort we ran to completion (or stopped because of space).
                    // Abandon our timeslice and do not reschedule.
                    jobFinished(jobParams, /* reschedule */ false);
                }
            }
        }.start();
        return true;
    }

    // Optimize the given packages and return the optimization result (one of the OPTIMIZE_* codes).
    private int idleOptimization(PackageManagerService pm, ArraySet<String> pkgs, Context context) {
        Log.i(TAG, "Performing idle optimizations");
        // If post-boot update is still running, request that it exits early.
        mExitPostBootUpdate.set(true);
        mAbortIdleOptimization.set(false);

        long lowStorageThreshold = getLowStorageThreshold(context);
        // Optimize primary apks.
        int result = optimizePackages(pm, pkgs, lowStorageThreshold, /*is_for_primary_dex*/ true,
                sFailedPackageNamesPrimary);

        if (result == OPTIMIZE_ABORT_BY_JOB_SCHEDULER) {
            return result;
        }

        if (SystemProperties.getBoolean("dalvik.vm.dexopt.secondary", false)) {
            result = reconcileSecondaryDexFiles(pm.getDexManager());
            if (result == OPTIMIZE_ABORT_BY_JOB_SCHEDULER) {
                return result;
            }

            result = optimizePackages(pm, pkgs, lowStorageThreshold, /*is_for_primary_dex*/ false,
                    sFailedPackageNamesSecondary);
        }
        return result;
    }

    private int optimizePackages(PackageManagerService pm, ArraySet<String> pkgs,
            long lowStorageThreshold, boolean is_for_primary_dex,
            ArraySet<String> failedPackageNames) {
        Collection<Package> packages = pm.mPackages.values();
        Set<String> inactivePackagesUnOptRequired = new HashSet<>();
        Set<String> inactivePackagesNoUnOptRequired = new HashSet<>();

        // Prepare a list of packages which haven't been used for 30 days in foreground.
        long currentTimeInMillis = System.currentTimeMillis();
        PackageDexUsage packageDexUsage = new PackageDexUsage();
        for (Package pkg : packages) {
            // If the app was active in foreground during the threshold period.
            boolean isActiveInForeground = (currentTimeInMillis
                - pkg.getLatestForegroundPackageUseTimeInMills())
                > UNOPTIMIZE_UNUSED_APPS_THRESHOLD_IN_MILLIS;

            // If the app was active in background during the threshold period and was used
            // by other packages.
            boolean isActiveInBackgroundAndUsedByOtherPackages = ((currentTimeInMillis
                - pkg.getLatestPackageUseTimeInMills())
                > UNOPTIMIZE_UNUSED_APPS_THRESHOLD_IN_MILLIS)
                && packageDexUsage.getPackageUseInfo(pkg.packageName).isUsedByOtherApps();

            boolean isActive = isActiveInBackgroundAndUsedByOtherPackages || isActiveInForeground;
            if (!isActive) {
                boolean unoptRequired = false;
                List<String> paths = pkg.getAllCodePathsExcludingResourceOnly();
                String[] instructionSets = getAppDexInstructionSets(pkg.applicationInfo);
                String[] dexCodeInstructionSets = getDexCodeInstructionSets(instructionSets);
                for (String instructionSet : dexCodeInstructionSets) {
                    if (unoptRequired) {
                        // If we are sure that unopt is required, we don't have to analyse
                        // other dex files.
                        break;
                    }
                    for (String path : paths) {
                        try {
                            int status = DexFile.getDexOptNeeded(path,
                                instructionSet,
                                getCompilerFilterForReason(REASON_INSTALL),
                                false,
                                /*strict*/ true);
                            if (status != NO_DEXOPT_NEEDED) {
                                unoptRequired = true;
                                break;
                            }
                        } catch (IOException ioe) {
                            unoptRequired = true;
                            Log.e(TAG,"[Exception]: " + ioe.getMessage());
                        }
                    }
                }
                if (unoptRequired) {
                    inactivePackagesUnOptRequired.add(pkg.packageName);
                } else {
                    inactivePackagesNoUnOptRequired.add(pkg.packageName);
                }
            }
        }

        for (String pkg : pkgs) {
            if (inactivePackagesNoUnOptRequired.contains(pkg)) {
                // Already unoptimized.
                continue;
            }

            int abort_code = abortIdleOptimizations(lowStorageThreshold);
            if (abort_code != OPTIMIZE_CONTINUE) {
                return abort_code;
            }

            synchronized (failedPackageNames) {
                if (failedPackageNames.contains(pkg)) {
                    // Skip previously failing package
                    continue;
                } else {
                    // Conservatively add package to the list of failing ones in case performDexOpt
                    // never returns.
                    failedPackageNames.add(pkg);
                }
            }

            int reason;
            boolean forceCompile;
            // Restore unused packages to their install state to save space.
            if (inactivePackagesUnOptRequired.contains(pkg)) {
                reason = PackageManagerService.REASON_INSTALL;
                forceCompile = true;
            } else {
                reason = PackageManagerService.REASON_BACKGROUND_DEXOPT;
                forceCompile = false;
            }

            // Optimize package if needed. Note that there can be no race between
            // concurrent jobs because PackageDexOptimizer.performDexOpt is synchronized.
            boolean success = is_for_primary_dex
                    ? pm.performDexOpt(pkg,
                            /* checkProfiles */ true,
                            reason,
                            forceCompile)
                    : pm.performDexOptSecondary(pkg,
                            reason,
                            forceCompile);
            if (success) {
                // Dexopt succeeded, remove package from the list of failing ones.
                synchronized (failedPackageNames) {
                    failedPackageNames.remove(pkg);
                }
            }
        }
        return OPTIMIZE_PROCESSED;
    }

    private int reconcileSecondaryDexFiles(DexManager dm) {
        // TODO(calin): should we blacklist packages for which we fail to reconcile?
        for (String p : dm.getAllPackagesWithSecondaryDexFiles()) {
            if (mAbortIdleOptimization.get()) {
                return OPTIMIZE_ABORT_BY_JOB_SCHEDULER;
            }
            dm.reconcileSecondaryDexFiles(p);
        }
        return OPTIMIZE_PROCESSED;
    }

    // Evaluate whether or not idle optimizations should continue.
    private int abortIdleOptimizations(long lowStorageThreshold) {
        if (mAbortIdleOptimization.get()) {
            // JobScheduler requested an early abort.
            return OPTIMIZE_ABORT_BY_JOB_SCHEDULER;
        }
        long usableSpace = mDataDir.getUsableSpace();
        if (usableSpace < lowStorageThreshold) {
            // Rather bail than completely fill up the disk.
            Log.w(TAG, "Aborting background dex opt job due to low storage: " + usableSpace);
            return OPTIMIZE_ABORT_NO_SPACE_LEFT;
        }

        return OPTIMIZE_CONTINUE;
    }

    /**
     * Execute the idle optimizations immediately.
     */
    public static boolean runIdleOptimizationsNow(PackageManagerService pm, Context context) {
        // Create a new object to make sure we don't interfere with the scheduled jobs.
        // Note that this may still run at the same time with the job scheduled by the
        // JobScheduler but the scheduler will not be able to cancel it.
        BackgroundDexOptService bdos = new BackgroundDexOptService();
        int result = bdos.idleOptimization(pm, pm.getOptimizablePackages(), context);
        return result == OPTIMIZE_PROCESSED;
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        if (DEBUG_DEXOPT) {
            Log.i(TAG, "onStartJob");
        }

        // NOTE: PackageManagerService.isStorageLow uses a different set of criteria from
        // the checks above. This check is not "live" - the value is determined by a background
        // restart with a period of ~1 minute.
        PackageManagerService pm = (PackageManagerService)ServiceManager.getService("package");
        if (pm.isStorageLow()) {
            if (DEBUG_DEXOPT) {
                Log.i(TAG, "Low storage, skipping this run");
            }
            return false;
        }

        final ArraySet<String> pkgs = pm.getOptimizablePackages();
        if (pkgs.isEmpty()) {
            if (DEBUG_DEXOPT) {
                Log.i(TAG, "No packages to optimize");
            }
            return false;
        }

        if (params.getJobId() == JOB_POST_BOOT_UPDATE) {
            return runPostBootUpdate(params, pm, pkgs);
        } else {
            return runIdleOptimization(params, pm, pkgs);
        }
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        if (DEBUG_DEXOPT) {
            Log.i(TAG, "onStopJob");
        }

        if (params.getJobId() == JOB_POST_BOOT_UPDATE) {
            mAbortPostBootUpdate.set(true);
        } else {
            mAbortIdleOptimization.set(true);
        }
        return false;
    }

    private static long getUnoptimizeUnusedAppsThresholdInMillis() {
        String sysPropValue = SystemProperties.get(UNOPTIMIZE_UNUSED_APPS_THRSHOLD_SYS_PROP);
        if (sysPropValue == null || sysPropValue.isEmpty()) {
            Log.e(TAG, "SysProp UNOPTIMIZE_UNUSED_APPS_THRSHOLD_SYS_PROP not set");
            return Long.MAX_VALUE;
        }
        try {
            return Long.valueOf(sysPropValue);
        } catch (NumberFormatException nfe) {
            Log.e(TAG, "Illegal value for UNOPTIMIZE_UNUSED_APPS_THRSHOLD_SYS_PROP: " +
                sysPropValue);
            return Long.MAX_VALUE;
        }
    }
}
