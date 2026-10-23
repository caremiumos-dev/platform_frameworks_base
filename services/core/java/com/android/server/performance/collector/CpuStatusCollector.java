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

package com.android.server.performance.collector;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.SystemClock;
import android.util.Slog;

import com.android.internal.os.ProcessCpuTracker;
import com.android.server.performance.PerformanceManagerDebugConfig;
import com.android.server.performance.PerformanceManagerServiceImpl;
import com.android.server.performance.status.CpuStatus;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Periodically samples {@link ProcessCpuTracker} and pushes {@link CpuStatus}
 * updates to the performance policies.
 *
 * @hide
 */
public class CpuStatusCollector {
    public static final long MONITOR_CPU_MIN_TIME = 5000L;

    private static final String TAG = "xtcperformance-CpuState";

    private static final int MSG_UPDATE_CPU_USAGE = 0;
    private static final int MSG_INIT = 1;

    private static final long MONITOR_CPU_AVG_TIME = 30000L;
    private static final float CPU_LOAD_MAY_HIGH = 50.0f;

    private final Handler mHandler;
    private final PerformanceManagerServiceImpl mService;
    private final ProcessCpuTracker mProcessCpuTracker = new ProcessCpuTracker(false);
    private final HandlerThread mThread = new HandlerThread("performance-cpu", 10);
    private final AtomicLong mLastCpuTime = new AtomicLong(0);
    private final CpuStatus cpuStatus = new CpuStatus();

    private boolean mLastStatusGood;
    private int mRelIdleTime;
    private int mRelIoWaitTime;
    private int mRelIrqTime;
    private int mRelSoftIrqTime;
    private int mRelSystemTime;
    private int mRelUserTime;
    private boolean MONITOR_ENABLED = true;
    private float mCurrentCpuLoading = 0.0f;

    public CpuStatusCollector(PerformanceManagerServiceImpl service) {
        this.mService = service;
        this.mThread.start();
        this.mHandler = new Handler(this.mThread.getLooper()) {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MSG_UPDATE_CPU_USAGE:
                        handleUpdateCpuUsage();
                        break;
                    case MSG_INIT:
                        mProcessCpuTracker.init();
                        scheduleUpdateCpuUsage(MONITOR_CPU_AVG_TIME);
                        break;
                    default:
                        break;
                }
            }
        };
        this.mHandler.sendEmptyMessage(MSG_INIT);
    }

    private void handleUpdateCpuUsage() {
        long start = SystemClock.uptimeMillis();
        if (!this.MONITOR_ENABLED
                || start - this.mLastCpuTime.get() < MONITOR_CPU_MIN_TIME) {
            return;
        }
        synchronized (this.mProcessCpuTracker) {
            this.mLastCpuTime.set(start);
            this.mProcessCpuTracker.update();
            if (this.mProcessCpuTracker.hasGoodLastStats()) {
                this.mLastStatusGood = true;
                this.mCurrentCpuLoading = this.mProcessCpuTracker.getTotalCpuPercent();
                this.mRelUserTime = this.mProcessCpuTracker.getLastUserTime();
                this.mRelSystemTime = this.mProcessCpuTracker.getLastSystemTime();
                this.mRelIoWaitTime = this.mProcessCpuTracker.getLastIoWaitTime();
                this.mRelIrqTime = this.mProcessCpuTracker.getLastIrqTime();
                this.mRelSoftIrqTime = this.mProcessCpuTracker.getLastSoftIrqTime();
                this.mRelIdleTime = this.mProcessCpuTracker.getLastIdleTime();
                if (this.mCurrentCpuLoading >= CPU_LOAD_MAY_HIGH) {
                    scheduleUpdateCpuUsage(MONITOR_CPU_MIN_TIME);
                } else {
                    scheduleUpdateCpuUsage(MONITOR_CPU_AVG_TIME);
                }
                this.cpuStatus.updateCpuInfo(this);
                if (PerformanceManagerDebugConfig.DEBUG_CPUSTAT) {
                    Slog.d(TAG, "cpuStatus=[" + this.cpuStatus + "], cost :"
                            + (SystemClock.uptimeMillis() - start) + "ms");
                }
                this.mService.notifyPolicyExecutorsCpuStatusChanged(this.cpuStatus);
            }
        }
    }

    public void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        writer.println("CPUSTATUS:");
        writer.println("TOTAL :" + getTotalCpuPercent() + "% USED");
        writer.println("----------------------------");
    }

    public final int getLastIdleTime() {
        return this.mRelIdleTime;
    }

    public final int getLastIoWaitTime() {
        return this.mRelIoWaitTime;
    }

    public final int getLastIrqTime() {
        return this.mRelIrqTime;
    }

    public final int getLastSoftIrqTime() {
        return this.mRelSoftIrqTime;
    }

    public final int getLastSystemTime() {
        return this.mRelSystemTime;
    }

    public final int getLastUserTime() {
        return this.mRelUserTime;
    }

    public float getTotalCpuPercent() {
        return this.mCurrentCpuLoading;
    }

    public boolean hasGoodLastStats() {
        return this.mLastStatusGood;
    }

    public void scheduleUpdateCpuUsage(long delay) {
        if (this.MONITOR_ENABLED) {
            this.mHandler.removeMessages(MSG_UPDATE_CPU_USAGE);
            this.mHandler.sendEmptyMessageDelayed(MSG_UPDATE_CPU_USAGE, delay);
        }
    }

    public void updateCpuUsageNow() {
        scheduleUpdateCpuUsage(0L);
    }
}