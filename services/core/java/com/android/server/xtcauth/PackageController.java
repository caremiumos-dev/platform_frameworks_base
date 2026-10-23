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

import android.app.ActivityManagerNative;
import android.app.IActivityManager;
import android.app.PackageInstallObserver;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageDeleteObserver;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.ParceledListSlice;
import android.net.Uri;
import android.os.Binder;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Log;

import com.xtc.auth.IPackageInstallCallback;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller exposing package install/delete/force-stop operations to the
 * management application.
 *
 * @hide
 */
class PackageController extends ControllerBase {
    public static final String TAG = "PackageController";
    static final boolean DEBUG = true;
    static boolean localLOGV = true;

    private static ControllerBase mInstance = null;
    private static IActivityManager mAm = null;
    private static IPackageManager mPm = null;
    private static boolean mInitPendingPackageDone = false;
    private static List<String> mPendingPackageInfos = new ArrayList<>();
    private static PackageDeleteObserver mPackageDeleteObserver = null;

    class PackageDeleteObserver extends IPackageDeleteObserver.Stub {
        @Override
        public void packageDeleted(String packageName, int returnCode) {
            Log.d(TAG, "packageDeleted(" + packageName + ", " + returnCode + ")");
        }
    }

    private static class PackageInstallObserverWrapper extends PackageInstallObserver {
        private final IPackageInstallCallback mCallback;

        PackageInstallObserverWrapper(IPackageInstallCallback callback) {
            mCallback = callback;
        }

        @Override
        public void onPackageInstalled(String basePackageName, int returnCode, String msg,
                android.os.Bundle extras) {
            Log.d(TAG, "packageInstalled(" + basePackageName + ", " + returnCode + ")");
            try {
                mCallback.onPackageInstalled(basePackageName, returnCode);
            } catch (RemoteException e) {
            }
        }
    }

    private PackageController() {
        mAm = getActivityManagerService();
        mPm = getPackageManagerService();
        initPendingPackageInfos();
    }

    public static final ControllerBase getInstance() {
        if (mInstance == null) {
            mInstance = new PackageController();
        }
        return mInstance;
    }

    public static int getCurrentUser() {
        try {
            return mAm.getCurrentUser().id;
        } catch (RemoteException e) {
            Log.e(TAG, "getCurrentUser() failed", e);
            return 55536;
        }
    }

    public static List<PackageInfo> getInstalledPackages(int userId) {
        return getInstalledPackages(userId, false);
    }

    private static List<PackageInfo> getInstalledPackages(int userId, boolean pending) {
        Log.d(TAG, "getInstalledPackages() at user(" + userId + ")");
        ArrayList<PackageInfo> result = new ArrayList<>();
        try {
            ParceledListSlice<ApplicationInfo> slice = mPm.getInstalledApplications(8704, userId);
            if (slice != null) {
                List<ApplicationInfo> list = slice.getList();
                if (list != null) {
                    for (ApplicationInfo info : list) {
                        if (isSystemApp(info)) {
                            continue;
                        }
                        PackageInfo packageInfo = mPm.getPackageInfo(info.packageName, 4096, userId);
                        if (packageInfo != null) {
                            result.add(packageInfo);
                        } else if (pending) {
                            Log.w(TAG, "Pending the un-ready package: " + info.packageName);
                            mPendingPackageInfos.add(info.packageName);
                        }
                    }
                }
            }
        } catch (RemoteException e) {
            Log.e(TAG, "getPackageInfo() failed!", e);
        }
        return result;
    }

    public static PackageInfo getPackageInfo(String packageName) {
        long token = Binder.clearCallingIdentity();
        try {
            return mPm.getPackageInfo(packageName, 4096, mAm.getCurrentUser().id);
        } catch (RemoteException e) {
            Log.e(TAG, "getPackageInfo() failed!", e);
            return null;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public static PackageInfo getPackageInfoByUser(int userId, String packageName) {
        try {
            return mPm.getPackageInfo(packageName, 4096, userId);
        } catch (RemoteException e) {
            Log.e(TAG, "getPackageInfoByUser() failed! with userId: " + userId, e);
            return null;
        }
    }

    private static void initPendingPackageInfos() {
        synchronized (mPendingPackageInfos) {
            getInstalledPackages(getCurrentUser(), true);
            mInitPendingPackageDone = true;
        }
    }

    public static boolean isCoreApp(String packageName) {
        PackageInfo packageInfo = getPackageInfo(packageName);
        if (packageInfo == null || packageInfo.applicationInfo == null) {
            return false;
        }
        int uid = packageInfo.applicationInfo.uid;
        return uid == 1000 || uid == 1001 || uid == 1002 || uid == 1027 || uid == 1007;
    }

    public static boolean isPendingPackage(String packageName) {
        return true;
    }

    public static boolean isSystemApp(int userId, String packageName) {
        PackageInfo packageInfo = getPackageInfoByUser(userId, packageName);
        if (packageInfo != null && packageInfo.applicationInfo != null) {
            return isSystemApp(packageInfo.applicationInfo);
        }
        Log.d(TAG, "isSystemApp() return false with null packageName");
        return false;
    }

    public static boolean isSystemApp(ApplicationInfo info) {
        return (info.flags & ApplicationInfo.FLAG_SYSTEM) != 0
                || UserHandle.getAppId(info.uid) == 1000;
    }

    public static boolean isSystemApp(PackageInfo packageInfo) {
        if (packageInfo.applicationInfo != null) {
            return isSystemApp(packageInfo.applicationInfo);
        }
        Log.d(TAG, "isSystemApp() return false with null app info");
        return false;
    }

    public static void removePendingPackage(String packageName) {
        if (mPendingPackageInfos != null) {
            synchronized (mPendingPackageInfos) {
                mPendingPackageInfos.remove(packageName);
            }
        }
    }

    public void deletePackage(String packageName) {
        Log.v(TAG, "deletePackage: " + packageName);
        if (isCoreApp(packageName)) {
            Log.w(TAG, "Ignore core package: " + packageName);
            return;
        }
        if (mPackageDeleteObserver == null) {
            mPackageDeleteObserver = new PackageDeleteObserver();
        }
        try {
            mPm.deletePackageAsUser(packageName, -1, mPackageDeleteObserver, getCurrentUser(), 2);
        } catch (RemoteException e) {
        }
    }

    public void forceStopPackage(String packageName) {
        Log.v(TAG, "forceStopPackage: " + packageName);
        if (isCoreApp(packageName)) {
            Log.w(TAG, "Ignore core package: " + packageName);
            return;
        }
        try {
            mAm.forceStopPackage(packageName, mAm.getCurrentUser().id);
        } catch (RemoteException e) {
        }
    }

    public void installPackage(Uri uri, IPackageInstallCallback callback) {
        Log.v(TAG, "installPackage: " + uri);
        new PackageInstallObserverWrapper(callback);
    }

    @Override
    public void dump(PrintWriter pw, MobileManagerService.DumpState dumpState) {
        pw.println("PackageController:");
    }

    @Override
    public void systemReady(Context context) {
    }
}