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

import android.content.Context;
import android.util.Slog;

import com.android.server.SystemService;

/**
 * System service hosting the XTC performance manager.
 *
 * @hide
 */
public class PerformanceManagerService extends SystemService {
    static final String TAG = "PerformanceManagerService";

    private final PerformanceManagerServiceImpl mImpl;

    public PerformanceManagerService(Context context) {
        super(context);
        this.mImpl = new PerformanceManagerServiceImpl(context);
    }

    @Override
    public void onBootPhase(int phase) {
        if (phase == 1000) {
            Slog.d(TAG, "onBootPhase BOOT_COMPLETED -> loadInit ");
            this.mImpl.loadInit();
        }
    }

    @Override
    public void onStart() {
        Slog.d(TAG, "onStart -> publishBinderService");
        publishBinderService("xtcperformance", this.mImpl);
    }
}