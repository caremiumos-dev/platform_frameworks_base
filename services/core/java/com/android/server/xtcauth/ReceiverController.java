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
import android.content.Context;
import android.content.pm.IPackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.os.Handler;
import android.os.UserHandle;
import android.util.Log;

import com.xtc.auth.BootReceiverPolicy;
import com.xtc.auth.ReceiverRecord;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Controller that filters the boot-completed broadcast receivers of third-party
 * applications based on the management application's policy.
 *
 * @hide
 */
class ReceiverController extends ControllerBase implements IPackageBasedController {
    static final String TAG = "ReceiverController";
    static final boolean DEBUG = true;

    private static Context mContext = null;
    private static ControllerBase mInstance = null;
    private static boolean mMonitorEnabled = true;

    private Handler mHandler;
    private IActivityManager mAm = null;
    private IPackageManager mPm = null;
    private final Map<Integer, ReceiverRecordHelper> mRecordHelperList = new HashMap<>();

    private ReceiverController(Context context, Handler handler) {
        mContext = context;
        mHandler = handler;
        List<Integer> identities = ManagerIdentityHelper.getIdentityList();
        if (identities != null) {
            for (Integer identity : identities) {
                if (ManagerIdentityHelper.getManagerIdentityInstalled(identity)) {
                    initRecordHelper(identity);
                }
            }
        }
        startMontor("Normal Bootup Start");
    }

    public static final ControllerBase getInstance(Context context, Handler handler) {
        if (mInstance == null) {
            mInstance = new ReceiverController(context, handler);
        }
        return mInstance;
    }

    private boolean checkStrictPolicyAllowed(String action, int userId, String packageName) {
        synchronized (mRecordHelperList) {
            for (Integer identity : mRecordHelperList.keySet()) {
                if (!mRecordHelperList.get(identity).getReceiverDataEnabled(userId, packageName)) {
                    Log.d(TAG, "Identity " + identity + " denied " + action + " to package: "
                            + packageName + " at User(" + userId + ")");
                    return false;
                }
            }
        }
        return true;
    }

    private void initRecordHelper(int identity) {
        synchronized (mRecordHelperList) {
            if (mRecordHelperList.containsKey(identity)) {
                return;
            }
            ManagerIdentityHelper.ManagerIdentity manager =
                    ManagerIdentityHelper.getManagerIdentityObject(identity);
            if (manager == null) {
                Log.e(TAG, "initRecordHelper() failed with identity: " + identity);
                return;
            }
            ReceiverRecorder recorder = new ReceiverRecorder(manager);
            ReceiverRecordHelper helper = new ReceiverRecordHelper(recorder, mContext, mHandler,
                    getUserManagerService(), getPackageManagerService());
            mRecordHelperList.put(recorder.getIdentity(), helper);
            helper.initReceiverList();
            Log.d(TAG, "init ReceiverRecordHelper for identity: " + recorder.getIdentity());
        }
    }

    private void updateReceiverList(int userId, boolean readFile) {
        synchronized (mRecordHelperList) {
            for (Integer identity : mRecordHelperList.keySet()) {
                mRecordHelperList.get(identity).updateReceiverCache(userId, readFile);
            }
        }
    }

    public void filterReceiver(String action, List<ResolveInfo> receivers, int userId) {
        if (!mMonitorEnabled) {
            return;
        }
        if (action == null) {
            Log.e(TAG, "filterReceiver() ignored with null action");
            return;
        }
        if (receivers == null) {
            return;
        }
        if (!isValidUserId(userId)) {
            Log.e(TAG, "filterReceiver() ignored with invalid userId: " + userId);
            return;
        }
        if (BootReceiverPolicy.match(action)) {
            Iterator<ResolveInfo> iterator = receivers.iterator();
            while (iterator.hasNext()) {
                ResolveInfo info = iterator.next();
                if (info.activityInfo != null
                        && !checkStrictPolicyAllowed(action, userId, info.activityInfo.packageName)) {
                    iterator.remove();
                }
            }
        }
    }

    public boolean getBootReceiverEnabledSetting(int uid, String packageName) {
        int userId = UserHandle.getUserId(uid);
        int identity = ManagerIdentityHelper.getManagerIdentity(uid);
        Log.d(TAG, "getBootReceiverEnabledSetting(" + packageName + ") at User(" + userId
                + ") with identity: " + identity);
        ReceiverRecordHelper helper = mRecordHelperList.get(identity);
        if (helper != null) {
            return helper.getReceiverDataEnabled(userId, packageName);
        }
        return true;
    }

    public List<ReceiverRecord> getBootReceiverList(int uid) {
        ReceiverRecordHelper helper =
                mRecordHelperList.get(ManagerIdentityHelper.getManagerIdentity(uid));
        return helper.getBootReceiverList(UserHandle.getUserId(uid));
    }

    public void setBootReceiverEnabledSetting(int uid, String packageName, boolean enabled) {
        int identity = ManagerIdentityHelper.getManagerIdentity(uid);
        int userId = UserHandle.getUserId(uid);
        Log.d(TAG, "setBootReceiverEnabledSetting(" + packageName + ", " + enabled + ") at User("
                + userId + ") with identity: " + identity);
        ReceiverRecordHelper helper = mRecordHelperList.get(identity);
        if (helper == null || helper.setReceiverDataEnabled(userId, packageName, enabled)) {
            return;
        }
        Log.w(TAG, uid + "setBootReceiverEnabledSetting(" + packageName + ") failed!");
    }

    public void setBootReceiverEnabledSettings(int uid, List<ReceiverRecord> records) {
        int identity = ManagerIdentityHelper.getManagerIdentity(uid);
        int userId = UserHandle.getUserId(uid);
        Log.d(TAG, "setBootReceiverEnabledSettings() at User(" + userId + ") with identity: "
                + identity);
        ReceiverRecordHelper helper = mRecordHelperList.get(identity);
        if (helper != null) {
            helper.setReceiverDataEnabledBatch(userId, records);
        }
    }

    public void startMontor(String reason) {
        Log.d(TAG, "startMontor(" + reason + ")");
        mMonitorEnabled = true;
    }

    public void stopMontor(String reason) {
        Log.d(TAG, "stopMontor(" + reason + ")");
        mMonitorEnabled = false;
    }

    public void updateReceiverList(List<UserInfo> users) {
        if (users != null) {
            for (UserInfo user : users) {
                updateReceiverList(user.id, true);
            }
        }
    }

    @Override
    public boolean clearAllSettings() {
        Log.d(TAG, "clearAllSettings()");
        return true;
    }

    @Override
    public boolean clearPackageSettings(String packageName) {
        Log.d(TAG, "clearPackageSettings(" + packageName + ")");
        return true;
    }

    @Override
    public void onPackageStatusChange(int uid, String packageName, int status) {
        Log.d(TAG, "onPackageStatusChange(" + packageName + ", " + status + ")");
        int userId = UserHandle.getUserId(uid);
        switch (status) {
            case 0:
                Log.v(TAG, "PACKAGE_ADDED: " + packageName + " at User(" + userId + ")");
                synchronized (mRecordHelperList) {
                    int identity = ManagerIdentityHelper.getManagerIdentity(packageName);
                    if (ManagerIdentityHelper.isBootControlIdetity(identity)) {
                        Log.d(TAG, "initRecordHelper(" + identity + ")");
                        initRecordHelper(identity);
                    }
                    for (Integer key : mRecordHelperList.keySet()) {
                        ReceiverRecordHelper helper = mRecordHelperList.get(key);
                        if (helper.matchBootPolicy(userId, packageName)) {
                            helper.addReceiverRecord(userId, packageName);
                        }
                    }
                }
                break;
            case 1:
                Log.v(TAG, "PACKAGE_REMOVED: " + packageName + " at User(" + userId + ")");
                synchronized (mRecordHelperList) {
                    int identity = ManagerIdentityHelper.getManagerIdentity(packageName);
                    for (Integer key : mRecordHelperList.keySet()) {
                        mRecordHelperList.get(key).removeReceiverRecordWrite(userId, packageName);
                    }
                    if (ManagerIdentityHelper.isBootControlIdetity(identity)) {
                        Log.d(TAG, "uninitReceiverCache(" + identity + ")");
                        ReceiverRecordHelper helper = mRecordHelperList.get(identity);
                        if (helper != null && helper.uninitReceiverCache(userId)) {
                            mRecordHelperList.remove(identity);
                        }
                    }
                }
                break;
            case 2:
                Log.v(TAG, "PACKAGE_UPDATED: " + packageName);
                synchronized (mRecordHelperList) {
                    for (Integer key : mRecordHelperList.keySet()) {
                        ReceiverRecordHelper helper = mRecordHelperList.get(key);
                        if (helper.matchBootPolicy(userId, packageName)) {
                            helper.addReceiverRecordWrite(userId, packageName);
                        } else {
                            helper.removeReceiverRecordWrite(userId, packageName);
                        }
                    }
                }
                break;
            case 3:
                Log.v(TAG, "PACKAGE_EXT_AVAILABLE: " + packageName);
                break;
            case 4:
                Log.v(TAG, "PACKAGE_EXT_UNAVAILABLE: " + packageName);
                break;
            default:
                break;
        }
    }

    @Override
    public void onUserStatusChange(int userId, int status) {
        switch (status) {
            case 0:
                if (!isValidUserId(userId)) {
                    return;
                }
                Log.d(TAG, "USER_ADDED: Init cache for User(" + userId + ")");
                synchronized (mRecordHelperList) {
                    for (Integer key : mRecordHelperList.keySet()) {
                        mRecordHelperList.get(key).initReceiverCache(userId);
                    }
                }
                break;
            case 1:
                if (!isValidUserId(userId)) {
                    return;
                }
                Log.d(TAG, "USER_REMOVED: Remove cache for User(" + userId + ")");
                synchronized (mRecordHelperList) {
                    for (Integer key : mRecordHelperList.keySet()) {
                        mRecordHelperList.get(key).removeReceiverCache(userId);
                    }
                }
                break;
            case 2:
                if (isValidUserId(userId)) {
                    Log.d(TAG, "USER_SWITCHED: Update cache for User(" + userId + ")");
                    updateReceiverList(userId, false);
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void dump(PrintWriter pw, MobileManagerService.DumpState dumpState) {
        pw.println("ReceiverController:");
    }

    @Override
    public void systemReady(Context context) {
        mPm = getPackageManagerService();
        mAm = getActivityManagerService();
    }
}