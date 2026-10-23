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
 * Command codes of the diag protocol.
 *
 * @hide
 */
public class XtcSocketCmdCode {
    public static final short CMD_TRANSMIT_STATUS = 1;
    public static final short CMD_REWRITE_WIFI_CONFIG = 2;
    public static final short CMD_RELOAD_WIFI_CONFIG = 3;
    public static final short CMD_REMOVE_WIFI_CONFIG = 4;
    public static final short CMD_FILE_OPERATE = 16;
    public static final short CMD_BBAT_RELEASE = 31;
    public static final short CMD_BBAT_RF_OPERATE = 32;
    public static final short CMD_BBAT_WIFI_OPERATE = 33;
    public static final short CMD_BBAT_RESTART = 34;
    public static final short CMD_SELF_TEST_OPERATE = 35;
    public static final short CMD_VALIDATION_TEST_OPERATE = 36;
    public static final short CMD_MMI_WIFI_CONNECT = 2001;
    public static final short CMD_MMI_WIFI_DISCONNECT = 2002;

    /** File operation codes. */
    public static class FileOperateCode {
        public static final int FILE_OPERATE_WRITE = 1;
        public static final int FILE_OPERATE_READ = 2;
        public static final int FILE_OPERATE_DELETE = 3;
        public static final int FILE_OPERATE_EXIST = 4;
        public static final int FILE_OPERATE_FILELEN = 5;
    }
}