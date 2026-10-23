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
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.UserHandle;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Hard-coded registry of applications that are allowed to act as "mobile
 * managers" (parental-control / device-management clients).
 *
 * <p>WARNING: This is a closed, vendor-controlled trust model. The system server
 * decides which applications may control other applications' permissions,
 * notifications, calls and SMS based on a baked-in package/signature list, and
 * the platform manager ("com.xtc.setting") is trusted unconditionally. This
 * concentrates sweeping control over third-party applications in the vendor and
 * its partners and cannot be reviewed or overridden by the user. It is recorded
 * here for completeness of the device restore, not endorsed.
 *
 * @hide
 */
class ManagerIdentityHelper {
    public static final String TAG = "ManagerIdentityHelper";

    public static final int IDENTITY_NONE = 0;
    public static final int IDENTITY_TCMANAGER = 100;
    public static final int IDENTITY_MOBILE_MANAGER_START = 100;
    public static final int IDENTITY_PERMISSION_CONTROL_START = 1000;
    public static final int IDENTITY_DEBUG = 1000;
    public static final int IDENTITY_IN_HOUSE = 1001;
    public static final int IDENTITY_TCUSER = 1002;

    private static final int INVALID_APPID = -1;

    private static String TAG_PLATFORM = "platform";
    private static String TAG_TENCENT = "mobilemanager";

    private static ManagerIdentityHelper mInstance = null;
    private static List<ManagerIdentity> mManagerList = new ArrayList<>();

    private static final Signature TencentSignature = new Signature(
            "30820239308201a2a00302010202044c96f48f300d06092a864886f70d01010505003060310b30"
            + "0906035504061302434e310b300906035504081302474431123010060355040713094775616e"
            + "677a686f753110300e060355040a130754656e63656e74310b3009060355040b130233473111"
            + "300f0603550403130857696c736f6e57753020170d3130303932303035343334335a180f3230"
            + "3635303632333035343334335a3060310b300906035504061302434e310b3009060355040813"
            + "02474431123010060355040713094775616e677a686f753110300e060355040a130754656e63"
            + "656e74310b3009060355040b130233473111300f0603550403130857696c736f6e577530819f"
            + "300d06092a864886f70d010101050003818d0030818902818100b56e79dbb1185a79e52d792b"
            + "b3d0bb3da8010d9b87da92ec69f7dc5ad66ab6bfdff2a6a1ed285dd2358f28b72a468be7c10a"
            + "2ce30c4c27323ed4edcc936080e5bedc2cbbca0b7e879c08a631182793f44bb3ea284179b263"
            + "410c298e5f6831032c9702ba4a74e2ccfc9ef857f12201451602fc8e774ac59d6398511586c8"
            + "3d1d0203010001300d06092a864886f70d0101050500038181002475615bb65b8d8786b89053"
            + "5802948840387d06b1692ff3ea47ef4c435719ba1865b81e6bfa6293ce31747c3cd6b34595b4"
            + "85cc1563fd90107ba5845c28b95c79138f0dec288940395bc10f92f2b69d8dc410999deb3890"
            + "0974ce9984b678030edfba8816582f56160d87e38641288d8588d2a31e20b89f223d788dd35c"
            + "c9c8");

    static class ManagerIdentity {
        private static final int VERIFYCOUNT_RESET_TIMES = 10;

        private int mIdentity;
        private String mTag;
        private String mPackageName;
        private String mInitServiceName;
        private Signature mSignature;
        private boolean mVerified;
        private boolean mDefVerified;
        private int mAppId = INVALID_APPID;
        private boolean mInstalled = false;
        private int mVerifiyCount = 0;

        public ManagerIdentity(int identity, String tag, String packageName, String initServiceName,
                Signature signature, boolean defVerified) {
            mIdentity = identity;
            mTag = tag;
            mPackageName = packageName;
            mInitServiceName = initServiceName;
            mSignature = signature;
            mVerified = defVerified;
            mDefVerified = defVerified;
        }

        public int getIdentity() {
            return mIdentity;
        }

        public String getTag() {
            return mTag;
        }

        public String getName() {
            return mPackageName;
        }

        public String getInitSrvName() {
            return mInitServiceName;
        }

        public Signature getSignature() {
            return mSignature;
        }

        public int getAppId() {
            return mAppId;
        }

        public void setAppId(int appId) {
            mAppId = appId;
        }

        public boolean isInstalled() {
            return mInstalled;
        }

        public void setInstalled(boolean installed) {
            mInstalled = installed;
        }

        public synchronized boolean isVerified() {
            noteVerified();
            return mVerified;
        }

        public synchronized void verified() {
            mVerified = true;
        }

        public synchronized boolean resetVerified() {
            mVerifiyCount = 0;
            mVerified = mDefVerified;
            return mVerified;
        }

        private synchronized void noteVerified() {
            mVerifiyCount++;
            if (mVerifiyCount > VERIFYCOUNT_RESET_TIMES) {
                resetVerified();
            }
        }
    }

    static {
        mManagerList.add(new ManagerIdentity(IDENTITY_TCMANAGER, TAG_TENCENT,
                "com.tencent.qqpimsecure", null, TencentSignature, false));
        mManagerList.add(new ManagerIdentity(IDENTITY_TCUSER, TAG_TENCENT, "com.tencent.tcuser",
                "com.tencent.tcuser.ztool.service.PermissionService", TencentSignature, false));
        mManagerList.add(new ManagerIdentity(IDENTITY_IN_HOUSE, TAG_PLATFORM, "com.xtc.setting",
                "com.xtc.setting.module.authoritymanage.serve.CtaAuthPermissionService", null,
                true));
        mManagerList.add(new ManagerIdentity(IDENTITY_DEBUG, TAG_PLATFORM,
                "com.mediatek.mom.test.app.mgremu", null, null, true));
    }

    ManagerIdentityHelper() {
    }

    public static ManagerIdentityHelper getInstance() {
        if (mInstance == null) {
            mInstance = new ManagerIdentityHelper();
        }
        return mInstance;
    }

    public static void init(Context context) {
        PackageManager pm = context.getPackageManager();
        for (ManagerIdentity identity : mManagerList) {
            try {
                ApplicationInfo info = pm.getApplicationInfo(identity.getName(), 8704);
                if (info != null) {
                    identity.setInstalled(true);
                    identity.setAppId(UserHandle.getAppId(info.uid));
                    Log.d(TAG, "Manager identity " + identity.getIdentity()
                            + " installed with appId: " + identity.getAppId());
                }
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "Can't find manager package: " + identity.getName());
            }
        }
    }

    public static void initManagerIdentity(int identity, int uid) {
        ManagerIdentity manager = getManagerIdentityObject(identity);
        if (manager != null) {
            manager.setInstalled(true);
            manager.setAppId(UserHandle.getAppId(uid));
            manager.resetVerified();
        }
    }

    public static void uninitManagerIdentity(int identity) {
        ManagerIdentity manager = getManagerIdentityObject(identity);
        if (manager != null) {
            manager.setInstalled(false);
            manager.setAppId(INVALID_APPID);
        }
    }

    public static boolean checkSignature(int identity, Signature signature) {
        for (ManagerIdentity manager : mManagerList) {
            if (identity == manager.getIdentity()
                    && (TAG_PLATFORM.equals(manager.getTag())
                            || manager.getSignature() == null
                            || manager.getSignature().equals(signature))) {
                return true;
            }
        }
        Log.e(TAG, "check C with " + identity + " failed");
        return false;
    }

    private static boolean findIdentity(int identity) {
        Iterator<ManagerIdentity> iterator = mManagerList.iterator();
        while (iterator.hasNext()) {
            if (identity == iterator.next().getIdentity()) {
                return true;
            }
        }
        return false;
    }

    public static int getAuthorizedIdentity(int uid, String seInfo, String packageName) {
        int appId = UserHandle.getAppId(uid);
        for (ManagerIdentity manager : mManagerList) {
            if (!packageName.equals(manager.getName())) {
                continue;
            }
            if (TAG_PLATFORM.equals(manager.getTag()) && appId != 1000) {
                continue;
            }
            return manager.getIdentity();
        }
        return IDENTITY_NONE;
    }

    public static List<Integer> getIdentityList() {
        return getIdentityListInner(false);
    }

    public static List<Integer> getPermControlIdentityList() {
        return getIdentityListInner(true);
    }

    private static List<Integer> getIdentityListInner(boolean permControlOnly) {
        ArrayList<Integer> list = new ArrayList<>();
        for (ManagerIdentity manager : mManagerList) {
            if (!permControlOnly) {
                list.add(manager.getIdentity());
            } else if (manager.getIdentity() >= IDENTITY_PERMISSION_CONTROL_START) {
                list.add(manager.getIdentity());
            }
        }
        Collections.sort(list, Collections.reverseOrder());
        return list;
    }

    public static int getManagerAppId(int identity) {
        for (ManagerIdentity manager : mManagerList) {
            if (identity == manager.getIdentity()) {
                return manager.getAppId();
            }
        }
        return INVALID_APPID;
    }

    public static int getManagerIdentity(int uid) {
        for (ManagerIdentity manager : mManagerList) {
            if (uid >= 0 && UserHandle.getAppId(uid) == manager.getAppId()) {
                return manager.getIdentity();
            }
        }
        return IDENTITY_NONE;
    }

    public static int getManagerIdentity(String packageName) {
        for (ManagerIdentity manager : mManagerList) {
            if (packageName != null && packageName.equals(manager.getName())) {
                return manager.getIdentity();
            }
        }
        return IDENTITY_NONE;
    }

    public static boolean getManagerIdentityInstalled(int identity) {
        ManagerIdentity manager = getManagerIdentityObject(identity);
        return manager != null && manager.isInstalled();
    }

    public static ManagerIdentity getManagerIdentityObject(int identity) {
        for (ManagerIdentity manager : mManagerList) {
            if (identity == manager.getIdentity()) {
                return manager;
            }
        }
        return null;
    }

    public static String getManagerInitService(int identity) {
        for (ManagerIdentity manager : mManagerList) {
            if (identity == manager.getIdentity()) {
                return manager.getInitSrvName();
            }
        }
        return null;
    }

    public static String getManagerPackage(int identity) {
        for (ManagerIdentity manager : mManagerList) {
            if (identity == manager.getIdentity()) {
                return manager.getName();
            }
        }
        return null;
    }

    public static boolean isBootControlIdetity(int identity) {
        if ((identity >= IDENTITY_MOBILE_MANAGER_START && identity < 1000)
                || identity == IDENTITY_IN_HOUSE || identity == IDENTITY_DEBUG) {
            return findIdentity(identity);
        }
        return false;
    }

    public static boolean isMobileManagerIdetity(int identity) {
        if (identity < IDENTITY_MOBILE_MANAGER_START || identity >= 1000) {
            return false;
        }
        return findIdentity(identity);
    }

    public static boolean isPermControlIdentity(int identity) {
        if (identity < IDENTITY_PERMISSION_CONTROL_START) {
            return false;
        }
        return findIdentity(identity);
    }

    public static boolean isValidIdentity(int identity) {
        return identity != IDENTITY_NONE;
    }

    public static boolean needVerifyProtocol(int identity) {
        for (ManagerIdentity manager : mManagerList) {
            if (identity == manager.getIdentity()
                    && !TAG_PLATFORM.equals(manager.getTag())) {
                return true;
            }
        }
        return false;
    }
}