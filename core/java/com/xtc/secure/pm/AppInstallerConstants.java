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

package com.xtc.secure.pm;

/**
 * Constants shared by the XTC app install verification path.
 *
 * @hide
 */
public class AppInstallerConstants {

    public interface InstallerFrom {
        int INSTALL_INVALID = 0;
        int INSTALL_FROM_API = 1;
        int INSTALL_FROM_ADB = 2;
    }

    public interface VerifyResult {
        int SUCCESS = 1;
        int FAIL_NOT_BIND = 2;
        int FAIL_REMOTE_EXCEPTION = 3;
    }

    public static int getInstallerFromByUid(int uid) {
        switch (uid) {
            case 0:
            case 2000:
                return InstallerFrom.INSTALL_FROM_ADB;
            case 1000:
                return InstallerFrom.INSTALL_FROM_API;
            default:
                return InstallerFrom.INSTALL_INVALID;
        }
    }
}