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
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Binder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.MediaStore;
import android.telephony.PhoneNumberUtils;
import android.util.Log;

/**
 * Static helpers for CTA (China Type Approval) permission enforcement backed by
 * the XTC mobile manager service.
 *
 * @hide
 */
public class MobileManagerUtils {
    public static final String TAG = "MobileManager";

    private static final boolean FEATURE_SUPPORT =
            SystemProperties.get("ro.xtc_mobile_management").equals("1");
    private static final boolean FEATURE_CTA_VERSION =
            SystemProperties.get("ro.xtc.ctaversion").equals("1");
    private static IMobileManagerService sMomInstance = null;

    private static final String[] CAMERA_INTENT_LIST = {
            MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA,
            MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE,
            MediaStore.ACTION_IMAGE_CAPTURE_SECURE,
            MediaStore.ACTION_IMAGE_CAPTURE,
            MediaStore.INTENT_ACTION_VIDEO_CAMERA
    };

    public static boolean isSupported() {
        return FEATURE_SUPPORT;
    }

    public static boolean isCtaVersion() {
        return FEATURE_CTA_VERSION;
    }

    public static boolean checkPermission(String permission, int callingUid) {
        return checkPermission(permission, callingUid, null);
    }

    public static boolean checkPermission(String permission, int callingUid, Bundle extra) {
        if (!FEATURE_CTA_VERSION) {
            return true;
        }
        try {
            return getServiceInstance().checkPermissionWithData(permission, callingUid, extra) == 0;
        } catch (RemoteException e) {
            Log.e(TAG, "checkPermission failed!", e);
            return true;
        }
    }

    public static void checkPermissionAsync(String permission, int callingUid,
            IRequestedPermissionCallback callback) {
        checkPermissionAsync(permission, callingUid, callback, null);
    }

    public static void checkPermissionAsync(String permission, int callingUid,
            IRequestedPermissionCallback callback, Bundle extra) {
        if (FEATURE_CTA_VERSION) {
            try {
                getServiceInstance().checkPermissionAsyncWithData(permission, callingUid, extra,
                        callback);
            } catch (RemoteException e) {
                Log.e(TAG, "checkPermissionAsync failed!", e);
            }
        }
    }

    public static boolean checkIntentPermission(Intent intent, ActivityInfo activityInfo,
            Context context, int callingUid) {
        String permission;
        if (!FEATURE_CTA_VERSION) {
            return true;
        }
        Bundle extra = new Bundle();
        return activityInfo == null
                || (permission = decidePermissionAndData(activityInfo, intent, context, extra))
                        == null
                || checkPermission(permission, callingUid, extra);
    }

    public static long getUserConfirmTime(int uid, long timeout) {
        if (!FEATURE_CTA_VERSION) {
            return 0L;
        }
        try {
            return getServiceInstance().getUserConfirmTime(uid, timeout);
        } catch (RemoteException e) {
            Log.e(TAG, "getUserConfirmTime() failed!", e);
            return 0L;
        }
    }

    public static void killAppByPackageName(String packageName) {
        if (FEATURE_CTA_VERSION) {
            Log.d(TAG, "killAppByPackageName name=" + packageName);
            try {
                getServiceInstance().forceStopPackage(packageName);
            } catch (RemoteException e) {
                Log.e(TAG, "killAppByPackageName failed!", e);
            }
        }
    }

    public static void killCallingPid(String reason) {
        int callingPid = Binder.getCallingPid();
        if (FEATURE_CTA_VERSION) {
            try {
                getServiceInstance().killPid(callingPid, reason);
            } catch (RemoteException e) {
                Log.e(TAG, "killCallingPid failed!", e);
            }
        }
    }

    private static String decidePermissionAndData(ActivityInfo activityInfo, Intent intent,
            Context context, Bundle extra) {
        Log.d(TAG, "decidePermissionAndData(intentPermission = " + activityInfo.permission + ")");
        if (android.Manifest.permission.CALL_PHONE.equals(activityInfo.permission)) {
            if (intent == null) {
                return SubPermissions.MAKE_CALL;
            }
            extra.putString(IMobileManager.PARAMETER_PHONENUMBER,
                    PhoneNumberUtils.getNumberFromIntent(intent, context));
            return SubPermissions.MAKE_CALL;
        }
        if (!isCameraIntent(intent)) {
            return null;
        }
        Log.d(TAG, "decidePermissionAndData() - " + intent.getAction()
                + ", this is one kind of camera intent.");
        return SubPermissions.OPEN_CAMERA;
    }

    private static boolean isCameraIntent(Intent intent) {
        if (intent != null && intent.getAction() != null) {
            for (int i = 0; i < CAMERA_INTENT_LIST.length; i++) {
                if (intent.getAction().equals(CAMERA_INTENT_LIST[i])) {
                    return true;
                }
            }
        }
        return false;
    }

    private static IMobileManagerService getServiceInstance() {
        if (sMomInstance == null) {
            sMomInstance = IMobileManagerService.Stub.asInterface(
                    ServiceManager.getService(Context.XTC_AUTH_SERVICE));
        }
        return sMomInstance;
    }
}