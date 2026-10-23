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

package com.android.server.xtcbehavior;

import android.content.Context;

import com.android.server.SystemService;

/**
 * System service hosting {@link XtcBehaviorServiceImpl}. See that class for the
 * condemnation of the telemetry it performs.
 *
 * @hide
 */
public class XtcBehaviorService extends SystemService {
    private static final boolean DEBUG = false;
    private static final String TAG = "XtcBehaviorService";

    private final XtcBehaviorServiceImpl mImpl;

    public XtcBehaviorService(Context context) {
        super(context);
        this.mImpl = new XtcBehaviorServiceImpl(context);
    }

    private void logd(String message) {
    }

    @Override
    public void onBootPhase(int phase) {
        logd("XtcBehaviorService phase: " + phase);
        if (phase == 1000) {
            this.mImpl.listenNativeMsg();
            this.mImpl.loadInit();
            this.mImpl.registerReceiver();
        }
    }

    @Override
    public void onStart() {
        logd("XtcBehaviorService onStart ");
        publishBinderService("xtcbehavior", this.mImpl);
    }
}