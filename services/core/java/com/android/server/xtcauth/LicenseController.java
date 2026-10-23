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
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Log;

import java.util.Arrays;

/**
 * Validates that a management application is licensed by checking the signature
 * and a handshake permission of its package.
 *
 * @hide
 */
class LicenseController extends ControllerBase {
    public static final String TAG = "LicenseController";
    public static final boolean DEBUG = true;

    private static ControllerBase mInstance = null;
    private static IPackageManager mPm = null;

    private LicenseController() {
        mPm = getPackageManagerService();
    }

    public static final ControllerBase getInstance() {
        if (mInstance == null) {
            mInstance = new LicenseController();
        }
        return mInstance;
    }

    public static int checkManagerIdentity(int uid) {
        try {
            String[] packages = mPm.getPackagesForUid(uid);
            if (packages == null) {
                Log.e(TAG, "No packages are found with uid: " + uid);
                return 0;
            }
            int identity = 0;
            for (String packageName : packages) {
                identity = checkManagerIdentityInner(packageName);
                if (identity != 0) {
                    break;
                }
            }
            return identity;
        } catch (RemoteException e) {
            Log.e(TAG, "getPackagesForUid() failed! ", e);
            return 0;
        }
    }

    public static int checkManagerIdentity(String packageName) {
        if (packageName == null) {
            throw new IllegalArgumentException("null packageName");
        }
        Log.d(TAG, "checkManagerIdentity(" + packageName + ")");
        return checkManagerIdentityInner(packageName);
    }

    private static int checkManagerIdentityInner(String packageName) {
        PackageInfo packageInfo = PackageController.getPackageInfo(packageName);
        if (packageInfo == null || packageInfo.applicationInfo == null) {
            return 0;
        }
        return ManagerIdentityHelper.getAuthorizedIdentity(packageInfo.applicationInfo.uid,
                packageInfo.applicationInfo.seInfo, packageName);
    }

    private boolean checkCertificate(PackageInfo packageInfo, int identity) {
        if (packageInfo == null || packageInfo.signatures == null) {
            Log.e(TAG, "Package without C! ");
            return false;
        }
        if (ManagerIdentityHelper.checkSignature(identity, packageInfo.signatures[0])) {
            return true;
        }
        Log.e(TAG, "Invalid C! ");
        return false;
    }

    private boolean checkHandshake(PackageInfo packageInfo) {
        if (packageInfo == null || packageInfo.requestedPermissions == null) {
            Log.e(TAG, "Package without P! ");
            return false;
        }
        if (Arrays.asList(packageInfo.requestedPermissions)
                .indexOf("android.permission.REBOOT_IPO") != -1) {
            return true;
        }
        Log.e(TAG, "Invalid P! ");
        return false;
    }

    public boolean checkProtocol(int uid, int identity) {
        if (identity == 0) {
            Log.e(TAG, "Not an authorized identity");
            return false;
        }
        if (!ManagerIdentityHelper.needVerifyProtocol(identity)) {
            return true;
        }
        ManagerIdentityHelper.ManagerIdentity manager =
                ManagerIdentityHelper.getManagerIdentityObject(identity);
        if (manager == null) {
            Log.e(TAG, "The identity " + identity + " with uid " + uid + " doesn't exist!");
            return false;
        }
        if (manager.isVerified()) {
            return true;
        }
        try {
            int userId = UserHandle.getUserId(uid);
            String[] packages = mPm.getPackagesForUid(uid);
            if (packages == null) {
                Log.e(TAG, "getPackagesForUid() with null packages! ");
                return false;
            }
            for (String packageName : packages) {
                PackageInfo packageInfo = mPm.getPackageInfo(packageName, 4160, userId);
                if (!checkCertificate(packageInfo, identity) || !checkHandshake(packageInfo)) {
                    return false;
                }
            }
            Log.d(TAG, "checkProtocol(" + identity + ") passed!");
            manager.verified();
            return true;
        } catch (RemoteException e) {
            Log.e(TAG, "get PackagesInfo failed! ", e);
            return false;
        }
    }

    @Override
    public void systemReady(Context context) {
    }
}