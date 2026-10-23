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

import android.app.INotificationManager;
import android.content.Context;
import android.os.Binder;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.util.Log;
import android.util.Slog;

import com.xtc.auth.NotificationCacheRecord;

import java.util.HashSet;
import java.util.List;

/**
 * Controller that can block and cancel notifications of selected packages on
 * behalf of the management application.
 *
 * @hide
 */
class NotificationController extends ControllerBase implements IPackageBasedController {
    public static final String NAME = "NotificationController";
    private static final String TAG = "MoMS.NotificationController";
    private static final boolean DBG = true;

    private static ControllerBase mInstance = null;

    private INotificationManager mService;
    private final HashSet<String> mBlockedPackages = new HashSet<>();
    private boolean mSystemReady = false;

    NotificationController() {
    }

    public static final ControllerBase getInstance() {
        if (mInstance == null) {
            mInstance = new NotificationController();
            System.out.printf("Create MobileManagerController: NotificationController",
                    new Object[0]);
        }
        return mInstance;
    }

    private INotificationManager getService() {
        if (mService != null) {
            return mService;
        }
        mService = INotificationManager.Stub.asInterface(
                ServiceManager.getService(Context.NOTIFICATION_SERVICE));
        return mService;
    }

    private void clearNotificationSettings() {
        mBlockedPackages.clear();
    }

    public void cancelNotification(String packageName) {
        long token = Binder.clearCallingIdentity();
        try {
            INotificationManager service = getService();
            if (service != null) {
                Slog.v(TAG, "cancelNotification in NMS");
                service.cancelAllNotifications(packageName, UserHandle.myUserId());
            }
        } catch (android.os.RemoteException e) {
            Log.e(NAME, "cancelNotification() faild!");
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public boolean getNotificationEnabledSetting(String packageName) {
        boolean enabled = !mBlockedPackages.contains(packageName);
        Slog.v(TAG, "notifications are " + (enabled ? "en" : "dis") + "abled for " + packageName);
        return enabled;
    }

    public void setNotificationEnabledSetting(String packageName, boolean enabled) {
        Slog.v(TAG, (enabled ? "en" : "dis") + "abling notifications for " + packageName);
        if (enabled) {
            mBlockedPackages.remove(packageName);
        } else {
            mBlockedPackages.add(packageName);
        }
    }

    public void setNotificationCache(List<NotificationCacheRecord> records) {
        clearNotificationSettings();
        if (records == null) {
            Slog.v(TAG, "setNotificationCache to null");
            return;
        }
        for (NotificationCacheRecord record : records) {
            if (record != null) {
                setNotificationEnabledSetting(record.packageName, record.enable);
            }
        }
    }

    @Override
    public boolean clearAllSettings() {
        clearNotificationSettings();
        return false;
    }

    @Override
    public boolean clearPackageSettings(String packageName) {
        setNotificationEnabledSetting(packageName, true);
        return false;
    }

    @Override
    public void onPackageStatusChange(int uid, String packageName, int status) {
        Log.d(TAG, "onPackageStatusChange() packageName:" + packageName + " status: " + status);
        if (status == 1) {
            setNotificationEnabledSetting(packageName, true);
        }
    }

    @Override
    public void onUserStatusChange(int userId, int status) {
    }

    @Override
    public void systemReady(Context context) {
        mSystemReady = true;
        if (getService() == null) {
            throw new RuntimeException("null INotificationManager");
        }
    }
}