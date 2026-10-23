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

package com.android.internal.os;

import android.os.BatteryStats;

/**
 * Computes the video subsystem power usage for an application.
 *
 * @hide
 */
public class VideoPowerCalculator extends PowerCalculator {
    private final double mVideoPowerOnAvg;

    public VideoPowerCalculator(PowerProfile powerProfile) {
        mVideoPowerOnAvg = powerProfile.getAveragePower(PowerProfile.POWER_VIDEO);
    }

    @Override
    public void calculateApp(BatterySipper app, BatteryStats.Uid u, long rawRealtimeUs,
            long rawUptimeUs, int statsType) {
        BatteryStats.Timer videoTurnedOnTimer = u.getVideoTurnedOnTimer();
        if (videoTurnedOnTimer == null) {
            app.videoTimeMs = 0L;
            app.videoPowerMah = 0.0d;
        } else {
            long totalTimeMs = videoTurnedOnTimer.getTotalTimeLocked(rawRealtimeUs, statsType)
                    / 1000;
            app.videoTimeMs = totalTimeMs;
            app.videoPowerMah = (totalTimeMs * mVideoPowerOnAvg) / (1000 * 60 * 60);
        }
    }
}