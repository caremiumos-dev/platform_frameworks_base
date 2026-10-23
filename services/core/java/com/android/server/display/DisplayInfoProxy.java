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

package com.android.server.display;

import android.hardware.display.DisplayManagerGlobal;
import android.view.DisplayInfo;

/**
 * Mutable holder for a {@link DisplayInfo}, invalidating the local display-info
 * caches when the value changes.
 *
 * @hide
 */
public class DisplayInfoProxy {
    private DisplayInfo mInfo;

    public DisplayInfoProxy(DisplayInfo info) {
        mInfo = info;
    }

    public void set(DisplayInfo info) {
        DisplayManagerGlobal.invalidateLocalDisplayInfoCaches();
        mInfo = info;
    }

    public DisplayInfo get() {
        return mInfo;
    }
}