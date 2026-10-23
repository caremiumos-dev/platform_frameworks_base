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

package com.android.internal.apm;

import org.json.JSONObject;

/**
 * Base class of the data blobs reported by the application performance
 * monitor.
 *
 * @hide
 */
public abstract class APMBaseDate {
    protected String mPackageName;
    protected String mProcessName;
    protected int mVersionCode;
    protected String mVersionName;

    public APMBaseDate(String packageName, String processName, int versionCode,
            String versionName) {
        mPackageName = packageName;
        mProcessName = processName;
        mVersionCode = versionCode;
        mVersionName = versionName;
    }

    public String getPackageName() {
        return mPackageName;
    }

    public String getProcessName() {
        return mProcessName;
    }

    public int getVersionCode() {
        return mVersionCode;
    }

    public String getVersionName() {
        return mVersionName;
    }

    public abstract String getDataType();

    public abstract long getTimestamp();

    public abstract JSONObject toJson();
}