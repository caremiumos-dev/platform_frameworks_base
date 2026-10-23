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

package com.xtc.auth;

import android.content.Intent;

import java.util.ArrayList;
import java.util.List;

/**
 * Policy describing which boot intents the XTC security manager considers
 * as boot completion signals.
 *
 * @hide
 */
public final class BootReceiverPolicy {
    private static final List<String> mBootIntentFilter = new ArrayList<>();

    static {
        mBootIntentFilter.add(Intent.ACTION_BOOT_COMPLETED);
        mBootIntentFilter.add("android.intent.action.ACTION_BOOT_IPO");
    }

    public static List<String> getBootPolicy() {
        return mBootIntentFilter;
    }

    public static boolean match(String action) {
        return mBootIntentFilter.contains(action);
    }
}