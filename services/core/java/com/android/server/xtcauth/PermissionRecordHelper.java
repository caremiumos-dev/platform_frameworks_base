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

import android.content.pm.PackageInfo;
import android.os.SystemProperties;
import android.util.Log;

import com.xtc.auth.Permission;
import com.xtc.auth.PermissionRecord;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Maintains the per-user, per-package permission cache used by the permission
 * controller.
 *
 * @hide
 */
class PermissionRecordHelper {
    private static final String TAG = "PermissionRecordHelper";
    private static PermissionRecordHelper mInstance = null;

    private final List<Permission> mParentPermissions = new ArrayList<>();
    private final Map<Integer, List<CacheInfo>> mPkgPermissionCache = new HashMap<>();

    private class CacheInfo {
        private String mPackageName;
        private List<Permission> mPermissions;

        CacheInfo(String packageName, List<Permission> permissions) {
            mPackageName = packageName;
            mPermissions = permissions;
        }

        public String getPackageName() {
            return mPackageName;
        }

        public List<Permission> getPermissions() {
            return mPermissions;
        }

        public void setPermissions(List<Permission> permissions) {
            mPermissions = permissions;
        }
    }

    private PermissionRecordHelper() {
        initStubSubPermission();
    }

    public static PermissionRecordHelper getInstance() {
        if (mInstance == null) {
            mInstance = new PermissionRecordHelper();
        }
        return mInstance;
    }

    private void bindPermission(String subPermission, String parentPermission) {
        bindPermission(subPermission, parentPermission, 2);
    }

    private void bindPermission(String subPermission, String parentPermission, int status) {
        ArrayList<Permission> subs = new ArrayList<>();
        subs.add(createSubPermission(subPermission, status, 2));
        mParentPermissions.add(new Permission(parentPermission, subs, 2, 1));
    }

    private void bindPermissions(String[] subPermissions, String parentPermission) {
        bindPermissions(subPermissions, parentPermission, 0);
    }

    private void bindPermissions(String[] subPermissions, String parentPermission, int status) {
        ArrayList<Permission> subs = new ArrayList<>();
        for (String sub : subPermissions) {
            subs.add(createSubPermission(sub, status, 2));
        }
        mParentPermissions.add(new Permission(parentPermission, subs, 2, 1));
    }

    private void initStubSubPermission() {
        bindPermissions(new String[] {
                "sub-permission.MODIFY_SMS", "sub-permission.MODIFY_MMS",
                "sub-permission.SEND_SMS"}, "android.permission.SEND_SMS");
        bindPermissions(new String[] {
                "sub-permission.SEND_MMS", "sub-permission.SEND_EMAIL"},
                "android.permission.INTERNET");
        bindPermissions(new String[] {
                "sub-permission.QUERY_SMS", "sub-permission.QUERY_MMS"},
                "android.permission.READ_SMS");
        bindPermission("sub-permission.QUERY_CONTACTS", "android.permission.READ_CONTACTS");
        bindPermission("sub-permission.MODIFY_CONTACTS", "android.permission.WRITE_CONTACTS");
        bindPermission("sub-permission.QUERY_CALL_LOG", "android.permission.READ_CALL_LOG");
        bindPermission("sub-permission.MODIFY_CALL_LOG", "android.permission.WRITE_CALL_LOG");
        bindPermission("sub-permission.ACCESS_LOCATION", "android.permission.ACCESS_FINE_LOCATION");
        bindPermission("sub-permission.RECORD_MIC", "android.permission.RECORD_AUDIO");
        bindPermission("sub-permission.OPEN_CAMERA", "android.permission.CAMERA");
        bindPermissions(new String[] {
                "sub-permission.MAKE_CALL", "sub-permission.MAKE_CONFERENCE_CALL"},
                "android.permission.CALL_PHONE");
        bindPermission("sub-permission.READ_PHONE_IMEI", "android.permission.READ_PHONE_STATE");
        bindPermission("sub-permission.CHANGE_NETWORK_STATE_ON",
                "android.permission.MODIFY_PHONE_STATE");
        bindPermission("sub-permission.CHANGE_WIFI_STATE_ON",
                "android.permission.CHANGE_WIFI_STATE");
        bindPermission("sub-permission.CHANGE_BT_STATE_ON", "android.permission.BLUETOOTH_ADMIN");
        if ("1".equals(SystemProperties.get("ro.mtk_hotknot_support"))) {
            bindPermission("sub-permission.ACCESS_HOTKNOT", "android.permission.HOTKNOT");
        }
        if ("1".equals(SystemProperties.get("ro.mtk_op01_rcs"))) {
            bindPermission("sub-permission.RCS_SET_PROFILE_INFO", "com.cmcc.ccs.WRITE_PROFILE");
            bindPermissions(new String[] {
                    "sub-permission.RCS_SET_GROUP_CHAIRMEN",
                    "sub-permission.RCS_MODIFY_GROUP_SUBJECT",
                    "sub-permission.RCS_ACCEPT_GROUP_INVITE",
                    "sub-permission.RCS_REMOVE_PARTICIPANTS",
                    "sub-permission.RCS_MODIFY_SELF_NICKNAME",
                    "sub-permission.RCS_QUIT_CONVERSATION",
                    "sub-permission.RCS_ABORT_CONVERSATION"}, "com.cmcc.ccs.WRITE_GROUP_CHAT");
            bindPermissions(new String[] {
                    "sub-permission.RCS_REPORT_PA", "sub-permission.RCS_FOLLOW_PA",
                    "sub-permission.RCS_UNFOLLOW_PA"}, "com.cmcc.ccs.WRITE_PUBLICACCOUNT");
        }
    }

    private static Permission createPermission(String name, List<Permission> subs, int status,
            int flag) {
        return new Permission(name, subs, status, flag);
    }

    private static Permission createSubPermission(String name, int status, int flag) {
        return createPermission(name, null, status, flag | 2);
    }

    public static Permission findPermissionByName(String name, List<Permission> list) {
        if (name == null || list == null) {
            throw new IllegalArgumentException("permissionName: " + name + "permissionList :" + list);
        }
        for (Permission permission : list) {
            if (permission.mPermissionName.equals(name)) {
                return permission;
            }
            if (permission.mSubPermissions != null) {
                Permission found = findPermissionByName(name, permission.mSubPermissions);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    public static Permission findParentPermissionByName(String name, List<Permission> list) {
        if (name == null || list == null) {
            throw new IllegalArgumentException("subPermissionName: " + name + "permissionList :"
                    + list);
        }
        for (Permission permission : list) {
            if (permission.mSubPermissions != null
                    && findPermissionByName(name, permission.mSubPermissions) != null) {
                return permission;
            }
        }
        return null;
    }

    public static boolean isSubPermission(int flag) {
        return (flag & 2) > 0;
    }

    public static boolean isSystemPermission(int flag) {
        return (flag & 8) > 0;
    }

    public static boolean isUserConfirmPermission(int flag) {
        return (flag & 4) > 0;
    }

    private List<Permission> getPermissionList(PackageInfo packageInfo) {
        if (packageInfo == null) {
            Log.d(TAG, "getPermissionList() failed! null package info");
            return null;
        }
        String[] requestedPermissions = packageInfo.requestedPermissions;
        if (requestedPermissions == null || requestedPermissions.length <= 0) {
            return null;
        }
        ArrayList<Permission> list = new ArrayList<>();
        for (String requested : requestedPermissions) {
            Permission parent = findPermissionByName(requested, mParentPermissions);
            if (parent != null) {
                ArrayList<Permission> subs = new ArrayList<>();
                for (Permission sub : parent.mSubPermissions) {
                    subs.add(new Permission(sub.mPermissionName, null, sub.getStatus(),
                            sub.getFlag()));
                }
                list.add(new Permission(requested, subs, 2, 1));
            }
        }
        return list;
    }

    private List<CacheInfo> preparePermissionCache(List<PackageInfo> packageInfos) {
        ArrayList<CacheInfo> list = new ArrayList<>();
        for (PackageInfo packageInfo : packageInfos) {
            List<Permission> permissions = getPermissionList(packageInfo);
            if (permissions != null) {
                list.add(new CacheInfo(packageInfo.packageName, permissions));
            }
        }
        return list;
    }

    private void copyPermissionSettings(List<Permission> source, List<Permission> target) {
        if (source == null || target == null) {
            Log.e(TAG, "copyPermissionSettings() failed!");
            return;
        }
        for (Permission permission : target) {
            Permission found = findPermissionByName(permission.mPermissionName, source);
            if (found != null) {
                Log.d(TAG, "Copy " + permission.mPermissionName + " with status: "
                        + found.getStatus());
                permission.setStatus(found.getStatus());
            }
            if (permission.mSubPermissions != null && found != null
                    && found.mSubPermissions != null) {
                copyPermissionSettings(found.mSubPermissions, permission.mSubPermissions);
            }
        }
    }

    private void resetPermissionCache(List<Permission> permissions) {
        if (permissions != null) {
            for (Permission permission : permissions) {
                permission.setStatus(0);
                if (permission.mSubPermissions != null) {
                    resetPermissionCache(permission.mSubPermissions);
                }
            }
        }
    }

    public void dump() {
        synchronized (mPkgPermissionCache) {
            Log.d(TAG, "[Dump Permission Cache for all monitored packages]");
            for (Integer userId : mPkgPermissionCache.keySet()) {
                List<CacheInfo> list = mPkgPermissionCache.get(userId);
                if (list != null) {
                    Log.d(TAG, "<User ID>: " + userId);
                    for (CacheInfo cacheInfo : list) {
                        Log.d(TAG, "    PackageName: " + cacheInfo.getPackageName());
                        List<Permission> permissions = cacheInfo.getPermissions();
                        for (Permission permission : permissions) {
                            Log.d(TAG, "     - " + permission.mPermissionName + "("
                                    + PermissionController.status2String(permission.getStatus())
                                    + ", " + permission.getFlag() + ")");
                            if (permission.mSubPermissions != null) {
                                for (Permission sub : permission.mSubPermissions) {
                                    Log.d(TAG, "         - " + sub.mPermissionName + "("
                                            + PermissionController.status2String(sub.getStatus())
                                            + ", " + sub.getFlag() + ")");
                                }
                            }
                        }
                        Log.d(TAG, " - No granted permission");
                    }
                }
            }
        }
    }

    public List<Permission> getPackageCachePermissions(int userId, String packageName) {
        synchronized (mPkgPermissionCache) {
            List<CacheInfo> list = mPkgPermissionCache.get(userId);
            if (list != null) {
                for (CacheInfo cacheInfo : list) {
                    if (cacheInfo.getPackageName().equals(packageName)) {
                        return cacheInfo.getPermissions();
                    }
                }
            }
            return null;
        }
    }

    public Permission getParentPermission(String subPermissionName) {
        return findParentPermissionByName(subPermissionName, mParentPermissions);
    }

    public int getPermissionStatusByPackage(int userId, String packageName, String permissionName) {
        if (packageName == null || permissionName == null) {
            throw new IllegalArgumentException("packageName: " + packageName + "permissionName :"
                    + permissionName);
        }
        Permission permission = searchPermissionCache(userId, packageName, permissionName);
        if (permission != null) {
            return permission.getStatus();
        }
        return 0;
    }

    public PermissionRecord getStrictPermissionForUid(int userId, String permissionName,
            String[] packages) {
        String strictPackage = null;
        int status = 0;
        for (String packageName : packages) {
            Permission permission = searchPermissionCache(userId, packageName, permissionName);
            if (permission == null) {
                continue;
            }
            int permissionStatus = permission.getStatus();
            if (status == 0) {
                if (permissionStatus == 1 || permissionStatus == 2) {
                    Log.d(TAG, "getStrictPermissionForUid() found package: " + packageName);
                    status = permissionStatus;
                    strictPackage = packageName;
                }
            } else if (status == 2) {
                if (permissionStatus == 1) {
                    status = permissionStatus;
                    strictPackage = packageName;
                    Log.d(TAG, "getStrictPermissionForUid() found package: " + packageName);
                }
            }
        }
        if (strictPackage != null) {
            return new PermissionRecord(strictPackage, permissionName, status, 0);
        }
        return null;
    }

    public boolean initPkgPermissionCache(int userId, List<PackageInfo> packageInfos) {
        if (packageInfos == null) {
            Log.d(TAG, "initPkgPermissionCache() failed with null pkgInfo");
            return false;
        }
        synchronized (mPkgPermissionCache) {
            if (mPkgPermissionCache.containsKey(userId)) {
                Log.d(TAG, "Update package data with userId: " + userId);
                removePermissionCacheByUser(userId);
            }
            mPkgPermissionCache.put(userId, preparePermissionCache(packageInfos));
        }
        Log.d(TAG, "initPkgPermissionCache() Done with userId: " + userId);
        return true;
    }

    public void purnPkgPermissionCache(int userId, List<PackageInfo> packageInfos) {
        if (packageInfos == null) {
            Log.d(TAG, "purnPkgPermissionCache() failed with null pkgInfo");
        }
        List<CacheInfo> newCache = preparePermissionCache(packageInfos);
        synchronized (mPkgPermissionCache) {
            if (mPkgPermissionCache.containsKey(userId)) {
                List<CacheInfo> oldCache = mPkgPermissionCache.get(userId);
                for (CacheInfo cacheInfo : newCache) {
                    for (CacheInfo old : oldCache) {
                        if (cacheInfo.getPackageName().equals(old.getPackageName())) {
                            copyPermissionSettings(old.getPermissions(), cacheInfo.getPermissions());
                        }
                    }
                }
                mPkgPermissionCache.remove(userId);
            }
            mPkgPermissionCache.put(userId, newCache);
        }
    }

    public void removePermissionCache(int userId, String packageName) {
        synchronized (mPkgPermissionCache) {
            List<CacheInfo> list = mPkgPermissionCache.get(userId);
            if (list != null) {
                Iterator<CacheInfo> iterator = list.iterator();
                while (iterator.hasNext()) {
                    if (iterator.next().getPackageName().equals(packageName)) {
                        iterator.remove();
                    }
                }
            }
        }
    }

    public void removePermissionCacheByUser(int userId) {
        synchronized (mPkgPermissionCache) {
            mPkgPermissionCache.remove(userId);
        }
    }

    public void resetPermissionCache(int userId) {
        synchronized (mPkgPermissionCache) {
            List<CacheInfo> list = mPkgPermissionCache.get(userId);
            if (list != null) {
                for (CacheInfo cacheInfo : list) {
                    resetPermissionCache(cacheInfo.getPermissions());
                }
            }
        }
    }

    public void resetPermissionCacheByPackage(int userId, String packageName) {
        synchronized (mPkgPermissionCache) {
            List<CacheInfo> list = mPkgPermissionCache.get(userId);
            if (list != null) {
                for (CacheInfo cacheInfo : list) {
                    if (cacheInfo.getPackageName().equals(packageName)) {
                        resetPermissionCache(cacheInfo.getPermissions());
                    }
                }
            }
        }
    }

    public Permission searchPermissionCache(int userId, String packageName, String permissionName) {
        synchronized (mPkgPermissionCache) {
            List<CacheInfo> list = mPkgPermissionCache.get(userId);
            if (list != null) {
                for (CacheInfo cacheInfo : list) {
                    if (cacheInfo.getPackageName().equals(packageName)) {
                        List<Permission> permissions = cacheInfo.getPermissions();
                        if (permissions != null) {
                            return findPermissionByName(permissionName, permissions);
                        }
                        Log.d(TAG, "Not monitored packageName: " + packageName + " permissionName: "
                                + permissionName + " in user: " + userId);
                        return null;
                    }
                }
            }
            return null;
        }
    }

    public void updatePermissionCache(int userId, PackageInfo packageInfo) {
        synchronized (mPkgPermissionCache) {
            List<CacheInfo> list = mPkgPermissionCache.get(userId);
            if (list == null) {
                return;
            }
            boolean found = false;
            Iterator<CacheInfo> iterator = list.iterator();
            while (iterator.hasNext()) {
                CacheInfo cacheInfo = iterator.next();
                if (cacheInfo.getPackageName().equals(packageInfo.packageName)) {
                    List<Permission> permissions = getPermissionList(packageInfo);
                    if (permissions != null) {
                        Log.d(TAG, " permissionList is no null");
                        cacheInfo.setPermissions(permissions);
                    } else {
                        Log.d(TAG, " permissionList is  null");
                        iterator.remove();
                    }
                    found = true;
                    break;
                }
            }
            Log.d(TAG, " bFound =" + found);
            if (!found) {
                List<Permission> permissions = getPermissionList(packageInfo);
                if (permissions != null) {
                    Log.d(TAG, " cache do ");
                    list.add(new CacheInfo(packageInfo.packageName, permissions));
                }
            }
        }
    }
}