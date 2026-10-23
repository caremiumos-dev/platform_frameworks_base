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

package com.xtc.xtcdiag.socket;

/**
 * Error codes exchanged with the PC-side diag tool.
 *
 * @hide
 */
public class XtcSocketErrorCode {
    public static final int FTM_SUCCESS = 0;
    public static final int FTM_ERROR = 1;
    public static final int FTM_ERROR_PACKET_CRC = 2;
    public static final int FTM_ERROR_CMD_NO_SUPPORT = 3;
    public static final int FTM_ERROR_SUB_CMD_NO_SUPPORT = 4;
    public static final int FTM_ERROR_FILE_OPER_FAIL = 5;
    public static final int FTM_ERROR_DEVICE_OPER_FAIL = 6;
    public static final int FTM_ERROR_CMD_FAIL = 7;
    public static final int FTM_ERROR_CMD_TIMEOUT = 8;
}