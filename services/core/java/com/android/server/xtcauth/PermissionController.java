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

import android.app.IActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.content.pm.UserInfo;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IUserManager;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Log;

import com.xtc.auth.IMobileConnectionCallback;
import com.xtc.auth.IRequestedPermissionCallback;
import com.xtc.auth.Permission;
import com.xtc.auth.PermissionRecord;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Controller implementing the runtime-permission override used by the mobile
 * manager. It can answer (and silently grant/deny) permission checks of
 * third-party applications on behalf of the management application.
 *
 * <p>WARNING: This is the core of the vendor permission-control mechanism. It
 * allows an allow-listed management application to override Android's runtime
 * permission model for other applications without user interaction, and it
 * keeps an interaction-time history to decide when a confirmation is
 * "legitimate". Restored for completeness of the framework rewrite; the
 * resulting loss of user control is not endorsed.
 *
 * @hide
 */
class PermissionController extends MultiUserControllerBase implements IPermissionController {
    public static final String TAG = "PermissionController";
    public static final int ID = 0;

    public static final String PARAM_PACKAGE = "param_package";
    public static final String PARAM_PERMISSION_RECORD = "param_permission_record";
    public static final String PARAM_FLAG = "param_flag";
    public static final String PARAM_UID = "param_uid";
    public static final String PARAM_DATA = "param_data";

    private static final String ACTION_PREBOOT_IPO = "android.intent.action.ACTION_PREBOOT_IPO";

    private static boolean DEBUG = true;
    private static ControllerBase mInstance = null;

    private IActivityManager mAM;
    private IPackageManager mPM;
    private IUserManager mUM;
    private final PermissionRecordHelper mCacheHelper;
    private final PermissionHistoryHelper mHistoryHelper;
    private final ManagerIdentityHelper mIdentityHelper;
    private final PackageController mPackageController;
    private final RunTimePermissions mRunTimePermissions = new RunTimePermissions();

    private Context mContext = null;
    private boolean mCacheInitDone = false;
    private boolean mSystemReady = false;
    final IntentFilter mFilter = new IntentFilter();

    private final Map<Integer, List<ManagerControlToken>> mManagerCtlTokens = new HashMap<>();
    private final Map<String, List<PermissionRecord>> mPendingSetting = new HashMap<>();

    private class PreBootReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_PREBOOT_IPO.equals(intent.getAction())) {
                requestAttachment();
            }
        }
    }

    private PermissionController(PackageController packageController) {
        mCacheHelper = PermissionRecordHelper.getInstance();
        mHistoryHelper = PermissionHistoryHelper.getInstance();
        mIdentityHelper = ManagerIdentityHelper.getInstance();
        mPM = getPackageManagerService();
        mAM = getActivityManagerService();
        mUM = getUserManagerService();
        mPackageController = packageController;
        initCacheForAllUser();
    }

    public static final ControllerBase getInstance(PackageController packageController) {
        if (mInstance == null) {
            mInstance = new PermissionController(packageController);
            Log.d(TAG, "Create MobileManagerController: PermissionController");
        }
        return mInstance;
    }

    public static void PRINT_DEBUG_LOG(int uid, int pid, String permissionName, String message) {
        if (DEBUG) {
            Log.d(TAG, "[" + pid + "-" + uid + "-" + permissionName + "] " + message);
        }
    }

    public static void PRINT_DEBUG_LOG(int uid, String permissionName, String message) {
        PRINT_DEBUG_LOG(uid, Binder.getCallingPid(), permissionName, message);
    }

    public static String result2String(int result) {
        switch (result) {
            case -1:
                return "DENIED";
            case 0:
                return "GRANTED";
            default:
                Log.e(TAG, "mapResult2String() with unknown result code: " + result);
                return "UNKNOWN";
        }
    }

    public static String status2String(int status) {
        switch (status) {
            case 0:
                return "GRANTED";
            case 1:
                return "DENIED";
            case 2:
                return "CHECKING";
            default:
                Log.e(TAG, "mapResult2String() with unknown status code: " + status);
                return "UNKNOWN";
        }
    }

    private void broadcastAttachment(int status, int uid, int userId) {
        Intent intent = new Intent("mom.action.MGR_CHANGE");
        intent.putExtra("mom.action.extra.status", status);
        intent.putExtra("mom.action.extra.uid", uid);
        mContext.sendBroadcastAsUser(intent, new UserHandle(userId));
    }

    private int checkPermission(String permissionName, int uid, Bundle data,
            IRequestedPermissionCallback callback) {
        try {
            if (getManagerApListener(UserHandle.getUserId(uid)) == null) {
                PRINT_DEBUG_LOG(uid, permissionName,
                        "checkPermission GRANTED because listener from MOMA is null");
                onPermissionCheckResultIfExist(callback, permissionName, uid, 0);
                return 0;
            }
            String[] packages = mPM.getPackagesForUid(UserHandle.getAppId(uid));
            PRINT_DEBUG_LOG(uid, permissionName, "packages = " + packages + ", name = "
                    + mContext.getPackageManager().getNameForUid(uid));
            if (packages == null) {
                PRINT_DEBUG_LOG(uid, permissionName, "checkPermission 1 UNMONITORED");
                onPermissionCheckResultIfExist(callback, permissionName, uid, 0);
                return 0;
            }
            if (packages.length == 1) {
                return checkPermission(packages[0], uid, permissionName, data, callback);
            }
            PermissionRecord strict = mCacheHelper.getStrictPermissionForUid(
                    UserHandle.getUserId(uid), permissionName, packages);
            if (strict != null) {
                return checkPermission(strict.mPackageName, uid, permissionName, data, callback);
            }
            PRINT_DEBUG_LOG(uid, permissionName, "checkPermission 2 UNMONITORED");
            onPermissionCheckResultIfExist(callback, permissionName, uid, 0);
            return 0;
        } catch (RemoteException e) {
            PRINT_DEBUG_LOG(uid, permissionName, "getPackagesForUid() faild!");
            onPermissionCheckResultIfExist(callback, permissionName, uid, 0);
            return 0;
        }
    }

    private int checkPermission(String packageName, int uid, String permissionName, Bundle data,
            IRequestedPermissionCallback callback) {
        int result = -1;
        Permission permission = mCacheHelper.searchPermissionCache(UserHandle.getUserId(uid),
                packageName, permissionName);
        PermissionRecord record = new PermissionRecord(packageName, permissionName, 0, 0);
        PRINT_DEBUG_LOG(uid, permissionName, "checkPermission with packageName: " + packageName
                + ", p = " + permission);
        if (permission == null) {
            PRINT_DEBUG_LOG(uid, permissionName, "checkPermission UNMONITORED");
            onPermissionCheckResultIfExist(callback, permissionName, uid, 0);
            return 0;
        }
        PackageInfo packageInfo;
        try {
            packageInfo = mContext.getPackageManager().getPackageInfo(packageName, 4096);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            packageInfo = null;
        }
        boolean legacy = packageInfo == null
                || packageInfo.applicationInfo.targetSdkVersion < 23;
        PRINT_DEBUG_LOG(uid, permissionName, "checkPermission with packageName: " + packageName
                + ", legacy = " + legacy);
        int status = permission.getStatus();
        if (PermissionRecordHelper.isUserConfirmPermission(permission.getFlag())) {
            permission.setStatus(2);
            status = 2;
        }
        record.setStatus(status);
        int userId = UserHandle.getUserId(uid);
        PRINT_DEBUG_LOG(uid, permissionName, "checkPermission " + status2String(status)
                + " with packageName: " + packageName);
        switch (status) {
            case 0:
                Log.d(TAG, "XTCNetin PERMISSION_STATUS_GRANTED");
                onPermissionCheckResultIfExist(callback, permissionName, uid, 0);
                return 0;
            case 1:
                record.setStatus(1);
                if (mHistoryHelper.isPackageChecking(userId, packageName, permissionName)) {
                    PRINT_DEBUG_LOG(uid, permissionName,
                            "checkPermission DENIED because checking is still ongoing");
                    onPermissionCheckResultIfExist(callback, permissionName, uid, -1);
                    return -1;
                }
                mHistoryHelper.addHistory(userId, packageName, permissionName, status);
                if (callback == null) {
                    return triggerSyncCheck(packageName, record, permission.getFlag(), uid, data);
                }
                triggerAsyncCheck(packageName, record, permission.getFlag(), uid, data,
                        callback.asBinder());
                return 0;
            case 2:
                record.setStatus(2);
                if (mHistoryHelper.isPackageChecking(userId, packageName, permissionName)) {
                    PRINT_DEBUG_LOG(uid, permissionName,
                            "checkPermission DENIED because checking is still ongoing");
                    onPermissionCheckResultIfExist(callback, permissionName, uid, -1);
                } else {
                    mHistoryHelper.addHistory(userId, packageName, permissionName, status);
                    if (callback != null) {
                        triggerAsyncCheck(packageName, record, permission.getFlag(), uid, data,
                                callback.asBinder());
                        result = 0;
                    } else {
                        result = triggerSyncCheck(packageName, record, permission.getFlag(), uid,
                                data);
                    }
                }
                return result;
            default:
                Log.e(TAG, "Unknown permission status: " + permission.toString());
                return 0;
        }
    }

    private void clearControlToken(int userId) {
        synchronized (mManagerCtlTokens) {
            if (mManagerCtlTokens.containsKey(userId)) {
                Log.d(TAG, "Clear control token(s) for User(" + userId + ")");
                mManagerCtlTokens.remove(userId);
            }
        }
    }

    private void excludeControlToken(int uid, boolean notify) {
        int userId = UserHandle.getUserId(uid);
        synchronized (mManagerCtlTokens) {
            List<ManagerControlToken> list = mManagerCtlTokens.get(userId);
            if (list != null) {
                for (ManagerControlToken token : list) {
                    if (token.getUid() != uid && token.isEnabled()) {
                        token.setEnable(false);
                        if (notify) {
                            token.notifyConnectionEnded();
                        }
                        Log.d(TAG, "Broadcast DETACHED a permission manager(" + token.getUid()
                                + ") at User(" + userId + ")");
                        broadcastAttachment(1, token.getUid(), userId);
                    }
                }
            }
        }
        Log.d(TAG, "Broadcast ATTACHED a permission manager(" + uid + ") at User(" + userId + ")");
        broadcastAttachment(0, uid, userId);
    }

    private ManagerControlToken findControlToken(int uid, boolean enabled) {
        synchronized (mManagerCtlTokens) {
            int userId = UserHandle.getUserId(uid);
            List<ManagerControlToken> list = mManagerCtlTokens.get(userId);
            if (list != null) {
                for (ManagerControlToken token : list) {
                    if (token.getUid() == uid && token.isEnabled() == enabled) {
                        return token;
                    }
                }
            }
            return null;
        }
    }

    private void handleInitCache(String packageName, int userId) {
        Log.d(TAG, "handleInitCache(" + packageName + ") at User(" + userId + ")");
        if (!isValidUserId(userId)) {
            return;
        }
        if (!mCacheInitDone) {
            Log.d(TAG, "handleInitCache() not ready!");
            return;
        }
        PackageInfo packageInfo = PackageController.getPackageInfoByUser(userId, packageName);
        if (packageInfo == null) {
            Log.e(TAG, "null pkgInfo when package added / removed");
        } else if (PackageController.isSystemApp(packageInfo.applicationInfo)) {
            Log.d(TAG, "Ignore system application");
        } else {
            mCacheHelper.updatePermissionCache(userId, packageInfo);
        }
    }

    private void initCacheForAllUser() {
        try {
            for (UserInfo user : mUM.getUsers(false)) {
                initPermissionCache(user.id, PackageController.getInstalledPackages(user.id));
                Log.d(TAG, "initCacheForAllUser() for User(" + user.id + ")");
            }
        } catch (RemoteException e) {
            Log.e(TAG, "getUsers() failed! ", e);
        }
    }

    private void initPermControlApInner(int identity) {
        if (mContext == null) {
            Log.e(TAG, "initPermControlAp() failed due to null context");
            return;
        }
        String managerPackage = ManagerIdentityHelper.getManagerPackage(identity);
        String managerInitService = ManagerIdentityHelper.getManagerInitService(identity);
        if (PackageController.getPackageInfo(managerPackage) == null
                || managerInitService == null) {
            Log.w(TAG, "initPermControlAp(" + managerPackage + ") skipped!");
            return;
        }
        Intent intent = new Intent();
        intent.setClassName(managerPackage, managerInitService);
        mContext.startService(intent);
        Log.d(TAG, "initPermControlAp(" + managerPackage + ")");
    }

    private void initPermissionCache(int userId, List<PackageInfo> packageInfos) {
        mCacheInitDone = mCacheHelper.initPkgPermissionCache(userId, packageInfos);
    }

    private Bundle initTriggerParams(String packageName, PermissionRecord record, int flag, int uid,
            Bundle data) {
        Bundle params = new Bundle();
        params.putString(PARAM_PACKAGE, packageName);
        params.putParcelable(PARAM_PERMISSION_RECORD, record);
        params.putInt(PARAM_FLAG, flag);
        params.putInt(PARAM_UID, uid);
        params.putBundle(PARAM_DATA, data);
        return params;
    }

    private void onPermissionCheckResultIfExist(IRequestedPermissionCallback callback,
            String permissionName, int uid, int result) {
        Log.d("PermissionController ifexist", "callback=" + callback + " checkResult=" + result
                + " permissionName=" + permissionName + " uid=" + uid);
        if (callback != null) {
            PRINT_DEBUG_LOG(uid, permissionName, "checkPermission MoMS >> Caller with result:"
                    + result);
            try {
                long token = Binder.clearCallingIdentity();
                callback.onPermissionCheckResult(permissionName, uid, result);
                Binder.restoreCallingIdentity(token);
            } catch (RemoteException e) {
                Log.e(TAG, "onPermissionCheckResult() failed", e);
            }
            PRINT_DEBUG_LOG(uid, permissionName, "checkPermission MoMS << Caller");
        }
    }

    private void purnPermissionCache(int userId, List<PackageInfo> packageInfos) {
        mCacheHelper.purnPkgPermissionCache(userId, packageInfos);
    }

    private void requestAttachment() {
        for (Integer identity : ManagerIdentityHelper.getPermControlIdentityList()) {
            initPermControlApInner(identity);
        }
    }

    private void resumeControlToken(int uid) {
        int userId = UserHandle.getUserId(uid);
        Log.d(TAG, "Broadcast DETACHED a permission manager(" + uid + ") at User(" + userId + ")");
        broadcastAttachment(1, uid, userId);
        synchronized (mManagerCtlTokens) {
            List<ManagerControlToken> list = mManagerCtlTokens.get(userId);
            if (list != null) {
                for (ManagerControlToken token : list) {
                    if (token.getUid() != uid && !token.isEnabled()) {
                        token.notifyConnectionResume();
                    }
                }
            }
        }
    }

    private void triggerAsyncCheck(String packageName, PermissionRecord record, int flag, int uid,
            Bundle data, IBinder callback) {
        int userId = UserHandle.getUserId(uid);
        Bundle params = initTriggerParams(packageName, record, flag, uid, data);
        ListenerTriggerHelper.getInstance();
        ListenerTriggerHelper.triggerListenerAsync(ID, getManagerApListener(userId), params, -1,
                callback);
    }

    private int triggerSyncCheck(String packageName, PermissionRecord record, int flag, int uid,
            Bundle data) {
        int userId = UserHandle.getUserId(uid);
        Bundle params = initTriggerParams(packageName, record, flag, uid, data);
        ListenerTriggerHelper.getInstance();
        return ListenerTriggerHelper.triggerListener(ID, getManagerApListener(userId), params, -1);
    }

    public boolean attach(IMobileConnectionCallback callback, int uid, int currentIdentity,
            int checkIdentity) {
        UserHandle.getAppId(uid);
        int userId = UserHandle.getUserId(uid);
        if (!ManagerIdentityHelper.isPermControlIdentity(checkIdentity)) {
            Log.e(TAG, "Attach failed with invalid uid: " + uid + " identity: " + checkIdentity);
            return false;
        }
        synchronized (mManagerCtlTokens) {
            if (findControlToken(uid, true) != null) {
                Log.d(TAG, "Already attached from uid: " + uid);
                return true;
            }
            List<ManagerControlToken> list = mManagerCtlTokens.get(userId);
            if (list == null) {
                list = new ArrayList<>();
                mManagerCtlTokens.put(userId, list);
            }
            boolean result;
            if (checkIdentity > currentIdentity) {
                excludeControlToken(uid, true);
                ManagerControlToken token = findControlToken(uid, false);
                if (token != null) {
                    Log.d(TAG, "Attach an exist manager control token from uid: " + uid
                            + " with identity: " + checkIdentity);
                    token.setCallback(callback);
                } else {
                    Log.d(TAG, "Attach a new manager control token from uid: " + uid
                            + " with identity: " + checkIdentity);
                    token = new ManagerControlToken(uid, checkIdentity, callback);
                    list.add(token);
                }
                token.setEnable(true);
                result = true;
            } else if (checkIdentity == currentIdentity) {
                ManagerControlToken token = findControlToken(uid, true);
                if (token != null) {
                    token.setCallback(callback);
                } else {
                    Log.d(TAG, "Attach new uid " + uid + " with existing identity: "
                            + checkIdentity);
                    token = new ManagerControlToken(uid, checkIdentity, callback);
                    list.add(token);
                }
                token.setEnable(true);
                result = true;
            } else {
                Log.w(TAG, "Attach failed due to minor identity: " + checkIdentity);
                ManagerControlToken token = findControlToken(uid, false);
                if (token != null) {
                    token.setCallback(callback);
                    token.setEnable(false);
                } else {
                    list.add(new ManagerControlToken(uid, checkIdentity, callback));
                }
                result = false;
            }
            return result;
        }
    }

    public boolean checkAttachment(int uid) {
        synchronized (mManagerCtlTokens) {
            if (findControlToken(uid, true) == null) {
                Log.d(TAG, "checkAttachment() failed with uid: " + uid);
                return false;
            }
            return true;
        }
    }

    public int checkPermission(String permissionName, int uid, Bundle data) {
        if (!mCacheInitDone) {
            return 0;
        }
        PRINT_DEBUG_LOG(uid, permissionName, "checkPermission START");
        if (UserHandle.getAppId(uid) > 1000) {
            return checkPermission(permissionName, uid, data, null);
        }
        PRINT_DEBUG_LOG(uid, permissionName, "checkPermision GRANTED with system uid");
        return 0;
    }

    public void checkPermissionAsync(String permissionName, int uid, Bundle data,
            IRequestedPermissionCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("Async check with null callback");
        }
        if (!mCacheInitDone) {
            Log.e(TAG, "callback=" + callback + " checkResult=0 permissionName=" + permissionName
                    + " uid=" + uid);
            onPermissionCheckResultIfExist(callback, permissionName, uid, 0);
            return;
        }
        PRINT_DEBUG_LOG(uid, permissionName, "checkPermissionAsync START");
        if (UserHandle.getAppId(uid) > 1000) {
            checkPermission(permissionName, uid, data, callback);
        } else {
            PRINT_DEBUG_LOG(uid, permissionName, "checkPermisionAsync GRANTED with system uid");
            onPermissionCheckResultIfExist(callback, permissionName, uid, 0);
        }
    }

    public void detach(int uid) {
        int userId = UserHandle.getUserId(uid);
        synchronized (mManagerCtlTokens) {
            ManagerControlToken token = findControlToken(uid, true);
            if (token != null) {
                Log.d(TAG, "detach control token from uid: " + uid + " with identity: "
                        + token.getIdentity());
                List<ManagerControlToken> list = mManagerCtlTokens.get(userId);
                list.remove(token);
                resumeControlToken(uid);
                if (list.size() == 0) {
                    mManagerCtlTokens.remove(userId);
                }
            }
        }
    }

    public void dump() {
        if (mCacheInitDone) {
            mCacheHelper.dump();
        }
        Log.d(TAG, "[Dump Current Attach Status]");
        synchronized (mManagerCtlTokens) {
            for (Integer userId : mManagerCtlTokens.keySet()) {
                List<ManagerControlToken> list = mManagerCtlTokens.get(userId);
                if (list != null && list.size() > 0) {
                    for (ManagerControlToken token : list) {
                        Log.d(TAG, " - " + token.toString());
                    }
                }
            }
        }
    }

    @Override
    public synchronized void enableController(boolean enable) {
        super.enableController(enable);
        Log.d(TAG, Binder.getCallingUid() + " enableController() enable: " + enable + " at User("
                + UserHandle.getCallingUserId() + ")");
        if (getManagerApListener() == null && enable) {
            throw new IllegalArgumentException(
                    "Enable permission controller without register listener!");
        }
        DEBUG = enable;
    }

    public ManagerControlToken getCurrentControlToken(int userId) {
        synchronized (mManagerCtlTokens) {
            List<ManagerControlToken> list = mManagerCtlTokens.get(userId);
            if (list == null || list.size() <= 0) {
                return null;
            }
            for (ManagerControlToken token : list) {
                if (token.isEnabled()) {
                    return token;
                }
            }
            return null;
        }
    }

    public int getCurrentIdentity(int uid) {
        ManagerControlToken token = getCurrentControlToken(UserHandle.getUserId(uid));
        if (token != null) {
            return token.getIdentity();
        }
        return 0;
    }

    public List<Permission> getPackageGrantedPermissions(int userId, String packageName) {
        if (packageName != null && packageName.length() > 0) {
            return mCacheHelper.getPackageCachePermissions(userId, packageName);
        }
        Log.d(TAG, "Invalid packageName: " + packageName);
        return null;
    }

    public String getParentPermission(String subPermissionName) {
        Permission parent = mCacheHelper.getParentPermission(subPermissionName);
        if (parent != null) {
            return parent.mPermissionName;
        }
        return null;
    }

    public long getUserConfirmTime(int userId, long timeBound) {
        long time = mHistoryHelper.getInteractionTime(userId, timeBound);
        if (time >= 1000) {
            return time;
        }
        Log.d(TAG, "getUserConfirmTime() fix extending time from " + time + " to 0");
        return 0L;
    }

    public void initPermControlAp(int identity) {
        if (ManagerIdentityHelper.isPermControlIdentity(identity)) {
            initPermControlApInner(identity);
        }
    }

    boolean isRuntimePermission(int protectionLevel) {
        return (protectionLevel & 15) == 1;
    }

    @Override
    public void onPackageStatusChange(int uid, String packageName, int status) {
        int userId = UserHandle.getUserId(uid);
        Log.d(TAG, "onPackageStatusChange() status: " + status + " packageName: " + packageName
                + " at User(" + userId + ")");
        if (!isValidUserId(userId)) {
            return;
        }
        switch (status) {
            case 0:
            case 2:
                handleInitCache(packageName, userId);
                break;
            case 1:
                mCacheHelper.removePermissionCache(userId, packageName);
                Log.d(TAG, "Remove cache for " + packageName + " at User(" + userId + ")");
                ManagerControlToken token = getCurrentControlToken(userId);
                if (token == null || !packageName.equals(
                        ManagerIdentityHelper.getManagerPackage(token.getIdentity()))) {
                    return;
                }
                Log.d(TAG, "Uninstall Permission Control App: " + packageName + " at User("
                        + userId + ")");
                detach(uid);
                break;
            case 3:
                if (mCacheHelper.getPackageCachePermissions(userId, packageName) == null) {
                    handleInitCache(packageName, userId);
                    synchronized (mPendingSetting) {
                        if (mPendingSetting.containsKey(packageName)) {
                            Log.d(TAG, "Set pending settings for package: " + packageName);
                            for (PermissionRecord record : mPendingSetting.get(packageName)) {
                                setPermissionRecord(userId, record);
                            }
                            mPendingSetting.remove(packageName);
                        }
                    }
                }
                break;
            case 4:
                break;
            default:
                Log.e(TAG, "Unkown package status!");
                break;
        }
    }

    @Override
    public void onPermissionChange(String packageName, String permissionName, int state) {
        int accept = 1;
        int status = 0;
        int callingUserId = UserHandle.getCallingUserId();
        if (packageName != null) {
            String subPermission;
            if ((packageName.startsWith("com.cttl") || packageName.startsWith("com.tsinghua"))
                    && (subPermission = mRunTimePermissions.permissionHashMap.get(
                            permissionName)) != null) {
                if (state != 0) {
                    if (state == -1) {
                        status = 1;
                        accept = 0;
                    } else {
                        accept = 0;
                        status = 2;
                    }
                }
                setPermissionRecord(callingUserId,
                        new PermissionRecord(packageName, subPermission, status, accept));
                Intent intent = new Intent("mom.action.PERMISSION_CHANGE");
                intent.putExtra("packageName", packageName);
                intent.putExtra("subPermission", subPermission);
                intent.putExtra("status", status);
                intent.putExtra("accept", accept);
                mContext.sendBroadcast(intent);
            }
        }
    }

    @Override
    public void onUserStatusChange(int userId, int status) {
        switch (status) {
            case 0:
                if (isValidUserId(userId)) {
                    Log.d(TAG, "Init cache for User(" + userId + ")");
                    initPermissionCache(userId, PackageController.getInstalledPackages(userId));
                }
                break;
            case 1:
                if (isValidUserId(userId)) {
                    Log.d(TAG, "Remove cache for User(" + userId + ")");
                    mCacheHelper.removePermissionCacheByUser(userId);
                    clearControlToken(userId);
                }
                break;
            case 2:
                if (isValidUserId(userId)) {
                    Log.d(TAG, "Purn cache for User(" + userId + ")");
                    purnPermissionCache(userId, PackageController.getInstalledPackages(userId));
                }
                break;
            default:
                break;
        }
    }

    public void resetCache(int userId) {
        if (isValidUserId(userId)) {
            mCacheHelper.resetPermissionCache(userId);
        }
    }

    public void setPermissionRecord(int userId, PermissionRecord record) {
        if (record == null || !isValidUserId(userId)) {
            Log.w(TAG, Binder.getCallingUid() + "setPermissionRecord() with null record!");
            return;
        }
        Permission permission = mCacheHelper.searchPermissionCache(userId, record.mPackageName,
                record.mPermissionName);
        if (permission == null) {
            if (!PackageController.isPendingPackage(record.mPackageName)) {
                Log.e(TAG, "Unknown permission setting: " + record.toString());
                return;
            }
            Log.d(TAG, Binder.getCallingUid() + "Set pending " + record.toString());
            synchronized (mPendingSetting) {
                if (mPendingSetting.containsKey(record.mPackageName)) {
                    mPendingSetting.get(record.mPackageName).add(record);
                } else {
                    ArrayList<PermissionRecord> list = new ArrayList<>();
                    list.add(record);
                    mPendingSetting.put(record.mPackageName, list);
                }
            }
            return;
        }
        if ((permission.getFlag() & 4) != 0) {
            Log.w(TAG, "Ignore setting user-confirm permission record:" + record.toString());
            return;
        }
        permission.setStatus(record.getStatus());
        Permission parent = mCacheHelper.getParentPermission(record.mPermissionName);
        if (parent != null) {
            try {
                PermissionInfo info = mContext.getPackageManager()
                        .getPermissionInfo(parent.mPermissionName, 0);
                if (info == null || !isRuntimePermission(info.protectionLevel)) {
                    Log.w(TAG, "permission = " + parent + " is not a changeable permission type");
                    return;
                }
                Log.d(TAG, "setPermissionRecord record=" + record);
                if (record.getStatus() == 0) {
                    Log.d(TAG, "setPermissionRecord: grantPermission");
                } else if (record.getStatus() == 1 || record.mPackageName.startsWith("com.cttl")) {
                    Log.w(TAG, "setPermissionRecord: revokePermission");
                }
                PackageInfo packageInfo = PackageController.getPackageInfoByUser(userId,
                        record.mPackageName);
                String[] requestedPermissions =
                        packageInfo != null ? packageInfo.requestedPermissions : null;
                Log.d(TAG, "setPermissionRecord requestedPermissions="
                        + java.util.Arrays.toString(requestedPermissions));
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    public void setPermissionRecord(int userId, List<PermissionRecord> records) {
        if (records == null) {
            Log.w(TAG, "setPermissionCache() with null setting!");
            return;
        }
        for (PermissionRecord record : records) {
            setPermissionRecord(userId, record);
        }
    }

    @Override
    public void systemReady(Context context) {
        mContext = context;
        requestAttachment();
        mFilter.addAction(ACTION_PREBOOT_IPO);
        context.registerReceiver(new PreBootReceiver(), mFilter);
        mSystemReady = true;
    }
}