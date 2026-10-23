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

import android.content.Context;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.RemoteException;
import android.util.Log;

import java.util.List;

/**
 * Client-side wrapper of the XTC mobile manager service, exposing the
 * {@link IMobileManager} API.
 *
 * @hide
 */
public class MobileManager implements IMobileManager {
    private static final String TAG = "MobileManager";

    private final Context mContext;
    private final IMobileManagerService mService;
    MobileManager mInstance = null;

    private MobileManager() {
        mContext = null;
        mService = null;
    }

    public MobileManager(Context context, IMobileManagerService service) {
        mContext = context;
        mService = service;
        if (mService == null) {
            throw new RuntimeException("null MobileManagerService!");
        }
    }

    private boolean checkLicense(String license) {
        return true;
    }

    @Override
    public boolean attach(IMobileConnectionCallback callback) {
        try {
            return mService.attach(callback);
        } catch (RemoteException e) {
            Log.e(TAG, "attach() failed: ", e);
            return false;
        }
    }

    @Override
    public void detach() {
        try {
            mService.detach();
        } catch (RemoteException e) {
            Log.e(TAG, "detach() failed: ", e);
        }
    }

    @Override
    public String getVersionName() {
        try {
            return mService.getVersionName();
        } catch (RemoteException e) {
            Log.e(TAG, "getVersionName() failed: ", e);
            return null;
        }
    }

    @Override
    public void enablePermissionController(boolean enable) {
        try {
            mService.enablePermissionController(enable);
        } catch (RemoteException e) {
            Log.e(TAG, "enablePermissionController() " + enable + " failed: ", e);
        }
    }

    @Override
    public void enableInterceptionController(boolean enable) {
        try {
            mService.enableInterceptionController(enable);
        } catch (RemoteException e) {
            Log.e(TAG, "enableInterceptionController() " + enable + " failed: ", e);
        }
    }

    @Override
    public void setPermissionRecord(PermissionRecord record) {
        try {
            mService.setPermissionRecord(record);
        } catch (RemoteException e) {
            Log.e(TAG, "setPermissionRecord() " + record + " failed: ", e);
        }
    }

    @Override
    public void setPermissionRecords(List<PermissionRecord> records) {
        try {
            mService.setPermissionRecords(records);
        } catch (RemoteException e) {
            Log.e(TAG, "setPermissionRecord() " + records + " failed: ", e);
        }
    }

    @Override
    public void setPermissionCache(List<PermissionRecord> records) {
        try {
            mService.setPermissionCache(records);
        } catch (RemoteException e) {
            Log.e(TAG, "setPermissionCache() " + records + " failed: ", e);
        }
    }

    @Override
    public List<Permission> getPackageGrantedPermissions(String packageName) {
        try {
            return mService.getPackageGrantedPermissions(packageName);
        } catch (RemoteException e) {
            Log.e(TAG, "getPackageGrantedPermissions() " + packageName + " failed: ", e);
            return null;
        }
    }

    @Override
    public void clearPackageSettings(String packageName) {
        try {
            mService.clearPackageSettings(packageName);
        } catch (RemoteException e) {
            Log.e(TAG, "clearPackageSettings() " + packageName + " failed: ", e);
        }
    }

    @Override
    public void clearAllSettings() {
        try {
            mService.clearAllSettings();
        } catch (RemoteException e) {
            Log.e(TAG, "clearAllSettings() failed: ", e);
        }
    }

    @Override
    public void setBootReceiverEnabledSetting(String packageName, boolean enable) {
        try {
            mService.setBootReceiverEnabledSetting(packageName, enable);
        } catch (RemoteException e) {
            Log.e(TAG, "setBootReceiverEnabledSetting() " + packageName + " enable: "
                    + enable + " failed: ", e);
        }
    }

    @Override
    public void setBootReceiverEnabledSettings(List<ReceiverRecord> records) {
        try {
            mService.setBootReceiverEnabledSettings(records);
        } catch (RemoteException e) {
            Log.e(TAG, "setBootReceiverEnabledSettings()", e);
        }
    }

    @Override
    public boolean getBootReceiverEnabledSetting(String packageName) {
        try {
            return mService.getBootReceiverEnabledSetting(packageName);
        } catch (RemoteException e) {
            Log.e(TAG, "getBootReceiverEnabledSetting() " + packageName + " failed: ", e);
            return false;
        }
    }

    @Override
    public List<ReceiverRecord> getBootReceiverList() {
        try {
            return mService.getBootReceiverList();
        } catch (RemoteException e) {
            Log.e(TAG, "getBootReceiverList()", e);
            return null;
        }
    }

    @Override
    public void setNotificationEnabledSetting(String packageName, boolean enable) {
        try {
            mService.setNotificationEnabledSetting(packageName, enable);
        } catch (RemoteException e) {
            Log.e(TAG, "setNotificationEnabledSetting() " + packageName + " enable: "
                    + enable + " failed: ", e);
        }
    }

    @Override
    public boolean getNotificationEnabledSetting(String packageName) {
        try {
            return mService.getNotificationEnabledSetting(packageName);
        } catch (RemoteException e) {
            Log.e(TAG, "getNotificationEnabledSetting() " + packageName + " failed: ", e);
            return false;
        }
    }

    @Override
    public void setNotificationCache(List<NotificationCacheRecord> records) {
        try {
            mService.setNotificationCache(records);
        } catch (RemoteException e) {
            Log.e(TAG, "setNotificationCache()", e);
        }
    }

    @Override
    public void setFirewallPolicy(int uid, int policy, boolean enable) {
        try {
            mService.setFirewallPolicy(uid, policy, enable);
        } catch (RemoteException e) {
            Log.e(TAG, "setFirewallPolicy() " + uid + " networkType: " + policy
                    + " enable: " + enable + " failed: ", e);
        }
    }

    @Override
    public void registerPermissionListener(IPermissionListener listener) {
        try {
            mService.registerManagerApListener(CONTROLLER_PERMISSION, listener.asBinder());
        } catch (RemoteException e) {
            Log.e(TAG, "registerPermissionListener() " + listener + " failed: ", e);
        }
    }

    @Override
    public void registerNotificationListener(INotificationListener listener) {
        try {
            mService.registerManagerApListener(CONTROLLER_NOTIFICATION, listener.asBinder());
        } catch (RemoteException e) {
            Log.e(TAG, "registerNotificationListener() " + listener + " failed: ", e);
        }
    }

    @Override
    public void registerCallInterceptionListener(ICallInterceptionListener listener) {
        try {
            mService.registerManagerApListener(CONTROLLER_CALL, listener.asBinder());
            mService.registerManagerApListener(CONTROLLER_INTERCEPTION, listener.asBinder());
        } catch (RemoteException e) {
            Log.e(TAG, "registerCallInterceptionListener() " + listener + " failed: ", e);
        }
    }

    @Override
    public void registerMessageInterceptListener(IMessageInterceptListener listener) {
        try {
            mService.registerManagerApListener(CONTROLLER_MESSAGE_INTERCEPT,
                    listener.asBinder());
        } catch (RemoteException e) {
            Log.e(TAG, "registerMessageInterceptListener() " + listener + " failed: ", e);
        }
    }

    @Override
    public void forceStopPackage(String packageName) {
        try {
            mService.forceStopPackage(packageName);
        } catch (RemoteException e) {
            Log.e(TAG, "forceStopPackage() " + packageName + " failed: ", e);
        }
    }

    @Override
    public void deletePackage(String packageName) {
        try {
            mService.deletePackage(packageName);
        } catch (RemoteException e) {
            Log.e(TAG, "deletePackage() " + packageName + " failed: ", e);
        }
    }

    @Override
    public void installPackage(Uri packageUri, IPackageInstallCallback callback) {
        try {
            mService.installPackage(packageUri, callback);
        } catch (RemoteException e) {
            Log.e(TAG, "installPackage() " + packageUri + " failed: ", e);
        }
    }

    @Override
    public List<PackageInfo> getInstalledPackages() {
        try {
            return mService.getInstalledPackages();
        } catch (RemoteException e) {
            Log.e(TAG, "getInstalledPackages() failed: ", e);
            return null;
        }
    }

    @Override
    public void cancelNotification(String packageName) {
        try {
            mService.cancelNotification(packageName);
        } catch (RemoteException e) {
            Log.e(TAG, "cancelNotification() " + packageName + " failed: ", e);
        }
    }
}