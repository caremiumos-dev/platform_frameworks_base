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
 * JSON keys used by the diag protocol.
 *
 * @hide
 */
public class ItemJsonKey {
    public static final String ITEM_KEY_TEST_ID = "id";
    public static final String ITEM_KEY_TEST_KEY = "key";
    public static final String ITEM_KEY_RESULE = "result";
    public static final String ITEM_KEY_DATA_LIST = "dataList";
    public static final String ITEM_KEY_BEGIN_TIME = "begin";
    public static final String ITEM_KEY_END_TIME = "end";

    public static final String ITEM_WIFI_CONFIG_SSID = "ssid";
    public static final String ITEM_WIFI_CONFIG_PWD = "pwd";
    public static final String ITEM_WIFI_CONFIG_CIPHER_TYPE = "cipherType";
    public static final String ITEM_WIFI_CONFIG_BRIGHTNESS = "brightness";

    public static final String ITEM_FILE_KEY_DEST_PATH = "destFilePath";
    public static final String ITEM_FILE_KEY_FILE_NAME = "fileName";
    public static final String ITEM_FILE_KEY_OP_CODE = "operateCode";
    public static final String ITEM_FILE_KEY_FILE_SIZE = "fileSize";
    public static final String ITEM_FILE_KEY_FILE_EXIST = "fileExist";

    public static final String ITEM_SELF_TEST_ALL_ITEM = "alltestItem";
    public static final String ITEM_SELF_TEST_ITEM_CODE = "itemTestCode";
    public static final String ITEM_SELF_TEST_ITEM_DESC = "itemTestDesc";
    public static final String ITEM_SELF_TEST_RESULT_NAME = "itemResultName";
    public static final String ITEM_SELF_TEST_RESULT_VALUE = "itemResultValue";

    public static final String ITEM_BBAT_CMD_FILE_NAME = "bbatCmdFileName";
    public static final String ITEM_BBAT_CONN_STATUS = "bbatConnStatus";
}