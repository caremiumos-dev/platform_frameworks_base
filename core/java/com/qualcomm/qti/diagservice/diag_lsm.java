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

package com.qualcomm.qti.diagservice;

/**
 * JNI wrapper around the Qualcomm diag LSM library.
 *
 * @hide
 */
public class diag_lsm {
    public static final int ENABLE = 1;
    public static final int DISABLE = 0;

    public static final int DIAG_PROC_MSM = 0;
    public static final int DIAG_PROC_MDM = 1;

    public static final int DIAG_CON_APSS = 1;
    public static final int DIAG_CON_MPSS = 2;
    public static final int DIAG_CON_LPASS = 4;
    public static final int DIAG_CON_WCNSS = 8;
    public static final int DIAG_CON_SENSORS = 16;
    public static final int DIAG_CON_WDSP = 32;

    public static final int IN_BUF_SIZE = 16384;

    public static final native boolean Diag_LSM_Init(long proc);

    public static final native boolean Diag_LSM_DeInit();
}