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

package com.android.server.performance.policy.prefork;

import com.android.server.performance.PolicyConfig;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Pre-fork policy constants loaded from the performance configuration.
 *
 * @hide
 */
public class PreforkConstants {
    public static final String TAG_NAME = "PreforkConstants";

    private static final String TAG = "xtcperformance-Prefork";
    private static final String TAG_PACKAGE = "packageName";
    private static final String TAG_PREFORK_PACKAGE = "PreforkpackageName";

    private ArrayList<String> mPreforkPackages = new ArrayList<>();

    public static PreforkConstants loadFromConfig(PolicyConfig policyConfig) {
        PreforkConstants constants = new PreforkConstants();
        HashMap<String, PolicyConfig.ConfigItem> items = policyConfig.getAllConfigItems();
        if (items == null) {
            return null;
        }
        PolicyConfig.ConfigItem item = items.get(TAG_PREFORK_PACKAGE);
        if (item != null) {
            constants.mPreforkPackages = item.getStringArray(TAG_PACKAGE);
        }
        return constants;
    }

    public int getPreforkPackagesSize() {
        return this.mPreforkPackages.size();
    }

    public boolean isPreForkSupportedDefault(String packageName) {
        for (String preforkPackage : this.mPreforkPackages) {
            if (packageName.contains(preforkPackage)) {
                return true;
            }
        }
        return false;
    }
}