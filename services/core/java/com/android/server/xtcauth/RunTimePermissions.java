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

package com.android.server.xtcauth;

import java.util.HashMap;

/**
 * Maps Android runtime permissions to the vendor "sub-permission" names used by
 * the permission-control application.
 *
 * @hide
 */
public class RunTimePermissions {
    public String[] permission = {
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.ACCESS_COARSE_LOCATION",
            "android.permission.CAMERA",
            "android.permission.READ_CONTACTS",
            "android.permission.WRITE_CONTACTS",
            "android.permission.RECORD_AUDIO",
            "android.permission.READ_PHONE_STATE",
            "android.permission.CALL_PHONE",
            "android.permission.READ_CALL_LOG",
            "android.permission.WRITE_CALL_LOG",
            "android.permission.SEND_SMS",
            "android.permission.RECEIVE_SMS",
            "android.permission.READ_SMS",
            "android.permission.RECEIVE_MMS"
    };

    public HashMap<String, String> permissionHashMap = new HashMap<>();

    public RunTimePermissions() {
        initHashMap();
    }

    public void initHashMap() {
        permissionHashMap.put("android.permission.ACCESS_FINE_LOCATION",
                "sub-permission.ACCESS_LOCATION");
        permissionHashMap.put("android.permission.ACCESS_COARSE_LOCATION",
                "sub-permission.ACCESS_LOCATION");
        permissionHashMap.put("android.permission.CAMERA", "sub-permission.OPEN_CAMERA");
        permissionHashMap.put("android.permission.READ_CONTACTS",
                "sub-permission.QUERY_CONTACTS");
        permissionHashMap.put("android.permission.WRITE_CONTACTS",
                "sub-permission.MODIFY_CONTACTS");
        permissionHashMap.put("android.permission.RECORD_AUDIO", "sub-permission.RECORD_MIC");
        permissionHashMap.put("android.permission.READ_PHONE_STATE",
                "sub-permission.READ_PHONE_IMEI");
        permissionHashMap.put("android.permission.CALL_PHONE", "sub-permission.MAKE_CALL");
        permissionHashMap.put("android.permission.READ_CALL_LOG",
                "sub-permission.QUERY_CALL_LOG");
        permissionHashMap.put("android.permission.WRITE_CALL_LOG",
                "sub-permission.MODIFY_CALL_LOG");
        permissionHashMap.put("android.permission.SEND_SMS", "sub-permission.SEND_SMS");
        permissionHashMap.put("android.permission.RECEIVE_SMS", "sub-permission.QUERY_SMS");
        permissionHashMap.put("android.permission.READ_SMS", "sub-permission.QUERY_SMS");
        permissionHashMap.put("android.permission.RECEIVE_MMS", "sub-permission.QUERY_MMS");
        permissionHashMap.put("android.permission.BLUETOOTH_ADMIN",
                "sub-permission.CHANGE_BT_STATE_ON");
        permissionHashMap.put("android.permission.CHANGE_WIFI_STATE",
                "sub-permission.CHANGE_WIFI_STATE_ON");
        permissionHashMap.put("android.permission.CHANGE_NETWORK_STATE",
                "sub-permission.CHANGE_NETWORK_STATE_ON");
    }
}