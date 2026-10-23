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

package com.android.server.performance.status;

import android.os.Process;

import com.android.server.performance.collector.CpuStatusCollector;

/**
 * Snapshot of CPU load/times used by the performance policies.
 *
 * @hide
 */
public class CpuStatus {
    private static final int[] LOAD_AVERAGE_FORMAT = {16416, 16416, 16416};

    private final float[] mLoadAverageData = new float[3];
    private float mCurrentLoadPercent;
    private float mLoad1;
    private float mLoad5;
    private float mLoad15 = 0.0f;
    private int mRelIdleTime;
    private int mRelIoWaitTime;
    private int mRelIrqTime;
    private int mRelSoftIrqTime;
    private int mRelSystemTime;
    private int mRelUserTime;

    private void updateCpuLoad() {
        try {
            if (Process.readProcFile("/proc/loadavg", LOAD_AVERAGE_FORMAT, null, null,
                    mLoadAverageData)) {
                float load1 = mLoadAverageData[0];
                float load5 = mLoadAverageData[1];
                float load15 = mLoadAverageData[2];
                if (load1 == this.mLoad1 && load5 == this.mLoad5 && load15 == this.mLoad15) {
                    return;
                }
                this.mLoad1 = load1;
                this.mLoad5 = load5;
                this.mLoad15 = load15;
            }
        } catch (Exception e) {
            // ignored
        }
    }

    public float getCpuLoadAvg15Min() {
        return this.mLoad15;
    }

    public float getCpuLoadAvg1Min() {
        return this.mLoad1;
    }

    public float getCpuLoadAvg5Min() {
        return this.mLoad5;
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
        return this.mCurrentLoadPercent;
    }

    @Override
    public String toString() {
        return "cpu_load:" + this.mCurrentLoadPercent + " ut:" + this.mRelUserTime
                + " st:" + this.mRelSystemTime + " iwt=" + this.mRelIoWaitTime
                + " irqt=" + this.mRelIrqTime + " sirqt=" + this.mRelSoftIrqTime
                + " idlet=" + this.mRelIdleTime + " Load1=" + this.mLoad1
                + " Load5=" + this.mLoad5 + " Load15=" + this.mLoad15;
    }

    public void updateCpuInfo(CpuStatusCollector collector) {
        updateCpuLoad();
        if (collector == null || !collector.hasGoodLastStats()) {
            return;
        }
        this.mCurrentLoadPercent = collector.getTotalCpuPercent();
        this.mRelUserTime = collector.getLastUserTime();
        this.mRelSystemTime = collector.getLastSystemTime();
        this.mRelIoWaitTime = collector.getLastIoWaitTime();
        this.mRelIrqTime = collector.getLastIrqTime();
        this.mRelSoftIrqTime = collector.getLastSoftIrqTime();
        this.mRelIdleTime = collector.getLastIdleTime();
    }
}