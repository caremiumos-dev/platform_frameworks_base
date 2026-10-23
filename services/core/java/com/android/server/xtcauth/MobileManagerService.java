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

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.IUserManager;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;

import com.android.internal.content.PackageMonitor;
import com.xtc.auth.IMobileConnectionCallback;
import com.xtc.auth.IMobileManagerService;
import com.xtc.auth.IPackageInstallCallback;
import com.xtc.auth.IRequestedPermissionCallback;
import com.xtc.auth.NotificationCacheRecord;
import com.xtc.auth.Permission;
import com.xtc.auth.PermissionRecord;
import com.xtc.auth.ReceiverRecord;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The "Mobile Manager Service" (MoMS) binder service. It exposes a powerful
 * remote API used by a small set of vendor-approved applications to control
 * other applications' runtime permissions, notifications, boot receivers, calls
 * and SMS, and to install/delete/force-stop packages.
 *
 * <p>WARNING: This service implements a closed device-management / parental
 * control surface. It lets an allow-listed (and, for the platform manager,
 * unconditionally trusted) application silently grant or revoke permissions,
 * suppress notifications, block autostart and intercept calls/SMS of arbitrary
 * third-party applications without their or the user's consent. It is restored
 * here for completeness of the framework rewrite; its trust model is not
 * endorsed.
 *
 * @hide
 */
public class MobileManagerService extends IMobileManagerService.Stub {
    private static final String TAG = "MobileManagerService";
    private static final String BR_PERMISSION_CHANGE = "com.mediatek.mom.BR_PERMISSION_CHANGE";

    private final Context mContext;
    private final MobileManagerHandler mHandler;
    private final HandlerThread mHandlerThread = new HandlerThread("MoMS.HandlerThread");
    private final Map<Integer, ControllerBase> mControllers = new HashMap<>();

    private PackageController mPackageController;
    private PermissionController mPermissionController;
    private NotificationController mNotificationController;
    private LicenseController mLicenseController;

    private ReceiverController mReceiverController = null;
    private FirewallController mFirewallController = null;
    private InterceptionController mInterceptionController = null;
    private CallInterceptionController mCallInterceptionController = null;
    private MessageInterceptController mMessageInterceptController = null;

    private boolean isSystemReady = false;

    final List<String> specialPermissionList = new ArrayList<>(Arrays.asList(
            "android.permission.BLUETOOTH",
            "android.permission.BLUETOOTH_ADMIN",
            "android.permission.MODIFY_PHONE_STATE",
            "android.permission.CHANGE_WIFI_STATE"));

    private final PackageMonitor mPackageMonitor = new PackageMonitor() {
        @Override
        public void onPackageAdded(String packageName, int uid) {
            int userId = getChangingUserId();
            Log.d(TAG, "onPackageAdded() packageName: " + packageName + " at User(" + userId + ")");
            if (isNeedCtaControl(packageName, uid)) {
                int identity = LicenseController.checkManagerIdentity(uid);
                if (ManagerIdentityHelper.isValidIdentity(identity)) {
                    ManagerIdentityHelper.initManagerIdentity(identity, uid);
                }
                onPackageStatusChange(uid, packageName, 0);
                updateCtaAuthPermission(packageName, 0, userId);
            }
        }

        @Override
        public void onPackageRemoved(String packageName, int uid) {
            int userId = getChangingUserId();
            Log.d(TAG, "onPackageRemoved() packageName: " + packageName + " at User(" + userId
                    + ")");
            if (uid > 1000) {
                onPackageStatusChange(uid, packageName, 1);
                int identity = ManagerIdentityHelper.getManagerIdentity(uid);
                if (ManagerIdentityHelper.isValidIdentity(identity)) {
                    ManagerIdentityHelper.uninitManagerIdentity(identity);
                }
                updateCtaAuthPermission(packageName, 1, userId);
            }
        }

        @Override
        public void onPackageUpdateFinished(String packageName, int uid) {
            int userId = UserHandle.getUserId(uid);
            Log.d(TAG, "onPackageUpdateFinished() packageName: " + packageName + " at User("
                    + userId + ")");
            if (uid <= 1000) {
                if (packageName == null || !packageName.startsWith("com.xtc.setting")) {
                    return;
                }
                startCtaAuthService();
                return;
            }
            int identity = ManagerIdentityHelper.getManagerIdentity(packageName);
            if (ManagerIdentityHelper.isValidIdentity(identity)) {
                ManagerIdentityHelper.initManagerIdentity(identity, uid);
            }
            onPackageStatusChange(uid, packageName, 2);
            updateCtaAuthPermission(packageName, 2, userId);
        }
    };

    static class DumpState {
        public static final int DUMP_PERMISSION = 1;
        public static final int DUMP_PACKAGE = 2;
        public static final int DUMP_RECEIVER = 4;
        public static final int DUMP_NOTIFICATION = 8;
        public static final int DUMP_FIREWALL = 16;
        public static final int DUMP_MESSAGE = 32;
        public static final int DUMP_CALL = 64;
        public static final int DUMP_ALL = 1048576;

        private int mTypes;

        DumpState() {
        }

        public boolean isDumping(int type) {
            return (mTypes & type) != 0;
        }

        public void setDump(int type) {
            mTypes |= type;
        }
    }

    private class UserChangeReceiver extends android.content.BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_USER_SWITCHED.equals(intent.getAction())) {
                int userId = intent.getIntExtra(Intent.EXTRA_USER_HANDLE, 55536);
                NotifyControllerUserStatusChanged(userId, 2);
                sendUserBroadcastAsUser(2, userId);
            } else if (Intent.ACTION_USER_REMOVED.equals(intent.getAction())) {
                int userId = intent.getIntExtra(Intent.EXTRA_USER_HANDLE, 55536);
                NotifyControllerUserStatusChanged(userId, 1);
                sendUserBroadcastAsUser(1, userId);
            } else if (Intent.ACTION_USER_ADDED.equals(intent.getAction())) {
                int userId = intent.getIntExtra(Intent.EXTRA_USER_HANDLE, 55536);
                NotifyControllerUserStatusChanged(userId, 0);
                sendUserBroadcastAsUser(0, userId);
            } else if (BR_PERMISSION_CHANGE.equals(intent.getAction())) {
                int state = intent.getIntExtra("state", 0);
                String packageName = intent.getStringExtra("packageName");
                String permissionName = intent.getStringExtra("permissionName");
                Log.d(TAG, "BR_PERMISSION_CHANGE state: " + state + " packageName: " + packageName
                        + " permissionName: " + permissionName);
                mPermissionController.onPermissionChange(packageName, permissionName, state);
            }
        }
    }

    public MobileManagerService(Context context) {
        mContext = context;
        mHandlerThread.start();
        mHandler = new MobileManagerHandler(mHandlerThread.getLooper());
        ListenerTriggerHelper.setHandler(mHandler);
        ManagerIdentityHelper.init(mContext);

        mControllers.put(1, PackageController.getInstance());
        mPackageController = (PackageController) getMobileManagerController(1);
        mControllers.put(0, PermissionController.getInstance(mPackageController));
        mPermissionController = (PermissionController) getMobileManagerController(0);
        mControllers.put(3, NotificationController.getInstance());
        mNotificationController = (NotificationController) getMobileManagerController(3);
        mControllers.put(7, LicenseController.getInstance());
        mLicenseController = (LicenseController) getMobileManagerController(7);

        mPackageMonitor.register(mContext, mHandlerThread.getLooper(), UserHandle.ALL, true);
    }

    private ControllerBase getMobileManagerController(int id) {
        return mControllers.get(id);
    }

    private void NotifyControllerUserStatusChanged(int userId, int status) {
        Log.d(TAG, "User Status Changed, userId: " + userId + " status: " + status);
        for (Map.Entry<Integer, ControllerBase> entry : mControllers.entrySet()) {
            ControllerBase controller = entry.getValue();
            if (controller instanceof IPackageBasedController) {
                ((IPackageBasedController) controller).onUserStatusChange(userId, status);
            } else if (controller instanceof IPermissionController) {
                ((IPermissionController) controller).onUserStatusChange(userId, status);
            }
        }
    }

    private boolean checkLicense(int uid) {
        int identity = LicenseController.checkManagerIdentity(uid);
        return identity != 0 && mLicenseController.checkProtocol(uid, identity);
    }

    private void clearAllSettingsInner() {
        Log.d(TAG, "clearAllSettings()");
        for (Map.Entry<Integer, ControllerBase> entry : mControllers.entrySet()) {
            ControllerBase controller = entry.getValue();
            if (controller instanceof IPackageBasedController) {
                ((IPackageBasedController) controller).clearAllSettings();
            }
        }
    }

    private void detachInner(int uid) {
        mPermissionController.detach(uid);
    }

    private void enforceCallingUid(String operation) {
        int uid = Binder.getCallingUid();
        if (uid >= 10000) {
            throw new SecurityException("Use MoMS API without valid uid: " + operation + " uid: "
                    + uid + " pid: " + Binder.getCallingPid());
        }
    }

    private void enforceCheckAttachment(String operation) {
        if (!mPermissionController.checkAttachment(Binder.getCallingUid())) {
            throw new SecurityException("Use MoMS API without attachment: " + operation + " uid: "
                    + Binder.getCallingUid() + " pid: " + Binder.getCallingPid());
        }
    }

    private void enforceCheckLicense(String operation) {
        if (!checkLicense(Binder.getCallingUid())) {
            throw new SecurityException("Use MoMS API without valid license: " + operation
                    + " uid: " + Binder.getCallingUid() + " pid: " + Binder.getCallingPid());
        }
    }

    private List<android.content.pm.UserInfo> getUserList() {
        try {
            IUserManager um = IUserManager.Stub.asInterface(ServiceManager.getService("user"));
            if (um != null) {
                return um.getUsers(false);
            }
            return null;
        } catch (RemoteException e) {
            Log.e(TAG, "getUsers() failed!", e);
            return null;
        }
    }

    private void handlePackageAvaliable(boolean available, String[] packages) {
        int status = available ? 3 : 4;
        for (String packageName : packages) {
            PackageInfo packageInfo = PackageController.getPackageInfo(packageName);
            int appId = (packageInfo == null || packageInfo.applicationInfo == null)
                    ? 0 : packageInfo.applicationInfo.uid;
            Log.d(TAG, "handlePackageAvaliable() package: " + packageName + " appId: " + appId);
            if (appId >= 10000) {
                for (android.content.pm.UserInfo user : getUserList()) {
                    onPackageStatusChange(UserHandle.getUid(user.id, appId), packageName, status);
                }
            }
            PackageController.removePendingPackage(packageName);
        }
        mReceiverController.updateReceiverList(getUserList());
        sendPackageListBroadcastAsUser(packages, status);
    }

    private static boolean isMultiUserSupport() {
        return UserManager.supportsMultipleUsers();
    }

    private boolean isNeedCtaControl(String packageName, int uid) {
        if (uid <= 1000) {
            return false;
        }
        PackageInfo packageInfo = null;
        try {
            packageInfo = mContext.getPackageManager().getPackageInfo(packageName, 4096);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "isNeedCtaControl error ", e);
        }
        if (packageInfo == null) {
            return false;
        }
        String[] requestedPermissions = packageInfo.requestedPermissions;
        if (requestedPermissions == null || requestedPermissions.length == 0) {
            Log.i(TAG, "packagePermissions is null");
            return false;
        }
        for (String permission : requestedPermissions) {
            if (specialPermissionList.contains(permission)) {
                return true;
            }
        }
        return false;
    }

    private void onPackageStatusChange(int uid, String packageName, int status) {
        for (Map.Entry<Integer, ControllerBase> entry : mControllers.entrySet()) {
            ControllerBase controller = entry.getValue();
            if (controller instanceof IPackageBasedController) {
                ((IPackageBasedController) controller).onPackageStatusChange(uid, packageName,
                        status);
            } else if (controller instanceof IPermissionController) {
                ((IPermissionController) controller).onPackageStatusChange(uid, packageName,
                        status);
            }
        }
        if (status == 1) {
            if (ManagerIdentityHelper.isMobileManagerIdetity(
                    LicenseController.checkManagerIdentity(packageName))) {
                clearAllSettingsInner();
            }
        } else if (status == 0) {
            mPermissionController.initPermControlAp(LicenseController.checkManagerIdentity(
                    packageName));
        }
    }

    private void sendPackageBroadcastAsUser(String packageName, int status, int userId) {
        if (isSystemReady) {
            Intent intent = new Intent("mom.action.PACKAGE_CHANGE");
            intent.putExtra("mom.action.extra.status", status);
            intent.putExtra("mom.action.extra.package", packageName);
            intent.putExtra("mom.action.extra.user", userId);
            if (userId != 55536) {
                mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
            } else {
                mContext.sendBroadcast(intent);
            }
        }
    }

    private void sendPackageListBroadcastAsUser(String[] packages, int status) {
        if (isSystemReady) {
            Intent intent = new Intent("mom.action.PACKAGE_CHANGE");
            intent.putExtra("mom.action.extra.status", status);
            intent.putExtra("mom.action.extra.package.list", packages);
            intent.putExtra("mom.action.extra.user", -1);
            mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        }
    }

    private void sendUserBroadcastAsUser(int status, int userId) {
        if (isSystemReady) {
            Intent intent = new Intent("mom.action.USER_CHANGE");
            intent.putExtra("mom.action.extra.status", status);
            intent.putExtra("mom.action.extra.user", userId);
            mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        }
    }

    private void startCtaAuthService() {
        try {
            Log.d(TAG, "startCtaAuthService: start service");
            Intent intent = new Intent("mom.action.START_AUTH");
            intent.setPackage("com.xtc.setting");
            mContext.startService(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateCtaAuthPermission(String packageName, int status, int userId) {
        try {
            Log.d(TAG, "updateCtaAuthPermission: packageName=" + packageName + ", status=" + status);
            Intent intent = new Intent("mom.action.PACKAGE_CHANGE");
            intent.setPackage("com.xtc.setting");
            intent.putExtra("mom.action.extra.status", status);
            intent.putExtra("mom.action.extra.package", packageName);
            mContext.startService(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public synchronized boolean attach(IMobileConnectionCallback callback) {
        enforceCheckLicense("attach()");
        int uid = Binder.getCallingUid();
        int checkIdentity = LicenseController.checkManagerIdentity(uid);
        int currentIdentity = mPermissionController.getCurrentIdentity(uid);
        Log.d(TAG, "attach() currentIdentity: " + currentIdentity + " checkIdentity: "
                + checkIdentity + " with uid: " + uid);
        if (callback == null) {
            Log.e(TAG, "attach with null callback");
            return false;
        }
        boolean attached = mPermissionController.attach(callback, uid, currentIdentity,
                checkIdentity);
        Log.d(TAG, "attach() with uid: " + uid + " identity: " + checkIdentity
                + (attached ? " successfully" : " failed"));
        return attached;
    }

    @Override
    public void cancelNotification(String packageName) {
        enforceCheckLicense("cancelNotification()");
        mNotificationController.cancelNotification(packageName);
    }

    @Override
    public int checkPermission(String permissionName, int uid) {
        int result = mPermissionController.checkPermission(permissionName, uid, null);
        Log.d(TAG, "checkPermission: permissionName=" + permissionName + ", result=" + result);
        if (result != 0) {
            String name = mContext.getPackageManager().getNameForUid(uid);
            Message message = Message.obtain();
            message.arg1 = uid;
            message.obj = name;
            message.what = MobileManagerHandler.MSG_KILL_PROCESS;
            mHandler.sendMessage(message);
        }
        return result;
    }

    @Override
    public void checkPermissionAsync(String permissionName, int uid,
            IRequestedPermissionCallback callback) {
        if (mPermissionController == null) {
            Log.e(TAG, "MobileManagerService.checkPermissionAsync == null");
        } else {
            Log.d(TAG, "checkPermisionAsync(" + permissionName + "," + uid + "," + callback + ")");
            mPermissionController.checkPermissionAsync(permissionName, uid, null, callback);
        }
    }

    @Override
    public void checkPermissionAsyncWithData(String permissionName, int uid, Bundle data,
            IRequestedPermissionCallback callback) {
        if (mPermissionController != null) {
            mPermissionController.checkPermissionAsync(permissionName, uid, data, callback);
        } else {
            Log.e(TAG, "MobileManagerService.checkPermissionAsyncWithData == null");
        }
    }

    @Override
    public int checkPermissionWithData(String permissionName, int uid, Bundle data) {
        return mPermissionController.checkPermission(permissionName, uid, data);
    }

    @Override
    public void clearAllSettings() {
        enforceCheckLicense("clearAllSettings()");
        clearAllSettingsInner();
    }

    @Override
    public void clearPackageSettings(String packageName) {
        enforceCheckLicense("clearPackageSettings()");
        Log.d(TAG, "clearPackageSettings(" + packageName + ")");
        for (Map.Entry<Integer, ControllerBase> entry : mControllers.entrySet()) {
            ControllerBase controller = entry.getValue();
            if (controller instanceof IPackageBasedController) {
                ((IPackageBasedController) controller).clearPackageSettings(packageName);
            }
        }
    }

    @Override
    public void deletePackage(String packageName) {
        enforceCheckLicense("deletePackage()");
        long token = Binder.clearCallingIdentity();
        mPackageController.deletePackage(packageName);
        Binder.restoreCallingIdentity(token);
    }

    @Override
    public void detach() {
        enforceCheckAttachment("detach()");
        detachInner(Binder.getCallingUid());
    }

    protected void dump(java.io.FileDescriptor fd, PrintWriter pw, String[] args) {
        if (mContext.checkCallingOrSelfPermission("android.permission.DUMP") != 0) {
            pw.println("Permission Denial: can't dump MobileManager from from pid="
                    + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid()
                    + " without permission android.permission.DUMP");
            return;
        }
        int i = 0;
        while (i < args.length && args[i] != null && args[i].length() > 0
                && args[i].charAt(0) == '-') {
            String arg = args[i];
            i++;
            if ("-a".equals(arg)) {
                continue;
            }
            if ("-h".equals(arg)) {
                pw.println("Mobile manager dump options:");
                pw.println("  [-h] [-c] [controller] ...");
                pw.println("    -c: print the specific controller");
                pw.println("    -h: print this help");
                pw.println("  controller may be one of:");
                pw.println("    permission: list permission configuration");
                pw.println("    package: ");
                pw.println("    receiver: list packages that cannot receive BOOT_COMPLETED");
                pw.println("    notification: ");
                pw.println("    firewall: ");
                return;
            }
            if (!"-c".equals(arg)) {
                pw.println("Unknown argument: " + arg + "; use -h for help");
            }
        }
        DumpState dumpState = new DumpState();
        if (i < args.length) {
            String controller = args[i];
            if ("permission".equals(controller)) {
                dumpState.setDump(DumpState.DUMP_PERMISSION);
                mPermissionController.dump();
            } else if ("package".equals(controller)) {
                dumpState.setDump(DumpState.DUMP_PACKAGE);
            } else if ("receiver".equals(controller)) {
                dumpState.setDump(DumpState.DUMP_RECEIVER);
            } else if ("notification".equals(controller)) {
                dumpState.setDump(DumpState.DUMP_NOTIFICATION);
            } else if ("firewall".equals(controller)) {
                dumpState.setDump(DumpState.DUMP_FIREWALL);
            }
        }
        if (dumpState.isDumping(DumpState.DUMP_PERMISSION)
                || dumpState.isDumping(DumpState.DUMP_RECEIVER)
                || dumpState.isDumping(DumpState.DUMP_PACKAGE)
                || dumpState.isDumping(DumpState.DUMP_NOTIFICATION)) {
            return;
        }
        dumpState.isDumping(DumpState.DUMP_FIREWALL);
    }

    @Override
    public void enableInterceptionController(boolean enable) {
        enforceCheckLicense("enableInterceptionController()");
        Log.d(TAG, "enableInterceptionController() enable: " + enable);
    }

    @Override
    public void enablePermissionController(boolean enable) {
        enforceCheckAttachment("enablePermissionController()");
        Log.d(TAG, "enablePermissionController() enable: " + enable);
        mPermissionController.enableController(enable);
    }

    @Override
    public void filterReceiver(Intent intent, List<ResolveInfo> receivers, int allowedUid) {
        enforceCallingUid("filterReceiver()");
        mReceiverController.filterReceiver(intent.getAction(), receivers, allowedUid);
    }

    @Override
    public void forceStopPackage(String packageName) {
        enforceCheckLicense("forceStopPackage()");
        long token = Binder.clearCallingIdentity();
        mPackageController.forceStopPackage(packageName);
        Binder.restoreCallingIdentity(token);
    }

    @Override
    public boolean getBootReceiverEnabledSetting(String packageName) {
        enforceCheckLicense("getBootReceiverEnabledSetting()");
        return mReceiverController.getBootReceiverEnabledSetting(Binder.getCallingUid(),
                packageName);
    }

    @Override
    public List<ReceiverRecord> getBootReceiverList() {
        enforceCheckLicense("getBootReceiverList()");
        return mReceiverController.getBootReceiverList(Binder.getCallingUid());
    }

    @Override
    public List<PackageInfo> getInstalledPackages() {
        enforceCheckLicense("getInstalledPackages()");
        return PackageController.getInstalledPackages(UserHandle.getCallingUserId());
    }

    @Override
    public boolean getInterceptionEnabledSetting() {
        enforceCallingUid("getInterceptionEnabledSetting()");
        return true;
    }

    @Override
    public boolean getNotificationEnabledSetting(String packageName) {
        enforceCallingUid("getNotificationEnabledSetting()");
        return mNotificationController.getNotificationEnabledSetting(packageName);
    }

    @Override
    public List<Permission> getPackageGrantedPermissions(String packageName) {
        enforceCheckLicense("getPackageGrantedPermissions()");
        return mPermissionController.getPackageGrantedPermissions(UserHandle.getCallingUserId(),
                packageName);
    }

    @Override
    public String getParentPermission(String subPermission) {
        return mPermissionController.getParentPermission(subPermission);
    }

    @Override
    public long getUserConfirmTime(int userId, long timeBound) {
        enforceCallingUid("getUserConfirmTime");
        if (userId == 55536) {
            Log.w(TAG, "getUserConfirmTime() skipped because of null user id");
            return 0L;
        }
        long time = mPermissionController.getUserConfirmTime(userId, timeBound);
        Log.d(TAG, "getUserConfirmTime() at User(" + userId + ") timeBound: " + timeBound
                + " totalTime: " + time);
        return time;
    }

    @Override
    public String getVersionName() {
        enforceCheckLicense("getVersionName()");
        return "MoMS.5.0.0";
    }

    @Override
    public void installPackage(Uri packageUri, IPackageInstallCallback callback) {
        enforceCheckLicense("installPackage()");
        long token = Binder.clearCallingIdentity();
        mPackageController.installPackage(packageUri, callback);
        Binder.restoreCallingIdentity(token);
    }

    @Override
    public void killPid(int pid, String reason) {
        android.app.IActivityManager am = android.app.ActivityManagerNative.getDefault();
        Log.d(TAG, "killPid : " + pid + ", reason = " + reason);
        long token = Binder.clearCallingIdentity();
        try {
            am.killPids(new int[] {pid}, reason, true);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        Binder.restoreCallingIdentity(token);
    }

    @Override
    public void registerManagerApListener(int controllerID, IBinder listener) {
        ControllerBase controller = getMobileManagerController(controllerID);
        Log.d(TAG, "registerManagerApListener() controllerID: " + controllerID + " listener: "
                + listener);
        if (controller != null) {
            controller.registerManagerApListener(listener);
        } else {
            Log.e(TAG, "registerManagerApListener() failed!");
        }
    }

    @Override
    public void setBootReceiverEnabledSetting(String packageName, boolean enable) {
        enforceCheckLicense("setBootReceiverEnabledSetting()");
        mReceiverController.setBootReceiverEnabledSetting(Binder.getCallingUid(), packageName,
                enable);
    }

    @Override
    public void setBootReceiverEnabledSettings(List<ReceiverRecord> records) {
        enforceCheckLicense("setBootReceiverEnabledSettings()");
        mReceiverController.setBootReceiverEnabledSettings(Binder.getCallingUid(), records);
    }

    @Override
    public void setFirewallPolicy(int uid, int policy, boolean enable) {
    }

    @Override
    public void setNotificationCache(List<NotificationCacheRecord> records) {
        enforceCheckLicense("setNotificationCache()");
        mNotificationController.setNotificationCache(records);
    }

    @Override
    public void setNotificationEnabledSetting(String packageName, boolean enable) {
        enforceCheckLicense("setNotificationEnabledSetting()");
        mNotificationController.setNotificationEnabledSetting(packageName, enable);
    }

    @Override
    public void setPermissionCache(List<PermissionRecord> records) {
        mPermissionController.resetCache(UserHandle.getCallingUserId());
        mPermissionController.setPermissionRecord(UserHandle.getCallingUserId(), records);
    }

    @Override
    public void setPermissionRecord(PermissionRecord record) {
        mPermissionController.setPermissionRecord(UserHandle.getCallingUserId(), record);
    }

    @Override
    public void setPermissionRecords(List<PermissionRecord> records) {
        mPermissionController.setPermissionRecord(UserHandle.getCallingUserId(), records);
    }

    @Override
    public void startMonitorBootReceiver(String packageName) {
        enforceCallingUid("startMonitorBootReceiver(" + packageName + ")");
        mReceiverController.startMontor(packageName);
    }

    @Override
    public void stopMonitorBootReceiver(String packageName) {
        enforceCallingUid("stopMonitorBootReceiver(" + packageName + ")");
        mReceiverController.stopMontor(packageName);
    }

    @Override
    public void systemReady() {
        Log.d(TAG, "systemReady()");
        for (Map.Entry<Integer, ControllerBase> entry : mControllers.entrySet()) {
            entry.getValue().systemReady(mContext);
        }
        isSystemReady = true;
    }

    @Override
    public int triggerManagerApListener(int controllerID, Bundle data, int defaultResult) {
        enforceCallingUid("triggerManagerApListener");
        ControllerBase controller = getMobileManagerController(controllerID);
        Log.d(TAG, "triggerManagerApListener() controllerID: " + controllerID + " defaultResult: "
                + defaultResult);
        if (controller == null) {
            Log.e(TAG, "triggerManagerApListener() failed!");
            return 0;
        }
        IBinder listener = controller.getManagerApListener();
        if (listener != null) {
            return ListenerTriggerHelper.triggerListener(controllerID, listener, data,
                    defaultResult);
        }
        Log.e(TAG, "Trigger null listener!");
        return 0;
    }

    @Override
    public void triggerManagerApListenerAsync(int controllerID, Bundle data, int defaultResult,
            IBinder callback) {
        enforceCallingUid("triggerManagerApListenerAsync");
        if (callback == null) {
            Log.e(TAG, "triggerManagerApListenerAsync() with null callback");
            return;
        }
        ControllerBase controller = getMobileManagerController(controllerID);
        Log.d(TAG, "triggerManagerApListenerAsync() controllerID: " + controllerID
                + " defaultResult: " + defaultResult);
        if (controller == null) {
            Log.e(TAG, "triggerManagerApListener() failed!");
            return;
        }
        IBinder listener = controller.getManagerApListener();
        if (listener != null) {
            ListenerTriggerHelper.triggerListenerAsync(controllerID, listener, data, defaultResult,
                    callback);
        } else {
            Log.e(TAG, "Trigger null listener!");
        }
    }
}