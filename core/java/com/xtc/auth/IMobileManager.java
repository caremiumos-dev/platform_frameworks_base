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

import android.content.pm.PackageInfo;
import android.net.Uri;

import java.util.List;

/**
 * Client-facing API of the XTC mobile manager (MoMS). Provides fine grained
 * permission, interception, notification and package controls for CTA
 * versions.
 *
 * @hide
 */
public interface IMobileManager {
    String VERSION_NAME = "MoMS.5.0.0";

    /** Controller identifiers. */
    int CONTROLLER_PERMISSION = 0;
    int CONTROLLER_PACKAGE = 1;
    int CONTROLLER_RECEIVER = 2;
    int CONTROLLER_NOTIFICATION = 3;
    int CONTROLLER_FIREWALL = 4;
    int CONTROLLER_MESSAGE_INTERCEPT = 5;
    int CONTROLLER_CALL = 6;
    int CONTROLLER_LICENSE = 7;
    int CONTROLLER_INTERCEPTION = 8;

    /** Manager attach states. */
    int MGR_ATTACHED = 0;
    int MGR_DETACHED = 1;

    /** Package change types. */
    int PACKAGE_ADDED = 0;
    int PACKAGE_REMOVED = 1;
    int PACKAGE_UPDATED = 2;
    int PACKAGE_EXT_AVAILABLE = 3;
    int PACKAGE_EXT_UNAVAILABLE = 4;

    /** User change types. */
    int USER_ADDED = 0;
    int USER_REMOVED = 1;
    int USER_SWITCHED = 2;

    /** Permission status values. */
    int PERMISSION_STATUS_GRANTED = 0;
    int PERMISSION_STATUS_DENIED = 1;
    int PERMISSION_STATUS_CHECK = 2;
    int PERMISSION_STATUS_NONE = 0;

    /** Permission flags. */
    int PERMISSION_FLAG_NONE = 0;
    int PERMISSION_FLAG_NORMAL = 1;
    int PERMISSION_FLAG_SUB = 2;
    int PERMISSION_FLAG_USERCONFIRM = 4;
    int PERMISSION_FLAG_SYSTEM = 8;
    int PERMISSION_FLAG_ALL = 15;

    /** Timeout (ms) for waiting for a listener decision. */
    int TRIGGER_LISTENER_TIMEOUT = 35000;

    /** Broadcast actions. */
    String ACTION_MGR_CHANGE = "mom.action.MGR_CHANGE";
    String ACTION_PACKAGE_CHANGE = "mom.action.PACKAGE_CHANGE";
    String ACTION_PERMISSION_CHANGE = "mom.action.PERMISSION_CHANGE";
    String ACTION_USER_CHANGE = "mom.action.USER_CHANGE";

    /** Action extras. */
    String ACTION_EXTRA_PACKAGE = "mom.action.extra.package";
    String ACTION_EXTRA_PACKAGE_LIST = "mom.action.extra.package.list";
    String ACTION_EXTRA_STATUS = "mom.action.extra.status";
    String ACTION_EXTRA_UID = "mom.action.extra.uid";
    String ACTION_EXTRA_USER = "mom.action.extra.user";

    /** Call interception parameters. */
    String PARAMETER_PHONENUMBER = "phoneNumber";
    String PARAMETER_CALLTYPE = "callType";
    String PARAMETER_SLOTID = "slotId";

    /** SMS interception message keys. */
    String SMS_MESSAGE_DATA = "data";
    String SMS_MESSAGE_FORMAT = "format";
    String SMS_MESSAGE_INTENT = "intent";
    String SMS_MESSAGE_SUBID = "subId";
    String SMS_MESSAGE_SIMID = "simId";
    String SMS_MESSAGE_RECIPIENT = "recipient";
    String SMS_MESSAGE_TEXT = "text";
    String SMS_MESSAGE_MULTIPARTDATA = "multipartdata";
    String SMS_MESSAGE_MULTIPARTTEXT = "multiparttext";

    /** Notification interception option keys. */
    String OPTION_NOTIFICATION_PKG = "notificationPkg";
    String OPTION_NOTIFICATION_ID = "notificationID";
    String OPTION_NOTIFICATION_TITLE = "notificationTitle";
    String OPTION_NOTIFICATION_CONTENT = "notificationContent";

    boolean attach(IMobileConnectionCallback callback);

    void detach();

    String getVersionName();

    void enablePermissionController(boolean enable);

    void enableInterceptionController(boolean enable);

    void setPermissionRecord(PermissionRecord record);

    void setPermissionRecords(List<PermissionRecord> records);

    void setPermissionCache(List<PermissionRecord> records);

    List<Permission> getPackageGrantedPermissions(String packageName);

    void clearPackageSettings(String packageName);

    void clearAllSettings();

    void setBootReceiverEnabledSetting(String packageName, boolean enable);

    void setBootReceiverEnabledSettings(List<ReceiverRecord> records);

    boolean getBootReceiverEnabledSetting(String packageName);

    List<ReceiverRecord> getBootReceiverList();

    void setNotificationEnabledSetting(String packageName, boolean enable);

    boolean getNotificationEnabledSetting(String packageName);

    void setNotificationCache(List<NotificationCacheRecord> records);

    void setFirewallPolicy(int uid, int policy, boolean enable);

    void registerPermissionListener(IPermissionListener listener);

    void registerNotificationListener(INotificationListener listener);

    void registerCallInterceptionListener(ICallInterceptionListener listener);

    void registerMessageInterceptListener(IMessageInterceptListener listener);

    void forceStopPackage(String packageName);

    void deletePackage(String packageName);

    void installPackage(Uri packageUri, IPackageInstallCallback callback);

    List<PackageInfo> getInstalledPackages();

    void cancelNotification(String packageName);
}