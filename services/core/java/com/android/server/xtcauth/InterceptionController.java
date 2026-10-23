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

package com.android.server.xtcauth;

import android.content.Context;
import android.util.Log;

import java.util.HashSet;

/**
 * Generic interception controller base.
 *
 * @hide
 */
class InterceptionController extends ControllerBase {
    public static final String NAME = "InterceptionController";
    private static final String TAG = "MoMS.InterceptionController";
    private static final boolean DBG = true;

    private static ControllerBase mInstance = null;

    private final HashSet<String> mBlockedPackages = new HashSet<>();
    private boolean mSystemReady = false;

    InterceptionController() {
    }

    public static final ControllerBase getInstance() {
        if (mInstance == null) {
            mInstance = new InterceptionController();
            System.out.printf("Create MobileManagerController: InterceptionController",
                    new Object[0]);
        }
        return mInstance;
    }

    @Override
    public void enableController(boolean enable) {
        super.enableController(enable);
        Log.d(TAG, "enableController() enable: " + enable);
        if (getManagerApListener() == null && enable) {
            throw new IllegalArgumentException(
                    "Enable interception controller without register listener!");
        }
    }

    @Override
    public void systemReady(Context context) {
        mSystemReady = true;
    }
}