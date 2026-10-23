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

package com.android.server.xgseserver.xss;

/**
 * Constants shared by the XGS/network exception reporting subsystem.
 *
 * @hide
 */
public interface HttpConfigOptions {

    interface HeaderKey {
        String ACCEPT_LANGUAGE = "Accept-Language";
        String AUTH_ID = "authId";
        String BASE_REQUEST_PARAM = "Base-Request-Param";
        String CONTENT_ENCODING = "Content-Encoding";
        String CONTENT_TYPE = "Content-Type";
        String DATA_CENTER = "dataCenterCode";
        String EEBBK_KEY = "Eebbk-Key";
        String EEBBK_KEY_ID = "Eebbk-Key-Id";
        String EEBBK_SIGN = "Eebbk-Sign";
        String ENCRYPTED = "encrypted";
        String GREY = "Grey";
        String HOST = "Host";
        String IM_SDK_VERSION_CODE = "imSdkVersion";
        String MEDIA_TYPE = "application/json;charset=utf-8";
        String MODEL = "model";
        String PACKAGE_NAME = "packageName";
        String PACKAGE_VERSION = "packageVersion";
        String PROGRAM = "program";
        String VERSION = "Version";
        String WATCH_TIME_ZONE = "Watch-Time-Zone";
    }

    interface MonitorCondition {
        long DAY_REQUEST_MAX_TRAFFIC = 524288000L;
        int FREQUENT_REQUEST_COUNT = 5;
        long FREQUENT_REQUEST_PERIOD = 60000L;
        int REQUESTING_TIME_OUT = 20000;
        long REQUEST_MAX_TRAFFIC = 10240L;
    }

    interface MonitorError {
        int MONITOR_ERROR_BOOT_LIMIT_TIME = 1001;
        int MONITOR_ERROR_FREQUENT_REQUEST = 1003;
        int MONITOR_ERROR_HEAVY_TRAFFIC = 1004;
        int MONITOR_ERROR_NO_NETWORK_CONNECTIVITY = 1005;
        int MONITOR_ERROR_NO_NETWORK_PERMISSION = 1006;
        int MONITOR_ERROR_REQUESTING = 1002;
    }

    interface PackageName {
        String PACKAGE_NAME_LAUNCHER = "com.xtc.i3launcher";
    }

    interface ProtocolType {
        String HTTP = "http://";
        String HTTPS = "https://";
    }

    interface ResponseCondition {
        int MAX_BODY_SIZE = 1048576;
    }

    interface TranspondCondition {
        int INTERVAL_TIME = 600000;
        int TRANSPOND_TIME_OUT = 20000;
    }
}