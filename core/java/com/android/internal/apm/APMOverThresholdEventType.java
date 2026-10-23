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

/**
 * Event types reported by the application performance monitor for
 * over-threshold callbacks.
 *
 * @hide
 */
public class APMOverThresholdEventType {
    public static final int TYPE_METHOD_COST_TIME = 1;
    public static final int TYPE_DROP_FRAME = 2;
    public static final int TYPE_ACTIVITY_LAUNCH_TIME = 3;
    public static final int TYPE_MAIN_THREAD_SQL_COST_TIME = 4;
}