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

import android.text.format.DateFormat;

/**
 * Records the launch time of a single activity component.
 *
 * @hide
 */
public class LaunchTimeData {
    private final String mComponentName;
    private final long mTime;

    public LaunchTimeData(String componentName, long time) {
        mComponentName = componentName;
        mTime = time;
    }

    public String getComponentName() {
        return mComponentName;
    }

    public long getTime() {
        return mTime;
    }

    @Override
    public String toString() {
        return "LaunchTimeData{mComponentName='" + mComponentName + DateFormat.QUOTE
                + ", mTime=" + mTime + '}';
    }
}