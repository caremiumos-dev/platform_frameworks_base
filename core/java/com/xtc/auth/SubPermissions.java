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

package com.xtc.auth;

/**
 * Sub-permission identifiers used by the XTC mobile manager for fine grained
 * permission control over phone, SMS, contacts, camera and RCS operations.
 *
 * @hide
 */
public class SubPermissions {
    public static final String MAKE_CALL = "sub-permission.MAKE_CALL";
    public static final String MAKE_CONFERENCE_CALL = "sub-permission.MAKE_CONFERENCE_CALL";
    public static final String MODIFY_CALL_LOG = "sub-permission.MODIFY_CALL_LOG";
    public static final String QUERY_CALL_LOG = "sub-permission.QUERY_CALL_LOG";
    public static final String SEND_SMS = "sub-permission.SEND_SMS";
    public static final String MODIFY_SMS = "sub-permission.MODIFY_SMS";
    public static final String QUERY_SMS = "sub-permission.QUERY_SMS";
    public static final String SEND_MMS = "sub-permission.SEND_MMS";
    public static final String MODIFY_MMS = "sub-permission.MODIFY_MMS";
    public static final String QUERY_MMS = "sub-permission.QUERY_MMS";
    public static final String MODIFY_CONTACTS = "sub-permission.MODIFY_CONTACTS";
    public static final String QUERY_CONTACTS = "sub-permission.QUERY_CONTACTS";
    public static final String OPEN_CAMERA = "sub-permission.OPEN_CAMERA";
    public static final String SEND_EMAIL = "sub-permission.SEND_EMAIL";
    public static final String ACCESS_LOCATION = "sub-permission.ACCESS_LOCATION";
    public static final String RECORD_MIC = "sub-permission.RECORD_MIC";
    public static final String READ_PHONE_IMEI = "sub-permission.READ_PHONE_IMEI";
    public static final String CHANGE_WIFI_STATE_ON = "sub-permission.CHANGE_WIFI_STATE_ON";
    public static final String CHANGE_BT_STATE_ON = "sub-permission.CHANGE_BT_STATE_ON";
    public static final String CHANGE_NFC_STATE_ON = "sub-permission.CHANGE_NFC_STATE_ON";
    public static final String CHANGE_NETWORK_STATE_ON = "sub-permission.CHANGE_NETWORK_STATE_ON";
    public static final String ACCESS_HOTKNOT = "sub-permission.ACCESS_HOTKNOT";
    public static final String RCS_ACCEPT_GROUP_INVITE = "sub-permission.RCS_ACCEPT_GROUP_INVITE";
    public static final String RCS_QUIT_CONVERSATION = "sub-permission.RCS_QUIT_CONVERSATION";
    public static final String RCS_ABORT_CONVERSATION = "sub-permission.RCS_ABORT_CONVERSATION";
    public static final String RCS_REMOVE_PARTICIPANTS = "sub-permission.RCS_REMOVE_PARTICIPANTS";
    public static final String RCS_MODIFY_GROUP_SUBJECT = "sub-permission.RCS_MODIFY_GROUP_SUBJECT";
    public static final String RCS_FOLLOW_PA = "sub-permission.RCS_FOLLOW_PA";
    public static final String RCS_UNFOLLOW_PA = "sub-permission.RCS_UNFOLLOW_PA";
    public static final String RCS_REPORT_PA = "sub-permission.RCS_REPORT_PA";
    public static final String RCS_SET_PROFILE_INFO = "sub-permission.RCS_SET_PROFILE_INFO";
    public static final String RCS_MODIFY_SELF_NICKNAME = "sub-permission.RCS_MODIFY_SELF_NICKNAME";
    public static final String RCS_SET_GROUP_CHAIRMEN = "sub-permission.RCS_SET_GROUP_CHAIRMEN";
}